# 迭代式类型推导实现方案

## 文档概述

本文档描述仓颉语言插件如何实现迭代式类型推导，以更好地支持 Lambda 表达式、Option 类型和复杂泛型场景。

---

## 架构概览

### 当前架构（单轮求解）

```
┌─────────────────────────────────────┐
│  收集约束 → 单次求解 → 固定所有变量  │
└─────────────────────────────────────┘
```

### 目标架构（迭代式求解）

```
┌─────────────────────────────────────────────────────┐
│  while (hasProgress) {                              │
│      1. 用部分解分析参数                             │
│      2. 求解约束（允许部分解）                        │
│      3. 如果有新的固定变量 → hasProgress = true      │
│  }                                                  │
└─────────────────────────────────────────────────────┘
```

---

## 实现步骤

### 1. 定义迭代状态

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/model/InferenceIterationState.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 迭代式推导的状态
 *
 * 追踪类型推导的迭代过程，包括部分解、进度信息和迭代计数。
 *
 * @property typeVariablesToSolve 需要求解的类型变量列表
 * @property partialSolution 当前部分解（已固定的类型变量映射）
 * @property unsolvedCount 未求解的变量数量
 * @property hasNewInfo 本轮是否有新信息
 * @property iterationCount 迭代次数
 * @property maxIterations 最大迭代次数（防止无限循环）
 */
