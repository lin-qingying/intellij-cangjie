package com.linqingying.cangjie.types.expressions

import com.google.common.collect.Lists
import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.builtins.BinaryOperatorRuleResultType
import com.linqingying.cangjie.builtins.CangJieBuiltIns.Companion.isFloat
import com.linqingying.cangjie.builtins.CangJieBuiltIns.Companion.isNothing
import com.linqingying.cangjie.builtins.CangJieBuiltIns.Companion.isNumber
import com.linqingying.cangjie.builtins.CangJieBuiltIns.Companion.isUnit
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.Errors.NO_GET_METHOD
import com.linqingying.cangjie.diagnostics.Errors.NO_SET_METHOD
import com.linqingying.cangjie.diagnostics.InvalidBinaryData
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjSingleValueToken
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.parsing.hasIllegalUnderscore
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.BindingContext.INDEXED_LVALUE_GET
import com.linqingying.cangjie.resolve.BindingContext.INDEXED_LVALUE_SET
import com.linqingying.cangjie.resolve.BindingContextUtils
import com.linqingying.cangjie.resolve.DescriptorUtils.isClass
import com.linqingying.cangjie.resolve.DescriptorUtils.isInterface
import com.linqingying.cangjie.resolve.TemporaryBindingTrace
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isCallableReferenceArgument
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isCollectionLiteralArgument
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isFunctionLiteralArgument
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isFunctionLiteralOrCallableReference
import com.linqingying.cangjie.resolve.calls.checkers.CallCheckerContext
import com.linqingying.cangjie.resolve.calls.context.ContextDependency
import com.linqingying.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl
import com.linqingying.cangjie.resolve.calls.model.ResolvedCallImpl
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResults
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsUtil
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValue.Companion.nullValue
import com.linqingying.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.linqingying.cangjie.resolve.calls.tasks.OldResolutionCandidate
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy
import com.linqingying.cangjie.resolve.calls.util.CallMaker
import com.linqingying.cangjie.resolve.calls.util.CallMaker.makeCall
import com.linqingying.cangjie.resolve.constants.CompileTimeConstantChecker
import com.linqingying.cangjie.resolve.constants.IntegerValueTypeConstant
import com.linqingying.cangjie.resolve.constants.TypedCompileTimeConstant
import com.linqingying.cangjie.resolve.constants.UnsignedErrorValueTypeConstant
import com.linqingying.cangjie.resolve.recordScope
import com.linqingying.cangjie.resolve.scopes.LexicalScopeKind
import com.linqingying.cangjie.resolve.scopes.findFirstClassifierWithDeprecationStatus
import com.linqingying.cangjie.resolve.scopes.getImplicitReceiversHierarchy
import com.linqingying.cangjie.resolve.scopes.receivers.ContextReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver.Companion.create
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils.createErrorType
import com.linqingying.cangjie.types.ErrorUtils.invalidType
import com.linqingying.cangjie.types.ErrorUtils.isError
import com.linqingying.cangjie.types.TypeSubstitutor.Companion.create
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.error.ErrorType
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import com.linqingying.cangjie.types.expressions.LabelResolver.LabeledReceiverResolutionResult.Companion.labelResolutionSuccess
import com.linqingying.cangjie.types.expressions.LabelResolver.resolveThisOrSuperLabel
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.errorTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.types.expressions.unqualifiedSuper.isPossiblyAmbiguousUnqualifiedSuper
import com.linqingying.cangjie.types.expressions.unqualifiedSuper.resolveUnqualifiedSuperFromExpressionContext
import com.linqingying.cangjie.types.isError
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.types.util.TypeUtils.isNullableType
import com.linqingying.cangjie.types.util.TypeUtils.makeNotNullable
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo
import com.linqingying.cangjie.utils.exceptions.OperatorConventions
import com.linqingying.cangjie.utils.exceptions.OperatorConventions.isConventionType
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class BasicExpressionTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {
    fun checkInExpression(
        callElement: CjElement,
        operationSign: CjSimpleNameExpression,
        leftArgument: ValueArgument,
        right: CjExpression?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val left = leftArgument.getArgumentExpression()
        val contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE)

        //        if (right == null) {
//            if (left != null) facade.getTypeInfo(left, contextWithNoExpectedType);
//            return TypeInfoFactoryCj.noTypeInfo(context);
//        }
        val rightTypeInfo = facade.getTypeInfo(right!!, contextWithNoExpectedType)
        val dataFlowInfo = rightTypeInfo.dataFlowInfo
        //
//        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, contextWithNoExpectedType);
//        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);
//
//        Call containsCall = CallMaker.makeCall(
//                callElement, receiver, null, operationSign,
//                Collections.singletonList(leftArgument), Call.CallType.CONTAINS
//        );
//
//        OverloadResolutionResults<FunctionDescriptor> resolutionResult = components.callResolver.resolveCallWithGivenName(
//                contextWithDataFlow,
//                containsCall,
//                operationSign,
//                OperatorNameConventions.CONTAINS);
//        CangJieType containsType = OverloadResolutionResultsUtil.getResultingType(resolutionResult, context);
//        ensureBooleanResult(operationSign, OperatorNameConventions.CONTAINS, containsType, context);
//
//        if (left != null) {
//            dataFlowInfo = facade.getTypeInfo(left, contextWithDataFlow).getDataFlowInfo().and(dataFlowInfo);
//            rightTypeInfo = rightTypeInfo.replaceDataFlowInfo(dataFlowInfo);
//        }
//        if (resolutionResult.isSuccess() || isResolutionSuccessfulWithOnlyInputTypesWarnings(resolutionResult.getResultingCalls(), context)) {
//            return rightTypeInfo.replaceType(components.builtIns.getBooleanType());
//        } else {
        return rightTypeInfo.clearType()
        //        }
    }

    private fun getTypeInfoForBinaryCall(
        name: Name,
        context: ExpressionTypingContext,
        binaryExpression: CjBinaryExpression
    ): CangJieTypeInfo {
        val left = binaryExpression.left
        val right = binaryExpression.right

        var typeInfo: CangJieTypeInfo
        typeInfo = if (left != null) {
            //left here is a receiver, so it doesn't depend on expected type
            facade.getTypeInfo(
                left,
                context.replaceContextDependency(ContextDependency.INDEPENDENT)
                    .replaceExpectedType(NO_EXPECTED_TYPE)
            )
        } else {
            noTypeInfo(context)
        }

        val contextWithDataFlow = context.replaceDataFlowInfo(typeInfo.dataFlowInfo)

        val resolutionResults: OverloadResolutionResults<FunctionDescriptor>
        if (left != null) {
            val receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, context)
            resolutionResults =
                components.callResolver.resolveBinaryCall(contextWithDataFlow, receiver, binaryExpression, name)
        } else {
            resolutionResults = OverloadResolutionResultsImpl.nameNotFound()
        }

        if (resolutionResults.isSingleResult) {
            typeInfo = typeInfo.replaceDataFlowInfo(resolutionResults.resultingCall.dataFlowInfoForArguments.resultInfo)
        }

        //        if (OverloadResolutionResultsUtil.getResultingType(resolutionResults, context) == null) {
