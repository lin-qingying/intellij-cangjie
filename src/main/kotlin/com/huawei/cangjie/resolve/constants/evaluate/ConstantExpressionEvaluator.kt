package com.huawei.cangjie.resolve.constants.evaluate

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.parsing.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContextUtils
import com.huawei.cangjie.resolve.StatementFilter
import com.huawei.cangjie.resolve.constants.*
import com.huawei.cangjie.types.BasicType
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.error.ErrorModuleDescriptor.builtIns
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.isGenericArrayOfTypeParameter
import com.intellij.openapi.project.Project


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

//
private class ConstantExpressionEvaluatorVisitor(
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val trace: BindingTrace
) : CjVisitor<CompileTimeConstant<*>?, CangJieType>() {
    private val languageVersionSettings = constantExpressionEvaluator.languageVersionSettings


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

        //        if (nodeElementType == CjNodeTypes.NULL) return NullValue().wrap()
//
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
                    "Float64" -> result = result.toDouble()
                    "Float32" -> result = result.toFloat()
//               "Float16" -> result = result.toFloat16()
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
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
            }
            if (result == 0.0 && !TypeConversionUtil.isFPZero(text)) {
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
            }
        }
//
        if (result is Float) {
            if (result.isInfinite()) {
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
            }
            if (result == 0.0f && !TypeConversionUtil.isFPZero(text)) {
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
            }
        }
//
//        if (result is Float16) {
//            if (result.isInfinite()) {
//                trace.report(Errors.FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
//            }
//            if (result == 0.0f && !TypeConversionUtil.isFPZero(text)) {
//                trace.report(Errors.FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
//            }
//        }
//
        val isIntegerConstant = nodeElementType == CjNodeTypes.INTEGER_CONSTANT
        val isUint64 =
            isIntegerConstant && hasUnsignedInt64Suffix(text) || expectedType?.let { CangJieBuiltIns.isUInt64(it) } == true
        val isUnsigned =
            isUint64 || hasUnsignedSuffix(text) || expectedType?.let { CangJieBuiltIns.isUnsignedNumber(it) } == true
        val isTyped = isUnsigned || hasInt64Suffix(text)
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

    private fun Long.createSimpleIntCompileTimeConst(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<*> {
        val value = this
        return if (parameters.isUnsignedNumberLiteral) {
            when (value) {
                value.toInt().fromUIntToLong() -> UInt32Value(value.toInt())
                else -> UInt64Value(value)
            }
        } else {
            when (value) {
                value -> Int64Value(value)
                value.toInt().toLong() -> Int32Value(value.toInt())
                value.toShort().toLong() -> Int16Value(value.toShort())
                value.toByte().toLong() -> Int8Value(value.toByte())

                else -> Int64Value(value)
            }
        }.wrap(parameters)

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
        val sb = StringBuilder()
        var interupted = false
        var canBeUsedInAnnotation = true
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