class InferenceIterationState(
    val typeVariablesToSolve: List<TypeVariableMarker>,
    val partialSolution: MutableMap<TypeVariableMarker, CangJieTypeMarker> = mutableMapOf(),
    var unsolvedCount: Int = typeVariablesToSolve.size,
    var hasNewInfo: Boolean = true,
    var iterationCount: Int = 0,
    val maxIterations: Int = 10
) {
    /**
     * 是否应该继续迭代
     *
     * 继续条件：
     * 1. 本轮有新信息
     * 2. 还有未求解的变量
     * 3. 未达到最大迭代次数
     */
    fun shouldContinue(): Boolean =
        hasNewInfo && unsolvedCount > 0 && iterationCount < maxIterations

    /**
     * 开始新一轮迭代
     */
    fun startNewIteration() {
        hasNewInfo = false
        iterationCount++
    }

    /**
     * 记录变量被固定
     *
     * @param variable 被固定的类型变量
     * @param type 固定后的类型
     */
    fun recordFixation(variable: TypeVariableMarker, type: CangJieTypeMarker) {
        if (variable !in partialSolution) {
            partialSolution[variable] = type
            unsolvedCount--
            hasNewInfo = true
        }
    }

    /**
     * 创建部分解替换器
     *
     * @return 基于当前部分解的类型替换器
     */
    fun createPartialSubstitutor(): TypeSubstitutor {
        return TypeSubstitutor.create(
            partialSolution.mapKeys { it.key.freshTypeConstructor() }
        )
    }

    /**
     * 检查指定变量是否已固定
     */
    fun isFixed(variable: TypeVariableMarker): Boolean = variable in partialSolution

    /**
     * 获取变量的固定类型（如果已固定）
     */
    fun getFixedType(variable: TypeVariableMarker): CangJieTypeMarker? = partialSolution[variable]

    override fun toString(): String {
        return "InferenceIterationState(" +
            "iteration=$iterationCount, " +
            "unsolved=$unsolvedCount/${typeVariablesToSolve.size}, " +
            "hasNewInfo=$hasNewInfo, " +
            "solution=$partialSolution)"
    }
}
```

---

### 2. 修改约束求解支持部分解

**修改文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/components/ResultTypeResolver.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.types.model.CangJieTypeMarker

/**
 * 结果类型解析器
 *
 * 负责从约束集合中解析出类型变量的结果类型。
 * 支持部分解模式，允许在不是所有变量都已固定时返回结果。
 */
class ResultTypeResolver(
    private val typeApproximator: AbstractTypeApproximator,
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle
) {
    /**
     * 查找结果类型
     *
     * @param c 约束系统完成上下文
     * @param variableWithConstraints 带约束的类型变量
     * @param direction 解析方向（子类型/超类型/未知）
     * @param allowPartial 是否允许部分解（即使结果包含未固定的变量也返回）
     * @return 解析出的结果类型，如果无法解析则返回 null
     */
    fun findResultType(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        allowPartial: Boolean = false
    ): CangJieTypeMarker? {
        // 1. 查找单位约束（单个 EQUALITY 约束）
        findResultTypeOfTheUnitConstraint(c, variableWithConstraints)?.let { return it }

        // 2. 根据方向求解
        val result = when (direction) {
            ResolveDirection.TO_SUBTYPE -> findSubType(c, variableWithConstraints)
            ResolveDirection.TO_SUPERTYPE -> findSuperType(c, variableWithConstraints)
            ResolveDirection.UNKNOWN -> findResultTypeOrNull(c, variableWithConstraints)
        }

        // 3. 检查结果是否为"正确类型"
        if (result != null) {
            if (allowPartial) {
                // 部分解模式：即使包含未固定变量也接受
                return result
            } else {
                // 严格模式：只接受不包含未固定变量的类型
                if (c.isProperType(result)) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * 尝试贪婪固定（对于确定性约束）
     *
     * 当约束满足以下条件时，可以立即固定类型变量：
     * 1. 单个 EQUALITY 约束且是正确类型且是 final 类型
     * 2. 单个 UPPER 约束且是 final 类型
     *
     * @return 如果可以贪婪固定，返回结果类型；否则返回 null
     */
    fun tryGreedyFixation(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        val constraints = variableWithConstraints.constraints

        // 条件 1: 单个 EQUALITY 约束且是正确类型且是 final 类型
        constraints.singleOrNull { it.kind == ConstraintKind.EQUALITY }?.let { eq ->
            if (c.isProperType(eq.type) && isFinalOrDefinitiveType(c, eq.type)) {
                return eq.type
            }
        }

        // 条件 2: 单个 UPPER 约束且是 final 类型
        constraints.singleOrNull { it.kind == ConstraintKind.UPPER }?.let { upper ->
            if (c.isProperType(upper.type) && isFinalType(c, upper.type)) {
                return upper.type
            }
        }

        return null
    }

    /**
     * 查找单位约束的结果类型
     *
     * 如果变量只有一个 EQUALITY 约束且类型是正确类型，直接返回该类型。
     */
    private fun findResultTypeOfTheUnitConstraint(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        val constraints = variableWithConstraints.constraints

        // 只有一个 EQUALITY 约束
        val equalityConstraint = constraints.singleOrNull { it.kind == ConstraintKind.EQUALITY }
        if (equalityConstraint != null && c.isProperType(equalityConstraint.type)) {
            return equalityConstraint.type
        }

        return null
    }

    /**
     * 求子类型（从 LOWER 约束求 LUB）
     *
     * 计算所有下界约束的最小上界（Least Upper Bound）。
     */
    private fun findSubType(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        val lowerConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.LOWER
        }

        if (lowerConstraints.isEmpty()) return null

        // 计算所有下界的 LUB
        val types = lowerConstraints.map { it.type }
        return c.commonSuperTypeOrNull(types) ?: c.intersectTypes(types)
    }

    /**
     * 求超类型（从 UPPER 约束求 GLB）
     *
     * 计算所有上界约束的最大下界（Greatest Lower Bound）。
     */
    private fun findSuperType(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        val upperConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.UPPER
        }

        if (upperConstraints.isEmpty()) return null

        // 计算所有上界的 GLB
        val types = upperConstraints.map { it.type }
        return c.intersectTypes(types)
    }

    /**
     * 通用求解（先尝试下界，再尝试上界）
     */
    private fun findResultTypeOrNull(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        // 先尝试 LOWER 约束
        findSubType(c, variableWithConstraints)?.let { return it }

        // 再尝试 UPPER 约束
        return findSuperType(c, variableWithConstraints)
    }

    /**
     * 检查是否为 final 类型（不可继承）
     */
    private fun isFinalType(c: ConstraintSystemCompletionContext, type: CangJieTypeMarker): Boolean {
        val classifier = type.typeConstructor().getClassifier()
        return when {
            classifier is ClassDescriptor -> classifier.isFinalClass
            type.isPrimitiveType() -> true
            else -> false
        }
    }

    /**
     * 检查是否为确定性类型（final 类型或原始类型）
     */
    private fun isFinalOrDefinitiveType(
        c: ConstraintSystemCompletionContext,
        type: CangJieTypeMarker
    ): Boolean {
        return isFinalType(c, type) || type.isPrimitiveType() || type.isStringType()
    }
}
```

---

### 3. 修改完成器支持迭代

**修改文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/components/CangJieConstraintSystemCompleter.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.InferenceIterationState
import org.cangnova.cangjie.resolve.calls.model.ResolvedAtom
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtom
import org.cangnova.cangjie.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeAtom
import org.cangnova.cangjie.types.model.CangJieTypeMarker

/**
 * 仓颉约束系统完成器
 *
 * 负责完成类型推导过程，包括：
 * 1. 迭代式求解类型变量
 * 2. 分析延迟参数（如 Lambda）
 * 3. 报告推导错误
 *
 * 采用迭代式求解策略，支持：
 * - 部分解（partial solution）
 * - 贪婪固定（greedy fixation）
 * - 参数顺序优化
 */
class CangJieConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    private val variableFixationFinder: VariableFixationFinder,
    private val postponedArgumentInputTypesResolver: PostponedArgumentInputTypesResolver,
    private val argumentOrderOptimizer: ArgumentOrderOptimizer
) {
    /**
     * 运行迭代式完成
     *
     * @param c 约束系统完成上下文
     * @param completionMode 完成模式
     * @param topLevelAtoms 顶层解析原子
     * @param topLevelType 顶层期望类型
     * @param diagnosticsHolder 诊断信息持有者
     * @param analyze 延迟参数分析函数
     */
    fun runCompletion(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: CangJieTypeMarker?,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        // 创建迭代状态
        val iterationState = InferenceIterationState(
            typeVariablesToSolve = c.notFixedTypeVariables.values.map { it.typeVariable }
        )

        // 主迭代循环
        while (iterationState.shouldContinue()) {
            iterationState.startNewIteration()

            val madeProgress = runSingleIteration(
                c, completionMode, topLevelAtoms, topLevelType,
                diagnosticsHolder, analyze, iterationState
            )

            if (!madeProgress) {
                break
            }
        }

        // 最终处理：报告错误、强制分析剩余参数
        finalizeCompletion(c, completionMode, topLevelAtoms, diagnosticsHolder, analyze)
    }

    /**
     * 单轮迭代
     *
     * 每轮迭代按以下优先级执行：
     * 1. 贪婪固定确定性约束
     * 2. 用部分解分析延迟参数
     * 3. 固定就绪的类型变量
     * 4. 解析 Lambda 参数类型
     *
     * @return 本轮是否有进展
     */
    private fun runSingleIteration(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: CangJieTypeMarker?,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit,
        state: InferenceIterationState
    ): Boolean {
        // Phase 1: 贪婪固定确定性约束
        if (tryGreedyFixations(c, state)) {
            return true
        }

        // Phase 2: 用部分解重新分析延迟参数
        if (analyzePostponedArgumentsWithPartialSolution(c, topLevelAtoms, analyze, state)) {
            return true
        }

        // Phase 3: 固定就绪的类型变量（允许部分解）
        if (fixReadyVariablesWithPartialSolution(c, completionMode, topLevelAtoms, state)) {
            return true
        }

        // Phase 4: 处理 Lambda 参数类型
        if (resolveLambdaParameterTypes(c, topLevelAtoms, analyze, state)) {
            return true
        }

        return false
    }

    /**
     * Phase 1: 贪婪固定
     *
     * 对于具有确定性约束的类型变量，立即固定。
     * 确定性约束包括：
     * - 单个 EQUALITY 约束且是 final 类型
     * - 单个 UPPER 约束且是 final 类型
     */
    private fun tryGreedyFixations(
        c: ConstraintSystemCompletionContext,
        state: InferenceIterationState
    ): Boolean {
        var madeProgress = false

        for ((_, variableWithConstraints) in c.notFixedTypeVariables) {
            val greedyResult = resultTypeResolver.tryGreedyFixation(c, variableWithConstraints)
            if (greedyResult != null) {
                c.fixVariable(variableWithConstraints.typeVariable, greedyResult)
                state.recordFixation(variableWithConstraints.typeVariable, greedyResult)
                madeProgress = true
            }
        }

        return madeProgress
    }

    /**
     * Phase 2: 用部分解分析延迟参数
     *
     * 使用当前的部分解替换延迟参数的期望类型，
     * 如果替换后类型变得更具体，重新分析该参数。
     */
    private fun analyzePostponedArgumentsWithPartialSolution(
        c: ConstraintSystemCompletionContext,
        topLevelAtoms: List<ResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit,
        state: InferenceIterationState
    ): Boolean {
        if (state.partialSolution.isEmpty()) return false

        val partialSubstitutor = state.createPartialSubstitutor()
        var madeProgress = false

        // 查找可以用部分解分析的延迟参数
        for (atom in topLevelAtoms) {
            val postponedAtom = atom as? PostponedResolvedAtom ?: continue
            if (postponedAtom.analyzed) continue

            // 检查期望类型是否可以用部分解确定
            val expectedType = postponedAtom.expectedType ?: continue
            val substitutedType = partialSubstitutor.substitute(expectedType)

            // 如果替换后类型变得更具体，重新分析
            if (substitutedType != expectedType && isMoreConcrete(substitutedType, expectedType)) {
                postponedAtom.updateExpectedType(substitutedType)
                analyze(postponedAtom)
                madeProgress = true
                state.hasNewInfo = true
            }
        }

        return madeProgress
    }

    /**
     * Phase 3: 固定就绪的变量（支持部分解）
     *
     * 使用 VariableFixationFinder 找到最佳候选变量，
     * 然后使用 ResultTypeResolver 求解（允许部分解）。
     */
    private fun fixReadyVariablesWithPartialSolution(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        state: InferenceIterationState
    ): Boolean {
        // 查找最佳候选变量
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            c, completionMode, topLevelAtoms
        ) ?: return false

        val variableWithConstraints = c.notFixedTypeVariables[variableForFixation.variable]
            ?: return false

        // 尝试求解（允许部分解）
        val resultType = resultTypeResolver.findResultType(
            c, variableWithConstraints, variableForFixation.direction,
            allowPartial = true
        )

        if (resultType != null) {
            c.fixVariable(variableWithConstraints.typeVariable, resultType)
            state.recordFixation(variableWithConstraints.typeVariable, resultType)
            return true
        }

        return false
    }

    /**
     * Phase 4: 解析 Lambda 参数类型
     *
     * 使用部分解确定 Lambda 参数的类型，
     * 当所有参数类型都已确定时，分析 Lambda 体。
     */
    private fun resolveLambdaParameterTypes(
        c: ConstraintSystemCompletionContext,
        topLevelAtoms: List<ResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit,
        state: InferenceIterationState
    ): Boolean {
        if (state.partialSolution.isEmpty()) return false

        val partialSubstitutor = state.createPartialSubstitutor()
        var madeProgress = false

        for (atom in topLevelAtoms) {
            val lambdaAtom = atom as? LambdaWithTypeVariableAsExpectedTypeAtom ?: continue
            if (lambdaAtom.analyzed) continue

            // 获取 Lambda 的期望函数类型
            val expectedFunctionType = lambdaAtom.expectedType as? FunctionType ?: continue

            // 用部分解替换参数类型
            val substitutedParamTypes = expectedFunctionType.parameterTypes.map { paramType ->
                partialSubstitutor.substitute(paramType)
            }

            // 检查参数类型是否都已确定
            if (substitutedParamTypes.all { c.isProperType(it) }) {
                // 可以分析 Lambda 了
                lambdaAtom.updateParameterTypes(substitutedParamTypes)
                analyze(lambdaAtom)
                madeProgress = true
                state.hasNewInfo = true
            }
        }

        return madeProgress
    }

    /**
     * 最终处理
     *
     * 迭代结束后：
     * 1. 报告无法推导的类型变量
     * 2. 强制分析剩余的延迟参数（使用 error type）
     */
    private fun finalizeCompletion(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        // 报告无法推导的类型变量
        for ((_, variableWithConstraints) in c.notFixedTypeVariables) {
            diagnosticsHolder.addDiagnostic(
                NotEnoughInformationForTypeParameter(
                    variableWithConstraints.typeVariable,
                    variableWithConstraints.constraints
                )
            )
        }

        // 强制分析剩余的延迟参数
        for (atom in topLevelAtoms) {
            val postponedAtom = atom as? PostponedResolvedAtom ?: continue
            if (!postponedAtom.analyzed) {
                // 使用 error type 分析
                analyze(postponedAtom)
            }
        }
    }

    /**
     * 检查 newType 是否比 oldType 更具体
     *
     * 更具体的定义：包含更少的类型变量
     */
    private fun isMoreConcrete(newType: CangJieTypeMarker, oldType: CangJieTypeMarker): Boolean {
        val newVarCount = countTypeVariables(newType)
        val oldVarCount = countTypeVariables(oldType)
        return newVarCount < oldVarCount
    }

    /**
     * 统计类型中包含的类型变量数量
     */
    private fun countTypeVariables(type: CangJieTypeMarker): Int {
        var count = 0
        type.forEachType {
            if (it.isTypeVariable()) count++
        }
        return count
    }
}
```

---

### 4. 添加参数顺序优化器

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/components/ArgumentOrderOptimizer.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.resolve.calls.model.ResolvedAtom
import org.cangnova.cangjie.types.model.CangJieTypeMarker

/**
 * 参数顺序优化器
 *
 * 仓颉编译器按以下顺序处理参数以优化类型推导：
 * 1. Option 类型（嵌套深度深的优先）
 * 2. 其他非理想类型
 * 3. 理想类型（最后处理）
 *
 * 优化原因：
 * - Option 类型优先：因为仓颉支持自动装箱，先处理 Option 类型可以正确推导 `Equatable<Option<A>>`
 * - 非理想类型优先：先处理具体类型，限制理想类型的范围
 * - 理想类型最后：理想类型可以转换为多种具体类型，最后处理避免过度泛化
 */
class ArgumentOrderOptimizer {

    /**
     * 获取优化后的参数处理顺序
     *
     * @param arguments 原始参数列表
     * @return 优化后的参数索引顺序
     */
    fun getOptimizedOrder(arguments: List<ResolvedAtom>): List<Int> {
        val indexed = arguments.mapIndexed { index, atom -> index to atom }

        val options = mutableListOf<Pair<Int, ResolvedAtom>>()
        val ideals = mutableListOf<Pair<Int, ResolvedAtom>>()
        val others = mutableListOf<Pair<Int, ResolvedAtom>>()

        for ((index, atom) in indexed) {
            val type = atom.expectedType ?: atom.resultType
            when {
                type != null && isOptionType(type) -> options.add(index to atom)
                type != null && isIdealType(type) -> ideals.add(index to atom)
                else -> others.add(index to atom)
            }
        }

        // Option 类型按嵌套深度排序（深的优先）
        options.sortByDescending { (_, atom) ->
            getOptionNestingLevel(atom.expectedType ?: atom.resultType!!)
        }

        // 合并：Option → 其他 → 理想类型
        return (options + others + ideals).map { it.first }
    }

    /**
     * 检查是否为 Option 类型
     */
    private fun isOptionType(type: CangJieTypeMarker): Boolean {
        return type.typeConstructor().getClassifier()?.let { classifier ->
            classifier is ClassDescriptor && classifier.name.asString() == "Option"
        } ?: false
    }

    /**
     * 检查是否为理想类型
     *
     * 理想类型：字面量的默认类型，如整数字面量的 IdealInt、浮点字面量的 IdealFloat
     */
    private fun isIdealType(type: CangJieTypeMarker): Boolean {
        return type.typeConstructor().getClassifier()?.let { classifier ->
            classifier.name.asString().startsWith("Ideal")
        } ?: false
    }

    /**
     * 获取 Option 类型的嵌套深度
     *
     * 示例：
     * - Option<Int64> → 1
     * - Option<Option<Int64>> → 2
     * - Option<Option<Option<Int64>>> → 3
     */
    private fun getOptionNestingLevel(type: CangJieTypeMarker): Int {
        var level = 0
        var current: CangJieTypeMarker? = type

        while (current != null && isOptionType(current)) {
            level++
            current = current.typeArguments().firstOrNull()?.type
        }

        return level
    }
}
```