//            if (right != null) {
//                CjSimpleNameExpression operationSign = binaryExpression.getOperationReference();
//                IElementType operationType = operationSign.getReferencedNameElementType();
//
//                CangJieTypeInfo rightInfo = facade.getTypeInfo(right
//                        , context.replaceContextDependency(ContextDependency.INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE));
//                context.trace.report(INVALID_BINARY_OPERATOR.on(operationSign, new InvalidBinaryData(
//                        ((CjSingleValueToken) operationType).getValue(), typeInfo.getType(), rightInfo.getType()
//                )));
//            }
//        }
        return typeInfo.replaceType(OverloadResolutionResultsUtil.getResultingType(resolutionResults, context))
    }

    private fun assignmentIsNotAnExpressionError(
        expression: CjBinaryExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        facade.checkStatementType(expression, context)
        if (!context.isDebuggerContext && context.isSaveTypeInfo) {
            context.trace.report(Errors.ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression))
        }
        return noTypeInfo(context)
    }

    override fun visitArrayAccessExpression(
        expression: CjArrayAccessExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return components.dataFlowAnalyzer.checkType(
            resolveArrayAccessGetMethod(expression, context),
            expression,
            context
        )
    }

    private fun resolveArrayAccessSpecialMethod(
        arrayAccessExpression: CjArrayAccessExpression,
        rightHandSide: CjExpression?, // only for 'set' method
        oldContext: ExpressionTypingContext,
        traceForResolveResult: BindingTrace,
        isGet: Boolean,
        isImplicit: Boolean
    ): CangJieTypeInfo {
        val arrayExpression = arrayAccessExpression.arrayExpression ?: return noTypeInfo(oldContext)

        val arrayTypeInfo = facade.safeGetTypeInfo(
            arrayExpression,
            oldContext.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(ContextDependency.INDEPENDENT)
        )
        val arrayType = ExpressionTypingUtils.safeGetType(arrayTypeInfo)

        val context = oldContext.replaceDataFlowInfo(arrayTypeInfo.dataFlowInfo)
        val receiver = create(arrayExpression, arrayType, context.trace.bindingContext)
        if (!isGet) assert(rightHandSide != null)

        val call = if (isGet) {
            CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD)
        } else {
            CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide!!, Call.CallType.ARRAY_SET_METHOD)
        }

        val functionResults = components.callResolver.resolveCallWithGivenName(
            context,
            call,
            arrayAccessExpression,

            if (isGet) OperatorNameConventions.GET else OperatorNameConventions.SET

        )

        val indices = arrayAccessExpression.indexExpressions

        val resultTypeInfo = computeAccumulatedInfoForArrayAccessExpression(
            arrayTypeInfo,
            indices,
            rightHandSide,
            isGet,
            context,
            facade
        )

        if ((isImplicit && !functionResults.isSuccess) || !functionResults.isSingleResult) {
            traceForResolveResult.report(
                if (isGet) NO_GET_METHOD.on(arrayAccessExpression) else NO_SET_METHOD.on(
                    arrayAccessExpression
                )
            )
            return resultTypeInfo.clearType()
        }

        if (isGet) {
            traceForResolveResult.record(INDEXED_LVALUE_GET, arrayAccessExpression, functionResults.resultingCall)
        } else {
            traceForResolveResult.record(INDEXED_LVALUE_SET, arrayAccessExpression, functionResults.resultingCall)
        }

        return resultTypeInfo.replaceType(functionResults.resultingDescriptor.returnType)
    }

    fun resolveArrayAccessGetMethod(
        arrayAccessExpression: CjArrayAccessExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, null, context, context.trace, true, false)
    }

    override fun visitBlockExpression(
        expression: CjBlockExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, false)
    }

    private fun visitAssignment(expression: CjBinaryExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        return assignmentIsNotAnExpressionError(expression, context)
    }

    fun operatorOverloading(
        operationType: IElementType?,
        context: ExpressionTypingContext?,
        expression: CjBinaryExpression?
    ): CangJieTypeInfo? {
        val result: CangJieTypeInfo? = null


        //        if (result != null && result.getType() instanceof ErrorType && ((ErrorType) result.getType()).getKind() == ErrorTypeKind.RETURN_TYPE_FOR_FUNCTION) {
//            return null;
//        }
        return result
    }

    private fun visitElvisExpression(
        expression: CjBinaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
        val left = expression.left
        val right = expression.right

        if (left == null || right == null) {
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade)
            return noTypeInfo(context)
        }

        val call =
            createCallForSpecialConstruction(expression, expression.operationReference, Lists.newArrayList(left, right))
        val resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
            call, ControlStructureTypingUtils.ResolveConstruct.ELVIS, Lists.newArrayList("left", "right"),
            Lists.newArrayList(true, false), contextWithExpectedType, null
        )
        val leftTypeInfo = BindingContextUtils.getRecordedTypeInfo(left, context.trace.bindingContext)
        val isLeftFunctionLiteral = isFunctionLiteralArgument(left, context)
        val isLeftCallableReference = isCallableReferenceArgument(left, context)
        val isLeftCollectionLiteral = isCollectionLiteralArgument(left)
        if (leftTypeInfo == null && (isLeftFunctionLiteral || isLeftCallableReference || isLeftCollectionLiteral)) {
            return noTypeInfo(context)
        }
        checkNotNull(leftTypeInfo) { "Left expression was not processed: $expression" }
        val leftType = leftTypeInfo.type
        val rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.bindingContext)
        if (rightTypeInfo == null && isFunctionLiteralOrCallableReference(right, context)) {
            // the type is computed later in call completer according to the '?:' semantics as a function
            return noTypeInfo(context)
        }
        checkNotNull(rightTypeInfo) { "Right expression was not processed: $expression" }
        val loopBreakContinuePossible = leftTypeInfo.jumpOutPossible || rightTypeInfo.jumpOutPossible
        val rightType = rightTypeInfo.type

        // Only left argument DFA is taken into account here: we cannot be sure that right argument is joined
        // (we merge it with right DFA if right argument contains no jump outside)
        var dataFlowInfo = resolvedCall.dataFlowInfoForArguments.getInfo(call.valueArguments[1])

        var type = resolvedCall.resultingDescriptor.returnType
        if (type == null || rightType == null || leftType == null && isNothing(rightType)) return noTypeInfo(
            dataFlowInfo
        )

        if (leftType != null) {
            val leftValue = components.dataFlowValueFactory.createDataFlowValue(left, leftType, context)
            var rightDataFlowInfo = resolvedCall.dataFlowInfoForArguments.resultInfo
            val jumpInRight = isNothing(rightType)
            val nullValue = nullValue(components.builtIns)
            // left argument is considered not-null if it's not-null also in right part or if we have jump in right part
//            if (jumpInRight || !rightDataFlowInfo.getStableNullability(leftValue).canBeNull()) {
//                dataFlowInfo = dataFlowInfo.disequate(leftValue, nullValue, components.languageVersionSettings);
//                if (left instanceof CjBinaryExpressionWithTypeRHS) {
//                    dataFlowInfo = establishSubtypingForTypeRHS((CjBinaryExpressionWithTypeRHS) left, dataFlowInfo, context,
//                            components.languageVersionSettings);
//                }
//            }
            val resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, context)
            dataFlowInfo =
                dataFlowInfo.assign(resultValue, leftValue /*, components.languageVersionSettings*/)
                    .disequate(resultValue, nullValue, components.languageVersionSettings)
            if (!jumpInRight) {
                val rightValue = components.dataFlowValueFactory.createDataFlowValue(right, rightType, context)
                rightDataFlowInfo =
                    rightDataFlowInfo.assign(resultValue, rightValue /*, components.languageVersionSettings*/)
                dataFlowInfo = dataFlowInfo.or(rightDataFlowInfo)
            }
        }

        // Sometimes return type for special call for elvis operator might be nullable,
        // but result is not nullable if the right type is not nullable
        if (!isNullableType(rightType) && isNullableType(type)) {
            type = makeNotNullable(type)
        }
        if (context.contextDependency == ContextDependency.DEPENDENT) {
            return createTypeInfo(type, dataFlowInfo)
        }

        // If break or continue was possible, take condition check info as the jump info
        return createTypeInfo(
            components.dataFlowAnalyzer.checkType(type, expression, contextWithExpectedType),
            dataFlowInfo,
            loopBreakContinuePossible,
            context.dataFlowInfo
        )
    }

    private fun visitComparison(
        expression: CjBinaryExpression,
        context: ExpressionTypingContext,
        operationSign: CjSimpleNameExpression
    ): CangJieTypeInfo {
        val operationType = operationSign.getReferencedNameElementType()

        val referencedName = OperatorConventions.COMPARISON_OPERATIONS_NAMES[operationType]

        return getTypeInfoForBinaryCall(referencedName!!, context, expression)
    }

    fun visitBooleanOperationExpression(
        operationType: IElementType?,
        left: CjExpression?,
        right: CjExpression?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val booleanType: CangJieType = components.builtIns.boolType
        val leftTypeInfo =
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context.replaceExpectedType(booleanType), facade)
        val dataFlowInfo = leftTypeInfo.dataFlowInfo

        val leftScope = ExpressionTypingUtils.newWritableScopeImpl(
            context,
            LexicalScopeKind.LEFT_BOOLEAN_EXPRESSION,
            facade.components.overloadChecker
        )
        // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
        val isAnd = operationType === CjTokens.ANDAND
        val flowInfoLeft =
            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(left, isAnd, context).and(dataFlowInfo)
        val rightScope = if (isAnd) leftScope else ExpressionTypingUtils.newWritableScopeImpl(
            context, LexicalScopeKind.RIGHT_BOOLEAN_EXPRESSION,
            facade.components.overloadChecker
        )

        val contextForRightExpr =
            context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope).replaceExpectedType(booleanType)
        if (right != null) {
            facade.getTypeInfo(right, contextForRightExpr)
        }
        return leftTypeInfo.replaceType(booleanType)
    }

    override fun visitRangeExpression(expression: CjRangeExpression, data: ExpressionTypingContext): CangJieTypeInfo {


        return components.rangeLiteralResolver.resolveRangeLiteral(expression, data)
    }

    override fun visitBinaryExpression(
        expression: CjBinaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = if (ExpressionTypingUtils.isBinaryExpressionDependentOnExpectedType(expression))
            contextWithExpectedType
        else
            contextWithExpectedType.replaceContextDependency(ContextDependency.INDEPENDENT)
                .replaceExpectedType(NO_EXPECTED_TYPE)

        val operationSign: CjSimpleNameExpression = expression.operationReference
        val left = expression.left
        val right = expression.right
        val operationType = operationSign.getReferencedNameElementType()

        val result: CangJieTypeInfo

        if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            val referencedName = OperatorConventions.BINARY_OPERATION_NAMES[operationType]
            result = getTypeInfoForBinaryCall(referencedName!!, context, expression)
        } else if (operationType === CjTokens.ELVIS) {
            //base expression of elvis operator is checked for 'type mismatch', so the whole expression shouldn't be checked
            return visitElvisExpression(expression, context)
        } else if (OperatorConventions.COMPARISON_OPERATIONS_NAMES.containsKey(operationType)) {
            result = visitComparison(expression, context, operationSign)
        } else if (operationType === CjTokens.EQ) {
            result = visitAssignment(expression, context)
        } else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, context)
        } else if (OperatorConventions.BOOLEAN_OPERATIONS_NAMES.containsKey(operationType)) {
            result = visitBooleanOperationExpression(operationType, left, right, context)
        } else {
            context.trace.report(Errors.UNSUPPORTED.on(operationSign, "Unknown operation"))
            result = noTypeInfo(context)
        }

        val value = components.constantExpressionEvaluator.evaluateExpression(
            expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        )
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(
                value,
                expression,
                contextWithExpectedType
            )
        }
        return components.dataFlowAnalyzer.checkType(result, expression, contextWithExpectedType)
    }

    private fun visitAssignmentOperation(
        expression: CjBinaryExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return assignmentIsNotAnExpressionError(expression, context)
    }

    private fun checkOperatorByType(
        leftTypeInfo: CangJieTypeInfo, rightTypeInfo: CangJieTypeInfo,
        operationSign: CjSimpleNameExpression, context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val operationType = operationSign.getReferencedNameElementType()


        if (leftTypeInfo.type is ErrorType) {
            return leftTypeInfo
        }

        val result =
            components.builtIns.matchBinaryOperatorRule(operationType as CjToken, leftTypeInfo.type, rightTypeInfo.type)

        when (result.resultType) {
            BinaryOperatorRuleResultType.LEFT -> {
                return leftTypeInfo
            }

            BinaryOperatorRuleResultType.RIGHT -> {
                return rightTypeInfo
            }

            BinaryOperatorRuleResultType.ERROR -> {
                if (isConventionType(operationType)) {
                    context.trace.report(
                        Errors.INVALID_BINARY_OPERATOR.on(
                            operationSign, InvalidBinaryData(
                                (operationType as CjSingleValueToken).value, leftTypeInfo.type!!, rightTypeInfo.type!!
                            )
                        )
                    )
                } else {
//                    不可被重载的操作符报告错误
                    throw UnsupportedOperationException("Unsupported operator: $operationType")
                }
            }
        }
        return errorTypeInfo(invalidType, context)
    }

    private fun checkOperatorByType(operationSign: CjSimpleNameExpression, type: CangJieType): Boolean {
        val operationType = operationSign.getReferencedNameElementType()

        //        int类型
        return if (isNumber(type) && CjTokens.INT_SUPPORT_OPERATOR.contains(operationType)) {
            true
        } else isFloat(type) && CjTokens.FLOAT_SUPPORT_OPERATOR.contains(operationType)
    }

    override fun visitCallExpression(
        cjCallExpression: CjCallExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getCallExpressionTypeInfo(cjCallExpression, data)
    }

    private fun checkNull(
        expression: CjSimpleNameExpression,
        context: ExpressionTypingContext,
        type: CangJieType?
    ) {
    }

    fun getDefaultType(constantType: IElementType): CangJieType {
        val builtIns = components.builtIns
        return if (constantType === CjNodeTypes.INTEGER_CONSTANT) {
            builtIns.int64Type
        } else if (constantType === CjNodeTypes.FLOAT_CONSTANT) {
            builtIns.float64Type
        } else if (constantType === CjNodeTypes.BOOLEAN_CONSTANT) {
            builtIns.boolType
        } else if (constantType === CjNodeTypes.RUNE_CONSTANT) {
            builtIns.runeType
        } else {
            throw IllegalArgumentException("Unsupported constant type: $constantType")
        }
    }

    /**
     * 检查字面量中的下划线
     *
     * @param elementType
     */
    private fun checkUnderscores(
        expression: CjConstantExpression,
        elementType: IElementType,
        context: ExpressionTypingContext
    ) {
        val text = expression.text.lowercase(Locale.getDefault())

        if (!text.contains("_")) return

        //        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.UnderscoresInNumericLiterals)) {
//            context.trace.report(Errors.UNSUPPORTED_FEATURE.on(expression,
//                    TuplesKt.to(LanguageFeature.UnderscoresInNumericLiterals, components.languageVersionSettings)));
//            return;
//        }
        if (hasIllegalUnderscore(expression.text, elementType)) {
            context.trace.report(Errors.ILLEGAL_UNDERSCORE.on(expression))
        }
    }

    override fun visitStringTemplateExpression(
        expression: CjStringTemplateExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        checkLiteralPrefixAndSuffix(expression, context)

        class StringTemplateVisitor : CjVisitorVoid() {
            var typeInfo: CangJieTypeInfo = noTypeInfo(context)

            override fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression) {
                val entryExpression = entry.expression
                if (entryExpression != null) {
                    val toString = context.scope.findFirstClassifierWithDeprecationStatus(
                        StandardNames.TOSTRING,
                        NoLookupLocation.FROM_BUILTINS
                    )

                    typeInfo = if (toString != null) {
                        //                  约束toString类型
                        facade.getTypeInfo(
                            entryExpression,
                            context.replaceExpectedType(toString.descriptor.defaultType)
                                .replaceDataFlowInfo(typeInfo.dataFlowInfo)
                        )
                    } else {
                        facade.getTypeInfo(entryExpression, context.replaceDataFlowInfo(typeInfo.dataFlowInfo))
                    }
                }
            }

            override fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry) {
                val value =
                    CompileTimeConstantChecker.escapedStringToCharacter(entry.text, entry)
                val diagnostic = value.diagnostic
                if (diagnostic != null) {
                    context.trace.report(diagnostic)
                }
            }
        }

        val visitor = StringTemplateVisitor()
        for (entry in expression.entries) {
            entry.accept(visitor)
        }
        components.constantExpressionEvaluator.evaluateExpression(
            expression,
            context.trace,
            contextWithExpectedType.expectedType
        )
        return components.dataFlowAnalyzer.checkType(
            visitor.typeInfo.replaceType(components.builtIns.stringType),
            expression,

            contextWithExpectedType
        )
    }

    //    根据字面量返回类型信息
    override fun visitConstantExpression(
        expression: CjConstantExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val elementType = expression.node.elementType
        if (elementType === CjNodeTypes.RUNE_CONSTANT || elementType === CjNodeTypes.INTEGER_CONSTANT || elementType === CjNodeTypes.FLOAT_CONSTANT) {
            checkLiteralPrefixAndSuffix(expression, context)
        }


        if (elementType === CjNodeTypes.INTEGER_CONSTANT || elementType === CjNodeTypes.FLOAT_CONSTANT) {
            checkUnderscores(expression, elementType, context)
        }

        val compileTimeConstant = components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )

        if (compileTimeConstant is UnsignedErrorValueTypeConstant) {
            val value = compileTimeConstant.errorValue
            context.trace.report(Errors.UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH.on(expression))

            return createTypeInfo(value.getType(components.moduleDescriptor), context)
        } else if (compileTimeConstant !is IntegerValueTypeConstant) {
            val constantChecker = CompileTimeConstantChecker(context, components.moduleDescriptor, false)
            val constantValue =
                if (compileTimeConstant != null) (compileTimeConstant as TypedCompileTimeConstant<*>).constantValue else null
            val hasError = constantChecker.checkConstantExpressionType(constantValue, expression, context.expectedType)
            if (hasError) {
                return createTypeInfo(
                    constantValue?.getType(components.moduleDescriptor) ?: getDefaultType(elementType),
                    context
                )
            }
        }

        checkNotNull(compileTimeConstant) {
            "CompileTimeConstant should be evaluated for constant expression or an error should be recorded " +
                    expression.text
        }
        return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(compileTimeConstant, expression, context)
    }

    override fun visitCollectionLiteralExpression(
        expression: CjCollectionLiteralExpression, context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return components.collectionLiteralResolver.resolveCollectionLiteral(expression, context)
    }

    fun visitQualifiedExpressionByCaseEnum(
        expression: CjQualifiedExpression,

        argument :List<ValueArgument>,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getQualifiedExpressionTypeInfoByCaseEnum(expression, argument,context)
    }

    fun visitQualifiedExpressionByEnum(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getQualifiedExpressionTypeInfoByEnum(expression, context)
    }

    override fun visitQualifiedExpression(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getQualifiedExpressionTypeInfo(expression, context)
    }

    override fun visitVariable(variable: CjVariable, context: ExpressionTypingContext): CangJieTypeInfo {
        components.localVariableResolver.process(variable, context, context.scope, facade)
        return declarationInIllegalContext(variable, context)
    }

    override fun visitParenthesizedExpression(
        expression: CjParenthesizedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val innerExpression = expression.expression ?: return noTypeInfo(context)
        var result = facade.getTypeInfo(innerExpression, context)
        val resultType = result.type
        if (resultType != null) {
            val innerValue = components.dataFlowValueFactory.createDataFlowValue(innerExpression, resultType, context)
            val resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, resultType, context)
            result = result.replaceDataFlowInfo(
                result.dataFlowInfo.assign(
                    resultValue, innerValue /*,             components.languageVersionSettings*/
                )
            )
        }
        return result
    }
    fun visitSimpleNameExpressionByCaseEnum(
        expression: CjSimpleNameExpression,
        argument :List<ValueArgument>,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {

        val callExpressionResolver = components.callExpressionResolver
        val typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfoByCaseEnum(expression, null, null, context,argument)


        checkNull(expression, context, typeInfo.type)

        components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context) // TODO : Extensions to this
    }
    fun visitSimpleNameExpressionByEnum(
        expression: CjSimpleNameExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
//        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
//            ReservedCheckingKt.checkReservedYield(expression, context.trace);
//        }
//
//        // TODO : other members
//        // TODO : type substitutions???
        val callExpressionResolver = components.callExpressionResolver
        val typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfoByEnum(expression, null, null, context)


        checkNull(expression, context, typeInfo.type)

        components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context) // TODO : Extensions to this
    }

    override fun visitSimpleNameExpression(
        expression: CjSimpleNameExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
//        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
//            ReservedCheckingKt.checkReservedYield(expression, context.trace);
//        }
//
//        // TODO : other members
//        // TODO : type substitutions???
        val callExpressionResolver = components.callExpressionResolver
        val typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, null, null, context)


        checkNull(expression, context, typeInfo.type)

        components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context) // TODO : Extensions to this
    }

    override fun visitUnaryExpression(
        expression: CjUnaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType.replaceContextDependency(ContextDependency.INDEPENDENT)
            .replaceExpectedType(NO_EXPECTED_TYPE)

        val baseExpression = expression.baseExpression ?: return noTypeInfo(context)

        val operationSign = expression.operationReference

        val operationType = operationSign.getReferencedNameElementType()


        // Type check the base expression
        var typeInfo = facade.safeGetTypeInfo(baseExpression, context)
        val type = ExpressionTypingUtils.safeGetType(typeInfo)
        val receiver = create(baseExpression, type, context.trace.bindingContext)

        val call = makeCall(receiver, expression)

        // Conventions for unary operations
        val name = OperatorConventions.UNARY_OPERATION_NAMES[operationType]
        if (name == null) {
            context.trace.report(Errors.UNSUPPORTED.on(operationSign, "visitUnaryExpression"))
            return typeInfo.clearType()
        }

        val deparenthesizedBaseExpression = CjPsiUtil.deparenthesize(baseExpression)

        // a[i]++/-- takes special treatment because it is actually let j = i, arr = a in arr.set(j, a.get(j).inc())
//        if ((operationType == CjTokens.PLUSPLUS || operationType == CjTokens.MINUSMINUS) &&
//                deparenthesizedBaseExpression instanceof CjArrayAccessExpression) {
//            CjExpression stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(
//                    baseExpression.getProject(), context.trace, "e", type);
//            TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(
//                    context.trace, "trace to resolve array access set method for unary expression", expression);
//            ExpressionTypingContext newContext = context.replaceBindingTrace(temporaryBindingTrace);
//            resolveImplicitArrayAccessSetMethod(
//                    (CjArrayAccessExpression) deparenthesizedBaseExpression,
//                    stubExpression,
//                    newContext,
//                    context.trace
//            );
//        }

        // Resolve the operation reference
        val resolutionResults = components.callResolver.resolveCallWithGivenName(
            context, call, expression.operationReference, name
        )

        if (!resolutionResults.isSuccess) {
            return typeInfo.clearType()
        }

        // Computing the return type
        val returnType = resolutionResults.resultingDescriptor.returnType
        val result: CangJieType?
        if (operationType === CjTokens.PLUSPLUS || operationType === CjTokens.MINUSMINUS) {
            checkNotNull(returnType) { "returnType is null for " + resolutionResults.resultingDescriptor }
            if (isUnit(returnType)) {
                result = createErrorType(ErrorTypeKind.UNIT_RETURN_TYPE_FOR_INC_DEC)
                context.trace.report(Errors.INC_DEC_SHOULD_NOT_RETURN_UNIT.on(operationSign))
            } else {
                val receiverType = receiver.type
                if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(returnType, receiverType)) {
                    context.trace.report(
                        Errors.RESULT_TYPE_MISMATCH.on(
                            operationSign,
                            name.asString(),
                            receiverType,
                            returnType
                        )
                    )
                } else {
                    context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression)
                    val stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(
                        baseExpression.project, context.trace, "e", type
                    )
                    checkLValue(context.trace, context, baseExpression, stubExpression, expression, false)
                }
                // x++ type is x type, but ++x type is x.inc() type
                val receiverValue = components.dataFlowValueFactory.createDataFlowValue(
                    (call.explicitReceiver as ReceiverValue?)!!, contextWithExpectedType
                )
                if (expression is CjPrefixExpression) {
                    result = returnType
                } else {
                    result = receiverType
                    // Also record data flow information for x++ value (= x)
                    val returnValue = components.dataFlowValueFactory.createDataFlowValue(
                        expression,
                        receiverType,
                        contextWithExpectedType
                    )
                    typeInfo = typeInfo.replaceDataFlowInfo(
                        typeInfo.dataFlowInfo.assign(
                            returnValue, receiverValue /*      ,
                            components.languageVersionSettings*/
                        )
                    )
                }
            }
        } else {
            result = returnType
        }

        val value = components.constantExpressionEvaluator.evaluateExpression(
            expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        )
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(
                value,
                expression,
                contextWithExpectedType
            )
        }

        return components.dataFlowAnalyzer.checkType(
            typeInfo.replaceType(result),
            expression,
            contextWithExpectedType.replaceDataFlowInfo(typeInfo.dataFlowInfo)
        )
    }

    /**
     * @return `true` iff expression can be assigned to
     */
    fun checkLValue(
        trace: BindingTrace,
        context: ExpressionTypingContext,
        expressionWithParenthesis: CjExpression,
        rightHandSide: CjExpression?,
        operationExpression: CjOperationExpression,
        arraySetMethodAlreadyResolved: Boolean
    ): Boolean {
        val expression = CjPsiUtil.deparenthesize(expressionWithParenthesis)

        //        if (expression instanceof CjArrayAccessExpression) {
//            CjArrayAccessExpression arrayAccessExpression = (CjArrayAccessExpression) expression;
//            CjExpression arrayExpression = arrayAccessExpression.getArrayExpression();
//            if (arrayExpression == null || rightHandSide == null) return false;
//
//            BindingTrace traceWithIndexedLValue;
//            boolean methodSetIsResolved;
//            if (!arraySetMethodAlreadyResolved) {
//                TemporaryBindingTrace ignoreReportsTrace = TemporaryBindingTrace.create(trace, "Trace for checking set function");
//                ExpressionTypingContext findSetterContext = context.replaceBindingTrace(ignoreReportsTrace);
//                CangJieTypeInfo info = resolveArrayAccessSetMethod(arrayAccessExpression, rightHandSide, findSetterContext, ignoreReportsTrace);
//
//                traceWithIndexedLValue = ignoreReportsTrace;
//                methodSetIsResolved = info.getType() != null;
//            } else {
//                traceWithIndexedLValue = trace;
//                methodSetIsResolved = true;
//            }
//
//            IElementType operationType = operationExpression.getOperationReference().getReferencedNameElementType();
//            if (CjTokens.AUGMENTED_ASSIGNMENTS.contains(operationType)
//                    || operationType == CjTokens.PLUSPLUS || operationType == CjTokens.MINUSMINUS) {
//                ResolvedCall<FunctionDescriptor> resolvedCall = traceWithIndexedLValue.get(INDEXED_LVALUE_SET, expression);
//                if (resolvedCall != null && trace.wantsDiagnostics()) {
//                    // Call must be validated with the actual, not temporary trace in order to report operator diagnostic
//                    // Only unary assignment expressions (++, --) and +=/... must be checked, normal assignments have the proper trace
//                    CallCheckerContext callCheckerContext =
//                            new CallCheckerContext(
//                                    context,
//                                    components.deprecationResolver,
//                                    components.moduleDescriptor,
//                                    components.missingSupertypesResolver,
//                                    components.callComponents,
//                                    trace
//                            );
//                    for (CallChecker checker : components.callCheckers) {
//                        checker.check(resolvedCall, expression, callCheckerContext);
//                    }
//                    // Should make sure resolved call for 'set' operator is recorded,
//                    if (trace.get(INDEXED_LVALUE_SET, expression) == null) {
//                        trace.record(INDEXED_LVALUE_SET, expression, resolvedCall);
//                    }
//                }
//            }
//
//            return methodSetIsResolved;
//        }
        val variable = BindingContextUtils.extractVariableDescriptorFromReference(trace.bindingContext, expression)

        var result = true
        var reportOn = expression ?: expressionWithParenthesis
        if (reportOn is CjQualifiedExpression) {
            val selector = reportOn.selectorExpression
            if (selector != null) reportOn = selector
        }

        //        if (variable instanceof PropertyDescriptor) {
//            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variable;
//            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
//            if (propertyDescriptor.isSetterProjectedOut()) {
//                trace.report(SETTER_PROJECTED_OUT.on(reportOn, propertyDescriptor));
//                result = false;
//            }
//            else if (setter != null) {
//                ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expressionWithParenthesis, context.trace.getBindingContext());
//                assert resolvedCall != null
//                        : "Call is not resolved for property setter: " + PsiUtilsKt.getElementTextWithContext(expressionWithParenthesis);
//                checkPropertySetterCall(context.replaceBindingTrace(trace), setter, resolvedCall, reportOn);
//            }
//        }
        if (variable == null) {
            trace.report(Errors.VARIABLE_EXPECTED.on(reportOn))
            result = false
        } else if (!variable.isVar) {
            result = false
        }

        return result
    }

    private fun recordThisOrSuperCallInTraceAndCallExtension(
        context: ExpressionTypingContext,
        descriptor: ReceiverParameterDescriptor,
        expression: CjExpression
    ) {
        val trace = context.trace
        val call = makeCall(expression, null, null, expression, emptyList())
        val resolutionCandidate =
            OldResolutionCandidate.create(
                call, descriptor, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null
            )

        val resolvedCall =
            ResolvedCallImpl.create(
                resolutionCandidate,
                TemporaryBindingTrace.create(trace, "Fake trace for fake 'this' or 'super' resolved call"),
                TracingStrategy.EMPTY,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            )
        resolvedCall.markCallAsCompleted()

        trace.record(BindingContext.RESOLVED_CALL, call, resolvedCall)
        trace.record(BindingContext.CALL, expression, call)

        if (context.trace.wantsDiagnostics()) {
            val callCheckerContext =
                createCallCheckerContext(context)
            for (checker in components.callCheckers) {
                checker.check(resolvedCall, expression, callCheckerContext)
            }
        }
    }

    private fun createCallCheckerContext(context: ExpressionTypingContext): CallCheckerContext {
        return CallCheckerContext(
            context,
            components.deprecationResolver,
            components.moduleDescriptor,
            components.missingSupertypesResolver,
            components.callComponents
        )
    }

    // No class receivers
    private fun resolveToReceiver(
        expression: CjInstanceExpressionWithLabel,
        context: ExpressionTypingContext,
        onlyClassReceivers: Boolean
    ): LabelResolver.LabeledReceiverResolutionResult {
        val labelName = expression.getLabelNameAsName()
        if (labelName != null) {
            val resolutionResult =
                resolveThisOrSuperLabel(expression, context, labelName)
            if (resolutionResult.success()) {
                val receiverParameterDescriptor = resolutionResult.getReceiverParameterDescriptor()
                recordThisOrSuperCallInTraceAndCallExtension(context, receiverParameterDescriptor!!, expression)
                if (onlyClassReceivers && !isDeclaredInClass(
                        receiverParameterDescriptor
                    )
                ) {
                    return labelResolutionSuccess(null)
                }
            }
            return resolutionResult
        } else {
            var result: ReceiverParameterDescriptor? = null
            val receivers = context.scope.getImplicitReceiversHierarchy()
            if (onlyClassReceivers) {
                for (receiver in receivers) {
                    if (isDeclaredInClass(receiver)) {
                        result = receiver
                        break
                    }
                }
            } else if (receivers.isNotEmpty()) {
                // `this` cannot point to context receiver
                for (receiver in receivers) {
                    if (receiver.value !is ContextReceiver) {
                        result = receiver
                        break
                    }
                }
            }
            if (result != null) {
                context.trace.record(
                    BindingContext.REFERENCE_TARGET,
                    expression.instanceReference,
                    result.containingDeclaration
                )
                recordThisOrSuperCallInTraceAndCallExtension(context, result, expression)
            }
            return labelResolutionSuccess(result)
        }
    }

    private fun checkPossiblyQualifiedSuper(
        expression: CjSuperExpression,
        context: ExpressionTypingContext,
        thisReceiver: ReceiverParameterDescriptor
    ): CangJieType? {
        var result: CangJieType? = null
        val thisType = thisReceiver.type
        val supertypes = thisType.constructor.supertypes
        val substitutor = create(thisType)

        val superTypeQualifier = expression.superTypeQualifier
        if (superTypeQualifier != null) {
            val typeElement = superTypeQualifier.typeElement

            var classifierCandidate: DeclarationDescriptor? = null
            var supertype: CangJieType? = null
            var redundantTypeArguments: PsiElement? = null
            if (typeElement is CjUserType) {
                // This may be just a superclass name even if the superclass is generic
                if (typeElement.typeArguments.isEmpty()) {
                    classifierCandidate = components.typeResolver.resolveClass(
                        context.scope,
                        typeElement,
                        context.trace,
                        context.isDebuggerContext
                    )
                } else {
                    supertype =
                        components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true)
                    redundantTypeArguments = typeElement.typeArgumentList
                }
            } else {
                supertype = components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true)
            }

            if (classifierCandidate is TypeAliasDescriptor) {
                classifierCandidate = classifierCandidate.classDescriptor
            }

            if (supertype != null) {
                if (supertypes.contains(supertype)) {
                    result = supertype
                }
            } else if (classifierCandidate is ClassDescriptor) {
                for (declaredSupertype in supertypes) {
                    if (declaredSupertype.constructor == classifierCandidate.typeConstructor) {
                        result = substitutor.safeSubstitute(declaredSupertype, Variance.INVARIANT)
                        break
                    }
                }
            }

            val validClassifier = classifierCandidate != null && !isError(classifierCandidate)
            val validType = supertype != null && !supertype.isError
            if (result == null && (validClassifier || validType)) {
                context.trace.report(Errors.NOT_A_SUPERTYPE.on(superTypeQualifier))
            } else if (redundantTypeArguments != null) {
                context.trace.report(Errors.TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.on(redundantTypeArguments))
            }

            //            if (!components.languageVersionSettings.supportsFeature(LanguageFeature.QualifiedSupertypeMayBeExtendedByOtherSupertype) &&
