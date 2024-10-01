package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.stubs.CangJiePropertyAccessorStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CjPropertyAccessor : CjDeclarationStub<CangJiePropertyAccessorStub >, CjDeclarationWithBody, CjModifierListOwner,
    CjDeclarationWithInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePropertyAccessorStub) : super(stub, CjStubElementTypes.PROPERTY_ACCESSOR)


    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPropertyAccessor(this, data)
    }

    val isSetter: Boolean
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
            if (stub != null) {
                return !stub.isGetter()
            }
            return findChildByType<PsiElement?>(CjTokens.SET_KEYWORD) != null
        }

    val isGetter: Boolean
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
            if (stub != null) {
                return stub.isGetter()
            }
            return findChildByType<PsiElement?>(CjTokens.GET_KEYWORD) != null
        }

    val parameterList: CjParameterList?
        get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    val parameter: CjParameter?
        get() {
            val parameterList: CjParameterList = parameterList ?: return null
            val parameters: List<CjParameter> = parameterList.parameters
            if (parameters.isEmpty()) return null
            return parameters[0]
        }

    override val valueParameters: List<CjParameter>
        get() {
            val parameter: CjParameter = parameter ?: return emptyList()
            return listOf(parameter)
        }

    override val bodyExpression: CjExpression?
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
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

    override val bodyBlockExpression: CjBlockExpression?
        get() {
            val stub: CangJiePropertyAccessorStub? = stub
            if (stub != null) {
                if (!(stub.hasBlockBody() && stub.hasBody())) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val bodyExpression: CjExpression? = findChildByClass(
                CjExpression::class.java
            )
            if (bodyExpression is CjBlockExpression) {
                return bodyExpression
            }

            return null
        }

    override fun hasBlockBody(): Boolean {
        val stub: CangJiePropertyAccessorStub? = stub
        if (stub != null) {
            return stub.hasBlockBody()
        }
        return equalsToken == null
    }

    override fun hasBody(): Boolean {
        val stub: CangJiePropertyAccessorStub? = stub
        if (stub != null) {
            return stub.hasBody()
        }
        return bodyExpression != null
    }

    override val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)


    override fun hasDeclaredReturnType(): Boolean {
        return true
    }

    val returnTypeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    val namePlaceholder: PsiElement
        get() {
            val get: PsiElement? = findChildByType(CjTokens.GET_KEYWORD)
            if (get != null) {
                return get
            }
            return findChildByType(CjTokens.SET_KEYWORD)!!
        }

    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)

    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)

    override val initializer: CjExpression?
        get() = PsiTreeUtil.getNextSiblingOfType(
            equalsToken,
            CjExpression::class.java
        )

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    val property: CjProperty
        get() {
            return parent!!.parent as   CjProperty
        }

    override fun getTextOffset(): Int {
        return namePlaceholder.textRange.startOffset
    }
}
