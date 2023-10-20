package com.huawei.cangjie.lang.core.resolve.ref

import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.intellij.psi.PsiPolyVariantReference

interface CjReference : PsiPolyVariantReference {

    override fun getElement(): CjElement

    override fun resolve(): CjElement?

    fun multiResolve(): List<CjElement>
}


