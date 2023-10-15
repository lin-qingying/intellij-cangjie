package com.huawei.cangjie.lang.core.psi.ext


import com.huawei.cangjie.lang.core.completion.getOriginalOrSelf
import com.huawei.cangjie.lang.core.psi.CjElementTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.CompositePsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NotNull

interface CjElement : PsiElement, UserDataHolderEx {
    /**
     * 在此文件*中找到父模块*。参见[CjMo.Super]
     */
    val containingMod: CjMod
        get() = contextStrict<CjMod>()?.getOriginalOrSelf()
            ?: error("Element outside of module: $text")

    val crateRoot: CjMod?
        get() = containingCjFileSkippingCodeFragments?.crateRoot
}

//abstract class CjElementImpl(type: IElementType) : CompositePsiElement(type), CjElement {
//
//
//
//}
//
//
//abstract  class  CjFunctionImplMixin : CangJieFunction, CjModificationTrackerOwner {
//
//}

//interface CjElement : NavigatablePsiElement {
////    fun getTokenType(): IElementType?
//
//
//}


//abstract class CjElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CjElement {
//
//
////    override fun getTokenType(): IElementType? = node.elementType
//
//
//}
abstract class CjElementImpl(type: IElementType) :   CompositePsiElement(type), CjElement {

    override fun getNavigationElement(): PsiElement {
        return  super.getNavigationElement()
    }

    override fun toString(): String = "${javaClass.simpleName}($elementType)"


    override fun getName(): String? {
        return super.getName()
    }
}


abstract class CjStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>, CjElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)





    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}
