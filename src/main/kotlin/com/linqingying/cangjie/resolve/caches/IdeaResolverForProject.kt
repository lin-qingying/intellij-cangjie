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

package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.analyzer.*
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.createBuiltIns
import com.linqingying.cangjie.context.ProjectContext
import com.linqingying.cangjie.context.withModule
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.impl.ModuleDescriptorImpl

import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.IdePackageOracleFactory
import com.linqingying.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import java.util.*

data class ModuleContent<out M : ModuleInfo>(
    val moduleInfo: M,
    val syntheticFiles: Collection<CjFile>,
    val moduleContentScope: GlobalSearchScope
)


class IdeaResolverForProject(
    debugName: String,
    projectContext: ProjectContext,
    modules: Collection<ModuleInfo>,
    private val syntheticFilesByModule: Map<ModuleInfo, Collection<CjFile>>,
    delegateResolver: ResolverForProject<ModuleInfo>,
    fallbackModificationTracker: ModificationTracker? = null,

    ) : AbstractResolverForProject<ModuleInfo>(

    debugName,
    projectContext,
    modules,
    fallbackModificationTracker,
    delegateResolver,
    projectContext.project.service<IdePackageOracleFactory>()
) {

    private val builtInsCache: BuiltInsCache =
        (delegateResolver as? IdeaResolverForProject)?.builtInsCache ?: BuiltInsCache(projectContext, this)
    private val created = Date().toString()


    private fun getResolverForModuleFactory(moduleInfo: ModuleInfo): ResolverForModuleFactory {


        return CangJieResolverForModuleFactory()
    }
    override fun modulesContent(module: ModuleInfo): ModuleContent<ModuleInfo> =
        ModuleContent(module, syntheticFilesByModule[module] ?: emptyList(), module.moduleContentScope)

    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: ModuleInfo): ResolverForModule {
        val moduleContent =
            ModuleContent(moduleInfo, syntheticFilesByModule[moduleInfo] ?: listOf(), moduleInfo.moduleContentScope)

        val project = projectContext.project
        val languageVersionSettings =
            project.service<LanguageSettingsProvider>().getLanguageVersionSettings(moduleInfo, project)

        val resolverForModuleFactory = getResolverForModuleFactory(moduleInfo)
        val optimizingOptions = ResolveOptimizingOptionsProvider.getOptimizingOptions(project, descriptor, moduleInfo)

        val resolverForModule = resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            moduleContent,
            this,
            languageVersionSettings,
            sealedInheritorsProvider = IdeSealedClassInheritorsProvider,
            resolveOptimizingOptions = optimizingOptions,
            absentDescriptorHandlerClass = IdeaAbsentDescriptorHandler::class.java
        )
//        ResolverForModuleComputationTrackerEx.getInstance(project)?.onCreateResolverForModule(descriptor, moduleInfo)
        return resolverForModule
    }

    override fun builtInsForModule(module: ModuleInfo): CangJieBuiltIns {


        return builtInsCache.getOrCreateIfNeeded(module)
    }

    class BuiltInsCache(private val projectContext: ProjectContext, private val resolver: IdeaResolverForProject) {
        private val cache = mutableMapOf<BuiltInsCacheKey, CangJieBuiltIns>()

        fun getOrCreateIfNeeded(module: ModuleInfo): CangJieBuiltIns = projectContext.storageManager.compute {
            ProgressManager.checkCanceled()

//            val sdk = resolverForSdk.sdkDependency(module)
//            val stdlib = findStdlibForModulesBuiltins(module)

            val key = module.getKeyForBuiltIns()
            val cachedBuiltIns = cache[key]
            if (cachedBuiltIns != null) return@compute cachedBuiltIns


            createBuiltIns(projectContext, resolver)
                .also {
                    // TODO: MemoizedFunction should be used here instead, but for proper we also need a module (for LV settings) that is not contained in the key
                    cache[key] = it
                }
        }


    }


}
