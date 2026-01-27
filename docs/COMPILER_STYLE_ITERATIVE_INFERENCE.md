# 编译器风格迭代式类型推导实现方案

## 文档概述

本文档描述如何在仓颉语言插件中实现与编译器一致的迭代式类型推导方案。

**选择理由**：编译器方案具有更强的表达能力，支持信息双向流动，能处理更多边界场景，且与仓颉语言设计一致。

---

## 编译器实现深度分析

### 双层迭代架构

编译器采用双层迭代架构：

```
┌─────────────────────────────────────────────────────────────────┐
│ 外层循环: PrepareTyArgsSynthesis (TypeArgumentInference.cpp)    │
│   while (stat.newInfo) {                                        │
│       1. 综合/检查参数（用 Quest 类型重新分析失败参数）           │
│       2. 收集有效参数类型                                        │
│       3. 调用内层推导                                            │
│       4. 检查是否有新信息                                        │
│       5. 准备 Quest 参数类型                                     │
│       6. Last Resort: Lambda 体分析                              │
│   }                                                             │
├─────────────────────────────────────────────────────────────────┤
│ 内层循环: FindSolution (LocalTypeArgumentSynthesis.cpp)         │
│   do {                                                          │
│       for (tyVar : tyVarsOfThisM) {                             │
│           1. 计算 Join (LUB)                                    │
│           2. 计算 Meet (GLB)                                    │
│           3. 检查贪婪固定条件                                    │
│           4. 固定类型变量                                        │
│       }                                                         │
│       thisM = ApplyTypeSubstForCS(thisSubst, thisM);            │
│   } while (newInfo);                                            │
└─────────────────────────────────────────────────────────────────┘
```

### 核心数据结构: TyArgSynState

```cpp
// TypeArgumentInference.cpp: 246-263
struct TyArgSynState {
    TyVars tyVarsToSolve;                    // 需要求解的类型变量
    std::vector<Ptr<Ty>> argTys;             // 参数类型列表
    std::vector<bool> failSet;               // 失败参数位图
    std::vector<Ptr<Ty>> questParamTys;      // Quest 类型（用于重新分析）
    size_t unsolvedCount;                    // 未求解变量数
    bool newInfo = true;                     // 本轮是否有新信息
    bool lastResortUnused = true;            // Last Resort 是否未使用
    std::optional<TypeSubst> solution;       // 当前解
};
```

**关键字段说明**：

| 字段 | 作用 |
|------|------|
| `failSet` | 记录哪些参数分析失败，需要用 Quest 类型重新分析 |
| `questParamTys` | 将未确定的类型变量替换为 Quest 类型后的参数类型 |
| `lastResortUnused` | 控制 Last Resort 机制（Lambda 体推导）是否已触发 |
| `newInfo` | 控制外层循环是否继续 |

---

## 关键机制详解

### 1. Quest 类型机制 (UnsolvedAsQuest)

**作用**：将包含未解决类型变量的类型转换为包含 Quest 占位符的类型，使参数可以被重新分析。

```cpp
// TypeArgumentInference.cpp: 217-244
std::optional<Ptr<Ty>> UnsolvedAsQuest(TypeManager& tyMgr, const TyVars& tyVarsToSolve, Ty& ty) {
    if (auto funcTy = DynamicCast<FuncTy>(&ty)) {
        // Lambda 类型：参数必须具体，返回类型可以是 Quest
        for (auto& it : funcTy->paramTys) {
            if (!IsConcrete(tyVarsToSolve, *it)) {
                return std::nullopt;  // Lambda 参数类型必须已确定
            }
        }
        if (auto retType = UnsolvedAsQuest(tyMgr, tyVarsToSolve, *funcTy->retTy)) {
            return tyMgr.GetFunctionTy(paramTys, *retType, ...);
        }
    } else {
        // 普通类型：具体类型返回自身，否则返回 Quest
        if (IsConcrete(tyVarsToSolve, ty)) {
            return &ty;
        } else {
            return TypeManager::GetQuestTy();  // 转换为 Quest 占位符
        }
    }
    return std::nullopt;
}
```

**Quest 类型的作用**：
- `?` 类型可以匹配任何类型
- 允许在类型变量未完全确定时重新分析参数
- 从参数表达式获取更多类型信息

### 2. 参数顺序优化 (GetOrderedCheckingIndexes)

**作用**：优化参数处理顺序，使类型信息流动更高效。

