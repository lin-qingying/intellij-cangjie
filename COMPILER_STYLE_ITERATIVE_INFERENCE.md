# 编译器风格迭代式类型推导实现方案

## 文档概述

本文档描述如何在仓颉语言插件中实现与编译器一致的迭代式类型推导方案。

**选择理由**：编译器方案具有更强的表达能力，支持信息双向流动，能处理更多边界场景，且与仓颉语言设计一致。

---

## 架构对比

### 当前插件架构（Kotlin 风格）

```
┌─────────────────────────────────────────────────────────┐
│ ResolutionParts.process()                               │
│     ↓                                                   │
│ 创建 FreshVariables (问题：丢弃显式类型)                 │
│     ↓                                                   │
│ 收集参数约束（一次性）                                   │
│     ↓                                                   │
│ CangJieConstraintSystemCompleter.runCompletion()        │
│     ↓                                                   │
│ 9阶段完成循环（Kotlin风格，信息单向流动）                │
└─────────────────────────────────────────────────────────┘
```

### 目标架构（编译器风格）

```
┌─────────────────────────────────────────────────────────┐
│ ResolutionParts.process()                               │
│     ↓                                                   │
│ 创建 FreshVariables + 处理显式类型参数                   │
│     ↓                                                   │
│ IterativeTypeInferenceEngine.run()                      │
│     ↓                                                   │
│ while (hasProgress && unsolvedCount > 0) {              │
│     1. 按优化顺序综合参数（用部分解）                    │
│     2. 收集约束                                         │
│     3. 求解（允许部分解）                               │
│     4. 更新部分解                                       │
│ }                                                       │
│     ↓                                                   │
│ 信息可双向流动，支持参数重新分析                         │
└─────────────────────────────────────────────────────────┘
```

### 核心差异

| 方面 | Kotlin 风格 | 编译器风格 |
|------|------------|-----------|
| 信息流向 | 单向（前向） | 双向（可回流） |
| 参数分析 | 一次性 | 可重新分析 |
| 部分解 | 有限支持 | 完整支持 |
| 迭代层级 | 完成阶段内 | 参数综合 + 求解 |

---

## 实现步骤

### 第一步：核心数据结构

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/IterativeInferenceContext.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.model.CangJieCall
import org.cangnova.cangjie.resolve.calls.model.ResolvedCallArgument
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 迭代式推导上下文
 *
 * 对应编译器的 TyArgSynState，追踪迭代推导的完整状态。
 *
 * @property call 当前调用
 * @property typeVariablesToSolve 需要求解的类型变量列表
 * @property arguments 参数综合状态列表
 */
class IterativeInferenceContext(
    val call: CangJieCall,
    val typeVariablesToSolve: List<TypeVariableMarker>,
    val arguments: List<ArgumentSynthesisState>
) {
    /** 当前部分解：已固定的类型变量 -> 具体类型 */
    val partialSolution: MutableMap<TypeVariableMarker, CangJieType> = mutableMapOf()

    /** 未求解变量数 */
    var unsolvedCount: Int = typeVariablesToSolve.size

    /** 本轮是否有新信息 */
    var hasNewInfo: Boolean = true

    /** 迭代计数 */
    var iteration: Int = 0

    /** 最大迭代次数（防止无限循环） */
    val maxIterations: Int = 10

    /**
     * 是否应继续迭代
     *
     * 继续条件：
     * 1. 本轮有新信息
     * 2. 还有未求解的变量
     * 3. 未达到最大迭代次数
     */
    fun shouldContinue(): Boolean =
        hasNewInfo && unsolvedCount > 0 && iteration < maxIterations

    /**
     * 记录变量固定
     */
    fun recordFixation(variable: TypeVariableMarker, type: CangJieType) {
        if (variable !in partialSolution) {
            partialSolution[variable] = type
            unsolvedCount--
            hasNewInfo = true
        }
    }

    /**
     * 开始新迭代
     */
    fun startIteration() {
        hasNewInfo = false
        iteration++
    }

    /**
     * 创建部分解替换器
     */
    fun createPartialSubstitutor(): TypeSubstitutor {
        return TypeSubstitutor.create(
            partialSolution.mapKeys { (v, _) -> v.freshTypeConstructor() }
        )
    }

    /**
     * 检查变量是否已固定
     */
    fun isFixed(variable: TypeVariableMarker): Boolean = variable in partialSolution

    /**
     * 获取变量的固定类型
     */
    fun getFixedType(variable: TypeVariableMarker): CangJieType? = partialSolution[variable]

    override fun toString(): String {
        return "IterativeInferenceContext(" +
                "iteration=$iteration, " +
                "unsolved=$unsolvedCount/${typeVariablesToSolve.size}, " +
                "hasNewInfo=$hasNewInfo, " +
                "solution=$partialSolution)"
    }
}

/**
 * 参数综合状态
 *
 * 追踪单个参数的综合过程。
 *
 * @property argument 原始参数
 * @property parameterType 期望的参数类型
 * @property index 参数索引
 */
