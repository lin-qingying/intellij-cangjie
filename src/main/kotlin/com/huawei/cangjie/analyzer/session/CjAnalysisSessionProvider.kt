package com.huawei.cangjie.analyzer.session

import com.huawei.cangjie.analyzer.CjAnalysisFacade
import com.huawei.cangjie.analyzer.CjAnalysisSession
import com.huawei.cangjie.analyzer.CjAnalysisSessionImpl
import com.huawei.cangjie.analyzer.ProjectStructureProvider
import com.huawei.cangjie.analyzer.lifetime.CjLifetimeTokenFactory
import com.huawei.cangjie.analyzer.lifetime.CjLifetimeTokenProvider
import com.huawei.cangjie.analyzer.lifetime.NoWriteActionInAnalyseCallChecker
import com.huawei.cangjie.psi.CjElement
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly


/**
 * Provides [CjAnalysisSession]s by use-site [CjElement]s or [CjModule]s.
 *
 * This provider should not be used directly.
 * Please use [analyze][org.jetbrains.kotlin.analysis.api.analyze] or [analyzeCopy][org.jetbrains.kotlin.analysis.api.analyzeCopy] instead.
 */
abstract class CjAnalysisSessionProvider(val project: Project) : Disposable {
    val tokenFactory: CjLifetimeTokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CjLifetimeTokenProvider.getService(project).getLifetimeTokenFactory()
    }

    @Suppress("LeakingThis")
    val noWriteActionInAnalyseCallChecker: NoWriteActionInAnalyseCallChecker =
        NoWriteActionInAnalyseCallChecker(this)

    inline fun <R> analyse(
        analysisSession: CjAnalysisSession,
        action: CjAnalysisSession.() -> R,
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

    abstract fun getAnalysisSession(useSiteCjElement: CjElement): CjAnalysisSession

    inline fun <R> analyse(
        useSiteCjElement: CjElement,
        action: CjAnalysisSession.() -> R,
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
    override fun getAnalysisSession(useSiteCjElement: CjElement): CjAnalysisSession {
        val facade = CjAnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project)
        val context = facade.getAnalysisContext(useSiteCjElement, token)
        val useSiteModule = ProjectStructureProvider.getModule(project, useSiteCjElement, contextualModule = null)
        return CjAnalysisSessionImpl(context, useSiteModule, token)
    }

    override fun clearCaches() {

    }
}
