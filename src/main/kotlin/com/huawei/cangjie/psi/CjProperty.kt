package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType

import com.huawei.cangjie.psi.stubs.CangJiePropertyStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

open class CjProperty : CjTypeParameterListOwnerStub<CangJiePropertyStub>, CjVariableDeclaration {
    companion object {
        private val LET_VAR_TOKEN_SET =
            TokenSet.create(CjTokens.PROP_KEYWORD, CjTokens.MUT_KEYWORD, CjTokens.CONST_KEYWORD)
        val LOG: Logger = Logger.getInstance(
            CjProperty::class.java
        )

    }
    val isLocal: Boolean
        get() =   !isMember


    val isMember: Boolean
        get() {
            val parent = parent
            return parent is CjTypeStatement || parent is CjAbstractClassBody
        }
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)
//
//    fun getDelegateExpression():CjExpression? {
//        val stub: CangJiePropertyStub? = stub
//        if (stub != null && !stub.hasDelegateExpression()) {
//            return null
//        }
//
//        val delegate: CjPropertyDelegate = getDelegate()
//        if (delegate != null) {
//            return delegate.getExpression()
//        }
//
//        return null
//    }
//    fun hasDelegateExpressionOrInitializer(): Boolean {
//        return hasDelegateExpression() || hasInitializer()
//    }

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitProperty(this, data)

    }

    fun hasBody(): Boolean {
//        if (hasDelegateExpressionOrInitializer()) return true

        if (getter != null && getter!!.hasBody()) {
            return true
        }

        if (setter != null && setter!!.hasBody()) {
            return true
        }
        return false
    }

    override val typeReference: CjTypeReference?
        get() {
            val stub: CangJiePropertyStub? = stub
            if (stub != null) {
                if (!stub.hasReturnTypeRef()) {
                    return null
                } else {
                    val typeReferences: List<CjTypeReference> =
                        getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                    val returnTypeRefPositionInPsi = /*if (stub.isExtension()) 1 else*/ 0
                    if (typeReferences.size <= returnTypeRefPositionInPsi) {
                        LOG.error(
                            """
                        Invalid stub structure built for property:
                        $text
                        """.trimIndent()
                        )
                        return null
                    }
                    return typeReferences[returnTypeRefPositionInPsi]
                }
            }
            return getTypeReference(this)
        }


    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        TODO("Not yet implemented")
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    override val initializer: CjExpression?
        get() = null

    override fun hasInitializer(): Boolean {
        val stub: CangJiePropertyStub? = stub
//        if (stub != null) {
//            return stub.hasInitializer()
//        }

        return initializer != null
    }

    override val letOrVarKeyword: PsiElement?
       get() {
        val element =
            checkNotNull(findChildByType(LET_VAR_TOKEN_SET)) { "Let or var should always exist for property" + this.text }
        return element
    }


    constructor(stub: CangJiePropertyStub) : super(stub, CjStubElementTypes.PROPERTY)
    constructor(node: ASTNode) : super(node)

    override val isVar: Boolean
        get() = hasModifier(CjTokens.MUT_KEYWORD)


    override fun toString(): String = super.toString() + ": " + name
    private val receiverTypeRefByTree: CjTypeReference?
        get() {
            val parent = this.getStrictParentOfType<CjExtend>()

            return parent?.receiverTypeReceiver
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

    override val valueParameterList: CjParameterList? = null
    override val valueParameters: List<CjParameter> = emptyList()

    val body :CjPropertyBody? get() = findChildByClass(CjPropertyBody::class.java)
    val accessors: List<CjPropertyAccessor>
        get() {
            return body?.accessors ?: emptyList()

        }

    val getter: CjPropertyAccessor?
        get() {
            for (accessor in accessors) {
                if (accessor.isGetter) return accessor
            }
            return null
        }

    val setter: CjPropertyAccessor?
        get() {
            for (accessor in accessors)
                if (accessor.isSetter) return accessor
            return null

        }


}
