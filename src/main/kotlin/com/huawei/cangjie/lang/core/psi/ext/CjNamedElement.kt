package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.ide.presentation.getPresentation

import com.huawei.cangjie.lang.core.psi.CjElementTypes.IDENTIFIER
import com.huawei.cangjie.lang.core.psi.CjPsiFactory
import com.huawei.cangjie.lang.core.stubs.CjNamedStub
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType



interface CjNamedElement : CjElement, PsiNamedElement, NavigatablePsiElement {

}

abstract class CjStubbedNamedElementImpl<StubT> : CjStubbedElementImpl<StubT>,
    CjNameIdentifierOwner
        where StubT : CjNamedStub, StubT : StubElement<*> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement? = findChildByType(IDENTIFIER)

    override fun getName(): String? {
        val stub = greenStub
        return if (stub !== null) stub.name else nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement? {
        CjPsiFactory(project).createIdentifier(name)?.let { nameIdentifier?.replace(it) }
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    override fun getPresentation(): ItemPresentation = getPresentation(this)
}

//abstract class CjNamedElementImpl(node: ASTNode) :CjPsiCompositeElementImpl(node), CjNamedElement {
//
//    //TODO 放到PSI工厂中
//    override val id: CjId?
//        get() = PsiTreeUtil.getChildOfType(this, CjId::class.java)
//
//
//    override fun setName(name: String): PsiElement {
//
//        println(name)
//
//        return this
//    }
//
//
//
//}

open class CjPsiCompositeElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CjElement {
    override fun toString(): String = node.elementType.toString()


}


interface CjNameIdentifierOwner : CjNamedElement, PsiNameIdentifierOwner


abstract class CjNamedElementImpl(type: IElementType) : CjElementImpl(type), CjNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = findChildByType(IDENTIFIER)?.psi

    override fun getName(): String? = nameIdentifier?.text




    override fun setName(name: String): PsiElement {


        println("名字："+name)

        CjPsiFactory(project).createIdentifier(name)?.let { nameIdentifier?.replace(it) }



        return this
    }
}
