package com.huawei.cangjie.psi

import com.huawei.cangjie.ide.navigation.SourceNavigationHelper


interface CangJieDeclarationNavigationPolicy {
    fun getOriginalElement(declaration: CjDeclaration): CjElement
    fun getNavigationElement(declaration: CjDeclaration): CjElement
}
class CangJieDeclarationNavigationPolicyImpl : CangJieDeclarationNavigationPolicy {
    override fun getOriginalElement(declaration: CjDeclaration) = SourceNavigationHelper.getOriginalElement(declaration)

    override fun getNavigationElement(declaration: CjDeclaration) = SourceNavigationHelper.getNavigationElement(declaration)
}
