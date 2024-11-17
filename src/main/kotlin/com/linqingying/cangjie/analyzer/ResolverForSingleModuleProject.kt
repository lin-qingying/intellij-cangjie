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

package com.linqingying.cangjie.analyzer

import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.config.LanguageVersionSettingsImpl
import com.linqingying.cangjie.context.ProjectContext
import com.linqingying.cangjie.context.withModule
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.CliSealedClassInheritorsProvider
import com.linqingying.cangjie.resolve.PackageOracleFactory
import com.linqingying.cangjie.resolve.caches.ModuleContent
import com.linqingying.cangjie.types.DefaultBuiltIns


class ResolverForSingleModuleProject<M : ModuleInfo>(
    debugName: String,
    projectContext: ProjectContext,
    private val module: M,
    private val resolverForModuleFactory: ResolverForModuleFactory,
    private val searchScope: GlobalSearchScope,
    private val builtIns: CangJieBuiltIns = DefaultBuiltIns,
    private val languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    private val syntheticFiles: Collection<CjFile> = emptyList(),
    private val sdkDependency: M? = null,
    knownDependencyModuleDescriptors: Map<M, ModuleDescriptor> = emptyMap()
) : AbstractResolverForProject<M>(
    debugName,
    projectContext,
    listOf(module) + knownDependencyModuleDescriptors.keys,
    null,
    EmptyResolverForProject(),
    PackageOracleFactory.OptimisticFactory
) {
//    override fun sdkDependency(module: M): M? = sdkDependency

    init {
        knownDependencyModuleDescriptors.forEach { (module, descriptor) ->
            descriptorByModule[module] = ModuleData(
                descriptor as ModuleDescriptorImpl,
                (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
            )
        }
    }

    override fun modulesContent(module: M): ModuleContent<M> = when (module) {
        this.module -> ModuleContent(module, syntheticFiles, searchScope)
        else -> ModuleContent(module, emptyList(), searchScope)
    }

    override fun builtInsForModule(module: M): CangJieBuiltIns = builtIns

    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: M): ResolverForModule =
        resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            modulesContent(moduleInfo),
            this,
            languageVersionSettings,
            CliSealedClassInheritorsProvider,
            resolveOptimizingOptions = null,
            absentDescriptorHandlerClass = null
        )
}
