package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.stubs.CangJieParameterStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil


class CjParameter : CjNamedDeclarationStub<CangJieParameterStub>, CjCallableDeclaration, CjLetVarKeywordOwner {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieParameterStub) : super(stub, CjStubElementTypes.VALUE_PARAMETER)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitParameter(this, data)
    }


    override val typeReference: CjTypeReference?
        get() =  getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    val destructuringDeclaration: CjDestructuringDeclaration?
        get() {
            if (stub != null) return null

            return findChildByType(CjNodeTypes.DESTRUCTURING_DECLARATION)
        }



    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)
    val isNamed: Boolean
        get() {
//            if (colon != null) {
//                return colon!!.node.treePrev.elementType === CjTokens.EXCL
//            }


            return findChildByType<PsiElement>(CjTokens.EXCL) != null
        }

    val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)

    fun hasDefaultValue(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasDefaultValue()
        }
        return defaultValue != null
    }

    val isLambdaParameter: Boolean
        /**
         * For example,
         * lambdaConsumer { lambdaParameter ->
         * ...
         * }
         *
         * @return [true] if this [CjParameter] is a parameter of a lambda.
         */
        get() = checkParentOfParentType(CjFunctionLiteral::class.java)

    val defaultValue: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasDefaultValue()) {
                    return null
                }

                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val equalsToken = equalsToken
            return if (equalsToken != null) PsiTreeUtil.getNextSiblingOfType(
                equalsToken,
                CjExpression::class.java
            ) else null
        }

    val isMutable: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isMutable()
            }

            return findChildByType<PsiElement?>(CjTokens.VAR_KEYWORD) != null
        }

    fun hasLetOrVar(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasValOrVar()
        }
        return letOrVarKeyword != null
    }

    override val letOrVarKeyword: PsiElement?
        get() {
            val stub = stub
            if (stub != null && !stub.hasValOrVar()) {
                return null
            }
            return findChildByType(LET_VAR_TOKEN_SET)
        }

    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }


    private fun <T : PsiElement> checkParentOfParentType(klass: Class<T>): Boolean {
        val parent = parent ?: return false
        return klass.isInstance(parent.parent)
    }

    val isCatchParameter: Boolean
        get() = checkParentOfParentType(CjCatchClause::class.java)



    override val contextReceivers: List<CjContextReceiver> = emptyList()
    override val valueParameterList: CjParameterList? = null
    override val valueParameters: List<CjParameter> = emptyList()
    override val receiverTypeReference: CjTypeReference? = null
    override val typeParameterList: CjTypeParameterList? = null
    override val typeConstraintList: CjTypeConstraintList? = null
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    override val typeParameters: List<CjTypeParameter> = emptyList()

    val ownerFunction: CjDeclarationWithBody?
        get() {
            val parent = parentByStub as? CjParameterList ?: return null
            return parent.ownerFunction
        }

    override fun getUseScope(): SearchScope {
        var owner: CjExpression? = ownerFunction
        if (owner is CjPrimaryConstructor) {
            if (hasLetOrVar()) return super.getUseScope()
            owner = owner.getContainingTypeStatement()
        }
        if (owner == null) {
            owner = PsiTreeUtil.getParentOfType(this, CjExpression::class.java)
        }
        return LocalSearchScope(owner ?: this)
    }

    companion object {
        val LET_VAR_TOKEN_SET: TokenSet =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.CONST_KEYWORD, CjTokens.VAR_KEYWORD)
    }
}
