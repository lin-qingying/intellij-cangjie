# 语义分析架构全面分析报告

## 概述

本报告对仓颉语言 IntelliJ 插件的语义分析系统进行全面分析，识别与编译器逻辑不一致的设计问题，并提出统一的架构改进方案。

**目标**: 使插件的语义分析与编译器的语义分析逻辑完全一致（实现可以差异）。

---

## 一、核心问题总结

### 1.1 根本问题：显式/隐式类型参数信息丢失

**问题描述**: 系统无法区分显式指定的类型参数和隐式填充的默认类型参数。

**根本原因**: `TypeResolver.resolveTypeForClass()` 在解析类型时，对于缺失的类型参数使用 `defaultType`（类型参数本身）填充，导致：

- `Result` 被解析为 `Result<T>`
- `Result<Int>` 也被解析为 `Result<Int>`
- 系统无法区分这两种情况

**影响范围**:
- 所有泛型类的静态成员访问
- 枚举构造器访问（如 `Option<Int>.None`）
- 类型推断错误报告

### 1.2 类型参数收集逻辑缺陷

**位置**: `CreateFreshVariablesSubstitutor.getTypeParameters()` (第 274-319 行)

**问题**:
```kotlin
fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
    // 只处理 ClassValueReceiver 的情况
    if (resolvedCall.dispatchReceiverArgument != null &&
        resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {
        // ... 处理逻辑
    }
    // 对于枚举类，classValueReceiver 是 null，这段代码永远不会执行！
    return candidateDescriptor.original.typeParameters
}
```

**缺陷**:
1. 枚举类的 `classValueReceiver` 为 null（见 `Qualifier.kt:180-187`）
2. 因此枚举类的类型参数永远不会被收集
3. 普通泛型类通过类名访问静态成员时也可能遗漏类型参数

### 1.3 类型变量检查错误

**位置**:
- `CreateFreshVariablesSubstitutor.kt:297`
- `Qualifier.kt:222`

**问题**:
```kotlin
// 错误的检查
val isTypeVariable = argType.constructor is TypeVariableTypeConstructorMarker

// 应该同时检查 TypeParameterTypeConstructor
// TypeParameterTypeConstructor 表示未绑定的类型参数（如 T 本身）
```

**影响**: 当类型参数是 `T` 本身（而非类型变量）时，被错误地认为是"具体类型"。

---

## 二、架构层面的问题分析

### 2.1 Qualifier/Receiver 设计问题

#### 2.1.1 ClassQualifier 的 classValueReceiver 设计

**当前设计**:
```kotlin
class ClassQualifier(...) : ClassifierQualifier {
    override val classValueReceiver: ClassValueReceiver? =
        if (descriptor.kind == ClassKind.ENUM) {
            null  // 枚举类返回 null
        } else {
            qualifierType?.let { ClassValueReceiver(this, it) }
        }
}
```

**问题**:
- 枚举类没有 `classValueReceiver`，导致类型参数信息无法通过常规路径传递
- `getTypeArgumentsForConstraints()` 方法虽然可以提取类型参数，但没有被 `CreateFreshVariablesSubstitutor` 使用

#### 2.1.2 类型信息传递路径断裂

```
PSI (显式类型参数)
    ↓
TypeResolver.resolveTypeForClass() [信息丢失点]
    ↓
qualifierType (已填充默认值，无法区分显式/隐式)
    ↓
ClassQualifier.getTypeArgumentsForConstraints() [使用错误的类型检查]
    ↓
CreateFreshVariablesSubstitutor [只处理 ClassValueReceiver]
    ↓
约束系统 (缺少必要的约束)
```

### 2.2 TypeResolver 设计问题

#### 2.2.1 类型参数填充策略

**位置**: `TypeResolver.kt:1311-1316`

```kotlin
private fun appendDefaultArgumentsForLocalClassifier(...) =
    constructorParameters.subList(fromIndex, constructorParameters.size).map {
        TypeArgumentImpl(it.original.defaultType)  // 使用类型参数自身作为默认值
    }
```

