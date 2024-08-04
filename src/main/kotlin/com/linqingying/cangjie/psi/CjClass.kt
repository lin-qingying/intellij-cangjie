package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.psiUtil.ClassIdCalculator
import com.linqingying.cangjie.psi.stubs.CangJieClassStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

open class CjClass : CjClassOrStruct {
//    fun isInterface(): Boolean =
//        _stub?.isInterface() ?: (findChildByType<PsiElement>(CjTokens.INTERFACE_KEYWORD) != null)

    private val _stub: CangJieClassStub?
        get() = stub as? CangJieClassStub

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassStub) : super(stub, CjStubElementTypes.CLASS)

    override val declarations: List<CjDeclaration>
        get() =  getBody()?.declarations.orEmpty()

    override fun isLocal(): Boolean  = stub?.isLocal() ?: CjPsiUtil.isLocal(this)
    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()

    override fun toString(): String {
        return node.elementType.toString() + " : $name"
    }




    override fun hasExplicitPrimaryConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasPrimaryConstructor(): Boolean {
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