---

### 5. 修改 ResolutionParts 处理显式类型参数

**修改文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/components/ResolutionParts.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.ClassValueReceiver
import org.cangnova.cangjie.resolve.calls.model.ResolutionCandidate
import org.cangnova.cangjie.types.CangJieType

/**
 * 创建 Fresh Variables 替换器
 *
 * 负责为类型推导创建新鲜类型变量，并处理显式类型参数。
 */
object CreateFreshVariablesSubstitutor : ResolutionPart() {

    /**
     * 获取需要推导的类型参数
     *
     * 改进：排除已有显式类型参数的情况
     *
     * @return 需要创建 Fresh Variable 的类型参数列表
     */
    fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
        if (resolvedCall.dispatchReceiverArgument != null &&
            resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver
        ) {
            val classValueReceiver = resolvedCall.dispatchReceiverArgument!!
                .receiver.receiverValue as ClassValueReceiver
            val classType = classValueReceiver.type

            // ✓ 改进：检查是否已有显式类型参数
            if (classType.arguments.isNotEmpty()) {
                // 类的类型参数已显式给出（如 a<Int64>），只返回方法的类型参数
                return candidateDescriptor.original.typeParameters
            }

            // 类型参数未给出（如 a），需要推导类和方法的类型参数
            return classValueReceiver.classQualifier.descriptor.declaredTypeParameters +
                    candidateDescriptor.original.typeParameters
        }

