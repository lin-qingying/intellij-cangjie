/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.calls.inference.components

import cn.cangnova.cangjie.resolve.calls.components.transformToResolvedLambda
import cn.cangnova.cangjie.resolve.calls.inference.model.*
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ErrorUtils
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.UnwrappedType
import cn.cangnova.cangjie.types.error.ErrorTypeKind
import cn.cangnova.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import cn.cangnova.cangjie.types.model.CangJieTypeMarker
import cn.cangnova.cangjie.types.model.TypeConstructorMarker
import cn.cangnova.cangjie.types.model.TypeVariableMarker
import cn.cangnova.cangjie.types.model.TypeVariableTypeConstructorMarker
import com.intellij.util.containers.addIfNotNull

class CangJieConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    val variableFixationFinder: VariableFixationFinder,
    private val postponedArgumentsInputTypesResolver: PostponedArgumentInputTypesResolver,
//    private val languageVersionSettings: LanguageVersionSettings
) {
    fun completeConstraintSystem(
        c: ConstraintSystemCompletionContext,
        topLevelType: UnwrappedType,
        topLevelAtoms: List<ResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        c.runCompletion(
            completionMode,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = true,
        ) {
            error("Shouldn't be called in complete constraint system mode")
        }
    }

    fun runCompletion(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        c.runCompletion(
            completionMode,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = false,
            analyze = analyze
        )
    }

    /**
     * 执行约束系统完成过程。
     *
     * 该函数驱动整个约束系统完成过程，包括分析延迟参数、固定类型变量以及在必要时尝试使用构建器推断完成调用。
     *
     * @param completionMode 约束系统完成模式，决定完成过程应进行到什么程度。
     * @param topLevelAtoms 顶级解析原子，表示主要分析元素。
     * @param topLevelType 顶级类型，即整个分析的目标类型。
     * @param diagnosticsHolder 诊断持有者，用于收集分析诊断。
     * @param collectVariablesFromContext 标志，指示是否从上下文中收集类型变量。
     * @param analyze 用于分析延迟解析原子的回调函数。
     */
    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        collectVariablesFromContext: Boolean,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()

        completion@ while (true) {
            // 获取未分析的延迟参数
            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            // 如果完成模式为 UNTIL_FIRST_LAMBDA 且存在需要分析的 lambda，则返回
            if (completionMode == ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA && hasLambdaToAnalyze(
                    postponedArguments
                )
            ) return

            // 阶段 1: 分析具有固定参数类型的延迟参数
            if (analyzeArgumentWithFixedParameterTypes(postponedArguments, analyze))
                continue

            // 检查是否有准备好固定的变量
            val isThereAnyReadyForFixationVariable = variableFixationFinder.findFirstVariableForFixation(
                this,
                getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
                postponedArguments,
                completionMode,
                topLevelType
            ) != null

            // 如果没有未分析的延迟参数且没有准备好固定的变量，则完成过程不需要继续
            if (postponedArguments.isEmpty() && !isThereAnyReadyForFixationVariable)
                break

            // 过滤出具有可修订预期类型的延迟参数
            val postponedArgumentsWithRevisableType = postponedArguments
                .filterIsInstance<PostponedAtomWithRevisableExpectedType>()

            // 创建依赖关系提供者
            val dependencyProvider =
                TypeVariableDependencyInformationProvider(notFixedTypeVariables, postponedArguments, topLevelType, this)

            // 阶段 2: 收集延迟参数的参数类型并构建新的预期类型
            val wasBuiltNewExpectedTypeForSomeArgument =
                postponedArgumentsInputTypesResolver.collectParameterTypesAndBuildNewExpectedTypes(
                    this,
                    postponedArgumentsWithRevisableType,
                    completionMode,
                    dependencyProvider,
                    topLevelTypeVariables
                )

            if (wasBuiltNewExpectedTypeForSomeArgument)
                continue

            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                // 阶段 3: 固定所有延迟参数的参数类型变量
                for (argument in postponedArguments) {
                    val variableWasFixed =
                        postponedArgumentsInputTypesResolver.fixNextReadyVariableForParameterTypeIfNeeded(
                            this,
                            argument,
                            postponedArguments,
                            topLevelType,
                            dependencyProvider,
                        ) {
                            findResolvedAtomBy(it, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
                        }

                    if (variableWasFixed)
                        continue@completion
                }

                // 阶段 4: 如有必要，创建具有新功能预期类型的原子
                for (argument in postponedArgumentsWithRevisableType) {
                    val argumentWasTransformed = transformToAtomWithNewFunctionalExpectedType(
                        this, argument, diagnosticsHolder
                    )

                    if (argumentWasTransformed)
                        continue@completion
                }
            }

            // 阶段 5: 分析下一个准备好的延迟参数
            if (analyzeNextReadyPostponedArgument(
                    postponedArguments,
                    completionMode,
                    analyze
                )
            )
                continue

            // 阶段 6: 使用适当的约束固定下一个准备好的类型变量
            if (
                fixNextReadyVariable(
                    completionMode,
                    topLevelAtoms,
                    topLevelType,
                    collectVariablesFromContext,
                    postponedArguments,
                    diagnosticsHolder
                )
            ) continue

            // 阶段 7: 尝试使用构建器推断完成调用，如果存在未推断的类型变量
            val areThereAppearedProperConstraintsForSomeVariable = tryToCompleteWithBuilderInference(
                completionMode,
                topLevelAtoms,
                topLevelType,
                postponedArguments,
                collectVariablesFromContext,
                diagnosticsHolder,
                analyze
            )

            if (areThereAppearedProperConstraintsForSomeVariable)
                continue

            // 阶段 8: 对未推断的类型变量报告“信息不足”
            reportNotEnoughTypeInformation(
                completionMode,
                topLevelAtoms,
                topLevelType,
                collectVariablesFromContext,
                postponedArguments,
                diagnosticsHolder
            )

            // 阶段 9: 强制分析剩余未分析的延迟参数并在必要时重新运行阶段
            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments, analyze))
                    continue
            }

            break
        }
    }


    private fun ConstraintSystemCompletionContext.tryToCompleteWithBuilderInference(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        postponedArguments: List<PostponedResolvedAtom>,
        collectVariablesFromContext: Boolean,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        if (completionMode != ConstraintSystemCompletionMode.FULL) return false


        val lambdaArguments =
            postponedArguments.filterIsInstance<ResolvedLambdaAtom>().takeIf { it.isNotEmpty() } ?: return false

        fun ResolvedLambdaAtom.notFixedInputTypeVariables(): List<TypeVariableTypeConstructorMarker> =
            inputTypes.flatMap { it.extractTypeVariables() }.filter { it !in fixedTypeVariables }


        val dangerousBuilderInferenceWithoutAnnotation =
            lambdaArguments.size >= 2 && lambdaArguments.count { it.notFixedInputTypeVariables().isNotEmpty() } >= 2

        val builder = getBuilder()
        for (argument in lambdaArguments) {
            val reallyHasBuilderInferenceAnnotation = argument.atom.hasBuilderInferenceAnnotation


            // Imitate having builder inference annotation. TODO: Remove after getting rid of @BuilderInference
            if (!reallyHasBuilderInferenceAnnotation) {
                argument.atom.hasBuilderInferenceAnnotation = true
            }

            val notFixedInputTypeVariables = argument.notFixedInputTypeVariables()

            // lambda is subject to builder inference past this point
            if (notFixedInputTypeVariables.isEmpty()) continue


            for (variable in notFixedInputTypeVariables) {
                builder.markPostponedVariable(notFixedTypeVariables.getValue(variable).typeVariable)
            }

            analyze(argument)
        }

        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
            postponedArguments,
            completionMode,
            topLevelType
        )

        // continue completion (rerun stages) only if ready for fixation variables with proper constraints have appeared
        // (after analysing a lambda with the builder inference)
        // otherwise we don't continue and report "not enough type information" error
        return variableForFixation?.isReady == true
    }

    private fun transformToAtomWithNewFunctionalExpectedType(
        c: ConstraintSystemCompletionContext,
        argument: PostponedAtomWithRevisableExpectedType,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ): Boolean = with(c) {
        val revisedExpectedType: UnwrappedType = argument.revisedExpectedType
            ?.takeIf { it.isFunctionWithAny() } as UnwrappedType? ?: return false

        when (argument) {
            is PostponedCallableReferenceAtom ->
                CallableReferenceWithRevisedExpectedTypeAtom(argument.atom, revisedExpectedType).also {
                    argument.setAnalyzedResults(null, listOf(it))
                }

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder, revisedExpectedType)

            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }

        return true
    }

    private fun ConstraintSystemCompletionContext.fixNextReadyVariable(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ): Boolean {
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
            postponedArguments,
            completionMode,
            topLevelType
        ) ?: return false

        if (!variableForFixation.isReady) return false

        fixVariable(
            this,
            notFixedTypeVariables.getValue(variableForFixation.variable),
            topLevelAtoms,
            diagnosticsHolder
        )

        return true
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        fixVariable(
            c,
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
            topLevelAtoms,
            diagnosticsHolder
        )
    }

    private fun reportWarningIfFixedIntoDeclaredUpperBounds(
        diagnosticsHolder: CangJieDiagnosticsHolder,
        variableWithConstraints: VariableWithConstraints,
        resultType: CangJieTypeMarker
    ) {
        var upperBoundType: CangJieTypeMarker? = null
        val constraintFromDeclaredUpperBoundExists = variableWithConstraints.constraints.any { constraint ->
            val position = constraint.position.from.let { basicPosition ->
                if (basicPosition is IncorporationConstraintPosition) basicPosition.from else basicPosition
            }
            if (position is BuilderInferenceSubstitutionConstraintPosition<*> && position.isFromNotSubstitutedDeclaredUpperBound) {
                upperBoundType = constraint.type
                true
            } else {
                false
            }
        }

//        具有多个最下公共父类时，报错，该错误转移到流程控制 @see [ControlFlowInformationProviderImpl.kt]
        if (resultType is MultipleSupertypeTypeInferenceFailure) {
            diagnosticsHolder.addDiagnostic(
                CangJieConstraintSystemDiagnostic(
                    MultipleMinimalCommonSupertypes(
                        variableWithConstraints.typeVariable,
                        resultType.intersectedTypes
                    )
                )
            )
        }
        if (constraintFromDeclaredUpperBoundExists && upperBoundType == resultType) {
            diagnosticsHolder.addDiagnostic(
                CangJieConstraintSystemDiagnostic(InferredIntoDeclaredUpperBounds(variableWithConstraints.typeVariable))
            )
        }
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)

        val variable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(variable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        reportWarningIfFixedIntoDeclaredUpperBounds(diagnosticsHolder, variableWithConstraints, resultType)

        c.fixVariable(variable, resultType, FixVariableConstraintPositionImpl(variable, resolvedAtom))
    }

    private fun ConstraintSystemCompletionContext.reportNotEnoughTypeInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        while (true) {
            val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
                this, getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
                postponedArguments, completionMode, topLevelType,
            ) ?: break

            assert(!variableForFixation.isReady) {
                "At this stage there should be no remaining variables with proper constraints"
            }

            if (completionMode == ConstraintSystemCompletionMode.PARTIAL) break

            val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
            processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms, diagnosticsHolder)
        }
    }

    private fun ConstraintSystemCompletionContext.processVariableWhenNotEnoughInformation(
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        if (resolvedAtom != null) {
            addError(
                NotEnoughInformationForTypeParameterImpl(
                    typeVariable,
                    resolvedAtom,
                    couldBeResolvedWithUnrestrictedBuilderInference()
                )
            )
        }

        val resultErrorType = when {
            typeVariable is TypeVariableFromCallableDescriptor -> {
                ErrorUtils.createErrorType(
                    ErrorTypeKind.UNINFERRED_TYPE_VARIABLE,
                    typeVariable.originalTypeParameter.name.asString()
                )
            }

            typeVariable is TypeVariableForLambdaParameterType && typeVariable.atom is LambdaCangJieCallArgument -> {
                diagnosticsHolder.addDiagnostic(
                    NotEnoughInformationForLambdaParameter(typeVariable.atom, typeVariable.index)
                )
                ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)
            }

            else -> ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, typeVariable.toString())
        }

        fixVariable(typeVariable, resultErrorType, FixVariableConstraintPositionImpl(typeVariable, resolvedAtom))

    }

    /**
     * 获取所有类型的变量列表
     *
     * 该函数根据给定的参数和上下文，收集并返回所有类型的变量列表
     * 主要用于约束系统中，为了完成某些逻辑而需要获取所有类型的变量
     *
     * @param collectVariablesFromContext 是否从上下文中收集变量
     * @param topLevelAtoms 顶层原子列表，用于收集类型变量
     * @return 返回一个包含所有类型变量的列表
     */
    private fun ConstraintSystemCompletionContext.getOrderedAllTypeVariables(
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<ResolvedAtom>
    ): List<TypeConstructorMarker> {
        // 如果需要从上下文中收集变量，则直接返回未固定类型的变量列表
        if (collectVariablesFromContext)
            return notFixedTypeVariables.keys.toList()

        /**
         * 从修正的期望类型中获取变量
         *
         * 此辅助函数用于从给定的修正期望类型中提取类型变量
         *
         * @param revisedExpectedType 修正后的期望类型
         * @return 返回从期望类型中提取的类型变量列表，如果没有则返回空列表
         */
        fun getVariablesFromRevisedExpectedType(revisedExpectedType: CangJieType?) =
            revisedExpectedType?.arguments?.map { it.type.constructor }?.filterIsInstance<TypeVariableTypeConstructor>()

        // 使用Set来存储结果，因为不同的原子可能共享相同的类型变量
        val result = linkedSetOf<TypeConstructor>()

        /**
         * 收集所有类型变量
         *
         * 此扩展函数用于遍历解析原子并收集其中的所有类型变量
         */
        fun ResolvedAtom.collectAllTypeVariables() {
            // 根据不同的解析原子类型，收集类型变量
            val typeVariables = when (this) {
                is ResolvedLambdaAtom -> {
                    listOfNotNull(typeVariableForLambdaReturnType?.freshTypeConstructor)
                }

                is LambdaWithTypeVariableAsExpectedTypeAtom -> {
                    getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty()
                }

                is PostponedCallableReferenceAtom -> {
                    getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty() +
                            candidate?.freshVariablesSubstitutor?.freshVariables?.map { it.freshTypeConstructor }
                                .orEmpty()
                }

                is ResolvedCallAtom -> freshVariablesSubstitutor.freshVariables.map { it.freshTypeConstructor }
                is ResolvedCallableReferenceArgumentAtom -> candidate?.freshVariablesSubstitutor?.freshVariables?.map { it.freshTypeConstructor }
                    .orEmpty()

                else -> emptyList()
            }

            // 将收集到的类型变量添加到结果集中，前提是它们是未固定的类型变量
            typeVariables.mapNotNullTo(result) {
                it.takeIf { notFixedTypeVariables.containsKey(it) }
            }

            /*
             * Hack for completing error candidates in delegate resolve
             */
            if (this is StubResolvedAtom && typeVariable in notFixedTypeVariables) {
                result += typeVariable
            }

            // 如果当前解析原子已被分析，则递归地收集其子解析原子中的类型变量
            if (analyzed) {
                subResolvedAtoms?.forEach { it.collectAllTypeVariables() }
            }
        }

        // 遍历顶层解析原子，收集它们的所有类型变量
        for (topLevelAtom in topLevelAtoms) {
            topLevelAtom.collectAllTypeVariables()
        }

        // 确保收集到的类型变量数量与未固定的类型变量数量一致
        require(result.size == notFixedTypeVariables.size) {
            val notFoundTypeVariables = notFixedTypeVariables.keys.toMutableSet().apply { removeAll(result) }
            "Not all type variables found: $notFoundTypeVariables"
        }

        // 将结果转换为列表并返回
        return result.toList()
    }

    companion object {
        fun findResolvedAtomBy(typeVariable: TypeVariableMarker, topLevelAtoms: List<ResolvedAtom>): ResolvedAtom? {
            fun ResolvedAtom.check(): ResolvedAtom? {
                val suitableCall = when (this) {
                    is ResolvedCallAtom -> typeVariable in freshVariablesSubstitutor.freshVariables
                    is ResolvedCallableReferenceArgumentAtom -> candidate?.freshVariablesSubstitutor?.freshVariables?.let { typeVariable in it }
                        ?: false

                    is ResolvedLambdaAtom -> typeVariable == typeVariableForLambdaReturnType
                    else -> false
                }

                if (suitableCall) {
                    return this
                }

                subResolvedAtoms?.forEach { subResolvedAtom ->
                    subResolvedAtom.check()?.let { result -> return@check result }
                }

                return null
            }

            for (topLevelAtom in topLevelAtoms) {
                topLevelAtom.check()?.let { return it }
            }

            return null
        }

        fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ResolvedAtom>): List<PostponedResolvedAtom> {
            fun ResolvedAtom.process(to: MutableList<PostponedResolvedAtom>) {
                to.addIfNotNull((this as? PostponedResolvedAtom)?.takeUnless { it.analyzed })

                if (analyzed) {
                    subResolvedAtoms?.forEach { it.process(to) }
                }
            }

            val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
            for (primitive in topLevelAtoms) {
                primitive.process(notAnalyzedArguments)
            }

            return notAnalyzedArguments
        }
    }
}
