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

    abstract fun getContainingClassOrStruct(): CjClassOrStruct

    override fun isLocal() = false

    override fun getValueParameterList() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    override fun getValueParameters() = valueParameterList?.parameters ?: emptyList()

    override fun getReceiverTypeReference() = null

    override fun getContextReceivers(): List<CjContextReceiver> = emptyList()

    override fun getTypeReference() = null

    @Throws(IncorrectOperationException::class)
    override fun setTypeReference(typeRef: CjTypeReference?) =
        throw IncorrectOperationException("setTypeReference to constructor")

    override fun getColon() = findChildByType<PsiElement>(CjTokens.COLON)

    override fun getBodyExpression(): CjBlockExpression? = null

    override fun getEqualsToken() = null

    override fun hasBlockBody() = hasBody()

    fun isDelegatedCallToThis(): Boolean {
        stub?.let { return it.isDelegatedCallToThis() }
        return when (this) {
            is CjPrimaryConstructor -> false
            is CjSecondaryConstructor -> getDelegationCallOrNull()?.isCallToThis() ?: true
            else -> throw IllegalStateException("Unknown constructor type: $this")
        }
    }

    override fun hasBody(): Boolean {
        stub?.let { return it.hasBody() }
        return bodyExpression != null
    }

    override fun hasDeclaredReturnType() = false

    override fun getTypeParameterList() = null

    override fun getTypeConstraintList() = null

    override fun getTypeConstraints() = emptyList<CjTypeConstraint>()

    override fun getTypeParameters() = emptyList<CjTypeParameter>()

    override fun getName(): String? = getContainingClassOrStruct().name


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
        return getContainingClassOrStruct().useScope
    }
}
