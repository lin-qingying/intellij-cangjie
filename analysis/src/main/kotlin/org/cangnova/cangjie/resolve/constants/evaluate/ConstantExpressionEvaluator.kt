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

package org.cangnova.cangjie.resolve.constants.evaluate


import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.UnsignedTypes
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.diagnostics.infos.warnings.FLOAT_LITERAL_CONFORMS_ZERO
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.parsing.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.StatementFilter
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.constants.*
import org.cangnova.cangjie.types.BasicType
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.types.isGenericArrayOfTypeParameter
import org.cangnova.cangjie.types.isSubtypeOf
import java.math.BigInteger


class ConstantExpressionEvaluator(
    internal val module: ModuleDescriptor,
    internal val languageVersionSettings: LanguageVersionSettings,
    project: Project,
//    internal val inlineConstTracker: InlineConstTracker = InlineConstTracker.DoNothing
) {

    companion object {
        @JvmStatic
        fun getPossiblyErrorConstant(
            expression: CjExpression,
            bindingContext: BindingContext
        ): CompileTimeConstant<*>? {
            return bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression)
        }

        @JvmStatic
        fun getConstant(expression: CjExpression, bindingContext: BindingContext): CompileTimeConstant<*>? {
            val constant = getPossiblyErrorConstant(expression, bindingContext) ?: return null
            return if (!constant.isError) constant else null
        }
    }

    private fun checkExperimentalityOfConstantLiteral(
        expression: CjExpression,
        constant: CompileTimeConstant<*>,
        expectedType: CangJieType?,
        trace: BindingTrace
    ) {
        if (constant.isError) return
        if (!constant.parameters.isUnsignedNumberLiteral && !constant.parameters.isUnsignedLongNumberLiteral) return
//
        val constantType = when {
            constant is TypedCompileTimeConstant<*> -> constant.type
            expectedType != null -> constant.toConstantValue(expectedType).getType(module)
            else -> return
        }

        if (!UnsignedTypes.isUnsignedType(constantType)) return

//        with(OptInUsageChecker) {
//            val descriptor = constantType.constructor.declarationDescriptor ?: return
//            val optInDescriptions = descriptor.loadOptIns(moduleAnnotationsResolver, trace.bindingContext, languageVersionSettings)
//
//            reportNotAllowedOptIns(
//                optInDescriptions, expression, languageVersionSettings, trace, EXPERIMENTAL_UNSIGNED_LITERALS_DIAGNOSTICS
//            )
//        }
    }

    fun evaluateToConstantValue(
        expression: CjExpression,
        trace: BindingTrace,
        expectedType: CangJieType
    ): ConstantValue<*>? {
        return evaluateExpression(expression, trace, expectedType)?.toConstantValue(expectedType)
    }

    fun evaluateExpression(
        expression: CjExpression,
        trace: BindingTrace,
        expectedType: CangJieType? = TypeUtils.NO_EXPECTED_TYPE
    ): CompileTimeConstant<*>? {
        val visitor = ConstantExpressionEvaluatorVisitor(this, trace)
        val constant = visitor.evaluate(expression, expectedType) ?: return null

        checkExperimentalityOfConstantLiteral(expression, constant, expectedType, trace)

        return if (!constant.isError) constant else null
    }

    fun updateNumberType(
        numberType: CangJieType,
        expression: CjExpression?,
        statementFilter: StatementFilter,
        trace: BindingTrace
    ) {
        if (expression == null) return
        BindingContextUtils.updateRecordedType(numberType, expression, trace, false)

        if (expression !is CjConstantExpression) {
            val deparenthesized = CjPsiUtil.getLastElementDeparenthesized(expression, statementFilter)
            if (deparenthesized !== expression) {
                updateNumberType(numberType, deparenthesized, statementFilter, trace)
            }
            return
        }

        evaluateExpression(expression, trace, numberType)
    }
