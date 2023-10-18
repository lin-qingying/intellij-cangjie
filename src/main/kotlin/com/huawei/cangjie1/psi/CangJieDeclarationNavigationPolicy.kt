package com.huawei.cangjie1.psi

import com.huawei.cangjie1.idea.navigation.SourceNavigationHelper


interface CangJieDeclarationNavigationPolicy {
    fun getOriginalElement(declaration: CjDeclaration): CjElement
    fun getNavigationElement(declaration: CjDeclaration): CjElement
}
//class CangJieDeclarationNavigationPolicyImpl : CangJieDeclarationNavigationPolicy {
//    override fun getOriginalElement(declaration: CjDeclaration) = SourceNavigationHelper.getOriginalElement(declaration)
//
//    override fun getNavigationElement(declaration: CjDeclaration) = SourceNavigationHelper.getNavigationElement(declaration)
//}