        return candidateDescriptor.original.typeParameters
    }

    /**
     * 提取已知的显式类型参数
     *
     * 从 ClassValueReceiver 的类型中提取显式给出的类型参数。
     *
     * 示例：
     * - a<Int64>.a1() → {T -> Int64}
     * - a.a1() → {}
     *
     * @return 类型参数到显式类型的映射
     */
    fun ResolutionCandidate.extractExplicitTypeArguments(): Map<TypeParameterDescriptor, CangJieType> {
        val receiver = resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue

        if (receiver is ClassValueReceiver) {
            val classDescriptor = receiver.classQualifier.descriptor
            val classType = receiver.type

            if (classType.arguments.isEmpty()) return emptyMap()

            return classDescriptor.declaredTypeParameters
                .zip(classType.arguments)
                .mapNotNull { (param, projection) ->
                    val argType = projection.type as? CangJieType
                    if (argType != null) param to argType else null
                }
                .toMap()
        }

        return emptyMap()
    }

    /**
     * 处理类型参数
     *
     * 1. 获取需要推导的类型参数
     * 2. 提取显式类型参数
     * 3. 创建 Fresh Variables
     * 4. 为显式类型参数添加 EQUALITY 约束
     */
    override fun ResolutionCandidate.process(workIndex: Int): List<CangJieCallDiagnostic> {
        val csBuilder = getSystem().getBuilder()

        // 1. 获取需要推导的类型参数
        val typeParameters = getTypeParameters()

        // 2. 提取显式类型参数
        val explicitTypeArguments = extractExplicitTypeArguments()

        // 3. 创建 Fresh Variables（仅对需要推导的）
        val (toFreshVariables, freshTypeVariables) = if (typeParameters.isEmpty()) {
            ComposableTypeSubstitutor.EMPTY to emptyList()
        } else {
            createToFreshVariableSubstitutorAndAddInitialConstraints(
                candidateDescriptor, cangjieCall, csBuilder, typeParameters
            )
        }

        // 4. 为显式类型参数添加 EQUALITY 约束
        for ((typeParam, explicitType) in explicitTypeArguments) {
            val freshVar = freshTypeVariables.find {
                it.originalTypeParameter == typeParam
            }
            if (freshVar != null) {
                csBuilder.addEqualityConstraint(
                    freshVar.defaultType,
                    explicitType,
                    ExplicitTypeArgumentConstraintPosition(typeParam)
                )
            }
        }

        // 5. 存储替换器供后续使用
        resolvedCall.typeParameterSubstitutor = buildCurrentSubstitutor(
            csBuilder, explicitTypeArguments, toFreshVariables
        )

        return emptyList()
    }

    /**
     * 构建包含显式类型参数的替换器
     */
    private fun buildCurrentSubstitutor(
        csBuilder: ConstraintSystemBuilder,
        explicitTypeArguments: Map<TypeParameterDescriptor, CangJieType>,
        toFreshVariables: ComposableTypeSubstitutor
    ): TypeSubstitutor {
        // 先应用显式类型参数，再应用 Fresh Variables
        val explicitSubstitutor = TypeSubstitutor.create(
            explicitTypeArguments.mapKeys { it.key.typeConstructor }
        )

        return ComposableTypeSubstitutor.chain(
            explicitSubstitutor,
            toFreshVariables,
            csBuilder.buildCurrentSubstitutor()
        )
    }
}

