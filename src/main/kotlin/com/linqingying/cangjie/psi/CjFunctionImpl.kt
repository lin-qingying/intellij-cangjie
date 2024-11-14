package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

abstract class CjFunctionImpl : CjTypeParameterListOwnerStub<CangJieFunctionStub>, CjFunction,
    CjDeclarationWithInitializer {
    constructor(node: ASTNode) : super(node)


    constructor(stub: CangJieFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val valueParameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    override fun toString(): String {
        return node.elementType.toString()
    }

    open fun hasTypeParameterListBeforeFunctionName(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName()
        }
        return hasTypeParameterListBeforeFunctionNameByTree()
    }

    private fun hasTypeParameterListBeforeFunctionNameByTree(): Boolean {
        val typeParameterList = typeParameterList ?: return false
        val nameIdentifier = nameIdentifier ?: return true
        return nameIdentifier.textOffset > typeParameterList.textOffset
    }

    override val receiverTypeReference: CjTypeReference?
        get() {

            val stub = stub
            if (stub != null) {
                if (!stub.isExtension()) {
                    return null
                }

                val parent = this.getStrictParentOfType<CjExtend>()

                return parent?.receiverTypeReceiver
            }
//            return null
            return receiverTypeRefByTree
        }
    val originalTypeParameterList: CjTypeParameterList? get() = super.typeParameterList

    override val typeParameterList: CjTypeParameterList?
        get() {

            val superTypeParameterList = super.typeParameterList

            if (superTypeParameterList != null) return superTypeParameterList


            return null
        }
    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            val parent = this.getStrictParentOfType<CjExtend>()

            return parent?.receiverTypeReceiver
        }


    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                val typeReferences =
                    getStubOrPsiChildrenAsList(
                        CjStubElementTypes.TYPE_REFERENCE
                    )
                val returnTypeIndex = if (stub.isExtension()) 1 else 0
                if (returnTypeIndex >= typeReferences.size) {
                    return null
                }
                return typeReferences[returnTypeIndex]
            }
            return getTypeReference(this)
        }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, valueParameterList, typeRef)
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)
    override val bodyExpression: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasBody()) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            return findChildByClass(CjExpression::class.java)
        }

    override val keyword: PsiElement?
        get() = findChildByType(CjTokens.FUNC_KEYWORD)
    override val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)
    override val bodyBlockExpression: CjBlockExpression?
        get() {

            val stub = stub
            if (stub != null) {
                if (!(stub.hasBlockBody() && stub.hasBody())) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val bodyExpression = findChildByClass(
                CjExpression::class.java
            )
            if (bodyExpression is CjBlockExpression) {
                return bodyExpression
            }

            return null
        }

    override fun hasBlockBody(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    override fun hasBody(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyExpression != null
    }

    override fun hasDeclaredReturnType(): Boolean {
        return false
    }

    override val valueParameters: List<CjParameter>
        get() {

            val list = valueParameterList
            return list?.parameters ?: emptyList()
        }


    override val isLocal: Boolean
        get() {
            val parent = parent
            return !(parent is CjFile || parent is CjAbstractClassBody)
        }
    override val isUnsafe
        get() = hasModifier(CjTokens.UNSAFE_KEYWORD)
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)
    override val isOperator: Boolean
        get() = hasModifier(CjTokens.OPERATOR_KEYWORD)

    override val initializer: CjExpression?
        //    public bool mayHaveContract() {
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java
        )

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    open val isTopLevel: Boolean
        //    @Override
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isTopLevel()
            }

            return parent is CjFile
        }

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement? {
        return super.setName(name)
    }
}
