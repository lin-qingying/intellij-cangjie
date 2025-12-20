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

package org.cangnova.cangjie.resolve

import com.intellij.openapi.progress.impl.CancellationCheck.Companion.runWithCancellationCheck
import org.cangnova.cangjie.container.get
import org.cangnova.cangjie.container.getService
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.resolve.caches.ProjectResolutionFacade
import org.cangnova.cangjie.resolve.caches.ResolutionFacadeModuleDescriptorProvider
import org.cangnova.cangjie.resolve.lazy.AbsentDescriptorHandler
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.resolve.binding.BindingContext

class ModuleResolutionFacadeImpl(
    private val projectFacade: ProjectResolutionFacade,
    private val context: IdeaModuleInfo
) : ResolutionFacade, ResolutionFacadeModuleDescriptorProvider {
    override val moduleDescriptor: ModuleDescriptor
        get() = findModuleDescriptor(context)
    override val project: Project
        get() = projectFacade.project

    @FrontendInternals
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T = getFrontendService(context, serviceClass)

    private fun <T : Any> getFrontendService(ideaModuleInfo: IdeaModuleInfo, serviceClass: Class<T>): T {
        return projectFacade.resolverForModuleInfo(ideaModuleInfo).componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        return projectFacade.resolverForModuleInfo(context).componentProvider.create(serviceClass)
    }

    override fun findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo) = projectFacade.findModuleDescriptor(ideaModuleInfo)
    override fun getResolverForProject(): ResolverForProject<IdeaModuleInfo> {
        return projectFacade.getResolverForProject()
    }

    override fun analyzeWithAllCompilerChecks(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()
        return runWithCancellationCheck {
            projectFacade.getAnalysisResultsForElement(element, callback)
        }

    }

    override fun analyzeWithAllCompilerChecks(
        elements: Collection<CjElement>,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        return runWithCancellationCheck {
            projectFacade.getAnalysisResultsForElements(elements, callback)
        }

    }

    override fun resolveToDescriptor(
        declaration: CjDeclaration,
        bodyResolveMode: BodyResolveMode
    ): DeclarationDescriptor =
        runWithCancellationCheck {
            if (CjPsiUtil.isLocal(declaration)) {
                val bindingContext = analyze(declaration, bodyResolveMode)
                bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                    ?: getFrontendService(context, AbsentDescriptorHandler::class.java).diagnoseDescriptorNotFound(
                        declaration
                    )
            } else {
                ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

                val resolveSession =
                    projectFacade.resolverForElement(declaration).componentProvider.get<ResolveSession>()
                resolveSession.resolveToDescriptor(declaration)
            }
        }

    override fun analyze(elements: Collection<CjElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        if (elements.isEmpty()) return BindingContext.EMPTY

        if (usePerFileAnalysisCache) {
            elements.singleOrNull()?.let { element ->
                fetchWithAllCompilerChecks(element)?.takeUnless { it.isError() }?.let { return it.bindingContext }
            }
        }

        @OptIn(FrontendInternals::class)
        val resolveElementCache = getFrontendService(elements.first(), ResolveElementCache::class.java)
        return runWithCancellationCheck {
            resolveElementCache.resolveToElements(elements, bodyResolveMode)
        }
    }


    override fun fetchWithAllCompilerChecks(element: CjElement): AnalysisResult? {
        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        return runWithCancellationCheck {
            projectFacade.fetchAnalysisResultsForElement(element)
        }
    }


    companion object {
        private val usePerFileAnalysisCache = Registry.`is`("cangjie.resolve.cache.uses.perfile.cache", true)
    }

    //    @FrontendInternals
//    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
//        return projectFacade.resolverForElement(element).componentProvider.getService(serviceClass)
//    }
    override fun analyze(element: CjElement, bodyResolveMode: BodyResolveMode): BindingContext {

        ResolveInDispatchThreadManager.assertNoResolveInDispatchThread()

        if (usePerFileAnalysisCache) {
            fetchWithAllCompilerChecks(element)?.takeUnless { it.isError() }?.let {


                return it.bindingContext


            }
        }

        @OptIn(FrontendInternals::class)
        val resolveElementCache = getFrontendService(element, ResolveElementCache::class.java)
        return runWithCancellationCheck {
            resolveElementCache.resolveToElement(element, bodyResolveMode)
        }
    }


    @FrontendInternals
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {

        return projectFacade.resolverForElement(element).componentProvider.getService(serviceClass)
    }
}


fun ResolutionFacade.findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo): ModuleDescriptor {
    return (this as ResolutionFacadeModuleDescriptorProvider).findModuleDescriptor(ideaModuleInfo)
}