/**
 * 显式类型参数约束位置
 *
 * 用于标记约束来源于显式类型参数，便于错误报告。
 */
class ExplicitTypeArgumentConstraintPosition(
    val typeParameter: TypeParameterDescriptor
) : ConstraintPosition {
    override fun toString(): String = "ExplicitTypeArgument($typeParameter)"
}
```

---

### 6. 整合到调用解析流程

**修改文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/CangJieCallResolver.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ArgumentOrderOptimizer
import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.model.CangJieCall
import org.cangnova.cangjie.resolve.calls.model.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.types.CangJieType

/**
 * 仓颉调用解析器
 *
 * 负责解析函数调用，包括：
 * 1. 创建 Fresh Variables
 * 2. 按优化顺序处理参数
 * 3. 运行迭代式类型推导
 * 4. 选择最佳候选
 */
class CangJieCallResolver(
    private val constraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val argumentOrderOptimizer: ArgumentOrderOptimizer
) {

    /**
     * 解析调用
     *
     * @param call 调用表达式
     * @param candidates 候选函数列表
     * @param expectedType 期望的返回类型
     * @return 成功解析的调用，如果全部失败则返回 null
     */
    fun resolveCall(
        call: CangJieCall,
        candidates: Collection<ResolutionCandidate>,
        expectedType: CangJieType?
    ): ResolvedCall<*>? {

        for (candidate in candidates) {
            val csBuilder = candidate.getSystem().getBuilder()

            // 1. 创建 Fresh Variables（已包含显式类型参数处理）
            CreateFreshVariablesSubstitutor.run { candidate.process(0) }

            // 2. 按优化顺序处理参数
            val optimizedOrder = argumentOrderOptimizer.getOptimizedOrder(
                candidate.resolvedCall.argumentMappings
            )

            for (argIndex in optimizedOrder) {
                val argument = candidate.resolvedCall.argumentMappings[argIndex]
                processArgument(candidate, argument, csBuilder)
            }

            // 3. 添加返回类型约束
            if (expectedType != null) {
                csBuilder.addSubtypeConstraint(
                    candidate.resolvedCall.resultType,
                    expectedType,
                    ExpectedTypeConstraintPosition
                )
            }

            // 4. 运行迭代式完成
            constraintSystemCompleter.runCompletion(
                csBuilder.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.FULL,
                candidate.resolvedCall.resolvedAtoms,
                expectedType,
                candidate.diagnosticsHolder
            ) { postponedAtom ->
                analyzePostponedArgument(candidate, postponedAtom)
            }

            // 5. 检查是否成功
            if (!csBuilder.hasContradiction &&
                csBuilder.notFixedTypeVariables.isEmpty()
            ) {
                return candidate.resolvedCall
            }
        }

        return null
    }

    /**
     * 处理参数
     */
    private fun processArgument(
        candidate: ResolutionCandidate,
        argument: ArgumentMapping,
        csBuilder: ConstraintSystemBuilder
    ) {
        // ... 参数处理逻辑
    }

    /**
     * 分析延迟参数
     */
    private fun analyzePostponedArgument(
        candidate: ResolutionCandidate,
        postponedAtom: PostponedResolvedAtom
    ) {
        // ... 延迟参数分析逻辑
    }
}
```