class ArgumentSynthesisState(
    val argument: ResolvedCallArgument,
    val parameterType: CangJieType,
    val index: Int
) {
    /** 综合后的参数类型 */
    var synthesizedType: CangJieType? = null

    /** 是否已失败 */
    var failed: Boolean = false

    /** 是否已分析 */
    var analyzed: Boolean = false

    /** 是否为 Lambda */
    val isLambda: Boolean get() = argument.isLambda()

    /** 是否为 Option 类型 */
    val isOptionType: Boolean get() = parameterType.isOptionType()

    /** Option 嵌套深度 */
    val optionNestingLevel: Int get() = parameterType.getOptionNestingLevel()

    /** 是否为理想类型 */
    val isIdealType: Boolean get() = parameterType.isIdealType()

    /**
     * 重置状态以便重新分析
     */
    fun reset() {
        synthesizedType = null
        analyzed = false
        // 注意：failed 状态不重置，失败的参数不会重新尝试
    }
}
```

---

### 第二步：参数顺序优化器

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ArgumentOrderOptimizer.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

/**
 * 参数顺序优化器
 *
 * 按以下优先级排序参数，以优化类型推导效果：
 *
 * 1. Option 类型（嵌套深度深的优先）
 *    - 原因：仓颉支持自动装箱，先处理 Option 可正确推导 Equatable<Option<A>>
 *
 * 2. 非 Lambda、非理想类型参数
 *    - 原因：这些参数提供最确定的类型信息
 *
 * 3. Lambda 参数
 *    - 原因：Lambda 参数类型通常依赖其他参数的推导结果
 *
 * 4. 理想类型参数（最后）
 *    - 原因：理想类型可转换为多种具体类型，最后处理避免过度泛化
 */
object ArgumentOrderOptimizer {

    /**
     * 获取优化后的参数处理顺序
     *
     * @param arguments 原始参数状态列表
     * @return 优化后的参数索引顺序
     */
    fun getOptimizedOrder(arguments: List<ArgumentSynthesisState>): List<Int> {
        data class IndexedArg(val index: Int, val arg: ArgumentSynthesisState)

        val indexed = arguments.mapIndexed { i, arg -> IndexedArg(i, arg) }

        // 分组
        val options = indexed.filter { it.arg.isOptionType && !it.arg.isLambda }
        val normalNonLambda = indexed.filter {
            !it.arg.isOptionType && !it.arg.isLambda && !it.arg.isIdealType
        }
        val lambdas = indexed.filter { it.arg.isLambda }
        val ideals = indexed.filter { it.arg.isIdealType && !it.arg.isLambda }

        // Option 按嵌套深度排序（深的优先）
        val sortedOptions = options.sortedByDescending { it.arg.optionNestingLevel }

        // 合并顺序：Option → 普通非Lambda → Lambda → 理想类型
        return (sortedOptions + normalNonLambda + lambdas + ideals).map { it.index }
    }

    /**
     * 获取可以用部分解分析的参数
     *
     * @param arguments 参数状态列表
     * @param partialSolution 当前部分解
     * @return 可以分析的参数索引列表
     */
    fun getAnalyzableArguments(
        arguments: List<ArgumentSynthesisState>,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): List<Int> {
        return arguments.indices.filter { index ->
            val arg = arguments[index]
            !arg.failed && !arg.analyzed && canAnalyzeWithPartialSolution(arg, partialSolution)
        }
    }

    /**
     * 检查参数是否可以用部分解分析
     */
    private fun canAnalyzeWithPartialSolution(
        arg: ArgumentSynthesisState,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): Boolean {
        if (!arg.isLambda) {
            // 非 Lambda 参数总是可以分析
            return true
        }

        // Lambda 参数需要检查参数类型是否已确定
        val functionType = arg.parameterType as? FunctionType ?: return false
        return functionType.parameterTypes.all { paramType ->
            !paramType.containsUnresolvedTypeVariables(partialSolution)
        }
    }
}
```

---

### 第三步：迭代式推导引擎

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/IterativeTypeInferenceEngine.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

/**
 * 迭代式类型推导引擎
 *
 * 实现编译器风格的迭代推导算法：
 *
 * ```
 * while (hasProgress && unsolvedCount > 0) {
 *     1. 用部分解综合参数
 *     2. 收集约束
 *     3. 求解（允许部分解）
 *     4. 更新部分解
 * }
 * ```
 *
 * 核心特性：
 * - 支持部分解（partial solution）
 * - 支持参数重新分析
 * - 支持信息双向流动
 * - 参数顺序优化
 */
