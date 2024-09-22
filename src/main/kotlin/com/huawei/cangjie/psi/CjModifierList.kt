package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjKeywordToken
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.psi.psiUtil.collectAnnotationEntriesFromStubOrPsi
import com.huawei.cangjie.psi.stubs.CangJieModifierListStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet


abstract class CjModifierList : CjElementImplStub<CangJieModifierListStub >, CjAnnotationsContainer {
    constructor(stub: CangJieModifierListStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    val annotations: List<CjAnnotation>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.ANNOTATION)

    constructor(node: ASTNode) : super(node)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitModifierList(this, data)
    }

    val annotationEntries: List<CjAnnotationEntry>
        get() = this.collectAnnotationEntriesFromStubOrPsi()

    fun hasModifier(tokenType: CjModifierKeywordToken): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasModifier(tokenType)
        }
        return getModifier(tokenType) != null
    }

    fun getModifier(tokenType: CjKeywordToken): PsiElement? {
        return findChildByType(tokenType)
    }

    fun getModifier(tokenTypes: TokenSet): PsiElement? {
        return findChildByType(tokenTypes)
    }


    val owner: PsiElement
        get() = parentByStub

    override fun deleteChildInternal(child: ASTNode) {
        super.deleteChildInternal(child)
        if (firstChild == null) {
            delete()
        }
    }
}
