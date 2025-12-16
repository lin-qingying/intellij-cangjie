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

package org.cangnova.cangjie.resolve.caches

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.context.withModule
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.ProjectDescriptor
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AbstractResolverForProject
import org.cangnova.cangjie.resolve.CangJieResolverForModuleFactory
import org.cangnova.cangjie.resolve.IdePackageOracleFactory
import org.cangnova.cangjie.resolve.LanguageSettingsProvider
import org.cangnova.cangjie.resolve.ResolverForModule
import org.cangnova.cangjie.resolve.ResolverForModuleFactory
import org.cangnova.cangjie.resolve.ResolverForProject
import org.cangnova.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import java.util.*

data class ModuleContent<out M : AnalysisContext>(
    val context: M,
    val syntheticFiles: Collection<CjFile>,
    val moduleContentScope: GlobalSearchScope
)


class IdeaResolverForProject(
    debugName: String,
    projectContext: ProjectContext,
    projectDescriptor: ProjectDescriptor,
    modules: Collection<AnalysisContext>,
    private val syntheticFilesByModule: Map<AnalysisContext, Collection<CjFile>>,
    delegateResolver: ResolverForProject<AnalysisContext>,
    fallbackModificationTracker: ModificationTracker? = null,

    ) : AbstractResolverForProject<AnalysisContext>(

    debugName,
    projectContext,
    projectDescriptor,
    modules,
    fallbackModificationTracker,
    delegateResolver,
    projectContext.project.service<IdePackageOracleFactory>()
) {

    private val created = Date().toString()


    private fun getResolverForModuleFactory(context: AnalysisContext): ResolverForModuleFactory {


        return CangJieResolverForModuleFactory()
    }

    override fun modulesContent(module: AnalysisContext): ModuleContent<AnalysisContext> =
        ModuleContent(module, syntheticFilesByModule[module] ?: emptyList(), module.scope)

    override fun createResolverForModule(descriptor: ModuleDescriptor, context: AnalysisContext): ResolverForModule {
        val moduleContent =
            ModuleContent(context, syntheticFilesByModule[context] ?: listOf(), context.scope)

        val project = projectContext.project
        val languageVersionSettings =
            project.service<LanguageSettingsProvider>().getLanguageVersionSettings(context, project)

        val resolverForModuleFactory = getResolverForModuleFactory(context)
        val optimizingOptions = ResolveOptimizingOptionsProvider.getOptimizingOptions(project, descriptor, context)

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
//        ResolverForModuleComputationTrackerEx.getInstance(project)?.onCreateResolverForModule(descriptor, context)
        return resolverForModule
    }




}