**问题**: 这种设计让类型看起来"完整"，但丢失了"哪些类型参数是显式指定的"这一关键信息。

#### 2.2.2 编译器的正确做法

编译器应该：
1. 保留原始的显式类型参数列表
2. 在类型推断阶段，为缺失的类型参数创建类型变量
3. 根据上下文添加约束来推导类型变量

### 2.3 调用解析流程问题

#### 2.3.1 ResolutionPart 执行顺序

当前顺序：
1. `CreateFreshVariablesSubstitutor` - 创建类型变量
2. `MapTypeArguments` - 映射显式类型参数
3. `MapArguments` - 映射值参数
4. `CheckArgumentsInParenthesis` - 检查参数类型
5. ...

**问题**: `CreateFreshVariablesSubstitutor` 依赖于从 `qualifierType` 提取类型参数信息，但此时信息已经丢失。

#### 2.3.2 Tower Resolution 中的信息传递

```kotlin
// TowerLevels.kt 中 QualifierScopeTowerLevel
class QualifierScopeTowerLevel(
    scopeTower: ImplicitScopeTower,
    val qualifier: ClassifierQualifier  // 包含类型信息
) : ScopeTowerLevel {
    // 但这些信息没有正确传递到约束系统
}
```

### 2.4 类型推断系统问题

#### 2.4.1 三种推断模式的定位

```kotlin
enum class InferenceMode {
    SINGLE_PASS,      // 原有 Kotlin 风格，单向
    ITERATIVE,        // 迭代式，双向
    HYBRID_FALLBACK   // 混合模式
}
```

**问题**: 即使使用迭代推断，如果初始约束收集阶段就缺少必要的约束，推断也无法成功。

#### 2.4.2 约束位置信息不完整

```kotlin
// 当前的约束位置类型
sealed interface ConstraintPosition {
    // 缺少：ClassTypeParameterConstraintPosition
    // 用于表示来自类限定符的类型参数约束
}
```

---

## 三、与编译器逻辑的差异对比

### 3.1 编译器的类型推断流程

```
1. 解析表达式，收集显式类型参数（不填充默认值）
2. 创建类型变量（为所有需要推导的类型参数）
3. 添加约束：
   - 显式类型参数 → EQUALITY 约束
   - 缺失类型参数 → 创建类型变量，等待推导
   - 参数类型 → SUBTYPE 约束
   - 返回类型 → 期望类型约束
4. 求解约束系统
5. 检查未解决的类型变量 → 报告推断错误
```

### 3.2 插件的当前流程

```
1. 解析表达式，填充默认类型参数（信息丢失）
2. 创建类型变量（只为方法的类型参数）
3. 添加约束：
   - 从 qualifierType 提取（但无法区分显式/隐式）
   - 类的类型参数被遗漏（对于枚举和某些静态访问）
4. 求解约束系统
5. 检查未解决的类型变量（但类的类型参数从未被检查）
```

### 3.3 具体差异示例

#### 示例 1: 枚举类型参数

```cangjie
enum Result<T> {
    Ok(T) | Err(String)
}

// 场景 1: 显式指定类型参数
let x = Result<Int>.Ok(42)  // 应该成功，T = Int

// 场景 2: 未指定类型参数
let y = Result.Ok(42)  // 应该通过参数推导 T = Int

// 场景 3: 无法推导
let z = Result.Err("error")  // 应该报错：无法推导 T
```

**编译器行为**:
- 场景 1: T 有 EQUALITY 约束，成功
- 场景 2: T 通过参数类型推导，成功
- 场景 3: T 无约束，报告 `[NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER]`

**插件行为**:
- 场景 1: 错误地报告无法推导 T
- 场景 2: 可能成功（如果参数推导工作正常）
- 场景 3: 不报告错误（类型参数未被追踪）

#### 示例 2: 普通泛型类静态成员

