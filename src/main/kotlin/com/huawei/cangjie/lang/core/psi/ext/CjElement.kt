package com.huawei.cangjie.lang.core.psi.ext

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NotNull

//interface CjElement : PsiElement, UserDataHolderEx {
//    /**
//     * 在此文件*中找到父模块*。参见[CjMo.Super]
////     */
//    val containingMod: CjMod
//        get() = contextStrict<CjMod>()?.getOriginalOrSelf()
//            ?: error("Element outside of module: $text")
//
////    val crateRoot: CjMod?
////        get() = containingCjFileSkippingCodeFragments?.crateRoot
//}

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

interface CjElement : NavigatablePsiElement {
    fun getTokenType(): IElementType?
}


abstract class CjElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CjElement {


    override fun getTokenType(): IElementType? = node.elementType


}