```cpp
// LocalTypeArgumentSynthesis.cpp: 32-60
std::vector<size_t> GetOrderedCheckingIndexes(const std::vector<Ptr<Ty>>& tys) {
    std::vector<size_t> ideals, options, others;

    for (size_t i = 0; i < tys.size(); ++i) {
        if (tys[i]->IsIdeal()) {
            ideals.emplace_back(i);      // 理想类型最后
        } else if (tys[i]->IsCoreOptionType()) {
            options.emplace_back(i);     // Option 类型优先
        } else {
            others.emplace_back(i);      // 其他类型
        }
    }

    // Option 按嵌套深度排序（深的优先）
    std::stable_sort(options.begin(), options.end(), [&tys](auto l, auto r) {
        return CountOptionNestedLevel(*tys[l]) > CountOptionNestedLevel(*tys[r]);
    });

    // 合并顺序：Option → 其他 → 理想类型
    options.insert(options.end(), others.begin(), others.end());
    options.insert(options.end(), ideals.begin(), ideals.end());
    return options;
}
```

**排序原因**：
1. **Option 类型优先**：仓颉支持自动装箱，先处理 Option 可正确推导 `Equatable<Option<A>>`
2. **嵌套深度深的优先**：`Option<Option<T>>` 比 `Option<T>` 先处理
3. **理想类型最后**：理想类型可转换为多种具体类型，最后处理避免过度泛化

### 3. 贪婪固定条件 (IsGreedySolution)

**作用**：判断类型变量是否可以立即固定，无需等待更多信息。

```cpp
// LocalTypeArgumentSynthesis.cpp: 1222-1236
bool IsGreedySolution(const TyVar& tv, const Ty& bound, bool isUpperbound) {
    // 条件 1: 泛型类型参数（非占位符）
    bool tyParam = bound.IsGeneric() && !bound.IsPlaceholder();

    // 条件 2: 外层作用域的占位符
    bool outerTyVar = bound.IsPlaceholder() && (ScopeDepth(bound) <= ScopeDepth(tv));

    // 条件 3: Final 类型（不可继承）
    bool finalType = (isUpperbound && !IsInheritableClass(bound)) ||
        (!bound.IsGeneric() && !bound.IsClassLike() && !bound.IsAny());

    // 条件 4: Any（作为下界）或 Nothing（作为上界）
    bool anyOrNothing = (bound.IsAny() && !isUpperbound) ||
                        (bound.IsNothing() && isUpperbound);

    return tyParam || outerTyVar || finalType || anyOrNothing;
}
```

**贪婪固定的好处**：
- 尽早固定类型变量，为其他变量提供约束
- 减少迭代次数
- 对于 Final 类型（如 Int64），可以立即确定

### 4. Last Resort 机制

**作用**：当常规迭代无法继续时，尝试分析 Lambda 体以获取更多类型信息。

```cpp
// TypeArgumentInference.cpp: PrepareTyArgsSynthesis
while (stat.newInfo) {
    // ... 常规迭代 ...

    // Last Resort: 当迭代无法继续但仍有未解决的变量
    if (!stat.newInfo && stat.unsolvedCount > 0 && stat.lastResortUnused) {
        stat.lastResortUnused = false;  // 标记已使用

        // 尝试通过 Lambda 体推导
        sol = PropagatePlaceholderAndSolve(tyMgr, tyVarsToSolve, ...);

        if (sol) {
            stat.solution = sol;
            stat.newInfo = true;  // 重新进入循环
        }
    }
}
```

### 5. 内层循环详解 (FindSolution)