```cangjie
class Container<T> {
    static func create(): Container<T> { ... }
}

// 场景 1: 显式指定
let a = Container<Int>.create()  // 应该成功

// 场景 2: 未指定
let b = Container.create()  // 应该报错：无法推导 T
```

**编译器行为**:
- 场景 1: 成功
- 场景 2: 报告错误

**插件行为**:
- 场景 1: 可能成功或失败（取决于 classValueReceiver 是否存在）
- 场景 2: 不报告错误

---

## 四、统一架构改进方案

### 4.1 核心改进：保留显式类型参数信息

#### 4.1.1 新增数据结构

```kotlin
/**
 * 类型参数来源信息
 */
sealed class TypeArgumentSource {
    /** 显式指定的类型参数 */
    data class Explicit(val type: CangJieType, val psiElement: CjTypeReference) : TypeArgumentSource()

    /** 隐式填充的类型参数（需要推导） */
    data class Implicit(val typeParameter: TypeParameterDescriptor) : TypeArgumentSource()

    /** 从上下文推导的类型参数 */
    data class Inferred(val typeVariable: TypeVariableMarker) : TypeArgumentSource()
}

/**
 * 带来源信息的类型参数映射
 */
data class TypeArgumentsWithSource(
    val arguments: Map<TypeParameterDescriptor, TypeArgumentSource>
) {
    val explicitArguments: Map<TypeParameterDescriptor, CangJieType>
        get() = arguments.filterValues { it is TypeArgumentSource.Explicit }
            .mapValues { (it.value as TypeArgumentSource.Explicit).type }

    val implicitParameters: List<TypeParameterDescriptor>
        get() = arguments.filterValues { it is TypeArgumentSource.Implicit }
            .keys.toList()
}
```

#### 4.1.2 修改 ClassifierQualifier 接口

```kotlin
interface ClassifierQualifier : Qualifier {
    override val descriptor: ClassifierDescriptorWithTypeParameters

    /**
     * 获取带来源信息的类型参数
     * 这是获取类型参数信息的主要方法
     */
    fun getTypeArgumentsWithSource(): TypeArgumentsWithSource

    // 废弃旧方法
    @Deprecated("Use getTypeArgumentsWithSource() instead")
    fun getTypeArgumentsForConstraints(): Map<TypeParameterDescriptor, CangJieType> =
        getTypeArgumentsWithSource().explicitArguments
}
```

#### 4.1.3 修改 TypeResolver

```kotlin
// 不再填充默认类型参数，保留原始信息
fun resolveTypeForClass(
    c: TypeResolutionContext,
    expression: CjUserType,
    typeConstructor: TypeConstructor
): TypeResolutionResult {
    val typeArguments = expression.typeArguments
    val parameters = typeConstructor.parameters

    // 构建带来源信息的类型参数映射
    val argumentsWithSource = buildMap {
        parameters.forEachIndexed { index, param ->
            val explicitArg = typeArguments.getOrNull(index)
            if (explicitArg != null) {
                put(param, TypeArgumentSource.Explicit(
                    resolveType(explicitArg),
                    explicitArg
                ))
            } else {
                put(param, TypeArgumentSource.Implicit(param))
            }
        }
    }

    return TypeResolutionResult(
        type = constructType(typeConstructor, argumentsWithSource),
        argumentsWithSource = TypeArgumentsWithSource(argumentsWithSource)
    )
}
```

### 4.2 修改 Qualifier 创建流程

#### 4.2.1 ExpressionQualifierResolver 修改

```kotlin
fun createClassQualifier(
    referenceExpression: CjSimpleNameExpression,
    classDescriptor: ClassAndEnumDescriptor,
    typeResolutionResult: TypeResolutionResult
): ClassQualifier {
    return ClassQualifier(
        referenceExpression = referenceExpression,
        descriptor = classDescriptor,
        qualifierType = typeResolutionResult.type,
        typeArgumentsWithSource = typeResolutionResult.argumentsWithSource
    )
}
```

#### 4.2.2 ClassQualifier 修改

