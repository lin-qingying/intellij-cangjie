package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.intellij.psi.PsiElement

interface DeclarationScopeProvider {
    fun getResolutionScopeForDeclaration(elementOfDeclaration: PsiElement): LexicalScope

    fun getOuterDataFlowInfoForDeclaration(elementOfDeclaration: PsiElement): DataFlowInfo
}
