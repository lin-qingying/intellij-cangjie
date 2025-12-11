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

package org.cangnova.cangjie.resolve.caches

import org.cangnova.cangjie.descriptors.BindingTrace
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.calls.checkers.CallChecker
import org.cangnova.cangjie.resolve.calls.checkers.CallCheckerContext
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.util.*
import com.intellij.psi.PsiElement

class PrimitiveNumericComparisonInfo(
    val comparisonType: CangJieType,
    val leftPrimitiveType: CangJieType,
    val rightPrimitiveType: CangJieType,
    val leftType: CangJieType,
    val rightType: CangJieType
)

object PrimitiveNumericComparisonCallChecker : CallChecker {
    private fun CangJieType.promoteIntegerTypeToIntIfRequired() =
        when {
            !isPrimitiveNumberType() -> throw AssertionError("Primitive number type expected: $this")
            isInt8() || isInt16() -> builtIns.int32Type
            else -> this
        }

    private fun leastCommonPrimitiveNumericType(t1: CangJieType, t2: CangJieType): CangJieType {
        val pt1 = t1.promoteIntegerTypeToIntIfRequired()
        val pt2 = t2.promoteIntegerTypeToIntIfRequired()

        return when {
            pt1.isFloat64() || pt2.isFloat64() -> t1.builtIns.float64Type
            pt1.isFloat32() || pt2.isFloat32() -> t1.builtIns.float32Type
            pt1.isFloat16() || pt2.isFloat16() -> t1.builtIns.float16Type

            pt1.isInt64() || pt2.isInt64() -> t1.builtIns.int64Type
            pt1.isInt32() || pt2.isInt32() -> t1.builtIns.int32Type
            else -> throw AssertionError("Unexpected types: t1=$t1, t2=$t2")
        }
    }

    private fun CangJieType.getPrimitiveTypeOrSupertype(): CangJieType? =
        when {
            constructor.declarationDescriptor is TypeParameterDescriptor ->
                immediateSupertypes().firstNotNullOfOrNull {
                    it.getPrimitiveTypeOrSupertype()
                }

            isPrimitiveNumber() ->
                this

            else ->
                null
        }

    private fun List<CangJieType>.findPrimitiveOrNullablePrimitiveType() =
        firstNotNullOfOrNull { it.getPrimitiveTypeOrSupertype() }

    fun inferPrimitiveNumericComparisonType(
        trace: BindingTrace,
        leftTypes: List<CangJieType>,
        rightTypes: List<CangJieType>,
        comparison: CjExpression
    ) {
        val leftPrimitiveOrNullableType = leftTypes.findPrimitiveOrNullablePrimitiveType() ?: return
        val rightPrimitiveOrNullableType = rightTypes.findPrimitiveOrNullablePrimitiveType() ?: return
        val leftPrimitiveType = leftPrimitiveOrNullableType.makeNotNullable()
        val rightPrimitiveType = rightPrimitiveOrNullableType.makeNotNullable()
        val leastCommonType = leastCommonPrimitiveNumericType(leftPrimitiveType, rightPrimitiveType)

        trace.record(
            BindingContext.PRIMITIVE_NUMERIC_COMPARISON_INFO,
            comparison,
            PrimitiveNumericComparisonInfo(
                leastCommonType,
                leftPrimitiveType, rightPrimitiveType,
                leftPrimitiveOrNullableType, rightPrimitiveOrNullableType
            )
        )
    }

    private fun ResolvedCall<*>.isStandardComparison(): Boolean =
        extensionReceiver == null &&
                dispatchReceiver != null
//                &&  CangJieBuiltIns.isUnderCangJiePackage(resultingDescriptor)

    private val comparisonOperatorTokens =
        setOf(CjTokens.EQEQ, CjTokens.EXCLEQ, CjTokens.LT, CjTokens.LTEQ, CjTokens.GT, CjTokens.GTEQ)

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        // Primitive number comparisons only take part in binary operator convention resolution
        val binaryExpression = resolvedCall.call.callElement as? CjBinaryExpression ?: return
        if (!comparisonOperatorTokens.contains(binaryExpression.operationReference.referencedNameElementType)) return

        if (!resolvedCall.isStandardComparison()) return

        val leftExpr = binaryExpression.left ?: return
        val rightExpr = binaryExpression.right ?: return

        val leftTypes = context.getStableTypesForExpression(leftExpr)
        val rightTypes = context.getStableTypesForExpression(rightExpr)

        inferPrimitiveNumericComparisonType(context.trace, leftTypes, rightTypes, binaryExpression)
    }

    private fun CallCheckerContext.getStableTypesForExpression(expression: CjExpression): List<CangJieType> {
        val type = trace.bindingContext.getType(expression) ?: return emptyList()
        val dataFlowValue = dataFlowValueFactory.createDataFlowValue(
            expression, type, trace.bindingContext, resolutionContext.scope.ownerDescriptor
        )
        val dataFlowInfo =
            trace.get(BindingContext.EXPRESSION_TYPE_INFO, expression)?.dataFlowInfo ?: return emptyList()
        val stableTypes = dataFlowInfo.getStableTypes(dataFlowValue, languageVersionSettings)
        return listOf(type) + stableTypes
    }
}
