package com.huawei.cangjie.psi

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.psi.psiUtil.ClassIdCalculator
import com.huawei.cangjie.psi.stubs.CangJieClassOrStructStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType


abstract class CjClassOrStruct :
    CjTypeParameterListOwnerStub<CangJieClassOrStructStub<out CjClassOrStruct>>, CjDeclarationContainer,
    CjNamedDeclaration,
    CjPureClassOrStruct, CjClassLikeDeclaration {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassOrStructStub<out CjClassOrStruct>, nodeType: IStubElementType<*, *>) : super(
        stub,
        nodeType
    )
    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()

    override fun isLocal(): Boolean  = stub?.isLocal() ?: CjPsiUtil.isLocal(this)
    override val declarations: List<CjDeclaration>
        get() =  getBody()?.declarations.orEmpty()

//    fun isTopLevel(): Boolean = stub?.isTopLevel() ?: (parent is CjFile)

    override fun toString(): String {
        return node.elementType.toString()
    }
    fun getSuperTypeList(): CjSuperTypeList? = getStubOrPsiChild(CjStubElementTypes.SUPER_TYPE_LIST)



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
