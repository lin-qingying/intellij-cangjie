/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.contracts.description

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