```kotlin
class ClassQualifier(
    override val referenceExpression: CjSimpleNameExpression,
    override val descriptor: ClassAndEnumDescriptor,
    private val qualifierType: CangJieType?,
    private val typeArgumentsWithSource: TypeArgumentsWithSource
) : ClassifierQualifier {

    // 对于所有类类型（包括枚举）都提供 classValueReceiver
    // 用于传递类型参数信息
    override val classValueReceiver: ClassValueReceiver? =
        qualifierType?.let { ClassValueReceiver(this, it, typeArgumentsWithSource) }

    override fun getTypeArgumentsWithSource(): TypeArgumentsWithSource = typeArgumentsWithSource
}
```

### 4.3 修改约束收集流程

#### 4.3.1 CreateFreshVariablesSubstitutor 重构

```kotlin
internal object CreateFreshVariablesSubstitutor : ResolutionPart() {

    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()

        // 统一获取所有类型参数（类 + 方法）
        val allTypeParameters = collectAllTypeParameters()

        // 获取带来源信息的类型参数
        val typeArgumentsWithSource = getTypeArgumentsWithSource()

        // 创建类型变量
        val (substitutor, freshVariables) = createFreshVariables(allTypeParameters, csBuilder)

        // 添加约束
        for ((param, variable) in allTypeParameters.zip(freshVariables)) {
            when (val source = typeArgumentsWithSource.arguments[param]) {
                is TypeArgumentSource.Explicit -> {
                    // 显式类型参数：添加 EQUALITY 约束
                    csBuilder.addEqualityConstraint(
                        variable.defaultType,
                        source.type.unwrap(),
                        ExplicitTypeParameterConstraintPositionImpl(source.psiElement)
                    )
                }
                is TypeArgumentSource.Implicit -> {
                    // 隐式类型参数：不添加约束，等待推导
                    // 类型变量已经创建，会参与后续的约束求解
                }
                is TypeArgumentSource.Inferred -> {
                    // 已推导的类型参数：添加 EQUALITY 约束
                    csBuilder.addEqualityConstraint(
                        variable.defaultType,
                        source.typeVariable.defaultType,
                        InferredTypeParameterConstraintPositionImpl()
                    )
                }
                null -> {
                    // 方法的类型参数，正常处理
                }
            }
        }

        // 存储结果
        resolvedCall.freshVariablesSubstitutor = substitutor
        resolvedCall.freshVariables = freshVariables
    }

    private fun ResolutionCandidate.collectAllTypeParameters(): List<TypeParameterDescriptor> {
        val classTypeParams = getClassTypeParameters()
        val methodTypeParams = candidateDescriptor.original.typeParameters
        return classTypeParams + methodTypeParams
    }

    private fun ResolutionCandidate.getClassTypeParameters(): List<TypeParameterDescriptor> {
        // 从 qualifier 获取类类型参数
        val qualifier = resolvedCall.atom.explicitReceiver?.receiver as? ClassifierQualifier
            ?: return emptyList()

        return qualifier.descriptor.declaredTypeParameters
    }

    private fun ResolutionCandidate.getTypeArgumentsWithSource(): TypeArgumentsWithSource {
        val qualifier = resolvedCall.atom.explicitReceiver?.receiver as? ClassifierQualifier
            ?: return TypeArgumentsWithSource(emptyMap())

        return qualifier.getTypeArgumentsWithSource()
    }
}
```

### 4.4 新增类型参数推断检查

#### 4.4.1 新增 ResolutionPart