```cpp
// LocalTypeArgumentSynthesis.cpp: 765-829
std::optional<TypeSubst> LocalTypeArgumentSynthesis::FindSolution(
    const TyVars& tyVarsOfThisM,
    ConstraintMap& thisM,
    bool allowPartial
) {
    TypeSubst thisSubst;
    bool newInfo = false;

    do {
        newInfo = false;

        for (auto tyVar : tyVarsOfThisM) {
            // 1. 计算 Join (LUB - 最小上界)
            Ptr<Ty> tyJ = JoinAndMeet(thisM[tyVar].lower, tyMgr)
                .JoinAsVisibleTy();

            // 2. 计算 Meet (GLB - 最大下界)
            Ptr<Ty> tyM = MeetUpperBounds(thisM[tyVar].upper);

            // 3. 检查并固定
            if (IsValidSolution(*tyJ, thisM[tyVar], allowPartial)) {
                thisSubst[tyVar] = tyJ;
                newInfo = true;
            } else if (IsValidSolution(*tyM, thisM[tyVar], allowPartial)) {
                thisSubst[tyVar] = tyM;
                newInfo = true;
            }
        }

        // 4. 应用当前解，传播约束
        thisM = ApplyTypeSubstForCS(thisSubst, thisM);

    } while (newInfo);

    return {thisSubst};
}
```

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
│     1. 按优化顺序综合参数（用部分解/Quest类型）          │
│     2. 收集约束                                         │
│     3. 求解（允许部分解 + 贪婪固定）                    │
│     4. 更新部分解                                       │
│     5. 准备 Quest 参数类型（用于重新分析）               │
│     6. Last Resort: Lambda 体分析                       │
│ }                                                       │
│     ↓                                                   │
│ 信息可双向流动，支持参数重新分析                         │
└─────────────────────────────────────────────────────────┘
```

### 核心差异

| 方面 | Kotlin 风格 | 编译器风格 |
|------|------------|-----------|
| 信息流向 | 单向（前向） | 双向（可回流） |
| 参数分析 | 一次性 | 可重新分析（Quest 类型） |
| 部分解 | 有限支持 | 完整支持 |
| 迭代层级 | 完成阶段内 | 双层迭代（外层+内层） |
| 失败处理 | 立即报错 | 标记失败，尝试重分析 |
| 贪婪固定 | 无 | 支持（Final类型等） |
| Last Resort | 无 | Lambda 体推导 |

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

    /** 失败参数位图（对应编译器的 failSet） */
    val failedArguments: MutableSet<Int> = mutableSetOf()

    /** Quest 参数类型（对应编译器的 questParamTys） */
    val questParameterTypes: MutableMap<Int, CangJieType> = mutableMapOf()

    /** 未求解变量数 */
    var unsolvedCount: Int = typeVariablesToSolve.size

    /** 本轮是否有新信息 */
    var hasNewInfo: Boolean = true

    /** Last Resort 是否未使用（对应编译器的 lastResortUnused） */
    var lastResortUnused: Boolean = true

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
     * 标记参数分析失败
     */
    fun markArgumentFailed(index: Int) {
        failedArguments.add(index)
    }

    /**
     * 设置参数的 Quest 类型
     */
    fun setQuestParameterType(index: Int, questType: CangJieType) {
        questParameterTypes[index] = questType
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
                "failed=${failedArguments.size}, " +
                "lastResortUnused=$lastResortUnused, " +
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
        // 注意：failed 状态不重置，失败的参数通过 Quest 类型重新分析
    }
}
```

---

### 第二步：Quest 类型生成器

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/QuestTypeGenerator.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.FunctionType
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * Quest 类型生成器
 *
 * 对应编译器的 UnsolvedAsQuest 函数。
 * 将包含未解决类型变量的类型转换为包含 Quest 占位符的类型。
 */
object QuestTypeGenerator {

    /**
     * 将类型中的未解决类型变量转换为 Quest 占位符
     *
     * @param type 原始类型
     * @param unsolvedVariables 未解决的类型变量集合
     * @param partialSolution 当前部分解
     * @return Quest 类型，如果无法转换则返回 null
     */
    fun generateQuestType(
        type: CangJieType,
        unsolvedVariables: Set<TypeVariableMarker>,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): CangJieType? {
        return when {
            type is FunctionType -> generateQuestFunctionType(type, unsolvedVariables, partialSolution)
            isConcrete(type, unsolvedVariables, partialSolution) -> type
            else -> QuestType  // 返回 Quest 占位符
        }
    }

    /**
     * 为函数类型生成 Quest 类型
     *
     * 关键：Lambda 的参数类型必须是具体的，只有返回类型可以是 Quest
     */
    private fun generateQuestFunctionType(
        funcType: FunctionType,
        unsolvedVariables: Set<TypeVariableMarker>,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): CangJieType? {
        // 检查所有参数类型是否具体
        for (paramType in funcType.parameterTypes) {
            if (!isConcrete(paramType, unsolvedVariables, partialSolution)) {
                return null  // Lambda 参数类型必须已确定
            }
        }

        // 返回类型可以转换为 Quest
        val questReturnType = generateQuestType(
            funcType.returnType,
            unsolvedVariables,
            partialSolution
        ) ?: return null

        return FunctionType(
            parameterTypes = funcType.parameterTypes,
            returnType = questReturnType,
            isC = funcType.isC,
            isClosure = funcType.isClosure
        )
    }

    /**
     * 检查类型是否具体（不包含未解决的类型变量）
     */
    private fun isConcrete(
        type: CangJieType,
        unsolvedVariables: Set<TypeVariableMarker>,
        partialSolution: Map<TypeVariableMarker, CangJieType>
    ): Boolean {
        var concrete = true
        type.forEachTypeVariable { tv ->
            if (tv in unsolvedVariables && tv !in partialSolution) {
                concrete = false
            }
        }
        return concrete
    }
}

/**
 * Quest 类型单例
 *
 * 表示"任意类型"占位符，可以匹配任何类型
 */
object QuestType : CangJieType() {
    override fun toString(): String = "?"
}
```

---

### 第三步：参数顺序优化器

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ArgumentOrderOptimizer.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

/**
 * 参数顺序优化器
 *
 * 对应编译器的 GetOrderedCheckingIndexes 函数。
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
}
```

---

### 第四步：贪婪固定判断器

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/GreedyFixationChecker.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.model.TypeVariableMarker

