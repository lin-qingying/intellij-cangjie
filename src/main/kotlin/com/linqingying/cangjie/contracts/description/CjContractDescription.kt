package com.linqingying.cangjie.contracts.description

interface CjContractDescriptionElement<Type, Diagnostic> {
    fun <R, D> accept(contractDescriptionVisitor: CjContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R

    val erroneous: Boolean
}

abstract class CjEffectDeclaration<Type, Diagnostic> : CjContractDescriptionElement<Type, Diagnostic> {
    override fun <R, D> accept(contractDescriptionVisitor: CjContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitEffectDeclaration(this, data)
}

interface CjBooleanExpression<Type, Diagnostic> : CjContractDescriptionElement<Type, Diagnostic> {
    override fun <R, D> accept(contractDescriptionVisitor: CjContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitBooleanExpression(this, data)
}
