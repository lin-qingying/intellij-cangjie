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

package cn.cangnova.cangjie.analyzer.session

import cn.cangnova.cangjie.analyzer.CjAnalysisFacade
import cn.cangnova.cangjie.analyzer.CangJieAnalysisSession
import cn.cangnova.cangjie.analyzer.CangJieAnalysisSessionImpl
import cn.cangnova.cangjie.analyzer.ProjectStructureProvider
import cn.cangnova.cangjie.analyzer.lifetime.CjLifetimeTokenFactory
import cn.cangnova.cangjie.analyzer.lifetime.CjLifetimeTokenProvider
import cn.cangnova.cangjie.analyzer.lifetime.NoWriteActionInAnalyseCallChecker
import cn.cangnova.cangjie.psi.CjElement
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