/**
 * 贪婪固定判断器
 *
 * 对应编译器的 IsGreedySolution 函数。
 * 判断类型变量是否可以立即固定，无需等待更多信息。
 */
object GreedyFixationChecker {

    /**
     * 检查是否可以贪婪固定
     *
     * @param typeVariable 类型变量
     * @param bound 约束类型
     * @param isUpperBound 是否为上界约束
     * @return 是否可以贪婪固定
     */
    fun isGreedySolution(
        typeVariable: TypeVariableMarker,
        bound: CangJieType,
        isUpperBound: Boolean
    ): Boolean {
        // 条件 1: 泛型类型参数（非占位符）
        val isTypeParam = bound.isGeneric() && !bound.isPlaceholder()

        // 条件 2: 外层作用域的占位符
        val isOuterPlaceholder = bound.isPlaceholder() &&
            (bound.scopeDepth() <= typeVariable.scopeDepth())

        // 条件 3: Final 类型（不可继承）
        val isFinalType = when {
            isUpperBound -> !isInheritableClass(bound)
            else -> !bound.isGeneric() && !bound.isClassLike() && !bound.isAny()
        }

        // 条件 4: Any（作为下界）或 Nothing（作为上界）
        val isAnyOrNothing = (bound.isAny() && !isUpperBound) ||
            (bound.isNothing() && isUpperBound)

        return isTypeParam || isOuterPlaceholder || isFinalType || isAnyOrNothing
    }

