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

package com.linqingying.cangjie.analyzer.components

import com.linqingying.cangjie.analyzer.CangJieAnalysisSession
import com.linqingying.cangjie.analyzer.lifetime.CjLifetimeOwner
import com.linqingying.cangjie.analyzer.lifetime.CjLifetimeToken
import com.linqingying.cangjie.analyzer.lifetime.withValidityAssertion
import com.linqingying.cangjie.ide.completion.providers.CangJieResolutionScopeProvider
import com.linqingying.cangjie.psi.psiUtil.contains
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

abstract class CangJieAnalysisSessionComponent {
    protected abstract val analysisSession: CangJieAnalysisSession
    protected open val token: CjLifetimeToken get() = analysisSession.token
}

interface CjAnalysisSessionMixIn : CjLifetimeOwner {
    val analysisSession: CangJieAnalysisSession
}

abstract class CangJieAnalysisScopeProvider : CangJieAnalysisSessionComponent() {
    abstract fun getAnalysisScope(): GlobalSearchScope

    abstract fun canBeAnalysed(psi: PsiElement): Boolean
}

class CangJieAnalysisScopeProviderImpl(
    override val analysisSession: CangJieAnalysisSession,
    override val token: CjLifetimeToken,
    private val shadowedScope: GlobalSearchScope
) : CangJieAnalysisScopeProvider() {
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
     * Return [GlobalSearchScope] represent a scope code in which can be analysed by current [CangJieAnalysisSession].
       */
    val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { analysisSession.analysisScopeProvider.getAnalysisScope() }


    /**
     * Checks if [PsiElement] is inside analysis scope.
     * That means [com.linqingying.cangjie.analysis.api.symbols.CjSymbol] can be built by this [PsiElement]
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
