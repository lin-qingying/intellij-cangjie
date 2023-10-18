package com.huawei.cangjie1.psi

import com.huawei.cangjie1.lexer.CjTokens
import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes.CLASS_BODY
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil


class CjClassBody : CjElementImplStub<CangJiePlaceHolderStub<CjClassBody>>, CjDeclarationContainer {
    private val lBraceTokenSet = TokenSet.create(CjTokens.LBRACE)
    private val rBraceTokenSet = TokenSet.create(CjTokens.RBRACE)

    constructor(node: ASTNode) : super(node)




    constructor(stub: CangJiePlaceHolderStub<CjClassBody>) : super(stub, CLASS_BODY)

    override fun getParent() = parentByStub


    override fun toString():String{
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D) = visitor.visitClassBody(this, data)
    override val declarations: List<CjDeclaration>
        get() =stub?.getChildrenByType(CjFile.FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)



}
