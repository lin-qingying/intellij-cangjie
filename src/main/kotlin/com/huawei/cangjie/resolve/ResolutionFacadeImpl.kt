package com.huawei.cangjie.resolve

import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.analyzer.ResolverForModule
import com.huawei.cangjie.container.getService
import com.huawei.cangjie.descriptors.DiagnosticSink
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.caches.ProjectResolutionFacade
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.utils.runWithCancellationCheck
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement

class ResolutionFacadeImpl(
    private val projectFacade: ProjectResolutionFacade,

    ) : ResolutionFacade {

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
//        return projectFacade
//        if (usePerFileAnalysisCache) {
//            fetchWithAllCompilerChecks(element)?.takeUnless { it.isError() }?.let { return it.bindingContext }
//        }

        @OptIn(FrontendInternals::class)
        val resolveElementCache = getFrontendService(element, ResolveElementCache::class.java)
        return runWithCancellationCheck {
            resolveElementCache.resolveToElement(element, bodyResolveMode)
        }
    }


    @FrontendInternals
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {

        return  projectFacade.resolverForElement(element).componentProvider.getService(serviceClass)
    }
}


/**
 * Indicates sensitive frontend API, which should be used with caution to avoid invariant violation.
 * Use sites of this annotation include all methods for direct access to frontend components.
 * Please make sure that components don't receive resolution results (descriptors etc.) from different resolution facade for processing.
 * The simplest way to do so is to explicitly provide the same resolution facade to all related computations.
 * Not following this rule may lead to obscure memory leaks and other potential problems.
 */
@RequiresOptIn
annotation class FrontendInternals