class IterativeTypeInferenceEngine(
    private val constraintSystemBuilder: ConstraintSystemBuilder,
    private val argumentSynthesizer: ArgumentSynthesizer,
    private val typeChecker: CangJieTypeChecker
) {

    /**
     * 运行迭代式推导
     *
     * @param context 推导上下文
     * @return 推导结果
     */
    fun run(context: IterativeInferenceContext): InferenceResult {
        // 获取优化后的参数顺序
        val argumentOrder = ArgumentOrderOptimizer.getOptimizedOrder(context.arguments)

        // 主迭代循环
        while (context.shouldContinue()) {
            context.startIteration()

            val madeProgress = runSingleIteration(context, argumentOrder)

            if (!madeProgress) {
                // 尝试强制分析延迟的 Lambda
                if (!tryForceAnalyzeLambdas(context)) {
                    break
                }
            }
        }

        return buildResult(context)
    }

    /**
     * 单轮迭代
     *
     * @return 本轮是否有进展
     */
    private fun runSingleIteration(
        context: IterativeInferenceContext,
        argumentOrder: List<Int>
    ): Boolean {
        var madeProgress = false

        // Phase 1: 用部分解综合参数
        if (synthesizeArgumentsWithPartialSolution(context, argumentOrder)) {
            madeProgress = true
        }

        // Phase 2: 收集约束
        collectConstraints(context)

        // Phase 3: 求解约束（允许部分解）
        if (solveConstraintsWithPartialSolution(context)) {
            madeProgress = true
        }

        return madeProgress
    }

    /**
     * Phase 1: 用部分解综合参数
     *
     * 关键改进：使用当前部分解替换期望类型后再分析参数
     */
    private fun synthesizeArgumentsWithPartialSolution(
        context: IterativeInferenceContext,
        argumentOrder: List<Int>
    ): Boolean {
        val substitutor = context.createPartialSubstitutor()
        var madeProgress = false

        for (argIndex in argumentOrder) {
            val argState = context.arguments[argIndex]

            // 跳过已失败的参数
            if (argState.failed) continue

            // 用部分解替换期望的参数类型
            val expectedType = substitutor.substitute(argState.parameterType)

            // 检查是否可以分析
            if (!canAnalyzeArgument(argState, expectedType, context)) {
                continue
            }

            // 综合参数
            val result = argumentSynthesizer.synthesize(
                argState.argument,
                expectedType,
                context.partialSolution
            )

            when (result) {
                is SynthesisResult.Success -> {
                    if (argState.synthesizedType != result.type) {
                        argState.synthesizedType = result.type
                        argState.analyzed = true
                        context.hasNewInfo = true
                        madeProgress = true
                    }
                }
                is SynthesisResult.Failure -> {
                    argState.failed = true
                }
                is SynthesisResult.Postponed -> {
                    // 保持延迟状态，等待更多信息
                }
            }
        }

        return madeProgress
    }

    /**
     * 检查参数是否可以分析
     */
    private fun canAnalyzeArgument(
        argState: ArgumentSynthesisState,
        expectedType: CangJieType,
        context: IterativeInferenceContext
    ): Boolean {
        // 已分析的参数在本轮跳过（但可能在下一轮重新分析）
        if (argState.analyzed) return false

        // Lambda 需要参数类型确定
        if (argState.isLambda) {
            val functionType = expectedType as? FunctionType ?: return false
            return functionType.parameterTypes.all { paramType ->
                isTypeFullyDetermined(paramType, context)
            }
        }

        return true
    }

    /**
     * Phase 2: 收集约束
     */
    private fun collectConstraints(context: IterativeInferenceContext) {
        val substitutor = context.createPartialSubstitutor()

        for (argState in context.arguments) {
            if (argState.failed || argState.synthesizedType == null) continue

            val expectedType = substitutor.substitute(argState.parameterType)
            val actualType = argState.synthesizedType!!

            // 添加子类型约束: actualType <: expectedType
            constraintSystemBuilder.addSubtypeConstraint(
                actualType,
                expectedType,
                ArgumentConstraintPosition(argState.index)
            )
        }
    }

    /**
     * Phase 3: 求解约束（允许部分解）
     *
     * 关键改进：即使结果包含未固定的类型变量，也尝试固定
     */
    private fun solveConstraintsWithPartialSolution(
        context: IterativeInferenceContext
    ): Boolean {
        val storage = constraintSystemBuilder.currentStorage()
        var madeProgress = false

        for ((_, variableWithConstraints) in storage.notFixedTypeVariables) {
            val variable = variableWithConstraints.typeVariable

            // 跳过已在部分解中的变量
            if (context.isFixed(variable)) continue

            // 尝试求解
            val resultType = tryFindResultType(variableWithConstraints, context)

            if (resultType != null) {
                // 检查结果是否可用
                if (isUsableResult(resultType, context)) {
                    constraintSystemBuilder.fixVariable(variable, resultType)
                    context.recordFixation(variable, resultType)
                    madeProgress = true
                }
            }
        }

        return madeProgress
    }

    /**
     * 尝试找到结果类型
     */
    private fun tryFindResultType(
        variableWithConstraints: VariableWithConstraints,
        context: IterativeInferenceContext
    ): CangJieType? {
        val constraints = variableWithConstraints.constraints
        val substitutor = context.createPartialSubstitutor()

        // 1. 检查 EQUALITY 约束
        constraints.filter { it.kind == ConstraintKind.EQUALITY }.forEach { eq ->
            val substitutedType = substitutor.substitute(eq.type as CangJieType)
            if (isUsableResult(substitutedType, context)) {
                return substitutedType
            }
        }

        // 2. 从 LOWER 约束求 Join（LUB）
        val lowerTypes = constraints
            .filter { it.kind == ConstraintKind.LOWER }
            .map { substitutor.substitute(it.type as CangJieType) }
            .filter { isUsableResult(it, context) }

        if (lowerTypes.isNotEmpty()) {
            val joined = computeJoin(lowerTypes)
            if (joined != null && isUsableResult(joined, context)) {
                return joined
            }
        }

        // 3. 从 UPPER 约束求 Meet（GLB）
        val upperTypes = constraints
            .filter { it.kind == ConstraintKind.UPPER }
            .map { substitutor.substitute(it.type as CangJieType) }
            .filter { isUsableResult(it, context) }

        if (upperTypes.isNotEmpty()) {
            val met = computeMeet(upperTypes)
            if (met != null && isUsableResult(met, context)) {
                return met
            }
        }

        return null
    }

    /**
     * 尝试强制分析延迟的 Lambda
     *
     * 当常规迭代无法继续时，尝试分析 Lambda 以获取更多信息
     */
    private fun tryForceAnalyzeLambdas(context: IterativeInferenceContext): Boolean {
        val substitutor = context.createPartialSubstitutor()
        var madeProgress = false

        for (argState in context.arguments) {
            if (!argState.isLambda || argState.failed || argState.analyzed) continue

            val expectedType = substitutor.substitute(argState.parameterType)
            val functionType = expectedType as? FunctionType ?: continue

            // 尝试分析 Lambda 体以推导返回类型
            val result = argumentSynthesizer.synthesizeLambdaWithUnknownParams(
                argState.argument,
                functionType
            )

            if (result is SynthesisResult.Success) {
                argState.synthesizedType = result.type
                argState.analyzed = true
                context.hasNewInfo = true
                madeProgress = true
            }
        }

        return madeProgress
    }

    /**
     * 检查类型是否完全确定
     */
    private fun isTypeFullyDetermined(
        type: CangJieType,
        context: IterativeInferenceContext
    ): Boolean {
        var fullyDetermined = true
        type.forEachTypeVariable { tv ->
            if (!context.isFixed(tv) &&
                tv !in constraintSystemBuilder.currentStorage().fixedTypeVariables
            ) {
                fullyDetermined = false
            }
        }
        return fullyDetermined
    }

    /**
     * 检查结果是否可用
     *
     * 可用条件：
     * 1. 不包含类型变量，或
     * 2. 所有类型变量都已在部分解或已固定变量中
     */
    private fun isUsableResult(type: CangJieType, context: IterativeInferenceContext): Boolean {
        var usable = true
        type.forEachTypeVariable { tv ->
            if (!context.isFixed(tv) &&
                tv !in constraintSystemBuilder.currentStorage().fixedTypeVariables
            ) {
                usable = false
            }
        }
        return usable
    }

    /**
     * 计算类型的 Join（LUB - 最小上界）
     */
    private fun computeJoin(types: List<CangJieType>): CangJieType? {
        if (types.isEmpty()) return null
        if (types.size == 1) return types[0]
        return types.reduce { acc, type ->
            typeChecker.commonSuperType(acc, type) ?: return null
        }
    }

    /**
     * 计算类型的 Meet（GLB - 最大下界）
     */
    private fun computeMeet(types: List<CangJieType>): CangJieType? {
        if (types.isEmpty()) return null
        if (types.size == 1) return types[0]
        return types.reduce { acc, type ->
            typeChecker.intersectTypes(acc, type) ?: return null
        }
    }

    /**
     * 构建最终结果
     */
    private fun buildResult(context: IterativeInferenceContext): InferenceResult {
        val unsolvedVariables = context.typeVariablesToSolve.filter { !context.isFixed(it) }

        return InferenceResult(
            success = unsolvedVariables.isEmpty(),
            solution = context.partialSolution.toMap(),
            unsolvedVariables = unsolvedVariables,
            iterations = context.iteration,
            hasContradiction = constraintSystemBuilder.hasContradiction
        )
    }
}

