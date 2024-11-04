package com.linqingying.cangjie.contracts.description



abstract class CjContractDescriptionVisitor<out R, in D, Type, Diagnostic> {
    open fun visitContractDescriptionElement(contractDescriptionElement: CjContractDescriptionElement<Type, Diagnostic>, data: D): R {
        throw IllegalStateException("Top of hierarchy reached, no overloads were found for element: $contractDescriptionElement")
    }

    // Effects
    open fun visitEffectDeclaration(effectDeclaration: CjEffectDeclaration<Type, Diagnostic>, data: D): R = visitContractDescriptionElement(effectDeclaration, data)

//    open fun visitConditionalEffectDeclaration(conditionalEffect: CjConditionalEffectDeclaration<Type, Diagnostic>, data: D): R =
//        visitEffectDeclaration(conditionalEffect, data)
//
//    open fun visitReturnsEffectDeclaration(returnsEffect: CjReturnsEffectDeclaration<Type, Diagnostic>, data: D): R =
//        visitEffectDeclaration(returnsEffect, data)
//
//    open fun visitCallsEffectDeclaration(callsEffect: CjCallsEffectDeclaration<Type, Diagnostic>, data: D): R =
//        visitEffectDeclaration(callsEffect, data)
//
//    open fun visitErroneousCallsEffectDeclaration(callsEffect: CjErroneousCallsEffectDeclaration<Type, Diagnostic>, data: D): R =
//        visitCallsEffectDeclaration(callsEffect, data)

    // Expressions
    open fun visitBooleanExpression(booleanExpression: CjBooleanExpression<Type, Diagnostic>, data: D): R =
        visitContractDescriptionElement(booleanExpression, data)

//    open fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: CjBinaryLogicExpression<Type, Diagnostic>, data: D): R =
//        visitBooleanExpression(binaryLogicExpression, data)
//
//    open fun visitLogicalNot(logicalNot: CjLogicalNot<Type, Diagnostic>, data: D): R = visitBooleanExpression(logicalNot, data)
//
//    open fun visitIsInstancePredicate(isInstancePredicate: CjIsInstancePredicate<Type, Diagnostic>, data: D): R =
//        visitBooleanExpression(isInstancePredicate, data)
//
//    open fun visitErroneousIsInstancePredicate(isInstancePredicate: CjErroneousIsInstancePredicate<Type, Diagnostic>, data: D): R =
//        visitIsInstancePredicate(isInstancePredicate, data)
//
//    open fun visitIsNullPredicate(isNullPredicate: CjIsNullPredicate<Type, Diagnostic>, data: D): R = visitBooleanExpression(isNullPredicate, data)
//
//    // Values
//    open fun visitValue(value: CjContractDescriptionValue<Type, Diagnostic>, data: D): R = visitContractDescriptionElement(value, data)
//
//    open fun visitConstantDescriptor(constantReference: CjConstantReference<Type, Diagnostic>, data: D): R = visitValue(constantReference, data)
//
//    open fun visitBooleanConstantDescriptor(booleanConstantDescriptor: CjBooleanConstantReference<Type, Diagnostic>, data: D): R =
//        visitConstantDescriptor(booleanConstantDescriptor, data)
//
//    open fun visitErroneousConstantReference(erroneousConstantReference: CjErroneousConstantReference<Type, Diagnostic>, data: D): R =
//        visitConstantDescriptor(erroneousConstantReference, data)
//
//    open fun visitValueParameterReference(valueParameterReference: CjValueParameterReference<Type, Diagnostic>, data: D): R =
//        visitValue(valueParameterReference, data)
//
//    open fun visitBooleanValueParameterReference(booleanValueParameterReference: CjBooleanValueParameterReference<Type, Diagnostic>, data: D): R =
//        visitValueParameterReference(booleanValueParameterReference, data)
//
//    open fun visitErroneousValueParameterReference(valueParameterReference: CjErroneousValueParameterReference<Type, Diagnostic>, data: D): R =
//        visitValueParameterReference(valueParameterReference, data)
//
//    // Error
//    open fun visitErroneousElement(element: CjErroneousContractElement<Type, Diagnostic>, data: D): R =
//        visitContractDescriptionElement(element, data)
}
