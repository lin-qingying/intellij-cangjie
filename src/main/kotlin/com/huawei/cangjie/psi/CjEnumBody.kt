package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

//
//class CjEnumBody : CjElementImplStub<CangJiePlaceHolderStub<CjEnumBody>>, CjDeclarationContainer {
//    private val lBraceTokenSet = TokenSet.create(CjTokens.LBRACE)
//    private val rBraceTokenSet = TokenSet.create(CjTokens.RBRACE)
//
//    constructor(node: ASTNode) : super(node)
//
//
//
//
//    constructor(stub: CangJiePlaceHolderStub<CjEnumBody>) : super(stub, CjStubElementTypes.ENUM_BODY)
//
//    override fun getParent() = parentByStub
//
//
//    override fun toString():String{
//        return node.elementType.toString()
//    }
//
//    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?) = visitor.visitEnumBody(this, data)
//    override val declarations: List<CjDeclaration>
//        get() =stub?.getChildrenByType(CjFile.FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
//            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)
//
//
//
//}
