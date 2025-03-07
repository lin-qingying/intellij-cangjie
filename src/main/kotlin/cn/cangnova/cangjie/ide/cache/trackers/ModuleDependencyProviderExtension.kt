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

package cn.cangnova.cangjie.ide.cache.trackers

import cn.cangnova.cangjie.analyzer.ModuleInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.util.Processor

interface ModuleDependencyProviderExtension {
    fun processAdditionalDependencyModules(module: Module, processor: Processor<Module>)

    companion object {
        val Default = object : ModuleDependencyProviderExtension {
            override fun processAdditionalDependencyModules(module: Module, processor: Processor<Module>) {
            }
        }

        fun getInstance(project: Project): ModuleDependencyProviderExtension = project.service()
    }
}


class ResolutionAnchorModuleDependencyProviderExtension(private val project: Project) :
    ModuleDependencyProviderExtension {
    /**
     * Consider modules M1, M2, M3, library L1 resolving via Resolution anchor M2, other libraries L2, L3 with the following dependencies:
     * M2 depends on M1
     * L1 depends on anchor M2
     * L2 depends on L1
     * L3 depends on L2
     * M3 depends on L3
     * Then modification of M1 should lead to complete invalidation of all modules and libraries in this example.
     *
     * Updates for libraries aren't managed here, corresponding ModificationTracker is responsible for that.
     * This extension provides missing dependencies from source-dependent library dependencies only to source modules.
     */
    override fun processAdditionalDependencyModules(module: Module, processor: Processor<Module>) {
//        if (!project.useLibraryToSourceAnalysis) return

//        val resolutionAnchorDependencies = HashSet<ModuleInfo>()
//        val libraryInfoCache = LibraryInfoCache.getInstance(project)
//        val anchorCacheService = ResolutionAnchorCacheService.getInstance(project)
//        ModuleRootManager.getInstance(module).orderEntries().recursively().forEachLibrary { library ->
//            libraryInfoCache[library].flatMapTo(resolutionAnchorDependencies) { libraryInfo ->
//                checkCanceled()
//                anchorCacheService.getDependencyResolutionAnchors(libraryInfo)
//            }
//
//            true
//        }

//        for (anchorModule in resolutionAnchorDependencies) {
//            ModuleRootManager.getInstance(anchorModule.module!!).orderEntries().recursively().forEachModule(processor)
//        }
    }
}