---

## 完整流程图

### 场景 1: 显式类型参数

```
a<Int64>.a1() 调用解析
    │
    ▼
┌─────────────────────────────────────────┐
│ 1. getTypeParameters()                  │
│    检测到 classType.arguments = [Int64] │
│    返回: [] (只有方法的类型参数)          │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 2. extractExplicitTypeArguments()       │
│    返回: {T -> Int64}                   │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 3. 迭代式完成循环                        │
│    ┌─────────────────────────────────┐  │
│    │ 第 1 轮:                        │  │
│    │ - T 已通过显式参数固定为 Int64   │  │
│    │ - 无其他变量需要推导            │  │
│    │ - unsolvedCount = 0             │  │
│    └─────────────────────────────────┘  │
│    退出循环                              │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 4. 返回成功解析的调用                    │
│    a1() 的返回类型: Int64               │
└─────────────────────────────────────────┘
```

### 场景 2: Lambda 参数推导

```
process([1, 2, 3]) { x => x > 0 } 调用解析
    │
    ▼
┌─────────────────────────────────────────┐
│ 1. 参数顺序优化                          │
│    [1,2,3] 不是 Option，优先处理        │
│    Lambda 延迟处理                       │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 2. 处理 [1, 2, 3]                       │
│    约束: Array<Int64> <: Array<T>       │
│    → T 有下界约束 Int64                  │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 3. 迭代式完成循环                        │
│    ┌─────────────────────────────────┐  │
│    │ 第 1 轮:                        │  │
│    │ - 贪婪固定: T = Int64           │  │
│    │ - partialSolution = {T: Int64}  │  │
│    │ - hasNewInfo = true             │  │
│    └─────────────────────────────────┘  │
│    ┌─────────────────────────────────┐  │
│    │ 第 2 轮:                        │  │
│    │ - 用部分解分析 Lambda           │  │
│    │ - x: Int64 (从 T = Int64)       │  │
│    │ - 分析 x > 0，类型正确          │  │
│    │ - unsolvedCount = 0             │  │
│    └─────────────────────────────────┘  │
│    退出循环                              │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 4. 返回成功                              │
│    process<Int64>([1,2,3]) {...}        │
└─────────────────────────────────────────┘
```

### 场景 3: 嵌套 Option 类型

```
mapOr(Some(1), "default") { x => x.toString() } 调用解析
    │
    ▼
┌─────────────────────────────────────────┐
│ 1. 参数顺序优化                          │
│    Some(1) 是 Option，优先处理          │
│    "default" 不是 Option                │
│    Lambda 延迟处理                       │
│    顺序: [0, 1, 2] → [0, 1, 2]          │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 2. 处理 Some(1)                         │
│    约束: Option<Int64> <: Option<T>     │
│    → T = Int64                          │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 3. 处理 "default"                       │
│    约束: String <: R                    │
│    → R 有下界约束 String                │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 4. 迭代式完成循环                        │
│    ┌─────────────────────────────────┐  │
│    │ 第 1 轮:                        │  │
│    │ - 贪婪固定: T = Int64           │  │
│    │ - partialSolution = {T: Int64}  │  │
│    └─────────────────────────────────┘  │
│    ┌─────────────────────────────────┐  │
│    │ 第 2 轮:                        │  │
│    │ - 用 T=Int64 分析 Lambda        │  │
│    │ - x: Int64                      │  │
│    │ - x.toString() : String         │  │
│    │ - 约束: String <: R             │  │
│    │ - R = String (贪婪固定)         │  │
│    │ - unsolvedCount = 0             │  │
│    └─────────────────────────────────┘  │
│    退出循环                              │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│ 5. 返回成功                              │
│    mapOr<Int64, String>(...)            │
└─────────────────────────────────────────┘
```

---

## 测试用例