```kotlin
/**
 * 检查类类型参数是否能被推导
 */
internal object CheckClassTypeParameterInference : ResolutionPart() {

    override fun ResolutionCandidate.process(workIndex: Int) {
        val typeArgumentsWithSource = getTypeArgumentsWithSource()

        for ((param, source) in typeArgumentsWithSource.arguments) {
            if (source is TypeArgumentSource.Implicit) {
                // 检查这个类型参数是否有足够的约束
                val variable = findFreshVariable(param)
                if (variable != null && !hasEnoughConstraints(variable)) {
                    addDiagnostic(
                        NoInformationForClassTypeParameter(
                            param,
                            resolvedCall.atom.psiCangJieCall
                        )
                    )
                }
            }
        }
    }

    private fun ResolutionCandidate.hasEnoughConstraints(variable: TypeVariableMarker): Boolean {
        val constraints = getSystem().getBuilder()
            .currentStorage()
            .notFixedTypeVariables[variable.freshTypeConstructor]
            ?.constraints
            ?: return false

        // 检查是否有非上界约束
        return constraints.any { it.kind != ConstraintKind.UPPER }
    }
}
```

### 4.5 约束位置信息完善

```kotlin
/**
 * 新增约束位置类型
 */

/** 来自类限定符的显式类型参数 */
data class ClassExplicitTypeParameterConstraintPositionImpl(
    val typeParameter: TypeParameterDescriptor,
    val psiElement: CjTypeReference
) : ConstraintPosition {
    override val from: OnlyInputTypeConstraintPosition = TODO()
}

/** 来自类限定符的隐式类型参数（需要推导） */
data class ClassImplicitTypeParameterConstraintPositionImpl(
    val typeParameter: TypeParameterDescriptor
) : ConstraintPosition {
    override val from: OnlyInputTypeConstraintPosition = TODO()
}
```

---

## 五、实施路线图

### 阶段 1: 基础数据结构（优先级：高）

1. 新增 `TypeArgumentSource` 和 `TypeArgumentsWithSource` 类
2. 修改 `ClassifierQualifier` 接口
3. 更新 `ClassQualifier` 和 `TypeAliasQualifier` 实现

### 阶段 2: TypeResolver 改造（优先级：高）

1. 修改 `resolveTypeForClass()` 保留类型参数来源信息
2. 新增 `TypeResolutionResult` 返回结构
3. 更新所有调用点

### 阶段 3: Qualifier 创建流程（优先级：高）

1. 修改 `ExpressionQualifierResolver`
2. 确保类型参数信息正确传递到 `ClassQualifier`

### 阶段 4: 约束收集重构（优先级：高）

1. 重构 `CreateFreshVariablesSubstitutor`
2. 统一处理类和方法的类型参数
3. 根据来源信息添加正确的约束

### 阶段 5: 推断检查增强（优先级：中）

1. 新增 `CheckClassTypeParameterInference` ResolutionPart
2. 完善诊断信息
3. 添加对应的错误消息

### 阶段 6: 测试和验证（优先级：高）

1. 添加单元测试覆盖各种场景
2. 与编译器行为对比验证
3. 回归测试确保不破坏现有功能

---

## 六、风险评估

### 6.1 高风险点

1. **TypeResolver 修改**: 核心组件，需要仔细测试所有类型解析场景
2. **约束系统变更**: 可能影响现有的类型推断结果

### 6.2 兼容性考虑

1. 保留旧接口作为 deprecated，逐步迁移
2. 添加特性开关，允许回退到旧行为
3. 分阶段发布，逐步验证

### 6.3 性能考虑

1. 新增的数据结构可能增加内存占用
2. 需要评估类型解析的性能影响
3. 考虑缓存策略优化

---

## 七、总结

本报告识别了插件语义分析系统中的核心问题：**显式/隐式类型参数信息在类型解析阶段丢失**。这个问题导致：

1. 枚举类型参数推断错误
2. 普通泛型类静态成员访问时类型参数处理不正确
3. 无法正确报告类型参数推断失败的错误

提出的解决方案通过引入 `TypeArgumentSource` 和 `TypeArgumentsWithSource` 数据结构，在整个解析流程中保留类型参数的来源信息，使约束系统能够：

1. 为显式类型参数添加 EQUALITY 约束
2. 为隐式类型参数创建类型变量并参与推导
3. 正确检测和报告无法推导的类型参数

这个方案从根本上解决了与编译器逻辑不一致的问题，使插件的语义分析与编译器保持一致。
