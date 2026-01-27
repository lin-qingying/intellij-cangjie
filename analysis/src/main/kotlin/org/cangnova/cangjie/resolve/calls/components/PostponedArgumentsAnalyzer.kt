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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.builtins.*
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.FilteredAnnotations
import org.cangnova.cangjie.resolve.calls.inference.addSubsystemFromArgument
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.freshTypeConstructor
import org.cangnova.cangjie.resolve.calls.inference.model.BuilderInferencePosition
import org.cangnova.cangjie.resolve.calls.inference.model.LambdaArgumentConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.NewTypeVariable
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.StubTypeForBuilderInference
import org.cangnova.cangjie.types.isOptionType
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.getValueParameterTypesFromFunctionType
import org.cangnova.cangjie.types.isBuiltinFunctionalType
import org.cangnova.cangjie.types.model.StubTypeMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker
import org.cangnova.cangjie.types.model.defaultType
import org.cangnova.cangjie.types.model.safeSubstitute
import org.cangnova.cangjie.types.builtIns

/**
 * 延迟参数分析器
 *
 * 负责分析在初始解析阶段无法立即确定类型的参数，主要包括：
 * - **Lambda 表达式**: 需要推断其参数和返回类型
 * - **可调用引用**: 需要解析其目标和类型
 * - **集合字面量**: 需要推断元素类型（暂未实现）
 *
 * 延迟分析的必要性：
 * 1. **类型依赖**: 这些参数的类型可能依赖于其他参数的类型推断结果
 * 2. **构建器推断**: 支持 DSL 模式中的延迟类型推断
 * 3. **上下文类型**: 需要利用期望类型进行更精确的推断
 *
 * 分析流程：
 * 1. 创建类型替换器，为延迟变量生成存根类型
 * 2. 分析 Lambda 表达式或可调用引用
 * 3. 将分析结果应用到约束系统
 * 4. 处理构建器推断的延迟变量
 *
 * @property callableReferenceArgumentResolver 可调用引用参数解析器
 * @property languageVersionSettings 语言版本设置，控制特性开关
 */