/**
 * 推导结果
 */
data class InferenceResult(
    /** 是否成功（所有变量都已求解） */
    val success: Boolean,
    /** 最终解：类型变量 -> 具体类型 */
    val solution: Map<TypeVariableMarker, CangJieType>,
    /** 未求解的变量列表 */
    val unsolvedVariables: List<TypeVariableMarker>,
    /** 迭代次数 */
    val iterations: Int,
    /** 是否存在矛盾 */
    val hasContradiction: Boolean
)
```

---

### 第四步：参数综合器

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ArgumentSynthesizer.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.model.ResolvedCallArgument
import org.cangnova.cangjie.types.CangJieType

/**
 * 参数综合器
 *
 * 负责分析参数表达式，推导其类型。
 */
class ArgumentSynthesizer(
    private val expressionTypingServices: ExpressionTypingServices
) {

    /**
     * 综合参数
     *
     * @param argument 参数
     * @param expectedType 期望类型（已用部分解替换）
     * @param partialSolution 当前部分解
     * @return 综合结果
     */
    fun synthesize(
        argument: ResolvedCallArgument,
        expectedType: CangJieType,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): SynthesisResult {
        return when {
            argument.isLambda() -> synthesizeLambda(argument, expectedType, partialSolution)
            argument.isCallableReference() -> synthesizeCallableReference(argument, expectedType)
            else -> synthesizeRegularArgument(argument, expectedType)
        }
    }

    /**
     * 综合 Lambda 表达式
     */
    private fun synthesizeLambda(
        argument: ResolvedCallArgument,
        expectedType: CangJieType,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): SynthesisResult {
        val functionType = expectedType as? FunctionType
            ?: return SynthesisResult.failure("Expected function type for lambda")

        val parameterTypes = functionType.parameterTypes

        // 检查所有参数类型是否已确定
        for (paramType in parameterTypes) {
            if (paramType.containsUnresolvedTypeVariables(partialSolution)) {
                return SynthesisResult.postponed("Lambda parameter type not yet determined")
            }
        }

        // 用确定的参数类型分析 Lambda 体
        val lambdaExpression = argument.getLambdaExpression()
        val analyzedType = expressionTypingServices.analyzeLambda(
            lambdaExpression,
            parameterTypes,
            functionType.returnType
        )

        return if (analyzedType != null) {
            SynthesisResult.success(analyzedType)
        } else {
            SynthesisResult.failure("Failed to analyze lambda body")
        }
    }

    /**
     * 综合 Lambda（参数类型未完全确定时）
     *
     * 用于 tryForceAnalyzeLambdas，尝试从 Lambda 体推导信息
     */
    fun synthesizeLambdaWithUnknownParams(
        argument: ResolvedCallArgument,
        functionType: FunctionType
    ): SynthesisResult {
        val lambdaExpression = argument.getLambdaExpression()

        // 尝试分析 Lambda 体，即使参数类型未完全确定
        // 这可以从 Lambda 体中获取返回类型信息
        val inferredReturnType = expressionTypingServices.inferLambdaReturnType(
            lambdaExpression,
            functionType.parameterTypes // 可能包含类型变量
        )

        return if (inferredReturnType != null) {
            // 构建推导出的函数类型
            val inferredFunctionType = FunctionType(
                parameterTypes = functionType.parameterTypes,
                returnType = inferredReturnType
            )
            SynthesisResult.success(inferredFunctionType)
        } else {
            SynthesisResult.failure("Cannot infer lambda return type")
        }
    }

    /**
     * 综合普通参数
     */
    private fun synthesizeRegularArgument(
        argument: ResolvedCallArgument,
        expectedType: CangJieType
    ): SynthesisResult {
        val expression = argument.getExpression()
        val analyzedType = expressionTypingServices.analyzeExpression(expression, expectedType)

        return if (analyzedType != null) {
            SynthesisResult.success(analyzedType)
        } else {
            SynthesisResult.failure("Failed to analyze argument")
        }
    }

    /**
     * 综合可调用引用
     */
    private fun synthesizeCallableReference(
        argument: ResolvedCallArgument,
        expectedType: CangJieType
    ): SynthesisResult {
        val reference = argument.getCallableReference()
        val resolvedType = expressionTypingServices.resolveCallableReference(reference, expectedType)

        return if (resolvedType != null) {
            SynthesisResult.success(resolvedType)
        } else {
            SynthesisResult.failure("Failed to resolve callable reference")
        }
    }
}

/**
 * 综合结果
 */
sealed class SynthesisResult {
    /** 成功 */
    data class Success(val type: CangJieType) : SynthesisResult()

    /** 失败 */
    data class Failure(val message: String) : SynthesisResult()

    /** 延迟（等待更多信息） */
    data class Postponed(val reason: String) : SynthesisResult()

    val success: Boolean get() = this is Success
    val type: CangJieType? get() = (this as? Success)?.type

    companion object {
        fun success(type: CangJieType) = Success(type)
        fun failure(message: String) = Failure(message)
        fun postponed(reason: String) = Postponed(reason)
    }
}
```