```kotlin
// 测试文件: inference/IterativeInferenceTest.kt

package org.cangnova.cangjie.resolve.calls.inference

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IterativeInferenceTest {

    @Test
    fun `explicit type argument on class`() {
        // a<Int64>.a1() 应该成功
        val result = resolve("a<Int64>.a1()")
        assertNotNull(result)
        assertEquals("Int64", result.returnType.toString())
    }

    @Test
    fun `explicit type argument conflict`() {
        // a<String>.a2(1) 应该报错：Int64 不兼容 String
        val result = resolve("a<String>.a2(1)")
        assertNull(result)
        assertContainsDiagnostic("type mismatch")
    }

    @Test
    fun `lambda parameter type from partial solution`() {
        // process([1,2,3]) { x => x > 0 }
        // x 应该被推导为 Int64
        val result = resolve("process([1, 2, 3]) { x => x > 0 }")
        assertNotNull(result)

        val lambdaParamType = result.getLambdaParameterType(0)
        assertEquals("Int64", lambdaParamType.toString())
    }

    @Test
    fun `option None with context type`() {
        // let x: Option<Int64> = None
        val result = resolve("let x: Option<Int64> = None")
        assertNotNull(result)
    }

    @Test
    fun `chained generic calls`() {
        // Some(1).map { x => x.toString() }
        val result = resolve("Some(1).map { x => x.toString() }")
        assertNotNull(result)
        assertEquals("Option<String>", result.returnType.toString())
    }

    @Test
    fun `nested option inference`() {
        // Some(Some(1))
        val result = resolve("Some(Some(1))")
        assertNotNull(result)
        assertEquals("Option<Option<Int64>>", result.returnType.toString())
    }

    @Test
    fun `multiple type variables with lambda`() {
        // mapOr(Some(1), "default") { x => x.toString() }
        val result = resolve("mapOr(Some(1), \"default\") { x => x.toString() }")
        assertNotNull(result)
        assertEquals("String", result.returnType.toString())
    }

    @Test
    fun `builder pattern inference`() {
        // QueryBuilder<User>().where { u => u.age > 18 }.select { u => u.name }
        val result = resolve("""
            QueryBuilder<User>()
                .where { u => u.age > 18 }
                .select { u => u.name }
        """)
        assertNotNull(result)
    }

    @Test
    fun `iteration limit prevents infinite loop`() {
        // 构造一个可能导致无限循环的场景
        // 验证迭代在达到最大次数后终止
        val result = resolveWithDiagnostics("complexRecursiveGenericCall()")
        // 即使推导失败，也不应该无限循环
        assertNotNull(result.diagnostics)
    }

    @Test
    fun `greedy fixation for final types`() {
        // func foo<T>(x: T): T where T <: Int64
        // foo(1) 应该立即固定 T = Int64
        val result = resolve("foo(1)")
        assertNotNull(result)
        assertEquals("Int64", result.returnType.toString())
    }

    @Test
    fun `argument order optimization - option first`() {
        // func bar<T>(opt: Option<T>, ideal: T): T
        // bar(Some(1), 2) 应该先处理 Option 参数
        val result = resolve("bar(Some(1), 2)")
        assertNotNull(result)
        assertEquals("Int64", result.returnType.toString())
    }
}
```

---

## 实现检查清单

### 核心组件

- [ ] `InferenceIterationState` - 迭代状态管理
- [ ] `ResultTypeResolver.findResultType(allowPartial)` - 部分解支持
- [ ] `ResultTypeResolver.tryGreedyFixation()` - 贪婪固定
- [ ] `CangJieConstraintSystemCompleter.runCompletion()` - 迭代主循环
- [ ] `ArgumentOrderOptimizer` - 参数顺序优化

### 显式类型参数处理

- [ ] `ResolutionCandidate.getTypeParameters()` - 排除已显式给出的参数
- [ ] `ResolutionCandidate.extractExplicitTypeArguments()` - 提取显式类型
- [ ] `ExplicitTypeArgumentConstraintPosition` - 约束位置标记
- [ ] EQUALITY 约束添加逻辑

### 测试覆盖

- [ ] 显式类型参数基本场景
- [ ] 显式类型与推导冲突
- [ ] Lambda 参数类型推导
- [ ] Option/None 上下文类型
- [ ] 链式泛型调用
- [ ] 嵌套 Option 推导
- [ ] 多类型变量场景
- [ ] Builder 模式
- [ ] 迭代限制测试
- [ ] 贪婪固定测试
- [ ] 参数顺序优化测试

---

## 与编译器实现的对应关系

| 编译器组件 | 插件组件 |
|-----------|---------|
| `TyArgSynState` | `InferenceIterationState` |
| `LocalTypeArgumentSynthesis` | `CangJieConstraintSystemCompleter` |
| `TyVarConstraintGraph` | `VariableFixationFinder` |
| `FindSolution` | `ResultTypeResolver` |
| `GetOrderedCheckingIndexes` | `ArgumentOrderOptimizer` |
| `SubstPack.inst` | `extractExplicitTypeArguments()` |
| `IsGreedySolution` | `tryGreedyFixation()` |

---

**文档版本**: 1.0
**创建日期**: 2026-01-18
**状态**: 设计完成，待实现
