package com.huawei.cangjie1.psi

import com.huawei.cangjie1.lexer.CjTokens
import com.huawei.cangjie1.name.ClassId
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
        get() = TODO("Not yet implemented")

    override fun isLocal(): Boolean {

        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return node.elementType.toString() + " : $name"
    }


    override fun getSuperTypeListEntries(): MutableList<CjSuperTypeListEntry> {
        TODO("Not yet implemented")
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

    override fun getBody(): CjClassBody? {
        TODO("Not yet implemented")
    }

    override fun getClassId(): ClassId? {
        TODO("Not yet implemented")
    }
}
