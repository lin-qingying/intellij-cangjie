/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.projectStructure

import cn.cangnova.cangjie.analyzer.LibraryInfo
import cn.cangnova.cangjie.ide.cache.project.LibraryInfoCache
import cn.cangnova.cangjie.ide.cache.project.getIdeaModelInfosCache
import cn.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus.checkCanceled
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap

/**
 * For each [LibraryInfo], [LibraryUsageIndex] contains all [Module]s which depend on that library info. The index supports deduplicated
 * libraries: if a [LibraryInfo] comprises two [Library] instances with the same roots, [getDependentModules] will return the union of both
 * [Library]'s dependents.
 *
 * The resulting dependents are stable and do not depend on the state of [LibraryInfoCache], as they are computed from the project model.
 */
interface LibraryUsageIndex {
    fun getDependentModules(libraryInfo: LibraryInfo): Sequence<Module>
    fun hasDependentModule(libraryInfo: LibraryInfo, module: Module): Boolean
}

class LibraryUsageIndexImpl(private val project: Project) : LibraryUsageIndex {
    private val moduleDependentsByLibrary: CachedValue<MultiMap<Library, Module>> =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result(
                computeLibraryModuleDependents(),
//                ModuleModificationTracker.getInstance(project),
//                JavaLibraryModificationTracker.getInstance(project),
            )
        }

    private fun computeLibraryModuleDependents(): MultiMap<Library, Module> = runReadAction {
        val moduleDependentsByLibrary = MultiMap.createSet<Library, Module>()
        val libraryCache = LibraryInfoCache.getInstance(project)
        for (module in ModuleManager.getInstance(project).modules) {
            checkCanceled()
            for (entry in ModuleRootManager.getInstance(module).orderEntries) {
                if (entry !is LibraryOrderEntry) continue
                val library = entry.library ?: continue
                val keyLibrary = libraryCache.deduplicatedLibrary(library)
                moduleDependentsByLibrary.putValue(keyLibrary, module)
            }
        }

        moduleDependentsByLibrary
    }

    override fun getDependentModules(libraryInfo: LibraryInfo): Sequence<Module> = sequence<Module> {
        val ideaModelInfosCache = getIdeaModelInfosCache(project)
        for (module in moduleDependentsByLibrary.value[libraryInfo.library]) {
            val mappedModuleInfos = ideaModelInfosCache.getModuleInfosForModule(module)
            if (mappedModuleInfos.any { true }) {
                yield(module)
            }
        }
    }

    override fun hasDependentModule(libraryInfo: LibraryInfo, module: Module): Boolean {
        return module in moduleDependentsByLibrary.value[libraryInfo.library]

    }
}
