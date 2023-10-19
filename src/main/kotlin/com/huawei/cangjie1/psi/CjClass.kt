package com.huawei.cangjie1.psi

import com.huawei.cangjie1.lexer.CjTokens
import com.huawei.cangjie1.name.ClassId
import com.huawei.cangjie1.psi.psiUtil.ClassIdCalculator
import com.huawei.cangjie1.psi.stubs.CangJieClassStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

open class CjClass : CjClassOrObject {
    fun isInterface(): Boolean =
        _stub?.isInterface() ?: (findChildByType<PsiElement>(CjTokens.INTERFACE_KEYWORD) != null)

    private val _stub: CangJieClassStub?
        get() = stub as? CangJieClassStub

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassStub) : super(stub, CjStubElementTypes.CLASS)

    override val declarations: List<CjDeclaration>
        get() =  getBody()?.declarations.orEmpty()

    override fun isLocal(): Boolean  = stub?.isLocal() ?: CjPsiUtil.isLocal(this)
    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()
    fun getSuperTypeList(): CjSuperTypeList? = getStubOrPsiChild(CjStubElementTypes.SUPER_TYPE_LIST)

    override fun toString(): String {
        return node.elementType.toString() + " : $name"
    }




    override fun hasExplicitPrimaryConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasPrimaryConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPrimaryConstructor(): CjPrimaryConstructor? {
        TODO("Not yet implemented")
    }

    override fun getPrimaryConstructorModifierList(): CjModifierList? {
        TODO("Not yet implemented")
    }

    override fun getPrimaryConstructorParameters(): MutableList<CjParameter> {
        TODO("Not yet implemented")
    }

    override fun getSecondaryConstructors(): MutableList<CjSecondaryConstructor> {
        TODO("Not yet implemented")
    }

    override fun getContextReceivers(): MutableList<CjContextReceiver> {
        TODO("Not yet implemented")
    }

    override fun getBody(): CjClassBody?  = getStubOrPsiChild(CjStubElementTypes.CLASS_BODY)

    override fun getClassId(): ClassId?  {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

}
