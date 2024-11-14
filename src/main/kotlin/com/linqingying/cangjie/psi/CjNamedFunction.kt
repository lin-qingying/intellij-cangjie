package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 来自扩展的方法
 * 携带扩展的类型参数，与本方法类型参数分开
 */
class CjNamedFunctionForExtend : CjNamedFunction,CjTypeParameterListOwnerForExtend {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieFunctionStub) : super(stub)

    val extendTypeParameterList get() = this.getStrictParentOfType<CjExtend>()?.typeParameterList
    override val extendTypeParameters: List<CjTypeParameter>
        get() = extendTypeParameterList?.parameters ?: emptyList()
}

open class CjNamedFunction : CjFunctionImpl {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieFunctionStub) : super(stub, CjStubElementTypes.FUNCTION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitNamedFunction(this, data)
    }

    override fun hasTypeParameterListBeforeFunctionName(): Boolean {
        val stub: CangJieFunctionStub? = stub
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName()
        }
        return hasTypeParameterListBeforeFunctionNameByTree()
    }

    private fun hasTypeParameterListBeforeFunctionNameByTree(): Boolean {
        val typeParameterList: CjTypeParameterList = typeParameterList ?: return false
        val nameIdentifier: PsiElement = nameIdentifier ?: return true
        return nameIdentifier.textOffset > typeParameterList.textOffset
    }

    override fun hasBlockBody(): Boolean {
        val stub: CangJieFunctionStub? = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    @get:IfNotParsed
    val funKeyword: PsiElement?
        get() = findChildByType(CjTokens.FUNC_KEYWORD)

    override val equalsToken: PsiElement?
        get() = super.equalsToken

    override val initializer: CjExpression?
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java
        )

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }

    override val valueParameterList: CjParameterList?
        get() {
            return super.valueParameterList
        }

    override val valueParameters: List<CjParameter>
        get() {
            val list: CjParameterList? = valueParameterList
            return list?.parameters ?: emptyList()
        }

    override val bodyExpression: CjExpression?
        get() {
            return super.bodyExpression
        }

    override val bodyBlockExpression: CjBlockExpression?
        get() {
            return super.bodyBlockExpression
        }

    override fun hasBody(): Boolean {
        val stub: CangJieFunctionStub? = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyBlockExpression != null
    }

    override fun hasDeclaredReturnType(): Boolean {
        return typeReference != null
    }


    override fun toString(): String {
//        return getNode().getElementType().toString();
        return node.elementType.toString() + ": " + name
    }

    override val contextReceivers: List<CjContextReceiver>
        get() {
            val contextReceiverList: CjContextReceiverList? =
                getStubOrPsiChild(CjStubElementTypes.CONTEXT_RECEIVER_LIST)
            return contextReceiverList?.contextReceivers() ?: emptyList()
        }

    override val typeReference: CjTypeReference?
        get() {
            return super.typeReference
        }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, valueParameterList, typeRef)
    }


    override val colon: PsiElement?
        get() {
            return super.colon
        }

    val isAnonymous: Boolean
        get() {
            return name == null && isLocal
        }

    override val isMut: Boolean
        get() = hasModifier(CjTokens.MUT_KEYWORD)
    override val isConst: Boolean
        get() = hasModifier(CjTokens.MUT_KEYWORD)

}