    /**
     * 检查类型是否可继承
     */
    private fun isInheritableClass(type: CangJieType): Boolean {
        val classifier = type.typeConstructor().getClassifier()
        return when {
            classifier is ClassDescriptor -> !classifier.isFinalClass && !classifier.isSealed
            else -> false
        }
    }
}
```

---

### 第五步：迭代式推导引擎

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/IterativeTypeInferenceEngine.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

/**
 * 迭代式类型推导引擎
 *
 * 实现编译器风格的双层迭代推导算法。
 *
 * 外层循环（对应 PrepareTyArgsSynthesis）：
 * ```
 * while (hasProgress) {
 *     1. 综合/检查参数（用 Quest 类型重新分析失败参数）
 *     2. 收集有效参数类型
 *     3. 调用内层推导
 *     4. 检查是否有新信息
 *     5. 准备 Quest 参数类型
 *     6. Last Resort: Lambda 体分析
 * }
 * ```
 *
 * 内层循环（对应 FindSolution）：
 * ```
 * do {
 *     for (tyVar : variables) {
 *         1. 计算 Join (LUB)
 *         2. 计算 Meet (GLB)
 *         3. 检查贪婪固定条件
 *         4. 固定类型变量
 *     }
 *     传播约束
 * } while (newInfo)
 * ```
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

        // 外层主迭代循环
        while (context.shouldContinue()) {
            context.startIteration()

            val madeProgress = runOuterIteration(context, argumentOrder)

            if (!madeProgress) {
                // 尝试 Last Resort: Lambda 体分析
                if (context.lastResortUnused && context.unsolvedCount > 0) {
                    context.lastResortUnused = false
                    if (tryLastResortLambdaAnalysis(context)) {
                        context.hasNewInfo = true
                        continue
                    }
                }
                break
            }
        }

        return buildResult(context)
    }

    /**
     * 外层单轮迭代
     *
     * @return 本轮是否有进展
     */
    private fun runOuterIteration(
        context: IterativeInferenceContext,
        argumentOrder: List<Int>
    ): Boolean {
        var madeProgress = false

        // Phase 1: 综合参数（用 Quest 类型重新分析失败参数）
        if (synthesizeArgumentsWithQuestTypes(context, argumentOrder)) {
            madeProgress = true
        }

        // Phase 2: 收集约束
        collectConstraints(context)

        // Phase 3: 内层求解循环
        if (runInnerSolvingLoop(context)) {
            madeProgress = true
        }

        // Phase 4: 准备 Quest 参数类型
        if (madeProgress) {
            prepareQuestParameterTypes(context)
        }

        return madeProgress
    }

    /**
     * Phase 1: 用 Quest 类型综合参数
     *
     * 对应编译器的参数综合逻辑：
     * - 首次分析：直接综合
     * - 失败后：用 Quest 类型重新检查
     * - Last Resort 后：强制综合
     */
    private fun synthesizeArgumentsWithQuestTypes(
        context: IterativeInferenceContext,
        argumentOrder: List<Int>
    ): Boolean {
        val substitutor = context.createPartialSubstitutor()
        var madeProgress = false

        for (argIndex in argumentOrder) {
            val argState = context.arguments[argIndex]

            // 情况 1: 失败参数 + 有 Quest 类型 → 用 Quest 类型重新检查
            if (argState.failed && context.questParameterTypes.containsKey(argIndex)) {
                val questType = context.questParameterTypes[argIndex]!!
                val result = argumentSynthesizer.checkWithCache(
                    argState.argument,
                    questType
                )
                if (result is SynthesisResult.Success) {
                    argState.synthesizedType = result.type
                    argState.analyzed = true
                    context.hasNewInfo = true
                    madeProgress = true
                }
            }
            // 情况 2: 未分析 → 首次综合
            else if (argState.synthesizedType == null && !argState.failed) {
                val expectedType = substitutor.substitute(argState.parameterType)
                val result = synthesizeArgument(argState, expectedType, context)

                when (result) {
                    is SynthesisResult.Success -> {
                        argState.synthesizedType = result.type
                        argState.analyzed = true
                        context.hasNewInfo = true
                        madeProgress = true
                    }
                    is SynthesisResult.Failure -> {
                        argState.failed = true
                        context.markArgumentFailed(argIndex)
                    }
                    is SynthesisResult.Postponed -> {
                        // 保持延迟状态
                    }
                }
            }
            // 情况 3: 失败 + Last Resort 已触发 → 强制综合
            else if (argState.failed && !context.lastResortUnused) {
                val result = argumentSynthesizer.synthesize(
                    argState.argument,
                    argState.parameterType,
                    context.partialSolution
                )
                if (result is SynthesisResult.Success) {
                    argState.synthesizedType = result.type
                    argState.analyzed = true
                    argState.failed = false
                    context.hasNewInfo = true
                    madeProgress = true
                }
            }
        }

        return madeProgress
    }

    /**
     * 综合单个参数
     */
    private fun synthesizeArgument(
        argState: ArgumentSynthesisState,
        expectedType: CangJieType,
        context: IterativeInferenceContext
    ): SynthesisResult {
        // Lambda 需要参数类型确定
        if (argState.isLambda) {
            val functionType = expectedType as? FunctionType
                ?: return SynthesisResult.failure("Expected function type for lambda")

            // 检查 Lambda 参数类型是否都已确定
            val allParamsDetermined = functionType.parameterTypes.all { paramType ->
                isTypeFullyDetermined(paramType, context)
            }

            if (!allParamsDetermined) {
                return SynthesisResult.postponed("Lambda parameter type not yet determined")
            }
        }

        return argumentSynthesizer.synthesizeWithCache(
            argState.argument,
            expectedType
        )
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
     * Phase 3: 内层求解循环
     *
     * 对应编译器的 FindSolution
     */
    private fun runInnerSolvingLoop(context: IterativeInferenceContext): Boolean {
        var madeProgress = false
        var innerNewInfo: Boolean

        do {
            innerNewInfo = false
            val storage = constraintSystemBuilder.currentStorage()

            for ((_, variableWithConstraints) in storage.notFixedTypeVariables) {
                val variable = variableWithConstraints.typeVariable

                // 跳过已在部分解中的变量
                if (context.isFixed(variable)) continue

                // 1. 计算 Join (LUB)
                val joinResult = computeJoin(variableWithConstraints, context)

                // 2. 计算 Meet (GLB)
                val meetResult = computeMeet(variableWithConstraints, context)

                // 3. 检查贪婪固定条件并固定
                val resultType = when {
                    joinResult != null && isValidSolution(joinResult, variableWithConstraints, context) -> {
                        // 检查是否可以贪婪固定
                        if (shouldGreedyFix(variable, variableWithConstraints)) {
                            joinResult
                        } else if (isUsableResult(joinResult, context)) {
                            joinResult
                        } else null
                    }
                    meetResult != null && isValidSolution(meetResult, variableWithConstraints, context) -> {
                        meetResult
                    }
                    else -> null
                }

                if (resultType != null) {
                    constraintSystemBuilder.fixVariable(variable, resultType)
                    context.recordFixation(variable, resultType)
                    innerNewInfo = true
                    madeProgress = true
                }
            }

            // 传播约束
            if (innerNewInfo) {
                constraintSystemBuilder.propagateConstraints()
            }

        } while (innerNewInfo)

        return madeProgress
    }

    /**
     * 检查是否应该贪婪固定
     */
    private fun shouldGreedyFix(
        variable: TypeVariableMarker,
        variableWithConstraints: VariableWithConstraints
    ): Boolean {
        // 检查所有约束
        for (constraint in variableWithConstraints.constraints) {
            val isUpperBound = constraint.kind == ConstraintKind.UPPER
            if (GreedyFixationChecker.isGreedySolution(variable, constraint.type as CangJieType, isUpperBound)) {
                return true
            }
        }
        return false
    }

    /**
     * Phase 4: 准备 Quest 参数类型
     */
    private fun prepareQuestParameterTypes(context: IterativeInferenceContext) {
        val unsolvedVariables = context.typeVariablesToSolve
            .filter { !context.isFixed(it) }
            .toSet()

        for (argState in context.arguments) {
            if (!argState.failed) continue

            val questType = QuestTypeGenerator.generateQuestType(
                argState.parameterType,
                unsolvedVariables,
                context.partialSolution
            )

            if (questType != null) {
                context.setQuestParameterType(argState.index, questType)
                context.hasNewInfo = true
            }
        }
    }

    /**
     * Last Resort: Lambda 体分析
     *
     * 当常规迭代无法继续时，尝试分析 Lambda 体以获取更多信息
     */
    private fun tryLastResortLambdaAnalysis(context: IterativeInferenceContext): Boolean {
        val substitutor = context.createPartialSubstitutor()
        var madeProgress = false

        for (argState in context.arguments) {
            if (!argState.isLambda || !argState.failed) continue

            val expectedType = substitutor.substitute(argState.parameterType)
            val functionType = expectedType as? FunctionType ?: continue

            // 尝试通过 Lambda 体推导返回类型
            val result = argumentSynthesizer.synthesizeLambdaWithUnknownParams(
                argState.argument,
                functionType
            )

            if (result is SynthesisResult.Success) {
                argState.synthesizedType = result.type
                argState.analyzed = true
                argState.failed = false
                madeProgress = true

                // 添加返回类型约束
                val inferredReturnType = (result.type as FunctionType).returnType
                val expectedReturnType = functionType.returnType
                constraintSystemBuilder.addSubtypeConstraint(
                    inferredReturnType,
                    expectedReturnType,
                    LambdaReturnTypePosition(argState.index)
                )
            }
        }

        return madeProgress
    }

    /**
     * 计算类型的 Join（LUB - 最小上界）
     */
    private fun computeJoin(
        variableWithConstraints: VariableWithConstraints,
        context: IterativeInferenceContext
    ): CangJieType? {
        val substitutor = context.createPartialSubstitutor()
        val lowerTypes = variableWithConstraints.constraints
            .filter { it.kind == ConstraintKind.LOWER }
            .map { substitutor.substitute(it.type as CangJieType) }
            .filter { isUsableResult(it, context) }

        if (lowerTypes.isEmpty()) return null
        if (lowerTypes.size == 1) return lowerTypes[0]

        return lowerTypes.reduce { acc, type ->
            typeChecker.commonSuperType(acc, type) ?: return null
        }
    }

    /**
     * 计算类型的 Meet（GLB - 最大下界）
     */
    private fun computeMeet(
        variableWithConstraints: VariableWithConstraints,
        context: IterativeInferenceContext
    ): CangJieType? {
        val substitutor = context.createPartialSubstitutor()
        val upperTypes = variableWithConstraints.constraints
            .filter { it.kind == ConstraintKind.UPPER }
            .map { substitutor.substitute(it.type as CangJieType) }
            .filter { isUsableResult(it, context) }

        if (upperTypes.isEmpty()) return null
        if (upperTypes.size == 1) return upperTypes[0]

        return upperTypes.reduce { acc, type ->
            typeChecker.intersectTypes(acc, type) ?: return null
        }
    }

    /**
     * 检查解是否有效
     */
    private fun isValidSolution(
        type: CangJieType,
        variableWithConstraints: VariableWithConstraints,
        context: IterativeInferenceContext
    ): Boolean {
        // 检查是否满足所有约束
        val substitutor = context.createPartialSubstitutor()

        for (constraint in variableWithConstraints.constraints) {
            val constraintType = substitutor.substitute(constraint.type as CangJieType)
            val satisfied = when (constraint.kind) {
                ConstraintKind.LOWER -> typeChecker.isSubtype(constraintType, type)
                ConstraintKind.UPPER -> typeChecker.isSubtype(type, constraintType)
                ConstraintKind.EQUALITY -> typeChecker.isEqual(type, constraintType)
            }
            if (!satisfied) return false
        }

        return true
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

### 第六步：参数综合器

**新文件**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ArgumentSynthesizer.kt`

