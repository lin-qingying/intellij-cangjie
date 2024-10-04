package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException


abstract class CjConstructor<T : CjConstructor<T>> : CjDeclarationStub<CangJieConstructorStub<T>>, CjFunction {
    protected constructor(node: ASTNode) : super(node)
    protected constructor(stub: CangJieConstructorStub<T>, nodeType: CjConstructorElementType<T>) : super(
        stub,
        nodeType
    )

    open fun getConstructorKeyword(): PsiElement? = findChildByType(CjTokens.INIT_KEYWORD)

    abstract fun getContainingTypeStatement(): CjTypeStatement

    override val isLocal  = false
    override val bodyExpression : CjInitBlockExpression?get()   {
        val stub = stub
        if (stub != null) {
            if (!stub.hasBody()) {
                return null
            }
            if (getContainingCjFile().isCompiled) {
                return null
            }
        }
        return findChildByClass(CjInitBlockExpression::class.java)
    }


    override val receiverTypeReference: CjTypeReference? = null

    override val contextReceivers: List<CjContextReceiver> = emptyList()

    override val valueParameters: List<CjParameter>
        get() = valueParameterList?.parameters ?: emptyList()
    override val typeReference: CjTypeReference? = null
    override val valueParameterList: CjParameterList? get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)
    fun getDelegationCall(): CjConstructorDelegationCall? = bodyExpression?.getDelegationCall()

    fun getDelegationCallOrNull(): CjConstructorDelegationCall? = bodyExpression?.getDelegationCallOrNull()

    fun hasImplicitDelegationCall(): Boolean = getDelegationCall()?.isImplicit == true

    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): CjConstructorDelegationCall {
        return bodyExpression!!.replaceImplicitDelegationCallWithExplicit(isThis)
    }

    @Throws(IncorrectOperationException::class)
    override fun setTypeReference(typeRef: CjTypeReference?) =
        throw IncorrectOperationException("setTypeReference to constructor")


    override val colon get() = findChildByType<PsiElement>(CjTokens.COLON)

    override val equalsToken  = null

    override fun hasBlockBody() = hasBody()

    fun isDelegatedCallToThis(): Boolean {
        stub?.let { return it.isDelegatedCallToThis() }
        return when (this) {
            is CjPrimaryConstructor -> false
            is CjSecondaryConstructor -> getDelegationCallOrNull()?.isCallToThis ?: true
            else -> throw IllegalStateException("Unknown constructor type: $this")
        }
    }

    override fun hasBody(): Boolean {
        stub?.let { return it.hasBody() }
        return bodyExpression != null
    }

    override val typeParameterList: CjTypeParameterList? = null
    override val typeConstraintList: CjTypeConstraintList? = null
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    override val typeParameters: List<CjTypeParameter> = emptyList()

    override fun hasDeclaredReturnType() = false


    override fun getName(): String? = getContainingTypeStatement().name


    override val fqName: FqName?
        get() = null

    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    override val nameAsName: Name?
        get() = nameAsSafeName

    override fun getNameIdentifier() = null

    override fun getIdentifyingElement(): PsiElement? = getInitKeyword()

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = throw IncorrectOperationException("setName to constructor")

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)


    open fun getInitKeyword(): PsiElement? = findChildByType(CjTokens.INIT_KEYWORD)

    fun hasConstructorKeyword(): Boolean = stub != null || getInitKeyword() != null

    override fun getTextOffset(): Int {
        return getInitKeyword()?.textOffset
            ?: valueParameterList?.textOffset
            ?: super.getTextOffset()
    }

    override fun getUseScope(): SearchScope {
        return getContainingTypeStatement().useScope
    }
}
