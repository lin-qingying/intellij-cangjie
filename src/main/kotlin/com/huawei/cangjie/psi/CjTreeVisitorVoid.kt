package com.huawei.cangjie.psi

import com.intellij.psi.PsiElement



open class CjTreeVisitorVoid : CjVisitorVoid() {
    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }
}

