package com.huawei.cangjie.analyzer.components

import com.huawei.cangjie.analyzer.CjAnalysisSession
import com.huawei.cangjie.analyzer.lifetime.CjLifetimeOwner
import com.huawei.cangjie.analyzer.lifetime.CjLifetimeToken
import com.huawei.cangjie.analyzer.lifetime.withValidityAssertion
import com.huawei.cangjie.ide.completion.back.providers.CangJieResolutionScopeProvider
import com.huawei.cangjie.psi.psiUtil.contains
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

abstract class CjAnalysisSessionComponent {
    protected abstract val analysisSession: CjAnalysisSession
    protected open val token: CjLifetimeToken get() = analysisSession.token
}

interface CjAnalysisSessionMixIn : CjLifetimeOwner {
    val analysisSession: CjAnalysisSession
}

abstract class CjAnalysisScopeProvider : CjAnalysisSessionComponent() {
    abstract fun getAnalysisScope(): GlobalSearchScope

    abstract fun canBeAnalysed(psi: PsiElement): Boolean
}

class CjAnalysisScopeProviderImpl(
    override val analysisSession: CjAnalysisSession,
    override val token: CjLifetimeToken,
    private val shadowedScope: GlobalSearchScope
) : CjAnalysisScopeProvider() {
    private val baseResolveScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CangJieResolutionScopeProvider.getInstance(analysisSession.useSiteModule.project)
            .getResolutionScope(analysisSession.useSiteModule)
    }
//    private val baseResolveScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        CangJieResolutionScopeProvider.getInstance(analysisSession.useSiteModule.project)
//            .getResolutionScope(analysisSession.useSiteModule)
//    }

    private val resolveScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CjAnalysisScopeProviderResolveScope(baseResolveScope/*, analysisSession.useSiteModule*/, shadowedScope)
    }

    override fun getAnalysisScope(): GlobalSearchScope = resolveScope
    override fun canBeAnalysed(psi: PsiElement): Boolean {
        return (baseResolveScope.contains(psi) && !shadowedScope.contains(psi))
        /*     || psi.isFromGeneratedModule()*/
    }
}

interface CjAnalysisScopeProviderMixIn : CjAnalysisSessionMixIn {
    /**
     * Return [GlobalSearchScope] represent a scope code in which can be analysed by current [CjAnalysisSession].
     * That means [org.jetbrains.kotlin.analysis.api.symbols.CjSymbol] can be built for the declarations from this scope.
     */
    val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { analysisSession.analysisScopeProvider.getAnalysisScope() }


    /**
     * Checks if [PsiElement] is inside analysis scope.
     * That means [org.jetbrains.kotlin.analysis.api.symbols.CjSymbol] can be built by this [PsiElement]
     *
     * @see analysisScope
     */
    fun PsiElement.canBeAnalysed(): Boolean =
        withValidityAssertion { analysisSession.analysisScopeProvider.canBeAnalysed(this) }
}

private class CjAnalysisScopeProviderResolveScope(
    private val base: GlobalSearchScope,
//    private val useSiteModule: CjModule,
    private val shadowed: GlobalSearchScope,
) : GlobalSearchScope() {
    override fun getProject(): Project? = base.project
    override fun isSearchInModuleContent(aModule: Module): Boolean = base.isSearchInModuleContent(aModule)
    override fun isSearchInLibraries(): Boolean = base.isSearchInLibraries
    override fun contains(file: VirtualFile): Boolean =
        (base.contains(file) && !shadowed.contains(file)) /*|| file.isFromGeneratedModule(useSiteModule)*/

    override fun toString() =
        "Analysis scope   (base: $base, shadowed: $shadowed)"
//    override fun toString() =
//        "Analysis scope for $useSiteModule (base: $base, shadowed: $shadowed)"
}
