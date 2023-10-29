package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getParentOfType
import com.huawei.cangjie.psi.psiUtil.sure
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


interface CjAnonymousInitializer : CjDeclaration, CjStatementExpression {
    val containingDeclaration: CjDeclaration
    val body: CjExpression?
}

class CjClassInitializer : CjDeclarationStub<CangJiePlaceHolderStub<CjClassInitializer>>, CjAnonymousInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjClassInitializer>) : super(stub, CjStubElementTypes.CLASS_INITIALIZER)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?) = visitor.visitClassInitializer(this, data)
    override val body: CjExpression?
        get() = findChildByClass(CjExpression::class.java)


    val openBraceNode: PsiElement?
        get() = (body as? CjBlockExpression)?.lBrace

    val initKeyword: PsiElement
        get() = findChildByType(CjTokens.INIT_KEYWORD)!!

    override val containingDeclaration: CjClass
        get() = getParentOfType<CjClass>(true).sure { "Should only be present in class or object" }

}

