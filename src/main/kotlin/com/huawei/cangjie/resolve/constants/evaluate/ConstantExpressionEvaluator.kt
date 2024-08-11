package com.huawei.cangjie.resolve.constants.evaluate

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjConstantExpression
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjPsiUtil
import com.huawei.cangjie.psi.CjVisitor
import com.huawei.cangjie.resolve.BindingContextUtils
import com.huawei.cangjie.resolve.StatementFilter
import com.huawei.cangjie.resolve.constants.CompileTimeConstant
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.util.TypeUtils

//
//import com.huawei.cangjie.descriptors.BindingTrace
//import com.huawei.cangjie.descriptors.Errors
//import com.huawei.cangjie.psi.CjCallExpression
//import com.huawei.cangjie.psi.CjExpression
//import com.huawei.cangjie.psi.CjSimpleNameExpression
//import com.huawei.cangjie.psi.CjVisitor
//import com.huawei.cangjie.resolve.constants.CompileTimeConstant
//import com.huawei.cangjie.types.CangJieType
//import com.huawei.cangjie.types.TypeUtils
//import com.intellij.psi.util.PsiTreeUtil
//
class ConstantExpressionEvaluator {
    private fun checkExperimentalityOfConstantLiteral(
        expression: CjExpression,
        constant: CompileTimeConstant<*>,
        expectedType: CangJieType?,
        trace: BindingTrace
    ) {
        if (constant.isError) return
        if (!constant.parameters.isUnsignedNumberLiteral && !constant.parameters.isUnsignedLongNumberLiteral) return
//
//        val constantType = when {
//            constant is TypedCompileTimeConstant<*> -> constant.type
//            expectedType != null -> constant.toConstantValue(expectedType).getType(module)
//            else -> return
//        }
//
//        if (!UnsignedTypes.isUnsignedType(constantType)) return
//
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
    //    private val languageVersionSettings = constantExpressionEvaluator.languageVersionSettings
//    private val builtIns = constantExpressionEvaluator.module.builtIns
//    private val inlineConstTracker =
//        if (constantExpressionEvaluator.inlineConstTracker is InlineConstTracker.DoNothing)
//            null
//        else
//            constantExpressionEvaluator.inlineConstTracker
//
    fun evaluate(expression: CjExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val recordedCompileTimeConstant = ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, trace.bindingContext)
//        if (recordedCompileTimeConstant != null) {
//            return recordedCompileTimeConstant
//        }
//
//        val compileTimeConstant = expression.accept(this, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
//        if (compileTimeConstant != null) {
//            if (shouldSkipComplexBooleanValue(expression, compileTimeConstant)) {
//                return null
//            }
//
//            // If constant is `Array` and its argument is some generic type, then we must wait for full resolve and only when we can record value
//            if (compileTimeConstant is TypedCompileTimeConstant && compileTimeConstant.type.isGenericArrayOfTypeParameter()) {
//                return compileTimeConstant
//            }
//
//            trace.record(BindingContext.COMPILE_TIME_VALUE, expression, compileTimeConstant)
//            return compileTimeConstant
//        }
        return null
    }
//
//    private fun shouldSkipComplexBooleanValue(
//        expression: CjExpression,
//        constant: CompileTimeConstant<*>
//    ): Boolean {
//        if (!ConstantExpressionEvaluator.isComplexBooleanConstant(expression, constant)) {
//            return false
//        }
//
//        if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitSimplificationOfNonTrivialConstBooleanExpressions)) {
//            return true
//        } else {
//            var parent = expression.parent
//            while (parent is CjParenthesizedExpression) {
//                parent = parent.parent
//            }
//            if (
//                parent is CjWhenConditionWithExpression ||
//                parent is CjContainerNode && (parent.parent is CjWhileExpression || parent.parent is CjDoWhileExpression)
//            ) {
//                val constantValue = constant.toConstantValue(builtIns.booleanType)
//                trace.report(Errors.NON_TRIVIAL_BOOLEAN_CONSTANT.on(expression, constantValue.value as Boolean))
//            }
//            return false
//        }
//    }
//
//    private val stringExpressionEvaluator = object : CjVisitor<TypedCompileTimeConstant<String>, Nothing?>() {
//        private fun createStringConstant(compileTimeConstant: CompileTimeConstant<*>): TypedCompileTimeConstant<String>? {
//            val constantValue = compileTimeConstant.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)
//            if (constantValue.isStandaloneOnlyConstant()) {
//                return null
//            }
//            return when (constantValue) {
//                is ErrorValue, is EnumValue -> return null
//                is NullValue -> StringValue("null")
//                else -> StringValue(constantValue.boxedValue().toString())
//            }.wrap(compileTimeConstant.parameters)
//        }
//
//        @Suppress("RedundantNullableReturnType")
//        fun evaluate(entry: CjStringTemplateEntry): TypedCompileTimeConstant<String>? {
//            return entry.accept(this, null)
//        }
//
//        override fun visitStringTemplateEntryWithExpression(
//            entry: CjStringTemplateEntryWithExpression,
//            data: Nothing?
//        ): TypedCompileTimeConstant<String>? {
//            val expression = entry.expression ?: return null
//
//            return evaluate(expression, builtIns.stringType)?.let {
//                createStringConstant(it)
//            }
//        }
//
//        override fun visitLiteralStringTemplateEntry(
//            entry: CjLiteralStringTemplateEntry,
//            data: Nothing?
//        ): TypedCompileTimeConstant<String> =
//            StringValue(entry.text).wrap()
//
//        override fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry, data: Nothing?): TypedCompileTimeConstant<String> =
//            StringValue(entry.unescapedValue).wrap()
//    }
//
//    override fun visitConstantExpression(expression: CjConstantExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val text = expression.text ?: return null
//
//        val nodeElementType = expression.node.elementType
//        if (nodeElementType == CjNodeTypes.NULL) return NullValue().wrap()
//
//        val result: Any = when (nodeElementType) {
//            CjNodeTypes.INTEGER_CONSTANT, CjNodeTypes.FLOAT_CONSTANT -> parseNumericLiteral(text, nodeElementType)
//            CjNodeTypes.BOOLEAN_CONSTANT -> parseBoolean(text)
//            CjNodeTypes.CHARACTER_CONSTANT -> CompileTimeConstantChecker.parseChar(expression)
//            else -> throw IllegalArgumentException("Unsupported constant: $expression")
//        } ?: return null
//
//        if (result is Double) {
//            if (result.isInfinite()) {
//                trace.report(Errors.FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
//            }
//            if (result == 0.0 && !TypeConversionUtil.isFPZero(text)) {
//                trace.report(Errors.FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
//            }
//        }
//
//        if (result is Float) {
//            if (result.isInfinite()) {
//                trace.report(Errors.FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
//            }
//            if (result == 0.0f && !TypeConversionUtil.isFPZero(text)) {
//                trace.report(Errors.FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
//            }
//        }
//
//        val isIntegerConstant = nodeElementType == CjNodeTypes.INTEGER_CONSTANT
//        val isUnsignedLong = isIntegerConstant && hasUnsignedLongSuffix(text)
//        val isUnsigned = isUnsignedLong || hasUnsignedSuffix(text)
//        val isTyped = isUnsigned || hasLongSuffix(text)
//
//        return createConstant(
//            result,
//            expectedType,
//            CompileTimeConstant.Parameters(
//                canBeUsedInAnnotation = true,
//                isPure = !isTyped,
//                isUnsignedNumberLiteral = isUnsigned,
//                isUnsignedLongNumberLiteral = isUnsignedLong,
//                usesVariableAsConstant = false,
//                usesNonConstValAsConstant = false,
//                isConvertableConstVal = false
//            )
//        )
//    }
//
//    override fun visitParenthesizedExpression(expression: CjParenthesizedExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val deparenthesizedExpression = CjPsiUtil.deparenthesize(expression)
//        if (deparenthesizedExpression != null && deparenthesizedExpression != expression) {
//            return evaluate(deparenthesizedExpression, expectedType)
//        }
//        return null
//    }
//
//    override fun visitLabeledExpression(expression: CjLabeledExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val baseExpression = expression.baseExpression
//        if (baseExpression != null) {
//            return evaluate(baseExpression, expectedType)
//        }
//        return null
//    }
//
//    override fun visitStringTemplateExpression(expression: CjStringTemplateExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val sb = StringBuilder()
//        var interupted = false
//        var canBeUsedInAnnotation = true
//        var usesVariableAsConstant = false
//        var usesNonConstantVariableAsConstant = false
//        for (entry in expression.entries) {
//            val constant = stringExpressionEvaluator.evaluate(entry)
//            if (constant == null) {
//                interupted = true
//                break
//            } else {
//                if (!constant.canBeUsedInAnnotations) canBeUsedInAnnotation = false
//                if (constant.usesVariableAsConstant) usesVariableAsConstant = true
//                if (constant.usesNonConstValAsConstant) usesNonConstantVariableAsConstant = true
//                sb.append(constant.constantValue.value)
//            }
//        }
//        return if (!interupted)
//            createConstant(
//                sb.toString(),
//                expectedType,
//                CompileTimeConstant.Parameters(
//                    isPure = false,
//                    isUnsignedNumberLiteral = false,
//                    isUnsignedLongNumberLiteral = false,
//                    canBeUsedInAnnotation = canBeUsedInAnnotation,
//                    usesVariableAsConstant = usesVariableAsConstant,
//                    usesNonConstValAsConstant = usesNonConstantVariableAsConstant,
//                    isConvertableConstVal = false
//                )
//            )
//        else null
//    }
//
//    private fun isStandaloneOnlyConstant(expression: CjExpression): Boolean {
//        return ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.isStandaloneOnlyConstant() ?: return false
//    }
//
//    override fun visitBinaryWithTypeRHSExpression(
//        expression: CjBinaryExpressionWithTypeRHS,
//        expectedType: CangJieType?
//    ): CompileTimeConstant<*>? {
//        val compileTimeConstant = evaluate(expression.left, expectedType)
//        if (compileTimeConstant != null) {
//            if (expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
//                val constantType = when (compileTimeConstant) {
//                    is TypedCompileTimeConstant<*> ->
//                        compileTimeConstant.type
//                    is IntegerValueTypeConstant ->
//                        compileTimeConstant.getType(expectedType)
//                    else ->
//                        throw IllegalStateException("Unexpected compileTimeConstant class: ${compileTimeConstant::class.java.canonicalName}")
//
//                }
//                if (!constantType.isSubtypeOf(expectedType)) return null
//
//            }
//        }
//
//        return compileTimeConstant
//    }
//
//    override fun visitBinaryExpression(expression: CjBinaryExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val leftExpression = expression.left ?: return null
//
//        val operationToken = expression.operationToken
//        if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
//            val booleanType = builtIns.booleanType
//            val leftConstant = evaluate(leftExpression, booleanType) ?: return null
//
//            val rightExpression = expression.right ?: return null
//
//            val rightConstant = evaluate(rightExpression, booleanType) ?: return null
//
//            val leftValue = leftConstant.getValue(booleanType)
//            val rightValue = rightConstant.getValue(booleanType)
//
//            if (leftValue !is Boolean || rightValue !is Boolean) return null
//            val result = when (operationToken) {
//                CjTokens.ANDAND -> leftValue && rightValue
//                CjTokens.OROR -> leftValue || rightValue
//                else -> throw IllegalArgumentException("Unknown bool operation token $operationToken")
//            }
//            return createConstant(
//                result, expectedType,
//                CompileTimeConstant.Parameters(
//                    canBeUsedInAnnotation = true,
//                    isPure = false,
//                    isUnsignedNumberLiteral = false,
//                    isUnsignedLongNumberLiteral = false,
//                    usesVariableAsConstant = leftConstant.usesVariableAsConstant || rightConstant.usesVariableAsConstant,
//                    usesNonConstValAsConstant = leftConstant.usesNonConstValAsConstant || rightConstant.usesNonConstValAsConstant,
//                    isConvertableConstVal = false
//                )
//            )
//        } else {
//            return evaluateCall(expression.operationReference, leftExpression, expectedType)
//        }
//    }
//
//    override fun visitCollectionLiteralExpression(
//        expression: CjCollectionLiteralExpression,
//        expectedType: CangJieType?
//    ): CompileTimeConstant<*>? {
//        val resolvedCall = trace.bindingContext[COLLECTION_LITERAL_CALL, expression] ?: return null
//        return createConstantValueForArrayFunctionCall(resolvedCall)
//    }
//
//    private fun evaluateCall(
//        callExpression: CjExpression,
//        receiverExpression: CjExpression,
//        expectedType: CangJieType?
//    ): CompileTimeConstant<*>? {
//        val resolvedCall = callExpression.getResolvedCall(trace.bindingContext) ?: return null
//        if (!CangJieBuiltIns.isUnderCangJiePackage(resolvedCall.resultingDescriptor)) return null
//
//        val resultingDescriptorName = resolvedCall.resultingDescriptor.name
//
//        val argumentForReceiver = createOperationArgumentForReceiver(resolvedCall, receiverExpression) ?: return null
//        if (isStandaloneOnlyConstant(argumentForReceiver.expression)) {
//            return null
//        }
//
//        val argumentsEntrySet = resolvedCall.valueArguments.entries
//        if (argumentsEntrySet.isEmpty()) {
//            val result = evaluateUnaryAndCheck(argumentForReceiver, resultingDescriptorName.asString(), callExpression) ?: return null
//
//            val isArgumentPure = isPureConstant(argumentForReceiver.expression)
//            val canBeUsedInAnnotation = canBeUsedInAnnotation(argumentForReceiver.expression)
//            val usesVariableAsConstant = usesVariableAsConstant(argumentForReceiver.expression)
//            val usesNonConstValAsConstant = usesNonConstValAsConstant(argumentForReceiver.expression)
//            val isNumberConversionMethod = resultingDescriptorName in OperatorConventions.NUMBER_CONVERSIONS
//            val isCharCode = argumentForReceiver.ctcType == CHAR && resultingDescriptorName == StandardNames.CHAR_CODE
//            return createConstant(
//                result,
//                expectedType,
//                CompileTimeConstant.Parameters(
//                    canBeUsedInAnnotation,
//                    !isNumberConversionMethod && !isCharCode && isArgumentPure,
//                    isUnsignedNumberLiteral = false,
//                    isUnsignedLongNumberLiteral = false,
//                    usesVariableAsConstant,
//                    usesNonConstValAsConstant,
//                    isConvertableConstVal = false
//                )
//            )
//        } else if (argumentsEntrySet.size == 1) {
//            val (parameter, argument) = argumentsEntrySet.first()
//            val argumentForParameter = createOperationArgumentForFirstParameter(argument, parameter) ?: return null
//            if (isStandaloneOnlyConstant(argumentForParameter.expression)) {
//                return null
//            }
//
//            if (isDivisionByZero(resultingDescriptorName.asString(), argumentForParameter.value)) {
//                val parentExpression: CjExpression = PsiTreeUtil.getParentOfType(receiverExpression, CjExpression::class.java)!!
//                trace.report(Errors.DIVISION_BY_ZERO.on(parentExpression))
//
//                if ((isIntegerType(argumentForReceiver.value) && isIntegerType(argumentForParameter.value)) ||
//                    !languageVersionSettings.supportsFeature(LanguageFeature.DivisionByZeroInConstantExpressions)
//                ) {
//                    return ErrorValue.create("Division by zero").wrap()
//                }
//            }
//
//            val result = evaluateBinaryAndCheck(
//                argumentForReceiver,
//                argumentForParameter,
//                resultingDescriptorName.asString(),
//                callExpression
//            ) ?: return null
//
//            val areArgumentsPure = isPureConstant(argumentForReceiver.expression) && isPureConstant(argumentForParameter.expression)
//            val canBeUsedInAnnotation =
//                canBeUsedInAnnotation(argumentForReceiver.expression) && canBeUsedInAnnotation(argumentForParameter.expression)
//            val usesVariableAsConstant =
//                usesVariableAsConstant(argumentForReceiver.expression) || usesVariableAsConstant(argumentForParameter.expression)
//            val usesNonConstValAsConstant =
//                usesNonConstValAsConstant(argumentForReceiver.expression) || usesNonConstValAsConstant(argumentForParameter.expression)
//            val parameters = CompileTimeConstant.Parameters(
//                canBeUsedInAnnotation,
//                areArgumentsPure,
//                isUnsignedNumberLiteral = false,
//                isUnsignedLongNumberLiteral = false,
//                usesVariableAsConstant,
//                usesNonConstValAsConstant,
//                isConvertableConstVal = false
//            )
//            return when (resultingDescriptorName) {
//                OperatorNameConventions.COMPARE_TO -> createCompileTimeConstantForCompareTo(result, callExpression)?.wrap(parameters)
//                OperatorNameConventions.EQUALS -> createCompileTimeConstantForEquals(result, callExpression)?.wrap(parameters)
//                else -> {
//                    createConstant(
//                        result,
//                        expectedType,
//                        parameters
//                    )
//                }
//            }
//        }
//
//        return null
//    }
//
//    private fun usesVariableAsConstant(expression: CjExpression) =
//        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.usesVariableAsConstant ?: false
//
//    private fun usesNonConstValAsConstant(expression: CjExpression) =
//        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.usesNonConstValAsConstant ?: false
//
//    private fun canBeUsedInAnnotation(expression: CjExpression) =
//        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.canBeUsedInAnnotations ?: false
//
//    private fun isPureConstant(expression: CjExpression) =
//        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.isPure ?: false
//
//    private fun evaluateUnaryAndCheck(receiver: OperationArgument, name: String, callExpression: CjExpression): Any? {
//        return evaluateUnaryAndCheck(name, receiver.ctcType, receiver.value) {
//            trace.report(Errors.INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType() ?: callExpression))
//        }
//    }
//
//    private fun evaluateBinaryAndCheck(
//        receiver: OperationArgument,
//        parameter: OperationArgument,
//        name: String,
//        callExpression: CjExpression
//    ): Any? {
//        return evaluateBinaryAndCheck(name, receiver.ctcType, receiver.value, parameter.ctcType, parameter.value) {
//            trace.report(Errors.INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType() ?: callExpression))
//        }
//    }
//
//    private fun isDivisionByZero(name: String, parameter: Any?): Boolean {
//        return name in DIVISION_OPERATION_NAMES && isZero(parameter)
//    }
//
//    override fun visitUnaryExpression(expression: CjUnaryExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val leftExpression = expression.baseExpression ?: return null
//        return evaluateCall(
//            expression.operationReference,
//            leftExpression,
//            expectedType
//        )
//    }
//
//    override fun visitSimpleNameExpression(expression: CjSimpleNameExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val enumDescriptor = trace.bindingContext.get(BindingContext.REFERENCE_TARGET, expression)
//        if (enumDescriptor != null && DescriptorUtils.isEnumEntry(enumDescriptor)) {
//            val enumClassId = (enumDescriptor.containingDeclaration as ClassDescriptor).classId ?: return null
//            return EnumValue(enumClassId, enumDescriptor.name).wrap()
//        }
//
//        val variableDescriptor = enumDescriptor as? VariableDescriptor
//        if (variableDescriptor != null
//            && isPropertyCompileTimeConstant(variableDescriptor)
//            && !variableDescriptor.containingDeclaration.isCompanionObject()
//        ) {
//            reportInlineConst(expression, variableDescriptor)
//        }
//
//        val resolvedCall = expression.getResolvedCall(trace.bindingContext)
//        if (resolvedCall != null) {
//            val callableDescriptor = resolvedCall.resultingDescriptor
//            if (callableDescriptor is VariableDescriptor) {
//                // TODO: FIXME: see KT-10425
//                if (callableDescriptor is PropertyDescriptor && callableDescriptor.modality != Modality.FINAL) return null
//
//                val isConvertableConstVal =
//                    callableDescriptor.isConst &&
//                            ImplicitIntegerCoercion.isEnabledFor(callableDescriptor, languageVersionSettings) &&
//                            callableDescriptor.compileTimeInitializer is Int32Value
//
//                return callableDescriptor.compileTimeInitializer?.wrap(
//                    CompileTimeConstant.Parameters(
//                        canBeUsedInAnnotation = isPropertyCompileTimeConstant(callableDescriptor),
//                        isPure = false,
//                        isUnsignedNumberLiteral = false,
//                        isUnsignedLongNumberLiteral = false,
//                        usesVariableAsConstant = true,
//                        usesNonConstValAsConstant = !callableDescriptor.isConst,
//                        isConvertableConstVal = isConvertableConstVal
//                    )
//                )
//            }
//        }
//        return null
//    }
//
//    private fun reportInlineConst(expression: CjSimpleNameExpression, variableDescriptor: VariableDescriptor) {
//        if (inlineConstTracker == null) return
//        val filePath = expression.containingFile.virtualFile?.path ?: return
//        val name = expression.getReferencedName()
//        val constType = variableDescriptor.type.toString()
//
//        // Transformation of fqName to the form "package.Outer$Inner"
//        val containingPackage = variableDescriptor.containingPackage()?.toString() ?: return
//        val fqName = variableDescriptor.containingDeclaration.fqNameSafe.asString()
//        val owner = if (fqName.startsWith("$containingPackage.")) {
//            containingPackage + "." + fqName.substring(containingPackage.length + 1).replace(".", "$")
//        } else {
//            fqName.replace(".", "$")
//        }
//        inlineConstTracker.report(filePath, owner, name, constType)
//    }
//
//    // TODO: Should be replaced with descriptor.isConst
//    private fun isPropertyCompileTimeConstant(descriptor: VariableDescriptor): Boolean {
//        if (descriptor.isVar) {
//            return false
//        }
//        if (DescriptorUtils.isObject(descriptor.containingDeclaration) ||
//            DescriptorUtils.isStaticDeclaration(descriptor)
//        ) {
//            return descriptor.type.canBeUsedForConstVal()
//        }
//        return false
//    }
//
//    override fun visitQualifiedExpression(expression: CjQualifiedExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val selectorExpression = expression.selectorExpression
//        // 1.toInt(); 1.plus(1);
//        if (selectorExpression is CjCallExpression) {
//            val qualifiedCallValue = evaluate(selectorExpression, expectedType)
//            if (qualifiedCallValue != null) {
//                return qualifiedCallValue
//            }
//
//            val calleeExpression = selectorExpression.calleeExpression
//            if (calleeExpression !is CjSimpleNameExpression) {
//                return null
//            }
//
//            val receiverExpression = expression.receiverExpression
//            return evaluateCall(calleeExpression, receiverExpression, expectedType)
//        }
//
//        if (selectorExpression is CjSimpleNameExpression) {
//            val result = evaluateCall(selectorExpression, expression.receiverExpression, expectedType)
//            if (result != null) return result
//        }
//
//        // MyEnum.A, Integer.MAX_VALUE
//        if (selectorExpression != null) {
//            return evaluate(selectorExpression, expectedType)
//        }
//
//        return null
//    }
//
//    override fun visitCallExpression(expression: CjCallExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val call = expression.getResolvedCall(trace.bindingContext) ?: return null
//
//        val resultingDescriptor = call.resultingDescriptor
//
//        // arrayOf() or emptyArray()
//        if (CompileTimeConstantUtils.isArrayFunctionCall(call)) {
//            return createConstantValueForArrayFunctionCall(call)
//        }
//
//        // Ann()
//        if (resultingDescriptor is ConstructorDescriptor) {
//            val classDescriptor = resultingDescriptor.constructedClass
//            return when {
//                DescriptorUtils.isAnnotationClass(classDescriptor) -> {
//                    val descriptor = AnnotationDescriptorImpl(
//                        classDescriptor.defaultType,
//                        constantExpressionEvaluator.resolveAnnotationArguments(call, trace),
//                        SourceElement.NO_SOURCE
//                    )
//                    AnnotationValue(descriptor).wrap()
//                }
//
//                classDescriptor.isInlineClass() && UnsignedTypes.isUnsignedClass(classDescriptor) ->
//                    createConstantValueForUnsignedTypeConstructor(call, resultingDescriptor, classDescriptor.inlineClassRepresentation!!)
//
//                else -> null
//            }
//        }
//
//        return null
//    }
//
//    private fun createConstantValueForUnsignedTypeConstructor(
//        call: ResolvedCall<*>,
//        constructorDescriptor: ConstructorDescriptor,
//        representation: InlineClassRepresentation<SimpleType>,
//    ): TypedCompileTimeConstant<*>? {
//        if (!constructorDescriptor.isPrimary) return null
//
//        val valueArguments = call.valueArguments
//        if (valueArguments.size > 1) return null
//
//        val argument = valueArguments.values.singleOrNull()?.arguments?.singleOrNull() ?: return null
//        val argumentExpression = argument.getArgumentExpression() ?: return null
//
//        val underlyingType = representation.underlyingType
//        val compileTimeConstant = evaluate(argumentExpression, underlyingType)
//        val evaluatedArgument = compileTimeConstant?.toConstantValue(underlyingType) ?: return null
//
//        val unsignedValue = ConstantValueFactory.createUnsignedValue(evaluatedArgument) ?: return null
//        return unsignedValue.wrap(compileTimeConstant.parameters)
//    }
//
//    private fun createConstantValueForArrayFunctionCall(
//        call: ResolvedCall<*>
//    ): TypedCompileTimeConstant<List<ConstantValue<*>>>? {
//        val returnType = call.resultingDescriptor.returnType ?: return null
//        val componentType = builtIns.getArrayElementType(returnType)
//
//        val arguments = call.valueArguments.values.flatMap { resolveArguments(it.arguments, componentType) }
//
//        // not evaluated arguments are not constants: function-calls, properties with custom getter...
//        val evaluatedArguments = arguments.filterNotNull()
//
//        return ConstantValueFactory.createArrayValue(evaluatedArguments.map { it.toConstantValue(componentType) }, returnType)
//            .wrap(
//                usesVariableAsConstant = evaluatedArguments.any { it.usesVariableAsConstant },
//                usesNonConstValAsConstant = arguments.any { it == null || it.usesNonConstValAsConstant }
//            )
//    }
//
//    override fun visitClassLiteralExpression(expression: CjClassLiteralExpression, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        val kClassType = trace.getType(expression)!!
//        if (kClassType.isError) return null
//        val descriptor = kClassType.constructor.declarationDescriptor
//        if (descriptor !is ClassDescriptor || !CangJieBuiltIns.isKClass(descriptor)) return null
//
//        val type = kClassType.arguments.singleOrNull()?.type ?: return null
//        if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitTypeParametersInClassLiteralsInAnnotationArguments) &&
//            ConstantExpressionEvaluator.isTypeParameterOrArrayOfTypeParameter(type)
//        ) {
//            return null
//        }
//
//        return KClassValue.create(type)?.wrap()
//    }
//
//    private fun resolveArguments(valueArguments: List<ValueArgument>, expectedType: CangJieType): List<CompileTimeConstant<*>?> {
//        val constants = arrayListOf<CompileTimeConstant<*>?>()
//        for (argument in valueArguments) {
//            val argumentExpression = argument.getArgumentExpression()
//            if (argumentExpression != null) {
//                constants.add(evaluate(argumentExpression, expectedType))
//            }
//        }
//        return constants
//    }
//
//    override fun visitCjElement(element: CjElement, expectedType: CangJieType?): CompileTimeConstant<*>? {
//        return null
//    }
//
//    private class OperationArgument(val value: Any, val ctcType: CompileTimeType, val expression: CjExpression)
//
//    private fun createOperationArgumentForReceiver(resolvedCall: ResolvedCall<*>, expression: CjExpression): OperationArgument? {
//        val receiverExpressionType = getReceiverExpressionType(resolvedCall) ?: return null
//
//        val receiverCompileTimeType = getCompileTimeType(receiverExpressionType) ?: return null
//
//        return createOperationArgument(expression, receiverExpressionType, receiverCompileTimeType)
//    }
//
//    private fun createOperationArgumentForFirstParameter(
//        argument: ResolvedValueArgument,
//        parameter: ValueParameterDescriptor
//    ): OperationArgument? {
//        val argumentCompileTimeType = getCompileTimeType(parameter.type) ?: return null
//
//        val arguments = argument.arguments
//        if (arguments.size != 1) return null
//
//        val argumentExpression = arguments.first().getArgumentExpression() ?: return null
//
//        return createOperationArgument(argumentExpression, parameter.type, argumentCompileTimeType)
//    }
//
//    private fun getCompileTimeType(c: CangJieType): CompileTimeType? =
//        when (TypeUtils.makeNotNullable(c)) {
//            builtIns.intType -> INT
//            builtIns.byteType -> BYTE
//            builtIns.shortType -> SHORT
//            builtIns.longType -> LONG
//            builtIns.doubleType -> DOUBLE
//            builtIns.floatType -> FLOAT
//            builtIns.charType -> CHAR
//            builtIns.booleanType -> BOOLEAN
//            builtIns.stringType -> STRING
//            builtIns.anyType -> ANY
//            else -> null
//        }
//
//    private fun createOperationArgument(
//        expression: CjExpression,
//        parameterType: CangJieType,
//        compileTimeType: CompileTimeType,
//    ): OperationArgument? {
//        val compileTimeConstant = constantExpressionEvaluator.evaluateExpression(expression, trace, parameterType) ?: return null
//        if (compileTimeConstant is TypedCompileTimeConstant && !compileTimeConstant.type.isSubtypeOf(parameterType)) return null
//        val constantValue = compileTimeConstant.toConstantValue(parameterType)
//        val evaluationResult = (if (compileTimeType == ANY) constantValue.boxedValue() else constantValue.value) ?: return null
//        return OperationArgument(evaluationResult, compileTimeType, expression)
//    }
//
//    private fun createConstant(
//        value: Any?,
//        expectedType: CangJieType?,
//        parameters: CompileTimeConstant.Parameters
//    ): CompileTimeConstant<*>? {
//        return if (parameters.isPure || parameters.isUnsignedNumberLiteral) {
//            return createCompileTimeConstant(value, parameters, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
//        } else {
//            ConstantValueFactory.createConstantValue(value)?.wrap(parameters)
//        }
//    }
//
//    private fun createCompileTimeConstant(
//        value: Any?,
//        parameters: CompileTimeConstant.Parameters,
//        expectedType: CangJieType
//    ): CompileTimeConstant<*>? {
//        return when (value) {
//            is Byte, is Short, is Int, is Long -> createIntegerCompileTimeConstant((value as Number).toLong(), parameters, expectedType)
//            else -> ConstantValueFactory.createConstantValue(value)?.wrap(parameters)
//        }
//    }
//
//    private fun createIntegerCompileTimeConstant(
//        value: Long,
//        parameters: CompileTimeConstant.Parameters,
//        expectedType: CangJieType
//    ): CompileTimeConstant<*> {
//        if (parameters.isUnsignedNumberLiteral && !checkAccessibilityOfUnsignedTypes()) {
//            return UnsignedErrorValueTypeConstant(value, constantExpressionEvaluator.module, parameters)
//        }
//
//        if (parameters.isUnsignedLongNumberLiteral) {
//            return ULongValue(value).wrap(parameters)
//        }
//
//        if (TypeUtils.noExpectedType(expectedType) || expectedType.isError) {
//            return createIntegerValueTypeConstant(
//                value,
//                constantExpressionEvaluator.module,
//                parameters,
//                languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
//            )
//        }
//        val integerValue = ConstantValueFactory.createIntegerConstantValue(
//            value, expectedType, parameters.isUnsignedNumberLiteral
//        )
//        if (integerValue != null) {
//            return integerValue.wrap(parameters)
//        }
//
//        return value.createSimpleIntCompileTimeConst(parameters)
//    }
//
//    private fun Long.createSimpleIntCompileTimeConst(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<*> {
//        val value = this
//        return if (parameters.isUnsignedNumberLiteral) {
//            when (value) {
//                value.toInt().fromUIntToLong() -> UIntValue(value.toInt())
//                else -> ULongValue(value)
//            }
//        } else {
//            when (value) {
//                value.toInt().toLong() -> Int32Value(value.toInt())
//                else -> LongValue(value)
//            }
//        }.wrap(parameters)
//    }
//
//    private fun checkAccessibilityOfUnsignedTypes(): Boolean {
//        val uInt = constantExpressionEvaluator.module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt) ?: return false
//        val accessibility = uInt.checkSinceCangJieVersionAccessibility(languageVersionSettings)
//        // Case `NotAccessibleButWasExperimental` will be checked later in `checkExperimentalityOfConstantLiteral`
//        return accessibility !is SinceCangJieAccessibility.NotAccessible
//    }
//
//    private fun <T> ConstantValue<T>.wrap(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<T> =
//        TypedCompileTimeConstant(this, constantExpressionEvaluator.module, parameters)
//
//    private fun <T> ConstantValue<T>.wrap(
//        canBeUsedInAnnotation: Boolean = this !is NullValue,
//        isPure: Boolean = false,
//        isUnsigned: Boolean = false,
//        isUnsignedLong: Boolean = false,
//        usesVariableAsConstant: Boolean = false,
//        usesNonConstValAsConstant: Boolean = false,
//        isConvertableConstVal: Boolean = false
//    ): TypedCompileTimeConstant<T> =
//        wrap(
//            CompileTimeConstant.Parameters(
//                canBeUsedInAnnotation,
//                isPure,
//                isUnsigned,
//                isUnsignedLong,
//                usesVariableAsConstant,
//                usesNonConstValAsConstant,
//                isConvertableConstVal
//            )
//        )
}