```kotlin
package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.model.ResolvedCallArgument
import org.cangnova.cangjie.types.CangJieType

/**
 * 参数综合器
 *
 * 负责分析参数表达式，推导其类型。
 * 支持缓存机制以支持重新分析。
 */
class ArgumentSynthesizer(
    private val expressionTypingServices: ExpressionTypingServices,
    private val cache: TypeCheckCache
) {

    /**
     * 综合参数（带缓存）
     *
     * 对应编译器的 SynthesizeWithCache
     */
    fun synthesizeWithCache(
        argument: ResolvedCallArgument,
        expectedType: CangJieType
    ): SynthesisResult {
        val cacheKey = CacheKey(argument, expectedType)
        cache.get(cacheKey)?.let { return it }

        val result = synthesize(argument, expectedType, emptyMap())
        cache.put(cacheKey, result)
        return result
    }

    /**
     * 检查参数（带缓存）
     *
     * 对应编译器的 CheckWithCache
     */
    fun checkWithCache(
        argument: ResolvedCallArgument,
        expectedType: CangJieType
    ): SynthesisResult {
        val cacheKey = CacheKey(argument, expectedType)
        cache.get(cacheKey)?.let { return it }

        val result = check(argument, expectedType)
        cache.put(cacheKey, result)
        return result
    }

    /**
     * 综合参数
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
     * 检查参数
     */
    private fun check(
        argument: ResolvedCallArgument,
        expectedType: CangJieType
    ): SynthesisResult {
        val expression = argument.getExpression()
        val success = expressionTypingServices.checkExpression(expression, expectedType)

        return if (success) {
            SynthesisResult.success(expression.type ?: expectedType)
        } else {
            SynthesisResult.failure("Type check failed")
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
     * 用于 Last Resort，尝试从 Lambda 体推导信息
     */
    fun synthesizeLambdaWithUnknownParams(
        argument: ResolvedCallArgument,
        functionType: FunctionType
    ): SynthesisResult {
        val lambdaExpression = argument.getLambdaExpression()

        // 尝试分析 Lambda 体，即使参数类型未完全确定
        val inferredReturnType = expressionTypingServices.inferLambdaReturnType(
            lambdaExpression,
            functionType.parameterTypes
        )

        return if (inferredReturnType != null) {
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

### 第七步：修改 ResolutionParts

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

Step 4: IterativeTypeInferenceEngine.run()
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
  ┌─────────────────────────────────────────────────────────────┐
  │ Outer Iteration 1:                                          │
  │   参数顺序: [0: Array, 1: Lambda]                           │
  │                                                             │
  │   Phase 1 (综合参数):                                       │
  │     参数 0: [1,2,3] → Array<Int64> ✓                        │
  │     参数 1: Lambda → 延迟 (T' 未确定)                        │
  │                                                             │
  │   Phase 2 (收集约束):                                       │
  │     Array<Int64> <: Array<T'>                               │
  │                                                             │
  │   Phase 3 (内层求解循环):                                   │
  │     ┌───────────────────────────────────────────────────┐   │
  │     │ Inner do-while:                                   │   │
  │     │   T' 有下界 Int64                                 │   │
  │     │   IsGreedySolution(T', Int64, false) = true       │   │
  │     │   (Int64 是 final 类型)                           │   │
  │     │   T' = Int64 ✓                                    │   │
  │     └───────────────────────────────────────────────────┘   │
  │                                                             │
  │   状态: partialSolution = {T': Int64}                       │
  │         hasNewInfo = true                                   │
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ Outer Iteration 2:                                          │
  │   Phase 1 (综合参数):                                       │
  │     参数 0: 已完成                                          │
  │     参数 1: Lambda                                          │
  │       期望类型: (T') -> Bool = (Int64) -> Bool              │
  │       x: Int64 ✓                                            │
  │       x > 0: Bool ✓                                         │
  │                                                             │
  │   状态: unsolvedCount = 0                                   │
  └─────────────────────────────────────────────────────────────┘
  shouldContinue() = false

输出: InferenceResult(success=true, solution={T': Int64}, iterations=2)
```

