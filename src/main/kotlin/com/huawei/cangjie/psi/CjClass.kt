package com.huawei.cangjie.psi

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.psi.psiUtil.ClassIdCalculator
import com.huawei.cangjie.psi.stubs.CangJieClassStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

open class CjClass : CjTypeStatement {
//    fun isInterface(): Boolean =
//        _stub?.isInterface() ?: (findChildByType<PsiElement>(CjTokens.INTERFACE_KEYWORD) != null)

    private val _stub: CangJieClassStub?
        get() = stub as? CangJieClassStub

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassStub) : super(stub, CjStubElementTypes.CLASS)

    val variables :List<CjVariable> = body?.variables.orEmpty()
    val properties :List<CjProperty> = body?.properties.orEmpty()

    override fun isLocal(): Boolean  = stub?.isLocal() ?: CjPsiUtil.isLocal(this)
    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()

    override fun toString(): String {
        return node.elementType.toString() + " : $name"
    }


    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitClass(this, data)
    }






    override fun getPrimaryConstructorModifierList(): CjModifierList? {
        TODO("Not yet implemented")
    }





    override fun getBody(): CjClassBody?  = getStubOrPsiChild(CjStubElementTypes.CLASS_BODY)

    override fun getClassId(): ClassId?  {
        stub?.let { return it.getClassId() }
        return ClassIdCalculator.calculateClassId(this)
    }

}
fun CjClass.createPrimaryConstructorIfAbsent(): CjPrimaryConstructor {
    val constructor = primaryConstructor
    if (constructor != null) return constructor
    var anchor: PsiElement? = typeParameterList
    if (anchor == null) anchor = nameIdentifier
    if (anchor == null) anchor = lastChild
    return addAfter(CjPsiFactory(project).createPrimaryConstructor(), anchor) as CjPrimaryConstructor
}

fun CjClass.createPrimaryConstructorParameterListIfAbsent(): CjParameterList {
    val constructor = createPrimaryConstructorIfAbsent()
    val parameterList = constructor.valueParameterList
    if (parameterList != null) return parameterList
    return constructor.add(CjPsiFactory(project).createParameterList("()")) as CjParameterList
}
