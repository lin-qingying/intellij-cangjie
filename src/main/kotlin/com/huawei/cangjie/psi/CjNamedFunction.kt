package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.stubs.CangJieFunctionStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

class CjNamedFunction : CjFunctionImpl {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieFunctionStub) : super(stub, CjStubElementTypes.FUNCTION)

    //    public bool mayHaveContract() {
    //        return mayHaveContract(true);
    //    }
    //    public bool mayHaveContract(bool isAllowedOnMembers) {
    //        CangJieFunctionStub stub = getStub();
    //        if (stub != null) {
    //            return stub.mayHaveContract();
    //        }
    //
    //        return CjPsiUtilKt.isContractPresentPsiCheck(this, isAllowedOnMembers);
    //    }
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

    override val receiverTypeReference: CjTypeReference?
        get() {
            val stub: CangJieFunctionStub? = stub
            if (stub != null) {
                if (!stub.isExtension()) {
                    return null
                }
                val childTypeReferences: List<CjTypeReference> =
                    getStubOrPsiChildrenAsList(
                        CjStubElementTypes.TYPE_REFERENCE
                    )
                return if (childTypeReferences.isNotEmpty()) {
                    childTypeReferences[0]
                } else {
                    null
                }
            }
            return receiverTypeRefByTree
        }

    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            var child: PsiElement? = firstChild
            while (child != null) {
                val tt: IElementType = child.node.elementType
                if (tt === CjTokens.LPAR || tt === CjTokens.COLON) break
                if (child is CjTypeReference) {
                    return child
                }
                child = child.nextSibling
            }

            return null
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

    override val isTopLevel: Boolean
        get() {
            return super.isTopLevel
        }
}
