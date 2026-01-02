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

package org.cangnova.cangjie.types.expressions

import com.google.common.collect.Lists
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.CATCH_PARAMETER_WITH_DEFAULT_VALUE
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.returnTarget
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.recordScope
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.tower.CangJieResolutionCallbacksImpl
import org.cangnova.cangjie.resolve.calls.tower.LambdaContextInfo
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils.createErrorType
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import org.cangnova.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createDataFlowInfoForArgumentsOfTryCall
import org.cangnova.cangjie.types.expressions.ExpressionTypingUtils.checkVariableShadowing
import org.cangnova.cangjie.types.expressions.ExpressionTypingUtils.getExpressionReceiver
import org.cangnova.cangjie.types.expressions.ExpressionTypingUtils.newWritableScopeImpl
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo

class ControlStructureTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {


    private fun checkLetExpression(
        letExpression: CjLetExpression,
        context: ExpressionTypingContext
    ): DataFlowInfo {

        facade.checkLetExpression(letExpression, context)

        return context.dataFlowInfo

    }

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


        val tryResourceParameters = mutableListOf<Pair<CjExpression, VariableDescriptor>>()
        if (tryExpression.tryResourceList != null) {
            val resources = tryExpression.tryResourceList!!.resources

            resources.forEach {
                val variableDescriptor =
                    it.parameter?.let { it1 ->
                        resolveAndCheckTryResourceParameter(
                            it1,
                            it.expression,
                            tryInputContext
                        )
                    }
                if (variableDescriptor != null) {
                    tryResourceParameters.add(Pair(tryBlock, variableDescriptor))
                }
            }


        }

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

//        val dataFlowInfoAfterTry = tryOutputContext.dataFlowInfo

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
            tryResourceParameters,
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
//        expression.catchClauses.forEach { catchClause ->
//            val parameters = catchClause.parameterList
//            if (parameters != null && parameters.stub == null) {
//                TrailingCommaChecker.check(
//                    parameters.trailingComma,
//                    typingContext.trace,
//                    typingContext.languageVersionSettings
//                )
//            }
//        }

//

