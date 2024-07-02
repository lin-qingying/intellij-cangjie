package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.intellij.psi.PsiElement

interface DeclarationScopeProvider {
    fun getResolutionScopeForDeclaration(elementOfDeclaration: PsiElement): LexicalScope

    fun getOuterDataFlowInfoForDeclaration(elementOfDeclaration: PsiElement): DataFlowInfo
}