### 示例 3: Quest 类型重分析 `processOrDefault(None, { 42 })`

```
输入: processOrDefault(None, { 42 })
签名: func processOrDefault<T>(opt: Option<T>, default: () -> T): T

Step 1: 创建 FreshVariable T'

Step 2: 迭代推导
  ┌─────────────────────────────────────────────────────────────┐
  │ Outer Iteration 1:                                          │
  │   参数顺序: [0: Option, 1: Lambda]                          │
  │                                                             │
  │   Phase 1:                                                  │
  │     参数 0: None → Option<T'> (T' 未知)                     │
  │       综合失败，标记 failSet[0] = true                      │
  │     参数 1: Lambda → 延迟                                   │
  │                                                             │
  │   Phase 2: 无新约束                                         │
  │   Phase 3: 无法求解                                         │
  │                                                             │
  │   Phase 4 (准备 Quest 类型):                                │
  │     参数 0: Option<T'> → Option<?> (Quest 类型)             │
  │     questParamTys[0] = Option<?>                            │
  │                                                             │
  │   hasNewInfo = true (有新 Quest 类型)                       │
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ Outer Iteration 2:                                          │
  │   Phase 1:                                                  │
  │     参数 0: 用 Quest 类型重新检查                           │
  │       CheckWithCache(None, Option<?>) → Option<?>           │
  │     参数 1: Lambda 仍延迟                                   │
  │                                                             │
  │   无新信息，尝试 Last Resort                                │
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ Last Resort Lambda Analysis:                                │
  │   分析 Lambda { 42 }                                        │
  │   推导返回类型: 42 → Int64                                  │
  │   约束: Int64 <: T'                                         │
  │                                                             │
  │   hasNewInfo = true                                         │
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ Outer Iteration 3:                                          │
  │   Phase 3 (内层求解):                                       │
  │     T' 有下界 Int64                                         │
  │     T' = Int64 ✓                                            │
  │                                                             │
  │   状态: unsolvedCount = 0                                   │
  └─────────────────────────────────────────────────────────────┘

输出: InferenceResult(success=true, solution={T': Int64}, iterations=3)
结果: None: Option<Int64>, 返回类型 T = Int64
```

