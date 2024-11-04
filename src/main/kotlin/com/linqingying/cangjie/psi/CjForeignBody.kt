package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

class CjForeignBody : CjElementImplStub<CangJiePlaceHolderStub<CjForeignBody>>, CjDeclarationContainer {
    private val lBraceTokenSet = TokenSet.create(CjTokens.LBRACE)
    private val rBraceTokenSet = TokenSet.create(CjTokens.RBRACE)

    constructor(node: ASTNode) : super(node)


    constructor(stub: CangJiePlaceHolderStub<CjForeignBody>) : super(stub, CjStubElementTypes.FOREIGN_BODY)

    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(CjFile.FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)


}
