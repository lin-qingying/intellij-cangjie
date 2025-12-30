/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.moduleinfo

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap
import org.cangnova.cangjie.moduleinfo.cache.LibraryInfoCache
import org.cangnova.cangjie.moduleinfo.util.ModuleModificationTracker

/**
 * 库使用索引实现
 *
 * 用于查找项目中依赖于特定库的所有模块，使用缓存机制提高性能。
 */
class LibraryUsageIndexImpl(private val project: Project) : LibraryUsageIndex {

    private val moduleDependentsByLibrary =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result(
                computeLibraryModuleDependents(),
                ModuleModificationTracker.getInstance(project)
            )
        }

    /**
     * 获取依赖于指定库的所有模块
     *
     * @param libraryInfo 库信息
     * @return 依赖该库的模块序列
     */
    override fun getDependentModules(libraryInfo: LibraryInfo): Sequence<Module> {
        return moduleDependentsByLibrary.value[libraryInfo.library].asSequence()
    }

    /**
     * 计算每个库被哪些模块依赖
     *
     * @return 库到模块的多重映射
     */
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
}