//
//
//    fun evaluateExpression(
//        expression: CjExpression,
//        trace: BindingTrace,
//        expectedType: CangJieType? = TypeUtils.NO_EXPECTED_TYPE
//    ): CompileTimeConstant<*>? {
//        val visitor = ConstantExpressionEvaluatorVisitor(this, trace)
//        val constant = visitor.evaluate(expression, expectedType) ?: return null
//
//        checkExperimentalityOfConstantLiteral(expression, constant, expectedType, trace)
//
//        return if (!constant.isError) constant else null
//    }
}


private class ConstantExpressionEvaluatorVisitor(
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val trace: BindingTrace
) : CjVisitor<CompileTimeConstant<*>, CangJieType?>() {
    private val languageVersionSettings = constantExpressionEvaluator.languageVersionSettings
    private val builtIns = constantExpressionEvaluator.module.builtIns


    fun evaluate(expression: CjExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
        val recordedCompileTimeConstant =
            ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, trace.bindingContext)
        if (recordedCompileTimeConstant != null) {
            return recordedCompileTimeConstant
        }

        val compileTimeConstant = expression.accept(this, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        if (compileTimeConstant != null) {
//            if (shouldSkipComplexBooleanValue(expression, compileTimeConstant)) {
//                return null
//            }

            // If constant is `Array` and its argument is some generic type, then we must wait for full resolve and only when we can record value
            if (compileTimeConstant is TypedCompileTimeConstant && compileTimeConstant.type.isGenericArrayOfTypeParameter()) {
                return compileTimeConstant
            }

            trace.record(BindingContext.COMPILE_TIME_VALUE, expression, compileTimeConstant)
            return compileTimeConstant
        }
        return null
    }


    override fun visitConstantExpression(
        expression: CjConstantExpression,
        expectedType: CangJieType?
    ): CompileTimeConstant<*>? {
        val text = expression.text ?: return null
        val nodeElementType = expression.node.elementType

        var result: Any = when (nodeElementType) {
            CjNodeTypes.INTEGER_CONSTANT, CjNodeTypes.FLOAT_CONSTANT -> parseNumericLiteral(text, nodeElementType)
            CjNodeTypes.BOOLEAN_CONSTANT -> parseBoolean(text)
            CjNodeTypes.RUNE_CONSTANT -> CompileTimeConstantChecker.parseRune(expression)
            CjNodeTypes.UNIT_CONSTANT -> Unit
            else -> throw IllegalArgumentException("Unsupported constant: $expression")
        } ?: return null
// 如果没有后缀，进行转换
        if (!hasFloatSuffix(text) && nodeElementType == CjNodeTypes.FLOAT_CONSTANT) {
            if (expectedType is BasicType && result is Number) {
                when (expectedType.typeName.toString()) {
//                    "Float64" -> result = result.toDouble()
//                    "Float32" -> result = result.toFloat()
                    "Float16" -> result = result.toFloat16()
                }
            }
        }
//        多写一步没什么不好，稳定
//        if (!hasIntegerSuffix(text) && nodeElementType == CjNodeTypes.INTEGER_CONSTANT) {
//            if (expectedType is BasicType && result is Number) {
//                when (expectedType.typeName.toString()) {
//                    "Int8", "Uint8" -> result = result.toByte()
//                    "Int16", "Uint16" -> result = result.toFloat()
//                    "Int32", "Uint32" -> result = result.toInt()
//                    "Int64", "Uint64" -> result = result.toLong()
//
//
////               "Float16" -> result = result.toFloat16()
//                }
//            }
//        }
        if (result is Double) {
            if (result.isInfinite()) {
                trace.report(FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
            }
            if (result == 0.0 && !TypeConversionUtil.isFPZero(text)) {
                trace.report( FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
            }
        }
//
        if (result is Float) {
            if (result.isInfinite()) {
                trace.report( FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
            }
            if (result == 0.0f && !TypeConversionUtil.isFPZero(text)) {
                trace.report( FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
            }
        }

        if (result is Float16) {
            if (result.isInfinite()) {
                trace.report( FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
            }
            if (result.toFloat() == 0.0f && !TypeConversionUtil.isFPZero(text)) {
                trace.report( FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
            }
        }

        val isIntegerConstant = nodeElementType == CjNodeTypes.INTEGER_CONSTANT
        val isUint64 =
            isIntegerConstant && hasUnsignedInt64Suffix(text) || expectedType?.let { CangJieBuiltIns.isUInt64(it) } == true
        val isUnsigned =
            isUint64 || hasUnsignedSuffix(text) || expectedType?.let { CangJieBuiltIns.isUnsignedNumber(it) } == true
        val isTyped = isUnsigned || hasIntegerSuffix(text) || hasFloatSuffix(text)
//
        return createConstant(
            result,
            expectedType,
            CompileTimeConstant.Parameters(
                canBeUsedInAnnotation = true,
                isPure = !isTyped,
                isUnsignedNumberLiteral = isUnsigned,
                isUnsignedLongNumberLiteral = isUint64,
                usesVariableAsConstant = false,
                usesNonConstValAsConstant = false,
                isConvertableConstVal = false
            )
        )


    }

    private fun createConstant(
        value: Any?,
        expectedType: CangJieType?,
        parameters: CompileTimeConstant.Parameters
    ): CompileTimeConstant<*>? {
        return if (parameters.isPure || parameters.isUnsignedNumberLiteral) {
            return createCompileTimeConstant(value, parameters, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        } else {
            ConstantValueFactory.createConstantValue(value)?.wrap(parameters)
        }
    }

    private fun createFloatCompileTimeConstant(
        value: Double,
        parameters: CompileTimeConstant.Parameters,
        expectedType: CangJieType
    ): CompileTimeConstant<*> {


        if (TypeUtils.noExpectedType(expectedType) || expectedType.isError) {
            return createFloatValueTypeConstant(
                value,
                constantExpressionEvaluator.module,
                parameters,
                languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
            )
        }
        val floatValue = ConstantValueFactory.createFloatConstantValue(
            value, expectedType
        )
        if (floatValue != null) {
            return floatValue.wrap(parameters)
        }

        return value.createSimpleFloatCompileTimeConst(parameters)
    }


    private fun createIntegerCompileTimeConstant(
        value: Long,
        parameters: CompileTimeConstant.Parameters,
        expectedType: CangJieType
    ): CompileTimeConstant<*> {


        if (parameters.isUnsignedLongNumberLiteral) {
            return UInt64Value(value).wrap(parameters)
        }

        if (TypeUtils.noExpectedType(expectedType) || expectedType.isError) {
            return createIntegerValueTypeConstant(
                value,
                constantExpressionEvaluator.module,
                parameters,
                languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
            )
        }
        val integerValue = ConstantValueFactory.createIntegerConstantValue(
            value, expectedType, parameters.isUnsignedNumberLiteral
        )
        if (integerValue != null) {
            return integerValue.wrap(parameters)
        }

        return value.createSimpleIntCompileTimeConst(parameters)
    }

    private fun Double.createSimpleFloatCompileTimeConst(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<*> {
        val value = this

        return when (value) {
            value -> Float64Value(value)
//                value.toInt().toLong() -> Int32Value(value.toInt())
//                value.toShort().toLong() -> Int16Value(value.toShort())
//                value.toByte().toLong() -> Int8Value(value.toByte())

            else -> Float64Value(value)
        }
            .wrap(parameters)

    }

    private fun Long.createSimpleIntCompileTimeConst(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<*> {
        val value = this
        return if (parameters.isUnsignedNumberLiteral) {
            when (value) {
                value.toInt().fromUInt32ToLong() -> UInt32Value(value.toInt())
                else -> UInt64Value(value)
            }
        } else {
            when (value) {
                value -> Int64Value(value)
//                value.toInt().toLong() -> Int32Value(value.toInt())
//                value.toShort().toLong() -> Int16Value(value.toShort())
//                value.toByte().toLong() -> Int8Value(value.toByte())

                else -> Int64Value(value)
            }
        }.wrap(parameters)

    }

    private class OperationArgument(val value: Any, val ctcType: CompileTimeType, val expression: CjExpression)

    private fun getCompileTimeType(c: CangJieType): CompileTimeType? =
        when (TypeUtils.makeNonOption(c)) {
            builtIns.int32Type -> CompileTimeType.Int32
            builtIns.int8Type -> CompileTimeType.Int8
            builtIns.int16Type -> CompileTimeType.Int16
            builtIns.int64Type -> CompileTimeType.Int64
            builtIns.float64Type -> CompileTimeType.Flout64
            builtIns.float32Type -> CompileTimeType.Flout32
            builtIns.float16Type -> CompileTimeType.Flout16

            builtIns.runeType -> CompileTimeType.Rune
            builtIns.boolType -> CompileTimeType.Bool
            builtIns.stringType -> CompileTimeType.String
            builtIns.anyType -> CompileTimeType.Any
            else -> null
        }

    private fun createOperationArgument(
        expression: CjExpression,
        parameterType: CangJieType,
        compileTimeType: CompileTimeType,
    ): OperationArgument? {
        val compileTimeConstant =
            constantExpressionEvaluator.evaluateExpression(expression, trace, parameterType) ?: return null
        if (compileTimeConstant is TypedCompileTimeConstant && !compileTimeConstant.type.isSubtypeOf(parameterType)) return null
        val constantValue = compileTimeConstant.toConstantValue(parameterType)
        val evaluationResult =
            (if (compileTimeType == CompileTimeType.Any) constantValue.boxedValue() else constantValue.value)
                ?: return null
        return OperationArgument(evaluationResult, compileTimeType, expression)
    }

    private fun createOperationArgumentForReceiver(
        resolvedCall: ResolvedCall<*>,
        expression: CjExpression
    ): OperationArgument? {
        val receiverExpressionType = getReceiverExpressionType(resolvedCall) ?: return null

        val receiverCompileTimeType = getCompileTimeType(receiverExpressionType) ?: return null

        return createOperationArgument(expression, receiverExpressionType, receiverCompileTimeType)
    }

    private fun usesVariableAsConstant(expression: CjExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.usesVariableAsConstant ?: false

    private fun usesNonConstValAsConstant(expression: CjExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.usesNonConstValAsConstant ?: false

    private fun canBeUsedInAnnotation(expression: CjExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.canBeUsedInAnnotations ?: false

    private fun isPureConstant(expression: CjExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.isPure ?: false

    private fun evaluateUnaryAndCheck(receiver: OperationArgument, name: String, callExpression: CjExpression): Any? {
        return evaluateUnaryAndCheck(name, receiver.ctcType, receiver.value) {
            trace.report( INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType() ?: callExpression))
        }
    }

    private fun isDivisionByZero(name: String, parameter: Any?): Boolean {
        return name in DIVISION_OPERATION_NAMES && isZero(parameter)
    }

    private fun evaluateBinaryAndCheck(
        receiver: OperationArgument,
        parameter: OperationArgument,
        name: String,
        callExpression: CjExpression
    ): Any? {
        return evaluateBinaryAndCheck(name, receiver.ctcType, receiver.value, parameter.ctcType, parameter.value) {
            trace.report( INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType() ?: callExpression))
        }
    }

    private fun createOperationArgumentForFirstParameter(
        argument: ResolvedValueArgument,
        parameter: ValueParameterDescriptor
    ): OperationArgument? {
        val argumentCompileTimeType = getCompileTimeType(parameter.type) ?: return null

        val arguments = argument.arguments
        if (arguments.size != 1) return null

        val argumentExpression = arguments.first().getArgumentExpression() ?: return null

        return createOperationArgument(argumentExpression, parameter.type, argumentCompileTimeType)
    }

    private fun evaluateCall(
        callExpression: CjExpression,
        receiverExpression: CjExpression,
        expectedType: CangJieType?
    ): CompileTimeConstant<*>? {
        val resolvedCall = callExpression.getResolvedCall(trace.bindingContext) ?: return null
//        if (!CangJieBuiltIns.isUnderCangJiePackage(resolvedCall.resultingDescriptor)) return null

        val resultingDescriptorName = resolvedCall.resultingDescriptor.name

        val argumentForReceiver = createOperationArgumentForReceiver(resolvedCall, receiverExpression) ?: return null
        if (isStandaloneOnlyConstant(argumentForReceiver.expression)) {
            return null
        }

        val argumentsEntrySet = resolvedCall.valueArguments.entries
        if (argumentsEntrySet.isEmpty()) {
            val result = evaluateUnaryAndCheck(argumentForReceiver, resultingDescriptorName.asString(), callExpression)
                ?: return null

            val isArgumentPure = isPureConstant(argumentForReceiver.expression)
            val canBeUsedInAnnotation = canBeUsedInAnnotation(argumentForReceiver.expression)
            val usesVariableAsConstant = usesVariableAsConstant(argumentForReceiver.expression)
            val usesNonConstValAsConstant = usesNonConstValAsConstant(argumentForReceiver.expression)
            val isNumberConversionMethod = resultingDescriptorName in OperatorConventions.NUMBER_CONVERSIONS
//            val isCharCode =
//                argumentForReceiver.ctcType == CompileTimeType.Rune && resultingDescriptorName == StandardNames.CHAR_CODE
            return createConstant(
                result,
                expectedType,
                CompileTimeConstant.Parameters(
                    canBeUsedInAnnotation,
                    !isNumberConversionMethod && /*!isCharCode &&*/ isArgumentPure,
                    isUnsignedNumberLiteral = false,
                    isUnsignedLongNumberLiteral = false,
                    usesVariableAsConstant,
                    usesNonConstValAsConstant,
                    isConvertableConstVal = false
                )
            )
        } else if (argumentsEntrySet.size == 1) {
            val (parameter, argument) = argumentsEntrySet.first()
            val argumentForParameter = createOperationArgumentForFirstParameter(argument, parameter) ?: return null
            if (isStandaloneOnlyConstant(argumentForParameter.expression)) {
                return null
            }

            if (isDivisionByZero(resultingDescriptorName.asString(), argumentForParameter.value)) {
                val parentExpression: CjExpression =
                    PsiTreeUtil.getParentOfType(receiverExpression, CjExpression::class.java)!!
                trace.report( DIVISION_BY_ZERO.on(parentExpression))

                if ((isIntegerType(argumentForReceiver.value) && isIntegerType(argumentForParameter.value)) /*||
                    !languageVersionSettings.supportsFeature(LanguageFeature.DivisionByZeroInConstantExpressions)*/
                ) {
                    return ErrorValue.create("Division by zero").wrap()
                }
            }

            val result = evaluateBinaryAndCheck(
                argumentForReceiver,
                argumentForParameter,
                resultingDescriptorName.asString(),
                callExpression
            ) ?: return null

            val areArgumentsPure =
                isPureConstant(argumentForReceiver.expression) && isPureConstant(argumentForParameter.expression)
            val canBeUsedInAnnotation =
                canBeUsedInAnnotation(argumentForReceiver.expression) && canBeUsedInAnnotation(argumentForParameter.expression)
            val usesVariableAsConstant =
                usesVariableAsConstant(argumentForReceiver.expression) || usesVariableAsConstant(argumentForParameter.expression)
            val usesNonConstValAsConstant =
                usesNonConstValAsConstant(argumentForReceiver.expression) || usesNonConstValAsConstant(
                    argumentForParameter.expression
                )
            val parameters = CompileTimeConstant.Parameters(
                canBeUsedInAnnotation,
                areArgumentsPure,
                isUnsignedNumberLiteral = false,
                isUnsignedLongNumberLiteral = false,
                usesVariableAsConstant,
                usesNonConstValAsConstant,
                isConvertableConstVal = false
            )
            return when (resultingDescriptorName) {
                OperatorNameConventions.COMPARE_LT,
                OperatorNameConventions.COMPARE_GT,
                OperatorNameConventions.COMPARE_GTEQ,
                OperatorNameConventions.COMPARE_LTEQ
                    -> createCompileTimeConstantForCompareTo(
                    result,
                    callExpression
                )?.wrap(parameters)

                OperatorNameConventions.EQUALS -> createCompileTimeConstantForEquals(result, callExpression)?.wrap(
                    parameters
                )

                else -> {
                    createConstant(
                        result,
                        expectedType,
                        parameters
                    )
                }
            }
        }

        return null
    }

    override fun visitUnaryExpression(
        expression: CjUnaryExpression,
        expectedType: CangJieType?
    ): CompileTimeConstant<*>? {
        val leftExpression = expression.baseExpression ?: return null
        return evaluateCall(
            expression.operationReference,
            leftExpression,
            expectedType
        )
    }

    private fun createCompileTimeConstant(
        value: Any?,
        parameters: CompileTimeConstant.Parameters,
        expectedType: CangJieType
    ): CompileTimeConstant<*>? {
        return when (value) {
            is Byte, is Short, is Int, is Long -> createIntegerCompileTimeConstant(
                (value as Number).toLong(),
                parameters,
                expectedType
            )

            is Float16, is Float32, is Float64 -> createFloatCompileTimeConstant(
                (value as Number).toDouble(),
                parameters,
                expectedType
            )

            else -> ConstantValueFactory.createConstantValue(value)?.wrap(parameters)
        }
    }

    private fun <T> ConstantValue<T>.wrap(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<T> =
        TypedCompileTimeConstant(this, constantExpressionEvaluator.module, parameters)

    private fun isStandaloneOnlyConstant(expression: CjExpression): Boolean {
        return ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.isStandaloneOnlyConstant()
            ?: return false
    }

    private val stringExpressionEvaluator = object : CjVisitor<TypedCompileTimeConstant<String>, Nothing?>() {
        private fun createStringConstant(compileTimeConstant: CompileTimeConstant<*>): TypedCompileTimeConstant<String>? {
            val constantValue = compileTimeConstant.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)
            if (constantValue.isStandaloneOnlyConstant()) {
                return null
            }
            return when (constantValue) {
                is ErrorValue/*, is EnumValue*/ -> return null

                else -> StringValue(constantValue.boxedValue().toString())
            }.wrap(compileTimeConstant.parameters)
        }

        @Suppress("RedundantNullableReturnType")
        fun evaluate(entry: CjStringTemplateEntry): TypedCompileTimeConstant<String>? {
            return entry.accept(this, null)
        }

        override fun visitStringTemplateEntryWithExpression(
            entry: CjStringTemplateEntryWithExpression,
            data: Nothing?
        ): TypedCompileTimeConstant<String>? {
            val expression = entry.expression ?: return null

            return evaluate(expression, builtIns.stringType)?.let {
                createStringConstant(it)
            }

        }

        override fun visitLiteralStringTemplateEntry(
            entry: CjLiteralStringTemplateEntry,
            data: Nothing?
        ): TypedCompileTimeConstant<String> =
            StringValue(entry.text).wrap()

        override fun visitEscapeStringTemplateEntry(
            entry: CjEscapeStringTemplateEntry,
            data: Nothing?
        ): TypedCompileTimeConstant<String> =
            StringValue(entry.unescapedValue).wrap()
    }

    private fun <T> ConstantValue<T>.wrap(
        canBeUsedInAnnotation: Boolean = true,
        isPure: Boolean = false,
        isUnsigned: Boolean = false,
        isUnsignedLong: Boolean = false,
        usesVariableAsConstant: Boolean = false,
        usesNonConstValAsConstant: Boolean = false,
        isConvertableConstVal: Boolean = false
    ): TypedCompileTimeConstant<T> =
        wrap(
            CompileTimeConstant.Parameters(
                canBeUsedInAnnotation,
                isPure,
                isUnsigned,
                isUnsignedLong,
                usesVariableAsConstant,
                usesNonConstValAsConstant,
                isConvertableConstVal
            )
        )

    override fun visitStringTemplateExpression(
        expression: CjStringTemplateExpression,
        expectedType: CangJieType?
    ): CompileTimeConstant<*>? {

        if (expression.isMultiLine) {
            if (!expression.stringContent.startsWith("\n")) {
                expression.findElementAt(3)?.let {
                    trace.report( NO_MULTILINE_NEWLINE.on(it))
                }
            }

            when (expression.isDoubleQuote) {
                true -> if (expression.stringContent.endsWith("\"")) {
                    trace.report( COMPILER_AFFECTED_SYNTAX_ERROR.on(expression.lastChild.prevSibling))
                }

                false -> if (expression.stringContent.endsWith("'")) {
                    trace.report( COMPILER_AFFECTED_SYNTAX_ERROR.on(expression.lastChild.prevSibling))
                }
            }

        }


        val sb = StringBuilder()
        var interupted = false
        val canBeUsedInAnnotation = true
        var usesVariableAsConstant = false
        var usesNonConstantVariableAsConstant = false
        for (entry in expression.entries) {
            val constant = stringExpressionEvaluator.evaluate(entry)
            if (constant == null) {
                interupted = true
                break
            } else {
//                if (!constant.canBeUsedInAnnotations) canBeUsedInAnnotation = false
                if (constant.usesVariableAsConstant) usesVariableAsConstant = true
                if (constant.usesNonConstValAsConstant) usesNonConstantVariableAsConstant = true
                sb.append(constant.constantValue.value)
            }
        }
        return if (!interupted)
            createConstant(
                sb.toString(),
                expectedType,
                CompileTimeConstant.Parameters(
                    isPure = false,
                    isUnsignedNumberLiteral = false,
                    isUnsignedLongNumberLiteral = false,
                    canBeUsedInAnnotation = canBeUsedInAnnotation,
                    usesVariableAsConstant = usesVariableAsConstant,
                    usesNonConstValAsConstant = usesNonConstantVariableAsConstant,
                    isConvertableConstVal = false
                )
            )
        else null
    }

    override fun visitParenthesizedExpression(
        expression: CjParenthesizedExpression,
        expectedType: CangJieType?
    ): CompileTimeConstant<*>? {
        val deparenthesizedExpression = CjPsiUtil.deparenthesize(expression)
        if (deparenthesizedExpression != null && deparenthesizedExpression != expression) {
            return evaluate(deparenthesizedExpression, expectedType)
        }
        return null
    }
}

fun ConstantValue<*>.isStandaloneOnlyConstant(): Boolean {
    return /*this is CClassValue || this is EnumValue || this is AnnotationValue ||*/ this is ArrayValue
}

fun CompileTimeConstant<*>.isStandaloneOnlyConstant(): Boolean {
    return when (this) {
        is TypedCompileTimeConstant -> this.constantValue.isStandaloneOnlyConstant()
        else -> return false
    }
}

enum class CompileTimeType {
    Int8,
    Int16,
    Int32,
    Int64,
    Flout64,
    Flout32,
    Flout16,
    Rune,
    Bool,
    String,
    Any
}

private fun getReceiverExpressionType(resolvedCall: ResolvedCall<*>): CangJieType? {
    return when (resolvedCall.explicitReceiverKind) {
        ExplicitReceiverKind.DISPATCH_RECEIVER -> resolvedCall.dispatchReceiver!!.type
        ExplicitReceiverKind.EXTENSION_RECEIVER -> resolvedCall.extensionReceiver!!.type
        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> null
        ExplicitReceiverKind.BOTH_RECEIVERS -> null

    }
}

private fun evaluateUnaryAndCheck(
    name: String,
    type: CompileTimeType,
    value: Any,
    reportIntegerOverflow: () -> Unit
): Any? =
    evalUnaryOp(name, type, value).also { result ->
        if (isIntegerType(value) && (name == "*operator_minus" || name == "*operator_unaryMinus") && value == result && !isZero(
                value
            )
        ) {
            reportIntegerOverflow()
        }
    }

fun isIntegerType(value: Any?) = value is Byte || value is Short || value is Int || value is Long

private fun isZero(value: Any?): Boolean {
    return when {
        isIntegerType(value) -> (value as Number).toLong() == 0L
        value is Float || value is Double -> (value as Number).toDouble() == 0.0
        else -> false
    }
}

private fun evaluateBinaryAndCheck(
    name: String,
    receiverType: CompileTimeType,
    receiverValue: Any,
    parameterType: CompileTimeType,
    parameterValue: Any,
    reportIntegerOverflow: () -> Unit,
): Any? {
    val actualResult = try {
        evalBinaryOp(name, receiverType, receiverValue, parameterType, parameterValue)
    } catch (e: Exception) {
        null
    }

    fun toBigInteger(value: Any?) = BigInteger.valueOf((value as Number).toLong())

    if (actualResult != null && isIntegerType(receiverValue) && isIntegerType(parameterValue)) {
        val checkedResult =
            checkBinaryOp(name, receiverType, toBigInteger(receiverValue), parameterType, toBigInteger(parameterValue))
        if (checkedResult != null && toBigInteger(actualResult) != checkedResult) {
            reportIntegerOverflow()
        }
    }

    return actualResult
}

private val DIVISION_OPERATION_NAMES =
    listOf(OperatorNameConventions.DIV, OperatorNameConventions.REM)
        .map(Name::asString)
        .toSet()

private fun createCompileTimeConstantForEquals(result: Any?, operationReference: CjExpression): ConstantValue<*>? {
    if (result is Boolean) {
        assert(operationReference is CjSimpleNameExpression) { "This method should be called only for equals operations" }
        val value: Boolean =
            when (val operationToken = (operationReference as CjSimpleNameExpression).referencedNameElementType) {
                CjTokens.EQEQ -> result
                CjTokens.EXCLEQ -> !result
                CjTokens.IDENTIFIER -> {
                    assert(operationReference.referencedNameAsName == OperatorNameConventions.EQUALS) { "This method should be called only for equals operations" }
                    result
                }

                else -> throw IllegalStateException("Unknown equals operation token: $operationToken ${operationReference.text}")
            }
        return BoolValue(value)
    }
    return null
}

private fun createCompileTimeConstantForCompareTo(result: Any?, operationReference: CjExpression): ConstantValue<*>? {
    if (result is Int) {
        assert(operationReference is CjSimpleNameExpression) { "This method should be called only for compareTo operations" }
        return when (val operationToken =
            (operationReference as CjSimpleNameExpression).referencedNameElementType) {
            CjTokens.LT -> BoolValue(result < 0)
            CjTokens.LTEQ -> BoolValue(result <= 0)
            CjTokens.GT -> BoolValue(result > 0)
            CjTokens.GTEQ -> BoolValue(result >= 0)

            else -> throw IllegalStateException("Unknown compareTo operation token: $operationToken")
        }
    }
    return null
}