        // 默认使用改进的类型推断系统
        return resolveTryExpressionWithNewInference(expression, typingContext)
    }

    private fun checkCatchParameterDeclaration(
        catchParameter: CjParameterBase,
        context: ExpressionTypingContext
    ) {
        components.identifierChecker.checkDeclaration(catchParameter, context.trace)
        val modifiersChecking: ModifiersChecker.ModifiersCheckingProcedure =
            components.modifiersChecker.withTrace(context.trace)
        modifiersChecking.checkParameterHasNoLetOrVar(
            catchParameter,
            LET_OR_VAR_ON_CATCH_PARAMETER
        )
        ModifierCheckerCore.check(
            catchParameter,
            context.trace,
            null,
            components.languageVersionSettings
        )

        if (catchParameter.hasDefaultValue()) {
            context.trace.report(
                CATCH_PARAMETER_WITH_DEFAULT_VALUE.on(
                    catchParameter
                )
            )
        }
    }

    private fun resolveAndCheckTryResourceParameter(
        parameter: CjParameterBase,
        expression: CjExpression?,
        context: ExpressionTypingContext
    ): VariableDescriptor {
        checkCatchParameterDeclaration(parameter, context)

        val variableDescriptor = components.descriptorResolver
            .resolveLocalVariableDescriptor(context.scope, parameter, expression, context.trace)
        val parameterType = variableDescriptor.type
        checkTrySourceParameterType(parameter, parameterType, context)
        val resourceType = components.builtIns.stdlibTypes.resource.defaultType
        components.dataFlowAnalyzer.checkType(
            parameterType,
            parameter,
            context.replaceExpectedType(resourceType)
        )
        return variableDescriptor
    }

    private fun resolveAndCheckCatchParameter(
        catchParameter: CjParameterBase,
        context: ExpressionTypingContext
    ): VariableDescriptor {
        checkCatchParameterDeclaration(catchParameter, context)

        val variableDescriptor = components.descriptorResolver
            .resolveLocalVariableDescriptor(context.scope, catchParameter, context.trace)
        val catchParameterType = variableDescriptor.type
        checkCatchParameterType(catchParameter, catchParameterType, context)
        val exceptionType = components.builtIns.stdlibTypes.exception.defaultType
        components.dataFlowAnalyzer.checkType(
            catchParameterType,
            catchParameter,
            context.replaceExpectedType(exceptionType)
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
                components.builtIns.stdlibTypes.exceptionType
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
        val letExpression = expression.letExpression

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

        val conditionDataFlowInfo: DataFlowInfo = if (condition == null && letExpression != null) {
            checkLetExpression(letExpression, context.replaceScope(thenScope))
        } else {
            checkCondition(condition, context)
        }
        val loopBreakContinuePossibleInCondition = condition != null && containsJumpOutOfLoop(condition, context)


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
        val psiFactory = CjPsiFactory(expression.project, false)
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
        context.trace.recordScope(thenScope, thenBlock)
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
            loopBreakContinuePossibleInCondition, elseBranch, thenBranch, (thenScope to elseScope), resolvedCall
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

        val bodyTypeInfo: CangJieTypeInfo
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
        dataFlowInfo = if (!containsJumpOutOfLoop(expression, context)) {
            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context)
                .and(conditionDataFlowInfo)
        } else {
            context.dataFlowInfo
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
        val scopeToExtend = newWritableScopeImpl(context, LexicalScopeKind.WHILE_BODY, components.overloadChecker)

        val condition = expression.condition
        val letExpression = expression.letExpression
        var dataFlowInfo = if (condition == null && letExpression != null) {
            checkLetExpression(letExpression, context.replaceScope(scopeToExtend))
        } else {
            checkCondition(condition, context)
        }

        val body = expression.body
        val conditionInfo =
            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(dataFlowInfo)
        val bodyTypeInfo: CangJieTypeInfo = if (body != null) {
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
        scope: Pair<LexicalScope, LexicalScope>,
        resolvedCall: ResolvedCall<FunctionDescriptor>
    ): CangJieTypeInfo {

        val (thenScope, elseScope) = scope
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
        val typeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
            presentScope,
            listOf(presentBranch),
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
                outerLoops: List<CjLoopExpression>
            ) {

            }


            override fun visitLoopExpression(
                loopExpression: CjLoopExpression,
                outerLoops: List<CjLoopExpression>
            ){
                val newOuterLoops = outerLoops.toMutableList()
                newOuterLoops.add(loopExpression)
              super.visitLoopExpression(loopExpression, newOuterLoops)
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

    override fun visitQuoteExpression(element: CjQuoteExpression, data: ExpressionTypingContext): CangJieTypeInfo {
        element.quoteInterpolates.forEach {

            it.expression?.let { it1 ->
                facade.getTypeInfo(
                    it1, data.replaceExpectedType(NO_EXPECTED_TYPE)
                )
            }
        }

        return createTypeInfo(
            facade.components.builtIns.stdlibTypes.tokensType
        )
    }

    /**
     * 访问返回表达式以确定其类型信息。
     *
     * @param expression 返回表达式对象，表示代码中的 `return` 语句。
     * @param context 表达式类型检查上下文，包含类型推断所需的各种信息。
     * @return 返回表达式的类型信息。
     */
    override fun visitReturnExpression(
        expression: CjReturnExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {

        val returnedExpression = expression.returnedExpression

        var newInferenceLambdaInfo: CangJieResolutionCallbacksImpl.LambdaInfo? = null

        var expectedType: CangJieType = NO_EXPECTED_TYPE
        var resultType: CangJieType? = components.builtIns.nothingType
        var parentDeclaration =
            expression.returnTarget ?: context.getContextParentOfType(expression, CjDeclaration::class.java)

        if (parentDeclaration is CjParameter) {
            // 在参数的默认值中不允许使用 `return` 语句
            context.trace.report(RETURN_NOT_ALLOWED.on(expression))
        }

        if (expression.getTargetLabel() == null) {

            // 获取父声明的描述符
            val declarationDescriptor = parentDeclaration?.let {

                context.trace.get<PsiElement, DeclarationDescriptor>(BindingContext.DECLARATION_TO_DESCRIPTOR, it)
            }

            val containingFunInfo: Pair<FunctionDescriptor?, PsiElement?> =
                BindingContextUtils.getContainingFunctionSkipFunctionLiterals(declarationDescriptor, false)
            val containingFunctionDescriptor = containingFunInfo.first

            if (containingFunctionDescriptor != null) {
                if (containingFunInfo.second is CjFunctionLiteral || containingFunInfo.second is CjFunction) {
                    expression.addExpression(context.trace, containingFunInfo.second as CjDeclaration)
                }

                if (isClassInitializer(containingFunInfo)) {
                    // 在类初始化器中不允许使用未限定的 `return` 语句
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression))
                    resultType = createErrorType(ErrorTypeKind.RETURN_NOT_ALLOWED)
                }
                if ((containingFunInfo.second as? CjFunctionImpl<*, *>)?.isInferReturnType != true) {

                    expectedType = getFunctionExpectedReturnType(
                        containingFunctionDescriptor,
                        containingFunInfo.second as CjElement,
                        context
                    )

                }

                newInferenceLambdaInfo =
                    ExpressionTypingServices.getNewInferenceLambdaInfo(context, containingFunInfo.second as CjElement)
            } else {
                // 在函数外部不允许使用 `return` 语句
                context.trace.report(RETURN_NOT_ALLOWED.on(expression))
//                resultType = createErrorType(ErrorTypeKind.RETURN_NOT_ALLOWED)
            }
        }

        if (returnedExpression != null) {
            if (newInferenceLambdaInfo != null) {
                val contextInfo: LambdaContextInfo
                val deparenthesizedReturnExpression = CjPsiUtil.deparenthesize(returnedExpression)
                if (deparenthesizedReturnExpression is CjLambdaExpression || deparenthesizedReturnExpression is CjCallableReferenceExpression) {
                    contextInfo = LambdaContextInfo(
                        CangJieTypeInfo(TypeUtils.DONT_CARE, context.dataFlowInfo),
                        null,
                        context.scope,
                        context.trace
                    )
                } else {
                    val result: CangJieTypeInfo = facade.getTypeInfo(
                        returnedExpression,
                        context.replaceExpectedType(newInferenceLambdaInfo.expectedType)
                            .replaceContextDependency(newInferenceLambdaInfo.contextDependency)
                    )
                    contextInfo = LambdaContextInfo(result, null, context.scope, context.trace)
                }
                newInferenceLambdaInfo.returnStatements.add(Pair(expression, contextInfo))
            } else {
                facade.getTypeInfo(
                    returnedExpression,
                    context.replaceExpectedType(expectedType).replaceContextDependency(ContextDependency.INDEPENDENT)
                )
            }
        } else {
            // 对于隐式返回类型为 Unit 的 lambda 表达式
            if (!TypeUtils.noExpectedType(expectedType) && !CangJieBuiltIns.isUnit(expectedType) && !TypeUtils.isDontCarePlaceholder(
                    expectedType
                )
            ) {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType))
            }
            newInferenceLambdaInfo?.returnStatements?.add(Pair(expression, null))
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

        val loopRange = expression.loopRange
        var expectedParameterType: CangJieType? = null
        val loopRangeInfo: CangJieTypeInfo
        if (loopRange != null) {
            val loopRangeReceiver = getExpressionReceiver(facade, loopRange, context)
            loopRangeInfo = facade.getTypeInfo(loopRange, context)
            if (loopRangeReceiver != null) {
                expectedParameterType =
                    components.forLoopConventionsChecker.checkIterableConvention(loopRangeReceiver, context)
            }
        } else {
            loopRangeInfo = noTypeInfo(context)
        }

        val loopScope = newWritableScopeImpl(context, LexicalScopeKind.FOR, components.overloadChecker)

//        for in 表达式 模式匹配
        expression.pattern?.let {
            val elementType = expectedParameterType ?: createErrorType(ErrorTypeKind.NO_TYPE_FOR_LOOP_RANGE)
            val iteratorNextAsReceiver = TransientReceiver(elementType)
            facade.defineLocalVariablesFromPattern(
                loopScope,
                it,
                iteratorNextAsReceiver,
                loopRange,
                context
            )
        }
        expression.patternGuard?.expression?.let {
            facade.getTypeInfo(
                it,
                context.replaceExpectedType(components.builtIns.boolType)
            )
        }

        val body = expression.body
        val bodyTypeInfo: CangJieTypeInfo = if (body != null) {
            components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                loopScope,
                listOf(body),
                CoercionStrategy.NO_COERCION,
                context.replaceDataFlowInfo(loopRangeInfo.dataFlowInfo)
                    .replaceExpectedType(components.builtIns.unitType)
            )
        } else {
            loopRangeInfo
        }

        return components.dataFlowAnalyzer
            .checkType(bodyTypeInfo.replaceType(components.builtIns.unitType), expression, contextWithExpectedType)
            .replaceDataFlowInfo(loopRangeInfo.dataFlowInfo)

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
        expectedParameterType: CangJieType?,
        context: ExpressionTypingContext
    ): VariableDescriptor {
        components.modifiersChecker.withTrace(context.trace)
            .checkParameterHasNoLetOrVar(loopParameter, LET_OR_VAR_ON_LOOP_PARAMETER)

        val typeReference = loopParameter.typeReference
        val variableDescriptor: VariableDescriptor
        if (typeReference != null) {
            variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(
                context.scope,
                loopParameter,
                context.trace
            )
            val actualParameterType = variableDescriptor.type
            if (expectedParameterType != null && !CangJieTypeChecker.DEFAULT.isSubtypeOf(
                    expectedParameterType,
                    actualParameterType
                )
            ) {
                context.trace.report(
                    TYPE_MISMATCH_IN_FOR_LOOP.on(
                        typeReference,
                        expectedParameterType,
                        actualParameterType
                    )
                )
            }
        } else {
            var expectedType = expectedParameterType
            if (expectedType == null) {
                expectedType = createErrorType(ErrorTypeKind.NO_TYPE_FOR_LOOP_PARAMETER)
            }
            variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(
                loopParameter,
                expectedType,
                context.trace,
                context.scope
            )
        }

        checkVariableShadowing(context.scope, context.trace, variableDescriptor)

        return variableDescriptor

    }

    companion object {

        private fun isClassInitializer(containingFunInfo: Pair<FunctionDescriptor?, PsiElement?>): Boolean {
            return containingFunInfo.first is ConstructorDescriptor && containingFunInfo.second !is CjSecondaryConstructor
        }

        private fun checkTrySourceParameterType(
            catchParameter: CjParameterBase,
            catchParameterType: CangJieType,
            context: ExpressionTypingContext
        ) {
//            val typeParameterDescriptor =
//                TypeUtils.getTypeParameterDescriptorOrNull(catchParameterType)
//            if (typeParameterDescriptor != null) {
//
//                context.trace.report(
//                    TYPE_PARAMETER_IN_CATCH_CLAUSE.on(
//                        catchParameter
//                    )
//                )
//
//            }
        }

        private fun checkCatchParameterType(
            catchParameter: CjParameterBase,
            catchParameterType: CangJieType,
            context: ExpressionTypingContext
        ) {
            val typeParameterDescriptor =
                TypeUtils.getTypeParameterDescriptorOrNull(catchParameterType)
            if (typeParameterDescriptor != null) {

                context.trace.report(
                    TYPE_PARAMETER_IN_CATCH_CLAUSE.on(
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

                if ((expectedType == null) && (function.typeReference != null || function.hasBlockBody())) {
                    expectedType = descriptor.returnType
                }
            } else {
                expectedType = descriptor.returnType
            }
            return expectedType ?: NO_EXPECTED_TYPE
        }
    }
}

fun CjReturnExpression.addExpression(bindingContext: BindingTrace, target: CjDeclaration) {
    if (returnedExpression == null) return
    val returnsByTarget = bindingContext[BindingContext.RETURN_TARGET, target]

    val expressions = returnsByTarget?.toMutableSet() ?: mutableSetOf()

    expressions.add(returnedExpression ?: return)

    bindingContext.record(BindingContext.RETURN_TARGET, target, expressions)


}
