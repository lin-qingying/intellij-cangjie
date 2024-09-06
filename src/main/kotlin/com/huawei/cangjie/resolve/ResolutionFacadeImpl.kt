package com.huawei.cangjie.resolve

import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.analyzer.ResolverForProject
import com.huawei.cangjie.container.getService
import com.huawei.cangjie.descriptors.DiagnosticSink
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.ide.FrontendInternals
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.caches.ProjectResolutionFacade
import com.huawei.cangjie.resolve.caches.ResolutionFacadeModuleDescriptorProvider
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.utils.runWithCancellationCheck
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement

class ModuleResolutionFacadeImpl(
    private val projectFacade: ProjectResolutionFacade,
    private val moduleInfo: ModuleInfo
) : ResolutionFacade, ResolutionFacadeModuleDescriptorProvider {
    override val moduleDescriptor: ModuleDescriptor
        get() = findModuleDescriptor(moduleInfo)
    override val project: Project
        get() = projectFacade.project
    @FrontendInternals
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T = getFrontendService(moduleInfo, serviceClass)

    private fun <T : Any> getFrontendService(ideaModuleInfo: ModuleInfo, serviceClass: Class<T>): T {
        return projectFacade.resolverForModuleInfo(ideaModuleInfo).componentProvider.getService(serviceClass)
    }

    override fun findModuleDescriptor(ideaModuleInfo: ModuleInfo) = projectFacade.findModuleDescriptor(ideaModuleInfo)
    override fun getResolverForProject(): ResolverForProject<ModuleInfo> {
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


                return   it.bindingContext


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





fun ResolutionFacade.findModuleDescriptor(ideaModuleInfo: ModuleInfo): ModuleDescriptor {
    return (this as ResolutionFacadeModuleDescriptorProvider).findModuleDescriptor(ideaModuleInfo)
}


