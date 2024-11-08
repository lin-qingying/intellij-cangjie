package com.linqingying.cangjie.psi

import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.psiUtil.ClassIdCalculator
import com.linqingying.cangjie.psi.stubs.CangJieClassStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes.CLASS_BODY

open class CjClass : CjTypeStatement {
//    fun isInterface(): Boolean =
//        _stub?.isInterface() ?: (findChildByType<PsiElement>(CjTokens.INTERFACE_KEYWORD) != null)

    private val _stub: CangJieClassStub?
        get() = stub as? CangJieClassStub

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassStub) : super(stub, CjStubElementTypes.CLASS)

    override val typeName: String
        get() = "class"
    override fun isLocal(): Boolean  = stub?.isLocal() ?: CjPsiUtil.isLocal(this)
    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> = getSuperTypeList()?.entries.orEmpty()

    override fun toString(): String {
        return node.elementType.toString() + " : $name"
    }


    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitClass(this, data)
    }



    override fun getBody(): CjAbstractClassBody?  =  getStubOrPsiChild( CLASS_BODY)

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
