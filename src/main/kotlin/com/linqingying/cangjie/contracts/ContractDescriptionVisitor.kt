package com.linqingying.cangjie.contracts

interface ContractDescriptionVisitor<out R, in D>{
    fun visitEffectDeclaration(effectDeclaration: EffectDeclaration, data: D): R = visitContractDescriptionElement(effectDeclaration, data)
    fun visitContractDescriptionElement(contractDescriptionElement: ContractDescriptionElement, data: D): R {
        throw IllegalStateException("Top of hierarchy reached, no overloads were found for element: $contractDescriptionElement")
    }
}