---

## 与编译器实现的对应关系

| 编译器 (C++) | 插件 (Kotlin) |
|-------------|--------------|
| `TyArgSynState` | `IterativeInferenceContext` |
| `TyArgSynState.failSet` | `IterativeInferenceContext.failedArguments` |
| `TyArgSynState.questParamTys` | `IterativeInferenceContext.questParameterTypes` |
| `TyArgSynState.lastResortUnused` | `IterativeInferenceContext.lastResortUnused` |
| `PrepareTyArgsSynthesis` (外层循环) | `IterativeTypeInferenceEngine.run()` |
| `LocalTypeArgumentSynthesis.FindSolution` (内层循环) | `IterativeTypeInferenceEngine.runInnerSolvingLoop()` |
| `GetOrderedCheckingIndexes` | `ArgumentOrderOptimizer.getOptimizedOrder()` |
| `UnsolvedAsQuest` | `QuestTypeGenerator.generateQuestType()` |
| `IsGreedySolution` | `GreedyFixationChecker.isGreedySolution()` |
| `JoinAndMeet.JoinAsVisibleTy` | `computeJoin()` |
| `MeetUpperBounds` | `computeMeet()` |
| `SynthesizeWithCache` | `ArgumentSynthesizer.synthesizeWithCache()` |
| `CheckWithCache` | `ArgumentSynthesizer.checkWithCache()` |
| `PropagatePlaceholderAndSolve` | `tryLastResortLambdaAnalysis()` |

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
    ├── QuestTypeGenerator.kt                 # (新增) Quest 类型生成
    ├── GreedyFixationChecker.kt              # (新增) 贪婪固定判断
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

- [x] `IterativeInferenceContext` - 迭代状态管理（含 failSet、questParamTys、lastResortUnused）
- [x] `IterativeTypeInferenceEngine` - 双层迭代推导引擎
- [x] `ArgumentOrderOptimizer` - 参数顺序优化（Option优先）
- [x] `ArgumentSynthesizer` - 参数综合器（含缓存）
- [x] `QuestTypeGenerator` - Quest 类型生成（使用 TypeUtils.DONT_CARE 替代自定义 QuestType）
- [x] `GreedyFixationChecker` - 贪婪固定条件判断

### 显式类型参数处理

- [x] `extractExplicitTypeArguments()` - 提取显式类型（在 ResolutionParts.kt 中实现）
- [x] `getTypeParametersToInfer()` - 排除已显式给出的参数
- [x] `createExplicitTypeSubstitutor()` - 创建显式类型替换器

### 迭代推导

- [x] 外层循环: `synthesizeArgumentsWithQuestTypes()` - Quest 类型重分析
- [x] 外层循环: `prepareQuestParameterTypes()` - 准备 Quest 类型
- [x] 外层循环: `tryLastResortLambdaAnalysis()` - Last Resort Lambda 分析
- [x] 内层循环: `runInnerSolvingLoop()` - 求解循环
- [x] 内层循环: `computeJoin()` / `computeMeet()` - LUB/GLB 计算
- [x] 内层循环: `shouldGreedyFix()` - 贪婪固定判断

### 整合

- [x] 修改 `ResolutionParts.kt` - 显式类型参数处理
- [x] 修改 `CangJieCallResolver.kt` - 迭代推导引擎集成
- [x] 添加诊断信息类型 - `ArgumentConstraintPositionByIndex`, `LambdaReturnTypePosition`

### 测试

- [ ] 显式类型参数基本场景
- [ ] Lambda 参数类型推导
- [ ] Option.None 上下文推导（Quest 类型重分析）
- [ ] 链式泛型调用
- [ ] 嵌套 Option 推导
- [ ] 贪婪固定测试（Final 类型）
- [ ] Last Resort Lambda 分析测试
- [ ] 迭代限制测试
- [ ] 错误场景测试

---

**文档版本**: 2.1
**创建日期**: 2026-01-18
**更新日期**: 2026-01-25
**状态**: 实现完成，待测试验证