---

### 第五步：修改 ResolutionParts

**修改文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/components/ResolutionParts.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.inference.ArgumentSynthesisState
import org.cangnova.cangjie.resolve.calls.inference.IterativeInferenceContext
import org.cangnova.cangjie.resolve.calls.model.ClassValueReceiver
import org.cangnova.cangjie.resolve.calls.model.ResolutionCandidate
import org.cangnova.cangjie.types.CangJieType

/**
 * 创建 Fresh Variables 替换器
 *
 * 改进版本：
 * 1. 正确处理显式类型参数
 * 2. 创建迭代推导上下文
 */
object CreateFreshVariablesSubstitutor : ResolutionPart() {

    override fun ResolutionCandidate.process(workIndex: Int): List<CangJieCallDiagnostic> {
        val csBuilder = getSystem().getBuilder()

        // 1. 提取显式类型参数
        val explicitTypeArguments = extractExplicitTypeArguments()

        // 2. 获取需要推导的类型参数（排除已显式给出的）
        val typeParametersToInfer = getTypeParametersToInfer(explicitTypeArguments)

        // 3. 创建 Fresh Variables（仅对需要推导的）
        val (toFreshVariables, freshTypeVariables) = if (typeParametersToInfer.isEmpty()) {
            ComposableTypeSubstitutor.EMPTY to emptyList()
        } else {
            createToFreshVariableSubstitutorAndAddInitialConstraints(
                candidateDescriptor, cangjieCall, csBuilder, typeParametersToInfer
            )
        }

        // 4. 为显式类型参数创建替换器（不创建 Fresh Variable）
        val explicitSubstitutor = createExplicitTypeSubstitutor(explicitTypeArguments)

        // 5. 合并替换器：显式类型优先
        resolvedCall.typeParameterSubstitutor = ComposableTypeSubstitutor.chain(
            explicitSubstitutor,
            toFreshVariables
        )

        // 6. 创建迭代推导上下文
        val inferenceContext = createIterativeInferenceContext(
            freshTypeVariables,
            explicitTypeArguments
        )
        resolvedCall.iterativeInferenceContext = inferenceContext

        return emptyList()
    }

