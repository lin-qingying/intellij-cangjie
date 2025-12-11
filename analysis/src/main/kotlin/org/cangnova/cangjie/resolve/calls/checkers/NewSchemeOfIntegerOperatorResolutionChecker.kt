/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.checkers


import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjUnaryExpression
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.constants.ErrorValue
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.resolve.constants.TypedCompileTimeConstant
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.lowerIfFlexible
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.isPrimitiveNumber

object NewSchemeOfIntegerOperatorResolutionChecker : CallChecker {

    fun needToCheck(expectedType: CangJieType): Boolean {
        if (TypeUtils.noExpectedType(expectedType)) return false
        return expectedType.lowerIfFlexible().isPrimitiveNumber()
    }

    private fun checkArgumentImpl(
        expectedType: SimpleType,
        argumentExpression: CjExpression,
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ) {
        val bindingContext = trace.bindingContext
        val callForArgument = argumentExpression.getResolvedCall(bindingContext) ?: return
//        if (!callForArgument.isIntOperator()) return
        val callElement = callForArgument.call.callElement as? CjExpression ?: return
        val deparenthesizedElement = CjPsiUtil.deparenthesize(callElement)!!
        if (deparenthesizedElement is CjConstantExpression) return
        if (deparenthesizedElement is CjUnaryExpression) {
            val token = deparenthesizedElement.operationToken
            if (token == CjTokens.PLUS || token == CjTokens.MINUS) return
        }

        val compileTimeValue = bindingContext[BindingContext.COMPILE_TIME_VALUE, argumentExpression] ?: return

        val newExpressionType = when (compileTimeValue) {
            is IntegerValueTypeConstant -> {
                val currentExpressionType = compileTimeValue.unknownIntegerType
                val valueTypeConstructor = currentExpressionType.constructor as? IntegerLiteralTypeConstructor ?: return
                valueTypeConstructor.getApproximatedType()
            }

            is TypedCompileTimeConstant<*> -> {
                val typeFromCall = callForArgument.resultingDescriptor.returnType?.lowerIfFlexible()
                if (typeFromCall != null) {
                    typeFromCall
                } else {
                    val constantValue = compileTimeValue.constantValue
                    if (constantValue is ErrorValue) return
                    // Values of all numeric constants are held in Long value
                    val value = constantValue.value as? Long ?: return
                    IntegerLiteralTypeConstructor(
                        value,
                        moduleDescriptor,
                        compileTimeValue.parameters
                    ).getApproximatedType()
                }
            }

            else -> return
        }
//        if (newExpressionType.constructor != expectedType.constructor) {
//            val willBeConversion = newExpressionType.isInt() && expectedType.makeNonOption().isLong()
//            if (!willBeConversion) {
//                trace.report(Errors.INTEGER_OPERATOR_RESOLVE_WILL_CHANGE.on(argumentExpression, newExpressionType))
//            }
//        }
    }


    @JvmStatic
    fun checkArgument(
        expectedType: CangJieType,
        argument: CjExpression,
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (needToCheck(expectedType)) {
            checkArgumentImpl(
                expectedType.lowerIfFlexible(),
                CjPsiUtil.deparenthesize(argument)!!,
                trace,
                moduleDescriptor
            )
        }
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        for ((valueParameter, arguments) in resolvedCall.valueArguments) {
            val expectedType =/* if (valueParameter.isVararg) {
                valueParameter.varargElementType ?: continue
            } else {*/
                valueParameter.type
                    /* }*/.unwrap().lowerIfFlexible()
            if (!needToCheck(expectedType)) {
                continue
            }
            for (argument in arguments.arguments) {
                val expression = CjPsiUtil.deparenthesize(argument.getArgumentExpression()) ?: continue
                checkArgumentImpl(expectedType, expression, context.trace, context.moduleDescriptor)
            }
        }
    }
}
