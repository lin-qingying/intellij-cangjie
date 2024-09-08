package com.huawei.cangjie.types.expressions

import com.google.common.collect.Lists
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContextUtils
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
import com.huawei.cangjie.types.ErrorUtils.createErrorType
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.types.util.TypeUtils
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

//    override fun visitIfExpression(expression: CjIfExpression, context: ExpressionTypingContext): CangJieTypeInfo {
//        val condition = expression.condition
//        val conditionDataFlowInfo: DataFlowInfo =
//            checkCondition(condition, context)
//        val loopBreakContinuePossibleInCondition = condition != null && containsJumpOutOfLoop(condition, context)
//
//        val elseBranch = expression.`else`
//        val thenBranch = expression.then
//
//        val thenScope: LexicalWritableScope =
//            ExpressionTypingUtils.newWritableScopeImpl(
//                context,
//                LexicalScopeKind.THEN,
//                components.overloadChecker
//            )
//        val elseScope: LexicalWritableScope =
//            ExpressionTypingUtils.newWritableScopeImpl(
//                context,
//                LexicalScopeKind.ELSE,
//                components.overloadChecker
//            )
//        val thenInfo  =
//            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context)
//                .and(conditionDataFlowInfo)
//        val elseInfo  =
//            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context)
//                .and(conditionDataFlowInfo)
//
//        if (elseBranch == null) {
//            if (thenBranch != null) {
//                val result: CangJieTypeInfo = getTypeInfoWhenOnlyOneBranchIsPresent(
//                    thenBranch, thenScope, thenInfo, elseInfo, context, expression
//                )
//                // If jump was possible, take condition check info as the jump info
//                return if (result.jumpOutPossible)
//                    result.replaceJumpOutPossible(true).replaceJumpFlowInfo(conditionDataFlowInfo)
//                else
//                    result
//            }
//            return createTypeInfo(components.builtIns.unitType, thenInfo.or(elseInfo))
//        }
//        if (thenBranch == null) {
//            return getTypeInfoWhenOnlyOneBranchIsPresent(
//                elseBranch, elseScope, elseInfo, thenInfo, context, expression
//            )
//        }
//        val psiFactory: CjPsiFactory = CjPsiFactory(expression.getProject(), false)
//        val thenBlock: CjBlockExpression = psiFactory.wrapInABlockWrapper(thenBranch)
//        val elseBlock: CjBlockExpression = psiFactory.wrapInABlockWrapper(elseBranch)
//        val callForIf: Call =
//            ControlStructureTypingUtils.createCallForSpecialConstruction(
//                expression,
//                expression,
//                Lists.newArrayList<CjBlockExpression>(thenBlock, elseBlock)
//            )
//        val dataFlowInfoForArguments: MutableDataFlowInfoForArguments =
//            ControlStructureTypingUtils.createDataFlowInfoForArgumentsForIfCall(
//                callForIf,
//                conditionDataFlowInfo,
//                thenInfo,
//                elseInfo
//            )
//        val resolvedCall: ResolvedCall<FunctionDescriptor> =
//            components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
//                callForIf,
//                ControlStructureTypingUtils.ResolveConstruct.IF,
//                Lists.newArrayList<String>("thenBranch", "elseBranch"),
//                Lists.newArrayList<Boolean>(false, false),
//                context,
//                dataFlowInfoForArguments
//            )
//
//        return processIfBranches(
//            expression, context, conditionDataFlowInfo,
//            loopBreakContinuePossibleInCondition, elseBranch, thenBranch, resolvedCall
//        )
//    }

    override fun visitReturnExpression(
        expression: CjReturnExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {


        val returnedExpression = expression.returnedExpression

        var newInferenceLambdaInfo: CangJieResolutionCallbacksImpl.LambdaInfo? = null

        var expectedType: CangJieType =
            TypeUtils.NO_EXPECTED_TYPE
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

    companion object {

        private fun isClassInitializer(containingFunInfo: com.intellij.openapi.util.Pair<FunctionDescriptor, PsiElement>): Boolean {
            return containingFunInfo.getFirst() is ConstructorDescriptor && containingFunInfo.getSecond() !is CjSecondaryConstructor
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
            return if (expectedType != null) expectedType else TypeUtils.NO_EXPECTED_TYPE
        }
    }
}

