package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens.LPAR
import com.huawei.cangjie.lexer.CjTokens.QUEST
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode

class CjOptionType : CjElementImplStub<CangJiePlaceHolderStub<CjOptionType>>, CjTypeElement {


    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJiePlaceHolderStub<CjOptionType>) : super(stub, CjStubElementTypes.OPTIONAL_TYPE)


    fun getQuestionMarkNode(): ASTNode {
        return node.findChildByType(QUEST) ?: this.children[0].node.findChildByType(LPAR) ?: this.node
    }

    override fun getTypeArgumentsAsTypes(): List<CjTypeReference> {
        val innerType = getInnerType()
        return innerType?.getTypeArgumentsAsTypes() ?: emptyList()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitOptionType(this, data)
    }

    @IfNotParsed
    fun getInnerType(): CjTypeElement? {
        return CjStubbedPsiUtil.getStubOrPsiChild(
            this,
            CjTokenSets.TYPE_ELEMENT_TYPES,
            CjTypeElement.ARRAY_FACTORY
        )
    }

    fun getModifierList(): CjModifierList? {
        return getStubOrPsiChild(CjStubElementTypes.MODIFIER_LIST)
    }

    fun getAnnotationEntries(): List<CjAnnotationEntry> {
        val modifierList: CjModifierList? = getModifierList()
        return modifierList?.annotationEntries ?: emptyList()
    }
}