    /**
     * 提取显式类型参数
     *
     * 从两个来源提取：
     * 1. ClassValueReceiver 的类型参数（如 a<Int64>.method()）
     * 2. 调用表达式的类型参数（如 method<Int64>()）
     */
    private fun ResolutionCandidate.extractExplicitTypeArguments(): Map<TypeParameterDescriptor, CangJieType> {
        val result = mutableMapOf<TypeParameterDescriptor, CangJieType>()

        // 从 ClassValueReceiver 提取类的显式类型参数
        val receiver = resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue
        if (receiver is ClassValueReceiver) {
            val classDescriptor = receiver.classQualifier.descriptor
            val classType = receiver.type

            if (classType.arguments.isNotEmpty()) {
                classDescriptor.declaredTypeParameters.zip(classType.arguments).forEach { (param, arg) ->
                    val argType = arg.type as? CangJieType
                    if (argType != null) {
                        result[param] = argType
                    }
                }
            }
        }

        // 从调用表达式提取方法的显式类型参数
        cangjieCall.typeArguments.forEachIndexed { index, typeArg ->
            val typeParam = candidateDescriptor.typeParameters.getOrNull(index)
            if (typeParam != null && typeArg != null) {
                result[typeParam] = typeArg
            }
        }

        return result
    }

    /**
     * 获取需要推导的类型参数
     */
    private fun ResolutionCandidate.getTypeParametersToInfer(
        explicitTypeArguments: Map<TypeParameterDescriptor, CangJieType>
    ): List<TypeParameterDescriptor> {
        val allTypeParameters = mutableListOf<TypeParameterDescriptor>()

        // 添加类的类型参数（如果未显式给出）
        val receiver = resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue
        if (receiver is ClassValueReceiver) {
            val classDescriptor = receiver.classQualifier.descriptor
            classDescriptor.declaredTypeParameters.forEach { param ->
                if (param !in explicitTypeArguments) {
                    allTypeParameters.add(param)
                }
            }
        }

        // 添加方法的类型参数（如果未显式给出）
        candidateDescriptor.typeParameters.forEach { param ->
            if (param !in explicitTypeArguments) {
                allTypeParameters.add(param)
            }
        }

        return allTypeParameters
    }

    /**
     * 创建显式类型参数的替换器
     */
    private fun createExplicitTypeSubstitutor(
        explicitTypeArguments: Map<TypeParameterDescriptor, CangJieType>
    ): TypeSubstitutor {
        if (explicitTypeArguments.isEmpty()) {
            return TypeSubstitutor.EMPTY
        }
        return TypeSubstitutor.create(
            explicitTypeArguments.mapKeys { (param, _) -> param.typeConstructor }
        )
    }