class PostponedArgumentsAnalyzer(
    private val callableReferenceArgumentResolver: CallableReferenceArgumentResolver,
    private val languageVersionSettings: LanguageVersionSettings
) {
    /**
     * Lambda 分析的替换器和存根类型
     *
     * 封装 Lambda 表达式分析所需的类型替换信息。
     * 在分析 Lambda 前，需要为所有延迟类型变量创建存根类型（stub type），
     * 这样可以在 Lambda 体内部使用这些类型进行推断。
     *
     * @property stubsForPostponedVariables 延迟变量到存根类型的映射表
     * @property substitute 类型替换函数，将类型中的延迟变量替换为存根类型
     */
    data class SubstitutorAndStubsForLambdaAnalysis(
        val stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>,
        val substitute: (CangJieType) -> UnwrappedType
    )

    /**
     * 创建 Lambda 分析的类型替换函数
     *
     * 为 Lambda 表达式分析创建类型替换上下文。此方法的主要作用是：
     * 1. 为所有延迟类型变量（postponed variables）创建存根类型
     * 2. 构建一个替换函数，将类型中的延迟变量替换为存根类型
     *
     * 存根类型的作用：
     * - 在 Lambda 体分析期间，延迟变量还没有确定的类型
     * - 存根类型作为占位符，允许 Lambda 体内部的类型检查继续进行
     * - 分析完成后，存根类型会被实际推断出的类型替换
     *
     * @receiver 延迟参数分析器上下文
     * @return 包含存根类型映射和替换函数的对象
     */
    fun PostponedArgumentsAnalyzerContext.createSubstituteFunctorForLambdaAnalysis(): SubstitutorAndStubsForLambdaAnalysis {
        val stubsForPostponedVariables = bindingStubsForPostponedVariables()
        val currentSubstitutor =
            buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor( ) })
        return SubstitutorAndStubsForLambdaAnalysis(stubsForPostponedVariables) {
            currentSubstitutor.safeSubstitute(this, it) as UnwrappedType
        }
    }

    /**
     * 对函数类型执行操作
     *
     * 辅助函数，用于安全地对函数类型执行某个操作。
     * 只有当类型是内置函数类型时才执行操作，否则返回 null。
     *
     * @param T 操作返回值的类型
     * @param f 要执行的操作，接收解包后的函数类型作为接收者
     * @return 操作结果，如果类型不是函数类型则返回 null
     */
    private inline fun <T> UnwrappedType?.forFunctionalType(f: UnwrappedType.() -> T?): T? {
        return if (this?.isBuiltinFunctionalType == true) f(this) else null
    }

    /**
     * 提取函数类型的值参数类型列表
     *
     * 从函数类型中提取所有值参数（不包括接收者）的类型。
     * 仓颉语言没有扩展函数类型，因此所有参数都是值参数。
     *
     * @return 参数类型列表，如果不是函数类型则返回 null
     */
    private fun UnwrappedType?.valueParameters(): List<UnwrappedType>? {
        return forFunctionalType { getValueParameterTypesFromFunctionType().map { it.type.unwrap() } }
    }

    /**
     * 分析 Lambda 表达式
     *
     * 这是 Lambda 表达式类型推断的核心方法。执行以下步骤：
     *
     * 1. **创建类型替换器**: 为延迟变量生成存根类型
     * 2. **确定接收者类型**: 仓颉没有扩展函数类型，接收者从第一个参数推导
     * 3. **确定参数类型**: 优先使用期望类型，否则使用实际类型
     * 4. **确定返回类型**: 处理 Unit 强制转换的特殊情况
     * 5. **调用回调进行体分析**: 分析 Lambda 体内的表达式
     * 6. **应用分析结果**: 将推断出的类型添加到约束系统
     *
     * 期望类型 vs 实际类型：
     * - 期望类型来自上下文（如函数参数的声明类型）
     * - 实际类型来自 Lambda 定义（如显式参数类型）
     * - 优先使用期望类型以提供更精确的诊断信息
     *
     * Unit 强制转换：
     * - 当 Lambda 期望返回 Unit 但体中有非 Unit 表达式时
     * - 自动将返回类型强制转换为 Unit
     *
     * @param c 延迟参数分析器上下文
     * @param resolutionCallbacks 解析回调接口，用于实际分析 Lambda 体
     * @param lambda 要分析的 Lambda 原子
     * @param completionMode 约束系统完成模式
     * @param diagnosticHolder 诊断信息持有者，用于收集错误和警告
     * @return Lambda 返回参数的分析结果
     */
    fun analyzeLambda(
        c: PostponedArgumentsAnalyzerContext,
        resolutionCallbacks: CangJieResolutionCallbacks,
        lambda: ResolvedLambdaAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: CangJieDiagnosticsHolder,
    ): ReturnArgumentsAnalysisResult {
        val substitutorAndStubsForLambdaAnalysis = c.createSubstituteFunctorForLambdaAnalysis()
        val substitute = substitutorAndStubsForLambdaAnalysis.substitute

        // Expected type has a higher priority against which lambda should be analyzed
        // Mostly, this is needed to report more specific diagnostics on lambda parameters
        fun expectedOrActualType(expected: UnwrappedType?, actual: UnwrappedType?): UnwrappedType? {
            val expectedSubstituted = expected?.let(substitute)
            return if (expectedSubstituted != null && c.canBeProper(expectedSubstituted)) expectedSubstituted else actual?.let(
                substitute
            )
        }

        val builtIns = c.getBuilder().builtIns

        val expectedParameters = lambda.expectedType.valueParameters()

        // 仓颉没有扩展函数类型，lambda 的接收器从第一个参数推导
        val receiver = lambda.receiver?.let {
            expectedOrActualType(expectedParameters?.getOrNull(0), lambda.receiver)
        }

        // 仓颉没有扩展函数类型，简化参数匹配逻辑
        val expectedParametersToMatchAgainst = when {
            receiver != null -> expectedParameters?.drop(1)
            else -> expectedParameters
        }

        val parameters =
            expectedParametersToMatchAgainst?.mapIndexed { index, expected ->
                expectedOrActualType(expected, lambda.parameters.getOrNull(index)) ?: builtIns.nothingType
            } ?: lambda.parameters.map(substitute)

        val rawReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(rawReturnType) -> substitute(rawReturnType)

            // For Unit-coercion
            !rawReturnType.isOptionType() && c.hasUpperOrEqualUnitConstraint(rawReturnType) -> builtIns.unitType

            else -> null
        }

        // 仓颉没有扩展函数类型，直接使用注解
        val convertedAnnotations = lambda.expectedType?.annotations ?: Annotations.EMPTY

        @Suppress("UNCHECKED_CAST")
        val returnArgumentsAnalysisResult = resolutionCallbacks.analyzeAndGetLambdaReturnArguments(
            lambda.atom,
            receiver,
            parameters,
            expectedTypeForReturnArguments,
            convertedAnnotations,
            substitutorAndStubsForLambdaAnalysis.stubsForPostponedVariables as Map<NewTypeVariable, StubTypeForBuilderInference>,
        )
        applyResultsOfAnalyzedLambdaToCandidateSystem(
            c,
            lambda,
            returnArgumentsAnalysisResult,
            completionMode,
            diagnosticHolder,
            substitute
        )
        return returnArgumentsAnalysisResult
    }

    /**
     * 将 Lambda 分析结果应用到候选约束系统
     *
     * Lambda 体分析完成后，需要将推断出的类型信息添加回约束系统。
     * 此方法处理以下内容：
     *
     * 1. **构建器推断检查**:
     *    - 如果有不适用的构建器推断调用，标记为可能需要无限制推断
     *    - 移除延迟变量，允许后续重试
     *
     * 2. **返回参数处理**:
     *    - 添加所有非错误返回参数的约束
     *    - 处理 Unit 强制转换的最后表达式
     *    - 解析每个返回表达式的仓颉原语
     *
     * 3. **无返回值处理**:
     *    - 如果 Lambda 体没有显式返回值，添加 Unit 约束
     *
     * 4. **构建器推断变量处理**:
     *    - 如果启用构建器推断且有推断会话，推断延迟变量
     *    - 将推断结果添加为子类型约束，允许多个 Lambda 贡献类型信息
     *    - 从约束系统中移除已推断的延迟变量
     *
     * 注意：当前的约束统一算法是不正确的（见代码中的 WARN 注释）。
     * 应该统一原始约束而非简单的结果类型约束，但这需要重新设计。
     *
     * @param c 延迟参数分析器上下文
     * @param lambda 已分析的 Lambda 原子
     * @param returnArgumentsAnalysisResult Lambda 返回参数的分析结果
     * @param completionMode 约束系统完成模式
     * @param diagnosticHolder 诊断信息持有者
     * @param substitute 类型替换函数，默认使用新创建的替换器
     */
    fun applyResultsOfAnalyzedLambdaToCandidateSystem(
        c: PostponedArgumentsAnalyzerContext,
        lambda: ResolvedLambdaAtom,
        returnArgumentsAnalysisResult: ReturnArgumentsAnalysisResult,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: CangJieDiagnosticsHolder,
        substitute: (CangJieType) -> UnwrappedType = c.createSubstituteFunctorForLambdaAnalysis().substitute
    ) {
        val (returnArgumentsInfo, inferenceSession, hasInapplicableCallForBuilderInference) =
            returnArgumentsAnalysisResult

        if (hasInapplicableCallForBuilderInference) {
            inferenceSession?.initializeLambda(lambda)
            c.getBuilder().markCouldBeResolvedWithUnrestrictedBuilderInference()
            c.getBuilder().removePostponedVariables()
            return
        }

        val returnArguments = returnArgumentsInfo.nonErrorArguments
        returnArguments.forEach { c.addSubsystemFromArgument(it) }

        val lastExpression = returnArgumentsInfo.lastExpression
        val allReturnArguments =
            if (lastExpression != null && returnArgumentsInfo.lastExpressionCoercedToUnit && c.addSubsystemFromArgument(
                    lastExpression
                )
            ) {
                returnArguments + lastExpression
            } else {
                returnArguments
            }

        val subResolvedKtPrimitives = allReturnArguments.map {
            resolveCjPrimitive(
                c.getBuilder(), it, lambda.returnType.let(substitute),
                diagnosticHolder, ReceiverInfo.notReceiver, convertedType = null,
                inferenceSession
            )
        }

        if (!returnArgumentsInfo.returnArgumentsExist) {
            val unitType = lambda.returnType.builtIns.unitType
            val lambdaReturnType = lambda.returnType.let(substitute)
            c.getBuilder()
                .addSubtypeConstraint(unitType, lambdaReturnType, LambdaArgumentConstraintPositionImpl(lambda))
        }

        lambda.setAnalyzedResults(returnArgumentsInfo, subResolvedKtPrimitives)

        // 默认启用：不需要注解就使用构建器推断
        val shouldUseBuilderInference = lambda.atom.hasBuilderInferenceAnnotation || true

        if (inferenceSession != null && shouldUseBuilderInference) {
            val constraintSystemBuilder = c.getBuilder()

            val postponedVariables = inferenceSession.inferPostponedVariables(
                lambda,
                constraintSystemBuilder,
                completionMode,
                diagnosticHolder
            )
            if (postponedVariables == null) {
                c.getBuilder().removePostponedVariables()
                return
            }

            // WARN: Following type constraint system unification algorithm is incorrect,
            // To perform constraint unification properly, original constraints should be
            // unified instead of simple result type based constraint
            // Other possible solution is to add equality constraint, but it will be too strict
            // and will limit usability
            // Nevertheless, proper design should be done before fixing this
            for ((constructor, resultType) in postponedVariables) {
                val variableWithConstraints =
                    constraintSystemBuilder.currentStorage().notFixedTypeVariables[constructor] ?: continue
                val variable = variableWithConstraints.typeVariable

                c.getBuilder().unmarkPostponedVariable(variable)

                // We add <inferred type> <: TypeVariable(T) to be able to contribute type info from several builder inference lambdas
                c.getBuilder().addSubtypeConstraint(resultType, variable.defaultType(c), BuilderInferencePosition)
            }

            c.removePostponedTypeVariablesFromConstraints(postponedVariables.keys)
        }
    }

    /**
     * 分析延迟参数
     *
     * 这是延迟参数分析的统一入口点。根据参数类型分发到不同的处理器：
     *
     * 1. **ResolvedLambdaAtom**: 已解析的 Lambda 表达式
     *    - 直接调用 analyzeLambda() 进行分析
     *
     * 2. **LambdaWithTypeVariableAsExpectedTypeAtom**: 期望类型为类型变量的 Lambda
     *    - 先转换为 ResolvedLambdaAtom，然后分析
     *    - 这种情况发生在 Lambda 的期望类型本身还未确定时
     *
     * 3. **ResolvedCallableReferenceArgumentAtom**: 可调用引用参数
     *    - 委托给 callableReferenceArgumentResolver 处理
     *    - 包括函数引用和属性引用
     *
     * 4. **ResolvedCollectionLiteralAtom**: 集合字面量
     *    - 目前未实现，抛出 TODO 异常
     *
     * @param c 延迟参数分析器上下文
     * @param resolutionCallbacks 解析回调接口
     * @param argument 要分析的延迟参数原子
     * @param completionMode 约束系统完成模式
     * @param diagnosticsHolder 诊断信息持有者
     * @throws IllegalStateException 如果遇到未预期的原语类型
     */
    fun analyze(
        c: PostponedArgumentsAnalyzerContext,
        resolutionCallbacks: CangJieResolutionCallbacks,
        argument: ResolvedAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, resolutionCallbacks, argument, completionMode, diagnosticsHolder)

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(
                    c,
                    resolutionCallbacks,
                    argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder),
                    completionMode,
                    diagnosticsHolder
                )

            is ResolvedCallableReferenceArgumentAtom ->
                callableReferenceArgumentResolver.processCallableReferenceArgument(
                    c.getBuilder(), argument, diagnosticsHolder, resolutionCallbacks
                )

            is ResolvedCollectionLiteralAtom -> TODO("Not supported")

            else -> error("Unexpected resolved primitive: ${argument.javaClass.canonicalName}")
        }
    }
}
