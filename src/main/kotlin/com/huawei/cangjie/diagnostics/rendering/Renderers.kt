package com.huawei.cangjie.diagnostics.rendering

import com.intellij.psi.PsiElement

object Renderers {


    @JvmField
    val ELEMENT_TEXT = Renderer<PsiElement> { it.text }
}