    /**
     * 创建迭代推导上下文
     */
    private fun ResolutionCandidate.createIterativeInferenceContext(
        freshTypeVariables: List<TypeVariableFromCallableDescriptor>,
        explicitTypeArguments: Map<TypeParameterDescriptor, CangJieType>
    ): IterativeInferenceContext {
        // 构建参数状态列表
        val argumentStates = resolvedCall.argumentMappings.mapIndexed { index, mapping ->
            ArgumentSynthesisState(
                argument = mapping.argument,
                parameterType = mapping.parameterType,
                index = index
            )
        }

        // 创建上下文
        return IterativeInferenceContext(
            call = cangjieCall,
            typeVariablesToSolve = freshTypeVariables,
            arguments = argumentStates
        )
    }
}
```

---

### 第六步：整合到调用解析器

**修改文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/CangJieCallResolver.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.IterativeTypeInferenceEngine
import org.cangnova.cangjie.resolve.calls.inference.InferenceResult
import org.cangnova.cangjie.resolve.calls.model.CangJieCall
import org.cangnova.cangjie.resolve.calls.model.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.types.CangJieType

/**
 * 仓颉调用解析器
 *
 * 使用迭代式类型推导引擎解析函数调用。
 */
class CangJieCallResolver(
    private val iterativeInferenceEngine: IterativeTypeInferenceEngine,
    private val diagnosticsReporter: DiagnosticsReporter
) {

    /**
     * 解析调用
     */
    fun resolveCall(
        call: CangJieCall,
        candidates: Collection<ResolutionCandidate>,
        expectedType: CangJieType?
    ): OverloadResolutionResults {
        val successfulCandidates = mutableListOf<ResolutionCandidate>()
        val failedCandidates = mutableListOf<ResolutionCandidate>()

        for (candidate in candidates) {
            val result = resolveCandidate(candidate, expectedType)

            if (result.success) {
                applyFinalSolution(candidate, result)
                successfulCandidates.add(candidate)
            } else {
                recordFailure(candidate, result)
                failedCandidates.add(candidate)
            }
        }

        return selectBestCandidate(successfulCandidates, failedCandidates)
    }

    /**
     * 解析单个候选
     */
    private fun resolveCandidate(
        candidate: ResolutionCandidate,
        expectedType: CangJieType?
    ): InferenceResult {
        // 1. 创建 Fresh Variables 和推导上下文
        CreateFreshVariablesSubstitutor.run { candidate.process(0) }

        // 2. 获取推导上下文
        val inferenceContext = candidate.resolvedCall.iterativeInferenceContext
            ?: return InferenceResult(
                success = true,
                solution = emptyMap(),
                unsolvedVariables = emptyList(),
                iterations = 0,
                hasContradiction = false
            )

        // 3. 添加返回类型约束
        if (expectedType != null) {
            val csBuilder = candidate.getSystem().getBuilder()
            csBuilder.addSubtypeConstraint(
                candidate.resolvedCall.resultType,
                expectedType,
                ExpectedTypeConstraintPosition
            )
        }

        // 4. 运行迭代式推导
        return iterativeInferenceEngine.run(inferenceContext)
    }

    /**
     * 应用最终解
     */
    private fun applyFinalSolution(
        candidate: ResolutionCandidate,
        result: InferenceResult
    ) {
        val substitutor = TypeSubstitutor.create(
            result.solution.mapKeys { (v, _) -> v.freshTypeConstructor() }
        )

        candidate.resolvedCall.finalSubstitutor = ComposableTypeSubstitutor.chain(
            candidate.resolvedCall.typeParameterSubstitutor,
            substitutor
        )
    }

    /**
     * 记录失败信息
     */
    private fun recordFailure(
        candidate: ResolutionCandidate,
        result: InferenceResult
    ) {
        if (result.hasContradiction) {
            candidate.diagnosticsHolder.addDiagnostic(
                TypeInferenceContradiction()
            )
        }

        for (variable in result.unsolvedVariables) {
            candidate.diagnosticsHolder.addDiagnostic(
                CannotInferTypeParameter(variable)
            )
        }
    }

    /**
     * 选择最佳候选
     */
    private fun selectBestCandidate(
        successful: List<ResolutionCandidate>,
        failed: List<ResolutionCandidate>
    ): OverloadResolutionResults {
        return when {
            successful.size == 1 -> OverloadResolutionResults.success(successful[0])
            successful.size > 1 -> OverloadResolutionResults.ambiguity(successful)
            failed.isNotEmpty() -> OverloadResolutionResults.failure(failed)
            else -> OverloadResolutionResults.empty()
        }
    }
}
```

---

## 完整流程示例

### 示例 1: 显式类型参数 `a<Int64>.a1()`

```
输入: a<Int64>.a1()

Step 1: extractExplicitTypeArguments()
  ClassValueReceiver.type = a<Int64>
  → {T -> Int64}

Step 2: getTypeParametersToInfer({T -> Int64})
  T 已显式给出
  → [] (无需推导)

Step 3: createIterativeInferenceContext()
  typeVariablesToSolve = []
  unsolvedCount = 0

Step 4: iterativeInferenceEngine.run()
  shouldContinue() = false (unsolvedCount = 0)
  → 直接返回成功

输出: InferenceResult(success=true, solution={}, iterations=0)
结果: a1() 返回类型 = Int64
```

### 示例 2: Lambda 参数推导 `filter([1, 2, 3]) { x => x > 0 }`

```
输入: filter([1, 2, 3]) { x => x > 0 }
签名: func filter<T>(arr: Array<T>, pred: (T) -> Bool): Array<T>

Step 1: extractExplicitTypeArguments() → {}

Step 2: getTypeParametersToInfer() → [T]

Step 3: 创建 FreshVariable T'

Step 4: 迭代推导
  ┌─────────────────────────────────────────────────────┐
  │ Iteration 1:                                        │
  │   参数顺序: [0: Array, 1: Lambda]                   │
  │                                                     │
  │   Phase 1 (综合参数):                               │
  │     参数 0: [1,2,3] → Array<Int64>                  │
  │     参数 1: Lambda → 延迟 (T' 未确定)                │
  │                                                     │
  │   Phase 2 (收集约束):                               │
  │     Array<Int64> <: Array<T'>                       │
  │                                                     │
  │   Phase 3 (求解):                                   │
  │     T' 有下界 Int64                                 │
  │     T' = Int64 ✓                                    │
  │                                                     │
  │   状态: partialSolution = {T': Int64}               │
  │         hasNewInfo = true                           │
  └─────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────┐
  │ Iteration 2:                                        │
  │   Phase 1 (综合参数):                               │
  │     参数 0: 已完成                                  │
  │     参数 1: Lambda                                  │
  │       期望类型: (T') -> Bool = (Int64) -> Bool      │
  │       x: Int64 ✓                                    │
  │       x > 0: Bool ✓                                 │
  │                                                     │
  │   状态: unsolvedCount = 0                           │
  └─────────────────────────────────────────────────────┘
  shouldContinue() = false

输出: InferenceResult(success=true, solution={T': Int64}, iterations=2)
```

