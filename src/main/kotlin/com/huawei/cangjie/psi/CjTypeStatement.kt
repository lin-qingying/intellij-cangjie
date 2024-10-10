package com.huawei.cangjie.psi

import com.huawei.cangjie.ide.quickfix.overrideImplement.getOrCreateBody
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.psi.psiUtil.ClassIdCalculator
import com.huawei.cangjie.psi.stubs.CangJieTypeStatementStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil


abstract class CjTypeStatement :
    CjTypeParameterListOwnerStub<CangJieTypeStatementStub<out CjTypeStatement>>, CjDeclarationContainer,
    CjNamedDeclaration,
    CjPureTypeStatement, CjClassLikeDeclaration {

    companion object {
        val EMPTY_ARRAY: Array<CjTypeStatement?> = arrayOfNulls(0)

    }

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypeStatementStub<out CjTypeStatement>, nodeType: IStubElementType<*, *>) : super(
        stub,
        nodeType
    )

    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()

    override fun isLocal(): Boolean = stub?.isLocal() ?: CjPsiUtil.isLocal(this)
    override val declarations: List<CjDeclaration>
        get() = getBody()?.declarations.orEmpty()

//    fun isTopLevel(): Boolean = stub?.isTopLevel() ?: (parent is CjFile)

    override fun toString(): String {
        return node.elementType.toString()
    }

    abstract val typeName: String
    val variables: List<CjVariable> get() = body?.variables.orEmpty()
    val properties: List<CjProperty> get() = body?.properties.orEmpty()

    fun getSuperTypeList(): CjSuperTypeList? = getStubOrPsiChild(CjStubElementTypes.SUPER_TYPE_LIST)

//    TODO 一定是顶层
//    fun isTopLevel(): Boolean = stub?.isTopLevel() ?: isCjFile(parent)

    inline fun <reified T : CjDeclaration> addDeclaration(declaration: T): T {
        val body = getOrCreateBody()
        val anchor = PsiTreeUtil.skipSiblingsBackward(body.rBrace ?: body.lastChild!!, PsiWhiteSpace::class.java)
        return if (anchor?.nextSibling is PsiErrorElement) {
            body.addBefore(declaration, anchor)
        } else {
            body.addAfter(declaration, anchor)
        } as T
    }

    override fun hasExplicitPrimaryConstructor(): Boolean = primaryConstructor != null

    fun hasSecondaryConstructors(): Boolean = !secondaryConstructors.isEmpty()

    override fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()


    override fun getPrimaryConstructor(): CjPrimaryConstructor? =
        body?.getStubOrPsiChild(CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun getPrimaryConstructorModifierList(): CjModifierList? = primaryConstructor?.modifierList


    override fun getPrimaryConstructorParameters(): List<CjParameter> {
        return getPrimaryConstructorParameterList()?.parameters.orEmpty()
    }

    fun getPrimaryConstructorParameterList(): CjParameterList? = primaryConstructor?.valueParameterList

    override fun getSecondaryConstructors(): List<CjSecondaryConstructor> = getBody()?.secondaryConstructors.orEmpty()
    override fun getPrimaryConstructors(): List<CjPrimaryConstructor> = getBody()?.primaryConstructors.orEmpty()
    override fun getEndSecondaryConstructors(): List<CjEndSecondaryConstructor> =
        getBody()?.endSecondaryConstructors.orEmpty()

    fun getConstructors(): List<CjConstructor<*>> =
        getSecondaryConstructors() + getPrimaryConstructors() + getEndSecondaryConstructors()

    fun getContextReceiverList(): CjContextReceiverList? = getStubOrPsiChild(CjStubElementTypes.CONTEXT_RECEIVER_LIST)

    override fun getContextReceivers(): List<CjContextReceiver> =
        getContextReceiverList()?.let { return it.contextReceivers() } ?: emptyList()

    override fun getBody(): CjClassBody? = getStubOrPsiChild(CjStubElementTypes.CLASS_BODY)

    override fun getClassId(): ClassId? {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

    fun isExtend(): Boolean {
        return this is CjExtend

    }
    fun isStruct(): Boolean {
        return this is CjStruct

    }
    fun isInterface(): Boolean {
        return this is CjInterface

    }
    fun isSealed(): Boolean = hasModifier(CjTokens.SEALED_KEYWORD)

    fun isEnum(): Boolean {
        return this is CjEnum

    }

}
