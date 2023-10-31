package com.huawei.cangjie.doc.psi.impl

import com.huawei.cangjie.doc.psi.CDocElement
import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import org.jetbrains.annotations.NotNull

abstract class CDocElementImpl(node:ASTNode): ASTWrapperPsiElement(node), CDocElement {


    @NotNull
    override fun getLanguage(): Language = CangJieLanguage


    override fun toString(): String = node.elementType.toString()


}
