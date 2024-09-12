package com.huawei.cangjie.types.expressions

import com.google.common.collect.Lists
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContextUtils
import com.huawei.cangjie.resolve.ModifierCheckerCore
import com.huawei.cangjie.resolve.ModifiersChecker
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.tower.CangJieResolutionCallbacksImpl
import com.huawei.cangjie.resolve.calls.tower.LambdaContextInfo
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.CommonSupertypes
import com.huawei.cangjie.types.ErrorUtils.createErrorType
import com.huawei.cangjie.types.checker.TrailingCommaChecker
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import com.huawei.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createDataFlowInfoForArgumentsOfTryCall
import com.huawei.cangjie.types.expressions.ExpressionTypingUtils.newWritableScopeImpl
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo
import com.intellij.psi.PsiElement

class ControlStructureTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    private fun checkCondition(
        condition: CjExpression?,
        context: ExpressionTypingContext
    ): DataFlowInfo {
        if (condition != null) {
            val conditionContext: ExpressionTypingContext =
                context.replaceExpectedType(components.builtIns.boolType)
                    .replaceContextDependency(ContextDependency.INDEPENDENT)
            val typeInfo: CangJieTypeInfo = facade.getTypeInfo(condition, conditionContext)

            return components.dataFlowAnalyzer.checkType(typeInfo, condition, conditionContext).dataFlowInfo
        }
        return context.dataFlowInfo
    }

    private fun resolveTryExpressionWithNewInference(
        tryExpression: CjTryExpression,
        tryInputContext: ExpressionTypingContext
    ): CangJieTypeInfo {
        val tryBlock = tryExpression.tryBlock
        val catchClauses = tryExpression.catchClauses
        val finallySection = tryExpression.finallyBlock

        val dataFlowInfoBeforeTry = tryInputContext.dataFlowInfo

        val tryVisitor = PreliminaryLoopVisitor.visitTryBlock(tryExpression)
        val tryOutputContext = tryInputContext.replaceDataFlowInfo(
            tryVisitor.clearDataFlowInfoForAssignedLocalVariables(
                dataFlowInfoBeforeTry,
                components.languageVersionSettings
            )
        )
        val dataFlowInfoAfterTry = tryOutputContext.dataFlowInfo

        val catchBlocks = mutableListOf<CjExpression>()
        val catchClausesBlocksAndParameters = mutableListOf<Pair<CjExpression, VariableDescriptor>>()

        for (catchClause in catchClauses) {
            val catchParameter = catchClause.catchParameter
            val catchBody = catchClause.catchBody
            if (catchParameter != null) {
                val variableDescriptor = resolveAndCheckCatchParameter(catchParameter, tryInputContext)
                if (catchBody != null) {
                    catchBlocks.add(catchBody)
                    catchClausesBlocksAndParameters.add(Pair(catchBody, variableDescriptor))
                }
            }
        }

        val finallyBlock = finallySection?.finalExpression

        val arguments = mutableListOf<CjExpression>(tryBlock)
        arguments.addAll(catchBlocks)

        val callForTry = createCallForSpecialConstruction(tryExpression, tryExpression, arguments)

        val dataFlowInfoForArguments =
            createDataFlowInfoForArgumentsOfTryCall(callForTry, dataFlowInfoBeforeTry, dataFlowInfoBeforeTry)

        val resolvedCall = components.controlStructureTypingUtils.resolveTryAsCall(
            callForTry,
            catchClausesBlocksAndParameters,
            tryInputContext,
            dataFlowInfoForArguments
        )
        val resultType = resolvedCall.resultingDescriptor.returnType

        val bindingContext = tryInputContext.trace.bindingContext

        return processTryBranches(
            tryExpression,
            tryBlock,
            tryInputContext,
            catchBlocks,
            finallyBlock,
            bindingContext,
            resultType
        )

    }

    private fun processTryBranches(
        tryExpression: CjTryExpression,
        tryBlock: CjBlockExpression,
        context: ExpressionTypingContext,
        catchBlocks: List<CjExpression>,
        finallyBlock: CjBlockExpression?,
        bindingContext: BindingContext,
        resultType: CangJieType?
    ): CangJieTypeInfo {
        val tryInfo = BindingContextUtils.getRecordedTypeInfo(tryBlock, bindingContext)
        val dataFlowInfoAfterTry = tryInfo?.dataFlowInfo ?: DataFlowInfo.EMPTY
        val nothingInAllCatchBranches = isCatchBranchesReturnsNothing(catchBlocks, bindingContext)

        val tryOutputContext =
            getCleanedContextFromTryWithAssignmentsToVar(tryExpression, nothingInAllCatchBranches, context)
                .replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)

        val result = createTypeInfo(resultType, tryOutputContext)

        return if (finallyBlock != null) {
            facade.getTypeInfo(finallyBlock, tryOutputContext).replaceType(resultType)
        } else if (!nothingInAllCatchBranches || tryInfo == null) {
            result
        } else {
            createTypeInfo(
                components.dataFlowAnalyzer.checkType(resultType, tryExpression, tryOutputContext),
                dataFlowInfoAfterTry
            )
        }
    }


    override fun visitTryExpression(
        expression: CjTryExpression,
        typingContext: ExpressionTypingContext
    ): CangJieTypeInfo {
        expression.catchClauses.forEach { catchClause ->
            val parameters = catchClause.parameterList
            if (parameters != null && parameters.stub == null) {
                TrailingCommaChecker.check(
                    parameters.trailingComma,
                    typingContext.trace,
                    typingContext.languageVersionSettings
                )
            }
        }

        if (typingContext.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
            return resolveTryExpressionWithNewInference(expression, typingContext)
        }
        val context = typingContext.replaceContextDependency(ContextDependency.INDEPENDENT)
        val tryBlock = expression.tryBlock
        val catchClauses = expression.catchClauses
        val finallyBlock = expression.finallyBlock
        val types = mutableListOf<CangJieType>()
        var nothingInAllCatchBranches = true
        for (catchClause in catchClauses) {
            val catchParameter = catchClause.catchParameter
            val catchBody = catchClause.catchBody
            var nothingInCatchBranch = false
            if (catchParameter != null) {
                val variableDescriptor = resolveAndCheckCatchParameter(catchParameter, context)

                if (catchBody != null) {
                    val catchScope = newWritableScopeImpl(context, LexicalScopeKind.CATCH, components.overloadChecker)
                    catchScope.addVariableDescriptor(variableDescriptor)
                    val type = facade.getTypeInfo(catchBody, context.replaceScope(catchScope)).type
                    if (type != null) {
                        types.add(type)
                        if (CangJieBuiltIns.isNothing(type)) {
                            nothingInCatchBranch = true
                        }
                    }
                }
            }
            if (!nothingInCatchBranch) {
                nothingInAllCatchBranches = false
            }
        }

        val tryResult = facade.getTypeInfo(tryBlock, context)
        val tryOutputContext =
            getCleanedContextFromTryWithAssignmentsToVar(expression, nothingInAllCatchBranches, context)

        var result = noTypeInfo(tryOutputContext)
        if (finallyBlock != null) {
            result = facade.getTypeInfo(finallyBlock.finalExpression!!, tryOutputContext)
        } else if (nothingInAllCatchBranches) {
            result = tryResult
        }

        val type = tryResult.type
        if (type != null) {
            types.add(type)
        }
        return if (types.isEmpty()) {
            result.clearType()
        } else {
            result.replaceType(CommonSupertypes.commonSupertype(types))
        }
    }

    private fun checkCatchParameterDeclaration(
        catchParameter: CjParameter,
        context: ExpressionTypingContext
    ) {
        components.identifierChecker.checkDeclaration(catchParameter, context.trace)
        val modifiersChecking: ModifiersChecker.ModifiersCheckingProcedure =
            components.modifiersChecker.withTrace(context.trace)
        modifiersChecking.checkParameterHasNoLetOrVar(
            catchParameter,
            Errors.LET_OR_VAR_ON_CATCH_PARAMETER
        )
        ModifierCheckerCore.check(
            catchParameter,
            context.trace,
            null,
            components.languageVersionSettings
        )

        if (catchParameter.hasDefaultValue()) {
            context.trace.report(
                Errors.CATCH_PARAMETER_WITH_DEFAULT_VALUE.on(
                    catchParameter
                )
            )
        }
    }

    private fun resolveAndCheckCatchParameter(
        catchParameter: CjParameter,
        context: ExpressionTypingContext
    ): VariableDescriptor {
        checkCatchParameterDeclaration(catchParameter, context)

        val variableDescriptor = components.descriptorResolver
            .resolveLocalVariableDescriptor(context.scope, catchParameter, context.trace)
        val catchParameterType = variableDescriptor.type
        checkCatchParameterType(catchParameter, catchParameterType, context)
        val throwableType = components.builtIns.throwable.defaultType
        components.dataFlowAnalyzer.checkType(
            catchParameterType,
            catchParameter,
            context.replaceExpectedType(throwableType)
        )
        return variableDescriptor
    }

    override fun visitThrowExpression(
        expression: CjThrowExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val thrownExpression = expression.thrownExpression
        if (thrownExpression != null) {
            val throwableType =
                components.builtIns.throwableType
            facade.getTypeInfo(
                thrownExpression,
                context.replaceExpectedType(throwableType)
                    .replaceContextDependency(ContextDependency.INDEPENDENT)
            )
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(
            components.builtIns.nothingType,
            context,
            expression
        )
    }

    private fun getCleanedContextFromTryWithAssignmentsToVar(
        tryExpression: CjTryExpression,
        nothingInAllCatchBranches: Boolean,
        context: ExpressionTypingContext
    ): ExpressionTypingContext {
        var context = context
        context = context.replaceExpectedType(NO_EXPECTED_TYPE)
//        if (!nothingInAllCatchBranches && facade.components.languageVersionSettings.supportsFeature(LanguageFeature.SoundSmartCastsAfterTry)) {
//            val tryVisitor: PreliminaryLoopVisitor =
//                PreliminaryLoopVisitor.visitTryBlock(tryExpression)
//            context = context.replaceDataFlowInfo(
//                tryVisitor.clearDataFlowInfoForAssignedLocalVariables(
//                    context.dataFlowInfo,
//                    components.languageVersionSettings
//                )
//            )
//        }
        return context
    }

    override fun visitIfExpression(expression: CjIfExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        val condition = expression.condition
        val conditionDataFlowInfo: DataFlowInfo =
            checkCondition(condition, context)
        val loopBreakContinuePossibleInCondition = condition != null && containsJumpOutOfLoop(condition, context)

        val elseBranch = expression.`else`
        val thenBranch = expression.then

        val thenScope: LexicalWritableScope =
            newWritableScopeImpl(
                context,
                LexicalScopeKind.THEN,
                components.overloadChecker
            )
        val elseScope: LexicalWritableScope =
            newWritableScopeImpl(
                context,
                LexicalScopeKind.ELSE,
                components.overloadChecker
            )
        val thenInfo =
            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context)
                .and(conditionDataFlowInfo)
        val elseInfo =
            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context)
                .and(conditionDataFlowInfo)

        if (elseBranch == null) {
            if (thenBranch != null) {
                val result: CangJieTypeInfo = getTypeInfoWhenOnlyOneBranchIsPresent(
                    thenBranch, thenScope, thenInfo, elseInfo, context, expression
                )
                // If jump was possible, take condition check info as the jump info
                return if (result.jumpOutPossible)
                    result.replaceJumpOutPossible(true).replaceJumpFlowInfo(conditionDataFlowInfo)
                else
                    result
            }
            return createTypeInfo(components.builtIns.unitType, thenInfo.or(elseInfo))
        }
        if (thenBranch == null) {
            return getTypeInfoWhenOnlyOneBranchIsPresent(
                elseBranch, elseScope, elseInfo, thenInfo, context, expression
            )
        }
        val psiFactory = CjPsiFactory(expression.getProject(), false)
        val thenBlock: CjBlockExpression = psiFactory.wrapInABlockWrapper(thenBranch)
        val elseBlock: CjBlockExpression = psiFactory.wrapInABlockWrapper(elseBranch)
        val callForIf: Call =
            createCallForSpecialConstruction(
                expression,
                expression,
                Lists.newArrayList<CjBlockExpression>(thenBlock, elseBlock)
            )
        val dataFlowInfoForArguments: MutableDataFlowInfoForArguments =
            ControlStructureTypingUtils.createDataFlowInfoForArgumentsForIfCall(
                callForIf,
                conditionDataFlowInfo,
                thenInfo,
                elseInfo
            )
        val resolvedCall: ResolvedCall<FunctionDescriptor> =
            components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                callForIf,
                ControlStructureTypingUtils.ResolveConstruct.IF,
                Lists.newArrayList<String>("thenBranch", "elseBranch"),
                Lists.newArrayList<Boolean>(false, false),
                context,
                dataFlowInfoForArguments
            )

        return processIfBranches(
            expression, context, conditionDataFlowInfo,
            loopBreakContinuePossibleInCondition, elseBranch, thenBranch, resolvedCall
        )
    }

    override fun visitDoWhileExpression(
        expression: CjDoWhileExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return visitDoWhileExpression(expression, context, false)
    }

    fun visitDoWhileExpression(
        expression: CjDoWhileExpression,
        contextWithExpectedType: ExpressionTypingContext,
        isStatement: Boolean
    ): CangJieTypeInfo {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(
            expression,
            contextWithExpectedType,
            facade
        )

        var context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
            ContextDependency.INDEPENDENT
        )
        var conditionScope = context.scope
        val loopVisitor = PreliminaryLoopVisitor.visitLoop(expression)
        context = context.replaceDataFlowInfo(
            loopVisitor.clearDataFlowInfoForAssignedLocalVariables(
                context.dataFlowInfo,
                components.languageVersionSettings
            )
        )

        var bodyTypeInfo: CangJieTypeInfo
        val body = expression.body
        if (body is CjLambdaExpression) {
            bodyTypeInfo = facade.getTypeInfo(body, context)
        } else if (body != null) {
            val writableScope =
                newWritableScopeImpl(context, LexicalScopeKind.DO_WHILE_BODY, components.overloadChecker)
            conditionScope = writableScope
            val block = if (body is CjBlockExpression) {
                body.statements
            } else {
                listOf(body)
            }
            bodyTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                writableScope, block, CoercionStrategy.NO_COERCION, context
            )
        } else {
            bodyTypeInfo = noTypeInfo(context)
        }

        val condition = expression.condition
        val conditionDataFlowInfo = checkCondition(condition, context.replaceScope(conditionScope))
        var dataFlowInfo: DataFlowInfo
        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context)
                .and(conditionDataFlowInfo)
        } else {
            dataFlowInfo = context.dataFlowInfo
        }

        if (body != null) {
            dataFlowInfo = dataFlowInfo.and(
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(
                    bodyTypeInfo.jumpFlowInfo,
                    components.languageVersionSettings
                )
            )
        }

        return components.dataFlowAnalyzer
            .checkType(bodyTypeInfo.replaceType(components.builtIns.unitType), expression, contextWithExpectedType)
            .replaceDataFlowInfo(dataFlowInfo)
    }

    override fun visitWhileExpression(
        expression: CjWhileExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return visitWhileExpression(expression, context, false)
    }


    fun visitWhileExpression(
        expression: CjWhileExpression,
        contextWithExpectedType: ExpressionTypingContext,
        isStatement: Boolean
    ): CangJieTypeInfo {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(
            expression,
            contextWithExpectedType,
            facade
        )

        var context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
            ContextDependency.INDEPENDENT
        )
        val loopVisitor = PreliminaryLoopVisitor.visitLoop(expression)
        context = context.replaceDataFlowInfo(
            loopVisitor.clearDataFlowInfoForAssignedLocalVariables(
                context.dataFlowInfo,
                components.languageVersionSettings
            )
        )

        val condition = expression.condition
        var dataFlowInfo = checkCondition(condition, context)

        val body = expression.body
        val conditionInfo =
            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(dataFlowInfo)
        val bodyTypeInfo: CangJieTypeInfo = if (body != null) {
            val scopeToExtend = newWritableScopeImpl(context, LexicalScopeKind.WHILE_BODY, components.overloadChecker)
            components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                scopeToExtend, listOf(body),
                CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(conditionInfo)
            )
        } else {
            noTypeInfo(conditionInfo)
        }

        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context)
                .and(dataFlowInfo)
        }

        if (body != null && CjPsiUtil.isTrueConstant(condition)) {
            dataFlowInfo = dataFlowInfo.and(
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(
                    bodyTypeInfo.jumpFlowInfo,
                    components.languageVersionSettings
                )
            )
        }

        return components.dataFlowAnalyzer
            .checkType(bodyTypeInfo.replaceType(components.builtIns.unitType), expression, contextWithExpectedType)
            .replaceDataFlowInfo(dataFlowInfo)
    }

    private fun processIfBranches(
        ifExpression: CjIfExpression,
        context: ExpressionTypingContext,
        conditionDataFlowInfo: DataFlowInfo,
        loopBreakContinuePossibleInCondition: Boolean,
        elseBranch: CjExpression,
        thenBranch: CjExpression,
        resolvedCall: ResolvedCall<FunctionDescriptor>
    ): CangJieTypeInfo {
        val bindingContext = context.trace.bindingContext
        val thenTypeInfo = BindingContextUtils.getRecordedTypeInfo(thenBranch, bindingContext)
        val elseTypeInfo = BindingContextUtils.getRecordedTypeInfo(elseBranch, bindingContext)

        val isThenPostponed = ArgumentTypeResolver.isFunctionLiteralOrCallableReference(thenBranch, context)
        val isElsePostponed = ArgumentTypeResolver.isFunctionLiteralOrCallableReference(thenBranch, context)

        assert(thenTypeInfo != null || elseTypeInfo != null || isThenPostponed || isElsePostponed) {
            "Both branches of if expression were not processed: ${ifExpression.text}"
        }

        if (thenTypeInfo == null && elseTypeInfo == null) {
            return noTypeInfo(context)
        }

        val resultType = resolvedCall.resultingDescriptor.returnType
        var loopBreakContinuePossible = loopBreakContinuePossibleInCondition
        lateinit var resultDataFlowInfo: DataFlowInfo

        if (elseTypeInfo == null) {
            loopBreakContinuePossible = loopBreakContinuePossible or thenTypeInfo!!.jumpOutPossible
            resultDataFlowInfo = thenTypeInfo.dataFlowInfo
        } else if (thenTypeInfo == null) {
            loopBreakContinuePossible = loopBreakContinuePossible or elseTypeInfo.jumpOutPossible
            resultDataFlowInfo = elseTypeInfo.dataFlowInfo
        } else {
            val thenType = thenTypeInfo.type
            val elseType = elseTypeInfo.type
            var thenDataFlowInfo = thenTypeInfo.dataFlowInfo
            var elseDataFlowInfo = elseTypeInfo.dataFlowInfo

            if (resultType != null && thenType != null && elseType != null) {
                val resultValue = components.dataFlowValueFactory.createDataFlowValue(ifExpression, resultType, context)
                val thenValue = components.dataFlowValueFactory.createDataFlowValue(thenBranch, thenType, context)
                thenDataFlowInfo =
                    thenDataFlowInfo.assign(resultValue, thenValue/* components.languageVersionSettings*/)
                val elseValue = components.dataFlowValueFactory.createDataFlowValue(elseBranch, elseType, context)
                elseDataFlowInfo =
                    elseDataFlowInfo.assign(resultValue, elseValue /*components.languageVersionSettings*/)
            }

            loopBreakContinuePossible =
                loopBreakContinuePossible or thenTypeInfo.jumpOutPossible or elseTypeInfo.jumpOutPossible

            val jumpInThen = thenType != null && CangJieBuiltIns.isNothing(thenType)
            val jumpInElse = elseType != null && CangJieBuiltIns.isNothing(elseType)

            resultDataFlowInfo = if (thenType == null && elseType == null) {
                thenDataFlowInfo or elseDataFlowInfo
            } else if (thenType == null || (jumpInThen && !jumpInElse)) {
                elseDataFlowInfo
            } else if (elseType == null || (jumpInElse && !jumpInThen)) {
                thenDataFlowInfo
            } else {
                thenDataFlowInfo or elseDataFlowInfo
            }

            if ((thenType == null && jumpInElse) || (elseType == null && jumpInThen)) {
                return noTypeInfo(resultDataFlowInfo)
            }
        }

        // If break or continue was possible, take condition check info as the jump info
        return createTypeInfo(
            components.dataFlowAnalyzer.checkType(resultType, ifExpression, context),
            resultDataFlowInfo,
            loopBreakContinuePossible,
            if (loopBreakContinuePossibleInCondition) context.dataFlowInfo else conditionDataFlowInfo
        )
    }

    private fun getTypeInfoWhenOnlyOneBranchIsPresent(
        presentBranch: CjExpression,
        presentScope: LexicalWritableScope,
        presentInfo: DataFlowInfo,
        otherInfo: DataFlowInfo,
        context: ExpressionTypingContext,
        ifExpression: CjIfExpression
    ): CangJieTypeInfo {
        val newContext: ExpressionTypingContext =
            context.replaceDataFlowInfo(presentInfo)
                .replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)
        val typeInfo: CangJieTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
            presentScope,
            listOf<CjExpression>(presentBranch),
            CoercionStrategy.NO_COERCION,
            newContext
        )
        val type = typeInfo.type
        val dataFlowInfo: DataFlowInfo =
            if (type != null && CangJieBuiltIns.isNothing(type)) {
                otherInfo
            } else {
                typeInfo.dataFlowInfo.or(otherInfo)
            }
        return components.dataFlowAnalyzer.checkType(
            typeInfo.replaceType(components.builtIns.unitType),
            ifExpression,
            context
        ).replaceDataFlowInfo(dataFlowInfo)
    }

    private fun containsJumpOutOfLoop(expression: CjExpression, context: ExpressionTypingContext): Boolean {
        val result = booleanArrayOf(false)
        //todo breaks in inline function literals
        expression.accept(object : CjTreeVisitor<List<CjLoopExpression>>() {
            override fun visitBreakExpression(
                breakExpression: CjBreakExpression,
                outerLoops: List<CjLoopExpression>?
            ): Void? {

                return null
            }


            override fun visitLoopExpression(
                loopExpression: CjLoopExpression,
                outerLoops: List<CjLoopExpression>?
            ): Void? {
                val newOuterLoops = outerLoops?.toMutableList() ?: mutableListOf()
                newOuterLoops.add(loopExpression)
                return super.visitLoopExpression(loopExpression, newOuterLoops)
            }
        }, if (expression is CjLoopExpression) listOf(expression) else listOf())

        return result[0]
    }

    override fun visitBreakExpression(
        expression: CjBreakExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {

        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.nothingType, context, expression)
            .replaceJumpOutPossible(true)
    }

    override fun visitReturnExpression(
        expression: CjReturnExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {


        val returnedExpression = expression.returnedExpression

        var newInferenceLambdaInfo: CangJieResolutionCallbacksImpl.LambdaInfo? = null

        var expectedType: CangJieType =
            NO_EXPECTED_TYPE
        var resultType: CangJieType? = components.builtIns.nothingType
        var parentDeclaration =
            context.getContextParentOfType(
                expression,
                CjDeclaration::class.java
            )

        if (parentDeclaration is CjParameter) {
            // In a default value for parameter
            context.trace.report(Errors.RETURN_NOT_ALLOWED.on(expression))
        }

        if (expression.getTargetLabel() == null) {
            while (parentDeclaration is CjDestructuringDeclaration) {
                parentDeclaration = context.getContextParentOfType(
                    parentDeclaration,
                    CjDeclaration::class.java
                )
            }

            // Parent declaration can be null in code fragments or in some bad error expressions
            val declarationDescriptor =

                parentDeclaration?.let {
                    context.trace.get<PsiElement, DeclarationDescriptor>(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        it
                    )
                }

            val containingFunInfo: com.intellij.openapi.util.Pair<FunctionDescriptor, PsiElement> =
                BindingContextUtils.getContainingFunctionSkipFunctionLiterals(
                    declarationDescriptor,
                    false
                )
            val containingFunctionDescriptor =
                containingFunInfo.first

            if (containingFunctionDescriptor != null) {
                if (
                    isClassInitializer(
                        containingFunInfo
                    )
                ) {
                    // Unqualified, in a function literal
                    context.trace.report(Errors.RETURN_NOT_ALLOWED.on(expression))
                    resultType = createErrorType(ErrorTypeKind.RETURN_NOT_ALLOWED)
                }

                expectedType =
                    getFunctionExpectedReturnType(
                        containingFunctionDescriptor,
                        containingFunInfo.getSecond() as CjElement,
                        context
                    )
                newInferenceLambdaInfo =
                    ExpressionTypingServices.getNewInferenceLambdaInfo(
                        context,
                        containingFunInfo.getSecond() as CjElement
                    )
            } else {
                // Outside a function
                context.trace.report(Errors.RETURN_NOT_ALLOWED.on(expression))
                resultType = createErrorType(ErrorTypeKind.RETURN_NOT_ALLOWED)
            }
        }

        if (returnedExpression != null) {
            if (newInferenceLambdaInfo != null) {
                val contextInfo: LambdaContextInfo
                val deparenthesizedReturnExpression =
                    CjPsiUtil.deparenthesize(returnedExpression)
                if (deparenthesizedReturnExpression is CjLambdaExpression ||
                    deparenthesizedReturnExpression is CjCallableReferenceExpression
                ) {
                    contextInfo = LambdaContextInfo(
                        CangJieTypeInfo(TypeUtils.DONT_CARE, context.dataFlowInfo),
                        null,
                        context.scope,
                        context.trace
                    )
                } else {
                    val result: CangJieTypeInfo = facade
                        .getTypeInfo(
                            returnedExpression, context.replaceExpectedType(newInferenceLambdaInfo.expectedType)
                                .replaceContextDependency(newInferenceLambdaInfo.contextDependency)
                        )
                    contextInfo = LambdaContextInfo(
                        result,
                        null,
                        context.scope,
                        context.trace
                    )
                }
                newInferenceLambdaInfo.returnStatements.add(
                    Pair<CjReturnExpression, LambdaContextInfo>(
                        expression,
                        contextInfo
                    )
                )
            } else {
                facade.getTypeInfo(
                    returnedExpression,
                    context.replaceExpectedType(expectedType)
                        .replaceContextDependency(ContextDependency.INDEPENDENT)
                )
            }
        } else {
            // for lambda with implicit return type Unit
            if (!TypeUtils.noExpectedType(expectedType) && !CangJieBuiltIns.isUnit(
                    expectedType
                ) && !TypeUtils.isDontCarePlaceholder(expectedType)
            ) {
                context.trace.report(
                    Errors.RETURN_TYPE_MISMATCH.on(
                        expression,
                        expectedType
                    )
                )
            }
            if (newInferenceLambdaInfo != null) {
                newInferenceLambdaInfo.returnStatements.add(
                    Pair<CjReturnExpression, LambdaContextInfo?>(
                        expression,
                        null
                    )
                )
            }
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(resultType, context, expression)
    }

    override fun visitForExpression(expression: CjForExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        return visitForExpression(expression, context, false)
    }

    fun visitForExpression(
        expression: CjForExpression,
        contextWithExpectedType: ExpressionTypingContext,
        isStatement: Boolean
    ): CangJieTypeInfo {
//        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(expression, contextWithExpectedType, facade)
//
//        var context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
//            ContextDependency.INDEPENDENT
//        )
//        val loopVisitor = PreliminaryLoopVisitor.visitLoop(expression)
//        context = context.replaceDataFlowInfo(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo, components.languageVersionSettings))
//
//        val loopRange = expression.loopRange
//        var expectedParameterType: CangJieType? = null
//        val loopRangeInfo: CangJieTypeInfo
//        if (loopRange != null) {
//            val loopRangeReceiver = getExpressionReceiver(facade, loopRange, context)
//            loopRangeInfo = facade.getTypeInfo(loopRange, context)
//            if (loopRangeReceiver != null) {
//                expectedParameterType = components.forLoopConventionsChecker.checkIterableConvention(loopRangeReceiver, context)
//            }
//        } else {
//            loopRangeInfo =  noTypeInfo(context)
//        }
//
//        val loopScope = newWritableScopeImpl(context, LexicalScopeKind.FOR, components.overloadChecker)
//
//        val loopParameter = expression.loopParameter
//        if (loopParameter != null) {
//            val variableDescriptor = createLoopParameterDescriptor(loopParameter, expectedParameterType, context)
//            val modifiersCheckingProcedure = components.modifiersChecker.withTrace(context.trace)
//            modifiersCheckingProcedure.checkModifiersForLocalDeclaration(loopParameter, variableDescriptor)
//            components.identifierChecker.checkDeclaration(loopParameter, context.trace)
//            loopScope.addVariableDescriptor(variableDescriptor)
//            val multiParameter = loopParameter.destructuringDeclaration
//            if (multiParameter != null) {
//                val elementType = expectedParameterType ?: ErrorUtils.createErrorType(ErrorTypeKind.NO_TYPE_FOR_LOOP_RANGE)
//                val iteratorNextAsReceiver = TransientReceiver(elementType)
////                components.annotationResolver.resolveAnnotationsWithArguments(loopScope, loopParameter.modifierList, context.trace)
////                components.destructuringDeclarationResolver.defineLocalVariablesFromDestructuringDeclaration(
////                    loopScope, multiParameter, iteratorNextAsReceiver, loopRange, context
////                )
//                modifiersCheckingProcedure.checkModifiersForDestructuringDeclaration(multiParameter)
//                components.identifierChecker.checkDeclaration(multiParameter, context.trace)
//            }
//        }
//
//        val body = expression.body
//        val bodyTypeInfo: CangJieTypeInfo = if (body != null) {
//            components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
//                loopScope, listOf(body), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(loopRangeInfo.dataFlowInfo)
//            )
//        } else {
//            loopRangeInfo
//        }
//
//        return components.dataFlowAnalyzer
//            .checkType(bodyTypeInfo.replaceType(components.builtIns.unitType), expression, contextWithExpectedType)
//            .replaceDataFlowInfo(loopRangeInfo.dataFlowInfo)
        TODO()
    }

    override fun visitContinueExpression(
        expression: CjContinueExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {

        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.nothingType, context, expression)
            .replaceJumpOutPossible(true)
    }

    private fun createLoopParameterDescriptor(
        loopParameter: CjParameter,
        expectedParameterType: CangJieType,
        context: ExpressionTypingContext
    ): VariableDescriptor {
//        components.modifiersChecker.withTrace(context.trace).checkParameterHasNoValOrVar(loopParameter, LET_OR_VAR_ON_LOOP_PARAMETER)
//
//        val typeReference = loopParameter.typeReference
//        val variableDescriptor: VariableDescriptor
//        if (typeReference != null) {
//            variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(context.scope, loopParameter, context.trace)
//            val actualParameterType = variableDescriptor.type
//            if (expectedParameterType != null && !CangJieTypeChecker.DEFAULT.isSubtypeOf(expectedParameterType, actualParameterType)) {
//                context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType))
//            }
//        } else {
//            var expectedType = expectedParameterType
//            if (expectedType == null) {
//                expectedType = ErrorUtils.createErrorType(ErrorTypeKind.NO_TYPE_FOR_LOOP_PARAMETER)
//            }
//            variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(loopParameter, expectedType, context.trace, context.scope)
//        }
//
//        checkVariableShadowing(context.scope, context.trace, variableDescriptor)
//
//        return variableDescriptor
        TODO()
    }

    companion object {

        private fun isClassInitializer(containingFunInfo: com.intellij.openapi.util.Pair<FunctionDescriptor, PsiElement>): Boolean {
            return containingFunInfo.getFirst() is ConstructorDescriptor && containingFunInfo.getSecond() !is CjSecondaryConstructor
        }

        private fun checkCatchParameterType(
            catchParameter: CjParameter,
            catchParameterType: CangJieType,
            context: ExpressionTypingContext
        ) {
            val typeParameterDescriptor =
                TypeUtils.getTypeParameterDescriptorOrNull(catchParameterType)
            if (typeParameterDescriptor != null) {

                context.trace.report(
                    Errors.TYPE_PARAMETER_IN_CATCH_CLAUSE.on(
                        catchParameter
                    )
                )

            }
        }

        private fun isCatchBranchesReturnsNothing(
            catchBlocks: List<CjExpression>,
            bindingContext: BindingContext
        ): Boolean {
            return whichCatchBranchesReturnNothing(catchBlocks, bindingContext).all { it }
        }

        private fun whichCatchBranchesReturnNothing(
            catchBlocks: List<CjExpression>,
            bindingContext: BindingContext
        ): List<Boolean> {
            return catchBlocks.map { catchBlock ->
                val catchTypeInfo = BindingContextUtils.getRecordedTypeInfo(catchBlock, bindingContext)
                catchTypeInfo?.let {
                    val catchType = catchTypeInfo.type
                    catchType == null || CangJieBuiltIns.isNothing(catchType)
                } ?: true
            }
        }

        private fun getFunctionExpectedReturnType(
            descriptor: FunctionDescriptor,
            function: CjElement,
            context: ExpressionTypingContext
        ): CangJieType {
            var expectedType: CangJieType?
            if (function is CjSecondaryConstructor) {
                expectedType = descriptor.builtIns.unitType
            } else if (function is CjFunction) {

                expectedType =
                    context.trace.get<CjFunction, CangJieType>(
                        BindingContext.EXPECTED_RETURN_TYPE,
                        function
                    )

                if ((expectedType == null) && (function.getTypeReference() != null || function.hasBlockBody())) {
                    expectedType = descriptor.getReturnType()
                }
            } else {
                expectedType = descriptor.getReturnType()
            }
            return if (expectedType != null) expectedType else NO_EXPECTED_TYPE
        }
    }
}

