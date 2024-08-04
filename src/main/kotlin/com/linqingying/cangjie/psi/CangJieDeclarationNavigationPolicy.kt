package com.linqingying.cangjie.psi


interface CangJieDeclarationNavigationPolicy {
    fun getOriginalElement(declaration: CjDeclaration): CjElement
    fun getNavigationElement(declaration: CjDeclaration): CjElement
}
