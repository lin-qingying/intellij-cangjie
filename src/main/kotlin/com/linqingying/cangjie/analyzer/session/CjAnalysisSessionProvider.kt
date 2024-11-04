package com.linqingying.cangjie.analyzer.session

import com.linqingying.cangjie.analyzer.CjAnalysisFacade
import com.linqingying.cangjie.analyzer.CangJieAnalysisSession
import com.linqingying.cangjie.analyzer.CangJieAnalysisSessionImpl
import com.linqingying.cangjie.analyzer.ProjectStructureProvider
import com.linqingying.cangjie.analyzer.lifetime.CjLifetimeTokenFactory
import com.linqingying.cangjie.analyzer.lifetime.CjLifetimeTokenProvider
import com.linqingying.cangjie.analyzer.lifetime.NoWriteActionInAnalyseCallChecker
import com.linqingying.cangjie.psi.CjElement
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly


/**
 * Provides [CangJieAnalysisSession]s by use-site [CjElement]s or [CjModule]s.
 *
 * This provider should not be used directly.
 */
abstract class CjAnalysisSessionProvider(val project: Project) : Disposable {
    val tokenFactory: CjLifetimeTokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CjLifetimeTokenProvider.getService(project).getLifetimeTokenFactory()
    }

    @Suppress("LeakingThis")
    val noWriteActionInAnalyseCallChecker: NoWriteActionInAnalyseCallChecker =
        NoWriteActionInAnalyseCallChecker(this)

    inline fun <R> analyse(
        analysisSession: CangJieAnalysisSession,
        action: CangJieAnalysisSession.() -> R,
    ): R {
        noWriteActionInAnalyseCallChecker.beforeEnteringAnalysisContext()
        tokenFactory.beforeEnteringAnalysisContext(analysisSession.token)
        return try {
            analysisSession.action()
        } finally {
            tokenFactory.afterLeavingAnalysisContext(analysisSession.token)
            noWriteActionInAnalyseCallChecker.afterLeavingAnalysisContext()
        }
    }

    abstract fun getAnalysisSession(useSiteCjElement: CjElement): CangJieAnalysisSession

    inline fun <R> analyse(
        useSiteCjElement: CjElement,
        action: CangJieAnalysisSession.() -> R,
    ): R {
        return analyse(getAnalysisSession(useSiteCjElement), action)
    }

    @TestOnly
    abstract fun clearCaches()

    override fun dispose() {}

    companion object {

        fun getInstance(project: Project): CjAnalysisSessionProvider =
            project.getService(CjAnalysisSessionProvider::class.java)
    }
}

class CjAnalysisSessionProviderImpl(project: Project) : CjAnalysisSessionProvider(project) {
    override fun getAnalysisSession(useSiteCjElement: CjElement): CangJieAnalysisSession {
        val facade = CjAnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project)
        val context = facade.getAnalysisContext(useSiteCjElement, token)
        val useSiteModule = ProjectStructureProvider.getModule(project, useSiteCjElement, contextualModule = null)
        return CangJieAnalysisSessionImpl(context, useSiteModule, token)
    }

    override fun clearCaches() {

    }
}