### 示例 3: Option.None 推导 `firstOrElse(None, { 42 })`

```
输入: firstOrElse(None, { 42 })
签名: func firstOrElse<T>(opt: Option<T>, f: () -> T): T

Step 1: 创建 FreshVariable T'

Step 2: 迭代推导
  ┌─────────────────────────────────────────────────────┐
  │ Iteration 1:                                        │
  │   参数顺序: [0: Option, 1: Lambda]                  │
  │                                                     │
  │   Phase 1:                                          │
  │     参数 0: None → Option<T'> (T' 未知，无约束)      │
  │     参数 1: Lambda → 延迟                           │
  │                                                     │
  │   Phase 2: 无新约束                                 │
  │   Phase 3: 无法求解                                 │
  │                                                     │
  │   hasNewInfo = false, 但有延迟 Lambda               │
  │   → tryForceAnalyzeLambdas()                        │
  └─────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────┐
  │ Force Lambda Analysis:                              │
  │   分析 Lambda { 42 }                                │
  │   推导返回类型: 42 → Int64                          │
  │   约束: Int64 <: T'                                 │
  │                                                     │
  │   hasNewInfo = true                                 │
  └─────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────┐
  │ Iteration 2:                                        │
  │   Phase 3:                                          │
  │     T' 有下界 Int64                                 │
  │     T' = Int64 ✓                                    │
  │                                                     │
  │   状态: unsolvedCount = 0                           │
  └─────────────────────────────────────────────────────┘

输出: InferenceResult(success=true, solution={T': Int64}, iterations=2)
结果: None: Option<Int64>, 返回类型 T = Int64
```

---

## 与编译器实现的对应关系

| 编译器 (C++) | 插件 (Kotlin) |
|-------------|--------------|
| `TyArgSynState` | `IterativeInferenceContext` |
| `LocalTypeArgumentSynthesis` | `IterativeTypeInferenceEngine` |
| `GetOrderedCheckingIndexes` | `ArgumentOrderOptimizer` |
| `SynthOrCheckArgument` | `ArgumentSynthesizer` |
| `FindSolution` | `tryFindResultType` |
| `JoinAndMeet` | `computeJoin` / `computeMeet` |
| `SubstPack.inst` | `explicitTypeArguments` |
| `allowPartial` | `isUsableResult` |

---

## 文件结构

```
analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/
├── CangJieCallResolver.kt                    # (修改) 整合迭代引擎
├── components/
│   └── ResolutionParts.kt                    # (修改) 处理显式类型参数
└── inference/
    ├── IterativeInferenceContext.kt          # (新增) 迭代上下文
    ├── IterativeTypeInferenceEngine.kt       # (新增) 迭代引擎
    ├── ArgumentOrderOptimizer.kt             # (新增) 参数顺序优化
    ├── ArgumentSynthesizer.kt                # (新增) 参数综合器
    ├── model/
    │   ├── ConstraintStorage.kt              # (现有)
    │   ├── InferenceResult.kt                # (新增) 推导结果
    │   └── SynthesisResult.kt                # (新增) 综合结果
    └── components/
        ├── ResultTypeResolver.kt             # (现有，可选修改)
        └── VariableFixationFinder.kt         # (现有)
```

---

## 实现检查清单

### 核心组件

- [ ] `IterativeInferenceContext` - 迭代状态管理
- [ ] `IterativeTypeInferenceEngine` - 迭代推导引擎
- [ ] `ArgumentOrderOptimizer` - 参数顺序优化
- [ ] `ArgumentSynthesizer` - 参数综合器

### 显式类型参数处理

- [ ] `extractExplicitTypeArguments()` - 提取显式类型
- [ ] `getTypeParametersToInfer()` - 排除已显式给出的参数
- [ ] `createExplicitTypeSubstitutor()` - 创建显式类型替换器

### 迭代推导

- [ ] `synthesizeArgumentsWithPartialSolution()` - 用部分解综合参数
- [ ] `collectConstraints()` - 收集约束
- [ ] `solveConstraintsWithPartialSolution()` - 部分解求解
- [ ] `tryForceAnalyzeLambdas()` - 强制分析 Lambda

### 整合

- [ ] 修改 `ResolutionParts.kt`
- [ ] 修改 `CangJieCallResolver.kt`
- [ ] 添加诊断信息类型

### 测试

- [ ] 显式类型参数基本场景
- [ ] Lambda 参数类型推导
- [ ] Option.None 上下文推导
- [ ] 链式泛型调用
- [ ] 嵌套 Option 推导
- [ ] 迭代限制测试
- [ ] 错误场景测试

---

**文档版本**: 1.0
**创建日期**: 2026-01-18
**状态**: 设计完成，待实现
