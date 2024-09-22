package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.CangJieValueArgumentStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.stubs.IStubElementType

open class CjValueArgument : CjElementImplStub<CangJieValueArgumentStub<out CjValueArgument>>,
    ValueArgument {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieValueArgumentStub<CjValueArgument>) : super(stub, CjStubElementTypes.VALUE_ARGUMENT)

    protected constructor(
        stub: CangJieValueArgumentStub<out CjValueArgument>,
        nodeType: IStubElementType<*, *>
    ) : super(stub, nodeType)


    override fun getArgumentExpression(): CjExpression? {
        val stub: CangJiePlaceHolderStub<out CjValueArgument>? = stub
        if (stub != null) {
            val constantExpressions =
                stub.getChildrenByType(CjNodeTypes.CONSTANT_EXPRESSIONS_TYPES, CjExpression.EMPTY_ARRAY)
            if (constantExpressions.isNotEmpty()) {
                return constantExpressions[0]
            }
        }

        return findChildByClass(CjExpression::class.java)
    }

    override fun getArgumentName(): ValueArgumentName? {
        return getStubOrPsiChild(CjStubElementTypes.VALUE_ARGUMENT_NAME)
    }

    override fun isNamed(): Boolean {
        return false
    }

    override fun asElement(): CjValueArgument {
        return this
    }

    override fun getSpreadElement(): LeafPsiElement? {
        return null
    }

    override fun isExternal(): Boolean {
        return false
    }

    val isSpread: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isSpread()
            }

            return getSpreadElement() != null
        }
}