//                    result != null &&
//                    (validClassifier || validType)
//            ) {
//                checkResolvedExplicitlyQualifiedSupertype(context.trace, result, supertypes, superTypeQualifier);
//            }
        } else {
            if (isPossiblyAmbiguousUnqualifiedSuper(expression, supertypes)) {
                val supertypesResolvedFromContextWithEqualsMigration =
                    resolveUnqualifiedSuperFromExpressionContext(
                        expression, supertypes, components.builtIns.anyType
                    )
                val supertypesResolvedFromContext = supertypesResolvedFromContextWithEqualsMigration.first
                if (supertypesResolvedFromContextWithEqualsMigration.second) {
                    context.trace.record(BindingContext.SUPER_EXPRESSION_FROM_ANY_MIGRATION, expression, true)
                }
                if (supertypesResolvedFromContext.size == 1) {
                    val singleResolvedType = supertypesResolvedFromContext.iterator().next()
                    result = substitutor.substitute(singleResolvedType, Variance.INVARIANT)
                } else if (supertypesResolvedFromContext.isEmpty()) {
                    // No supertype found, either with concrete or abstract members.
                    // Resolve to 'Any' (this will cause diagnostics for unresolved member reference).
                    result = components.builtIns.anyType
                } else {
                    context.trace.report(Errors.AMBIGUOUS_SUPER.on(expression))
                }
            } else {
                // supertypes may be empty when all the supertypes are error types (are not resolved, for example)
                val type = if (supertypes.isEmpty())
                    components.builtIns.anyType
                else
                    supertypes.iterator().next()
                result = substitutor.substitute(type, Variance.INVARIANT)
            }
        }
        if (result != null) {
            if (isInterface(thisType.constructor.declarationDescriptor)) {
                if (isClass(result.constructor.declarationDescriptor)) {
                    context.trace.report(Errors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE.on(expression))
                }
            }
            context.trace.recordType(expression.instanceReference, result)
            context.trace.record(
                BindingContext.REFERENCE_TARGET, expression.instanceReference,
                result.constructor.declarationDescriptor
            )
            context.trace.record(BindingContext.THIS_TYPE_FOR_SUPER_EXPRESSION, expression, thisType)
        }

        context.trace.recordScope(context.scope, superTypeQualifier)
        return result
    }

    override fun visitThisExpression(expression: CjThisExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        var result: CangJieType? = null
        val resolutionResult = resolveToReceiver(expression, context, false)

        when (resolutionResult.code) {
            LabelResolver.LabeledReceiverResolutionResult.Code.LABEL_RESOLUTION_ERROR -> {}
            LabelResolver.LabeledReceiverResolutionResult.Code.NO_THIS -> context.trace.report(
                Errors.NO_THIS.on(
                    expression
                )
            )

            LabelResolver.LabeledReceiverResolutionResult.Code.SUCCESS -> {
                val descriptor = resolutionResult.getReceiverParameterDescriptor()
                context.trace.record(BindingContext.THIS_REFERENCE_TARGET, expression.instanceReference, descriptor)
                result = descriptor!!.type
                context.trace.recordType(expression.instanceReference, result)
            }
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(result, context, expression)
    }

    override fun visitSuperExpression(
        expression: CjSuperExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val resolutionResult = resolveToReceiver(expression, context, true)

        if (!CjPsiUtil.isLHSOfDot(expression)) {
            context.trace.report(Errors.SUPER_IS_NOT_AN_EXPRESSION.on(expression, expression.text))
            return errorInSuper(expression, context)
        }

        when (resolutionResult.code) {
            LabelResolver.LabeledReceiverResolutionResult.Code.LABEL_RESOLUTION_ERROR ->                 // The error is already reported
                return errorInSuper(expression, context)

            LabelResolver.LabeledReceiverResolutionResult.Code.NO_THIS -> {
                context.trace.report(Errors.SUPER_NOT_AVAILABLE.on(expression))
                return errorInSuper(expression, context)
            }

            LabelResolver.LabeledReceiverResolutionResult.Code.SUCCESS -> {
                val result = checkPossiblyQualifiedSuper(
                    expression, context,
                    resolutionResult.getReceiverParameterDescriptor()!!
                )
                if (result != null) {
                    context.trace.recordType(expression.instanceReference, result)
                }
                return components.dataFlowAnalyzer.createCheckedTypeInfo(result, context, expression)
            }
        }
        throw IllegalStateException("Unknown code: " + resolutionResult.code)
    }

    private fun errorInSuper(expression: CjSuperExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        val superTypeQualifier = expression.superTypeQualifier
        if (superTypeQualifier != null) {
            components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true)
        }
        return noTypeInfo(context)
    }

    override fun visitDeclaration(dcl: CjDeclaration, context: ExpressionTypingContext): CangJieTypeInfo {
        return declarationInIllegalContext(dcl, context)
    } //

    //        /*package*/ CangJieTypeInfo resolveImplicitArrayAccessSetMethod(
    //             CjArrayAccessExpression arrayAccessExpression,
    //             CjExpression rightHandSide,
    //             ExpressionTypingContext context,
    //             BindingTrace traceForResolveResult
    //    ) {
    //        return resolveArrayAccessSpecialMethod(arrayAccessExpression, rightHandSide, context, traceForResolveResult, false, true);
    //    }
    //
    //
    //
    //    private CangJieTypeInfo resolveArrayAccessSpecialMethod(
    //             CjArrayAccessExpression arrayAccessExpression,
    //              CjExpression rightHandSide, //only for 'set' method
    //             ExpressionTypingContext oldContext,
    //             BindingTrace traceForResolveResult,
    //            boolean isGet,
    //            boolean isImplicit
    //    ) {
    //       CjExpression arrayExpression = arrayAccessExpression.getArrayExpression();
    //        if (arrayExpression == null) return TypeInfoFactoryKt.noTypeInfo(oldContext);
    //
    //
    //        CangJieTypeInfo arrayTypeInfo = facade.safeGetTypeInfo(arrayExpression, oldContext.replaceExpectedType(NO_EXPECTED_TYPE)
    //                .replaceContextDependency(ContextDependency.INDEPENDENT));
    //        CangJieType arrayType = ExpressionTypingUtils.safeGetType(arrayTypeInfo);
    //
    //        ExpressionTypingContext context = oldContext.replaceDataFlowInfo(arrayTypeInfo.getDataFlowInfo());
    //        ExpressionReceiver receiver = ExpressionReceiver.Companion.create(arrayExpression, arrayType, context.trace.getBindingContext());
    //        if (!isGet) assert rightHandSide != null;
    //
    //        Call call = isGet
    //                ? CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD)
    //                : CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide, Call.CallType.ARRAY_SET_METHOD);
    //        OverloadResolutionResults<FunctionDescriptor> functionResults = components.callResolver.resolveCallWithGivenName(
    //                context, call, arrayAccessExpression, isGet ? OperatorNameConventions.GET : OperatorNameConventions.SET);
    //
    //        List<CjExpression> indices = arrayAccessExpression.getIndexExpressions();
    //
    //      CangJieTypeInfo resultTypeInfo =
    //                computeAccumulatedInfoForArrayAccessExpression(arrayTypeInfo, indices, rightHandSide, isGet, context, facade);
    //
    //        if ((isImplicit && !functionResults.isSuccess()) || !functionResults.isSingleResult()) {
    //            traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
    //            return resultTypeInfo.clearType();
    //        }
    //
    //        if (isGet) {
    //            traceForResolveResult.record(INDEXED_LVALUE_GET, arrayAccessExpression, functionResults.getResultingCall());
    //        } else {
    //            traceForResolveResult.record(INDEXED_LVALUE_SET, arrayAccessExpression, functionResults.getResultingCall());
    //        }
    //
    //        return resultTypeInfo.replaceType(functionResults.getResultingDescriptor().getReturnType());
    //    }
    companion object {
        fun isLValue(expression: CjSimpleNameExpression, parent: PsiElement?): Boolean {
            if (parent !is CjBinaryExpression) {
                return false
            }

            //        if (!OperatorConventions.BINARY_OPERATION_NAMES.containsKey(binaryExpression.getOperationToken()) &&
//                !CjTokens.ALL_ASSIGNMENTS.contains(binaryExpression.getOperationToken())) {
//            return false;
//        }
            return PsiTreeUtil.isAncestor(parent.left, expression, false)
        }

        private fun isLValueOrUnsafeReceiver(expression: CjSimpleNameExpression): Boolean {
            val parent = PsiTreeUtil.skipParentsOfType(
                expression,
                CjParenthesizedExpression::class.java
            )
            if (parent is CjQualifiedExpression) {
                // so we have to analyze its nullability here

                return parent.operationSign === CjTokens.DOT &&
                        parent.receiverExpression === CjPsiUtil.deparenthesize(expression)
            }

            return isLValue(expression, parent)
        }

        private fun getTypeInfo(
            expression: CjExpression,
            facade: ExpressionTypingInternals,
            context: ExpressionTypingContext,
            forceExpressionResolve: Boolean
        ): CangJieTypeInfo? {
            return if (forceExpressionResolve) {
                facade.getTypeInfo(expression, context)
            } else {
                BindingContextUtils.getRecordedTypeInfo(expression, context.trace.bindingContext)
            }
        }

        private fun computeAccumulatedInfoForArrayAccessExpression(
            arrayTypeInfo: CangJieTypeInfo,
            indices: List<CjExpression>,
            rightHandSide: CjExpression?,
            isGet: Boolean,
            context: ExpressionTypingContext,
            facade: ExpressionTypingInternals
        ): CangJieTypeInfo {
            var accumulatedTypeInfo: CangJieTypeInfo? = null
            val forceResolve = !context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)

            // The accumulated data flow info of all index expressions is saved on the last index
            if (indices.isNotEmpty()) {
                accumulatedTypeInfo = getTypeInfo(indices[indices.size - 1], facade, context, forceResolve)
            }

            if (!isGet && rightHandSide != null) {
                accumulatedTypeInfo = getTypeInfo(rightHandSide, facade, context, forceResolve)
            }

            return accumulatedTypeInfo ?: arrayTypeInfo
        }

        //字面量前缀和后缀
        private fun checkLiteralPrefixAndSuffix(expression: PsiElement, context: ExpressionTypingContext) {
            if (expression is StubBasedPsiElement<*> && expression.stub != null) {
                return
            }

            checkLiteralPrefixOrSuffix(PsiTreeUtil.prevLeaf(expression), context)
            checkLiteralPrefixOrSuffix(PsiTreeUtil.nextLeaf(expression), context)
        }

        //    前缀或后缀
        private fun checkLiteralPrefixOrSuffix(prefixOrSuffix: PsiElement?, context: ExpressionTypingContext) {
            if (illegalLiteralPrefixOrSuffix(prefixOrSuffix)) {
                context.trace.report(Errors.UNSUPPORTED.on(prefixOrSuffix, "literal prefixes and suffixes"))
            }
        }

        private fun illegalLiteralPrefixOrSuffix(element: PsiElement?): Boolean {
            if (element == null) return false

            val elementType = element.node.elementType
            return elementType === CjTokens.IDENTIFIER || elementType === CjTokens.INTEGER_LITERAL || elementType === CjTokens.FLOAT_LITERAL ||
                    elementType is CjKeywordToken
        }

        private fun declarationInIllegalContext(
            declaration: CjDeclaration,
            context: ExpressionTypingContext
        ): CangJieTypeInfo {
            context.trace.report(Errors.DECLARATION_IN_ILLEGAL_CONTEXT.on(declaration))
            return noTypeInfo(context)
        }

        private fun isDeclaredInClass(receiver: ReceiverParameterDescriptor): Boolean {
            return receiver.containingDeclaration is ClassDescriptor
        }

        private fun checkResolvedExplicitlyQualifiedSupertype(
            trace: BindingTrace,
            result: CangJieType,
            supertypes: Collection<CangJieType>,
            superTypeQualifier: CjTypeReference
        ) {
            if (supertypes.size > 1) {
                val resultClassifierDescriptor = result.constructor.declarationDescriptor
                for (otherSupertype in supertypes) {
                    val otherSupertypeClassifierDescriptor = otherSupertype.constructor.declarationDescriptor
                    if (otherSupertypeClassifierDescriptor === resultClassifierDescriptor) {
                        continue
                    }
                    if (CangJieTypeChecker.DEFAULT.isSubtypeOf(otherSupertype, result)) {
                        trace.report(
                            Errors.QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE.on(
                                superTypeQualifier,
                                otherSupertype
                            )
                        )
                        break
                    }
                }
            }
        }
    }
}
