# 仓颉语言类型参数推导设计改进方案

## 目录
- [问题背景](#问题背景)
- [核心原则](#核心原则)
- [当前实现分析](#当前实现分析)
- [设计问题](#设计问题)
- [改进方案](#改进方案)
- [实现示例](#实现示例)
- [测试用例](#测试用例)

---

## 问题背景

仓颉语言的类型推导系统需要处理多层类型参数：
1. **外层类型参数**：来自类、枚举的类型参数（如 `class a<T>`）
2. **内层类型参数**：来自方法的类型参数（如 `func foo<U>()`）

当前实现从 Kotlin 迁移而来，存在语义不匹配的问题。

### 示例场景

```cangjie
// 枚举定义
enum Option<T> {
    Some(T) | None
}

// 类定义
class a<T> {
    static func a1() {}
    static func a2(a: T) {}
}
```

**调用方式**：
- `Option.None` - 需推导 T
- `Option<String>.None` - T 已显式给出
- `Some(1)` - 从参数推导 T
- `a.a1()` - 需推导 T（但无约束，推导失败）
- `a<Int64>.a1()` - T 已显式给出
- `a.a2(1)` - 从参数推导 T

---

## 核心原则

### 1. 全量推导原则

**规则**：只要类型定义了类型参数，就必须推导（或显式指定）

```cangjie
class Box<T> {
    static func create() {}
}

Box.create()         // ✗ 推导失败：T 无约束
Box<Int64>.create()  // ✓ 成功：T 已显式给出
```

### 2. 逐层推导原则

**规则**：从外层（类/枚举）到内层（方法）逐层收集和推导类型参数

```cangjie
class Container<T> {
    func process<U>(value: U): T {}
}

Container<String>.process(42)
// 推导过程：
// 1. T = String（来自限定符）
// 2. U = Int64（来自参数）
```

### 3. 显式优先原则

**规则**：显式给出的类型参数优先于类型推导

```cangjie
Option<String>.Some(42)
// T = String（显式给出）
// 参数类型检查：Int64 vs String → 类型错误
```

---

## 当前实现分析

### 实现位置

`analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/components/ResolutionParts.kt`

**CreateFreshVariablesSubstitutor 的类型参数收集**：

```kotlin
fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
    // 如果接收器是DISPATCH_RECEIVER，并且它是一个静态调用
    if (resolvedCall.dispatchReceiverArgument != null &&
        resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

        return (resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver)
            .classQualifier.descriptor.declaredTypeParameters +  // 类的类型参数
            candidateDescriptor.original.typeParameters          // 方法的类型参数
    }

    return candidateDescriptor.original.typeParameters
}
```

### 限定符解析流程

`analysis/src/main/kotlin/org/cangnova/cangjie/resolve/qualified/resolvers/ExpressionQualifierResolver.kt:195`

```kotlin
fun resolveNameExpressionAsQualifier(...): QualifierReceiver? {
    val qualifierDescriptor = resolveQualifierDescriptor(name, receiver, context, location)

    if (qualifierDescriptor != null) {
        // ✓ 正确：解析了显式类型参数
        val resolvedType = typeResolver.resolveTypeForClass(...)  // 得到 a<Int64>

        // ✓ 正确：保存了带类型参数的类型
        return createQualifierReceiver(expression, qualifierDescriptor, resolvedType, context)
    }

    return null
}
```

**ClassValueReceiver 结构**：

```kotlin
class ClassValueReceiver(
    val classQualifier: ClassifierQualifier,
    override val type: CangJieType,  // ✓ 包含完整类型信息（如 a<Int64>）
    original: ClassValueReceiver? = null
)
```

---

## 设计问题

### 问题 1：丢弃显式类型参数信息

**现象**：已解析的类型信息被丢弃

```cangjie
a<Int64>.a1()
```

**实际流程**：

| 阶段 | 处理 | 结果 |
|------|------|------|
| 1. 限定符解析 | `typeResolver.resolveTypeForClass()` | ✓ 得到 `a<Int64>` |
| 2. 创建接收器 | `ClassValueReceiver(qualifier, a<Int64>)` | ✓ 保存了类型 |
| 3. 收集类型参数 | `descriptor.declaredTypeParameters` | ✗ 返回 `[T]` |
| 4. 创建新鲜变量 | `TypeVariableFromCallableDescriptor(T)` | ✗ 忽略了 `Int64` |
| 5. 推导 | 无约束 | ✗ **推导失败**（错误！） |

**问题根源**：
- `ClassValueReceiver.type` 包含了 `a<Int64>` 的完整信息
- 但 `getTypeParameters()` 只看 `descriptor.declaredTypeParameters`，返回原始类型参数 `[T]`
- 没有从 `ClassValueReceiver.type.arguments` 中提取显式给出的类型

### 问题 2：无法区分不同的类型参数来源

**当前实现**：一刀切地合并所有类型参数

```kotlin
classQualifier.descriptor.declaredTypeParameters +   // 类的 T
candidateDescriptor.original.typeParameters          // 方法的 U
```

**问题**：没有区分类型参数的来源和状态：
- 显式给出的（如 `a<Int64>` 中的 `Int64`）
- 需要推导的（如 `a.a1()` 中的 `T`）
- 从上下文确定的（如实例方法调用）

### 问题 3：Kotlin vs 仓颉语义差异

| 特性 | Kotlin | 仓颉 |
|------|--------|------|
| 静态方法访问类型参数 | ✓ 可以（通过 companion） | ✗ **不能**（除非签名使用） |
| 枚举构造器 | 特殊处理（sealed class） | **真正的构造器** |
| 显式类型参数 | `ClassName<Type>.method()` | `ClassName<Type>.method()` |
| 类型推导位置 | 方法级别 | **逐层**（外层+内层） |

---

## 改进方案

### 方案概述

**核心思路**：在收集类型参数时，检查是否已有显式类型参数，避免重复推导

### 改进的类型参数收集逻辑

```kotlin
fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
    if (resolvedCall.dispatchReceiverArgument != null &&
        resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

        val classValueReceiver = resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver
        val classDescriptor = classValueReceiver.classQualifier.descriptor

        // ✓ 关键改进：检查类型是否已经包含具体类型参数
        val classType = classValueReceiver.type

        // 情况1：类型参数已显式给出（如 a<Int64>）
        if (classType.arguments.isNotEmpty()) {
            // 不需要推导类的类型参数，直接使用已有类型
            // 只收集方法自己的类型参数
            return candidateDescriptor.original.typeParameters
        }

        // 情况2：类型参数未给出（如 a）
        // 需要推导类的类型参数
        return classDescriptor.declaredTypeParameters +
               candidateDescriptor.original.typeParameters
    }

    return candidateDescriptor.original.typeParameters
}
```

### 处理显式类型参数的约束

在 `CreateFreshVariablesSubstitutor.process()` 中添加：

```kotlin
override fun ResolutionCandidate.process(workIndex: Int) {
    val csBuilder = getSystem().getBuilder()

    val typeParameters = getTypeParameters()

    // ✓ 关键：提取显式给出的类型参数
    val knownTypeArguments = extractKnownTypeArguments()

    val (toFreshVariables, freshTypeVariables) =
        if (typeParameters.isEmpty())
            ComposableTypeSubstitutor.EMPTY to emptyList()
        else
            createToFreshVariableSubstitutorAndAddInitialConstraints(
                candidateDescriptor,
                resolvedCall.atom,
                csBuilder,
                typeParameters
            )

    // ✓ 关键：为显式给出的类型参数添加相等约束
    for ((typeParam, explicitType) in knownTypeArguments) {
        val freshVar = freshTypeVariables.find { it.originalTypeParameter == typeParam }
        if (freshVar != null) {
            csBuilder.addEqualityConstraint(
                freshVar.defaultType,
                explicitType,
                ExplicitTypeParameterConstraintPositionImpl(
                    SimpleTypeArgument(explicitType, ...)
                )
            )
        }
    }

    resolvedCall.freshVariablesSubstitutor = toFreshVariables
    resolvedCall.freshVariables = freshTypeVariables

    // ... 后续处理
}
```

### 提取显式类型参数的辅助函数

```kotlin
/**
 * 从 ClassValueReceiver 中提取显式给出的类型参数
 *
 * @return 类型参数到具体类型的映射
 *
 * 示例：
 * - a<Int64>.a1() → { T -> Int64 }
 * - Option<String>.None → { T -> String }
 * - a.a1() → {} (空映射)
 */
private fun ResolutionCandidate.extractKnownTypeArguments(): Map<TypeParameterDescriptor, UnwrappedType> {
    val receiver = resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue

    if (receiver is ClassValueReceiver) {
        val classDescriptor = receiver.classQualifier.descriptor
        val classType = receiver.type

        // 从 a<Int64> 中提取 [T -> Int64] 的映射
        return classDescriptor.declaredTypeParameters.zip(classType.arguments)
            .mapNotNull { (param, arg) ->
                val argType = arg.type as? UnwrappedType
                if (argType != null) param to argType else null
            }
            .toMap()
    }

    return emptyMap()
}
```

---

## 实现示例

### 完整的处理流程

```kotlin
// 在 CreateFreshVariablesSubstitutor 中
object CreateFreshVariablesSubstitutor : ResolutionPart() {

    fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
        val candidateDescriptor = this.candidateDescriptor

        // 特殊情况：枚举构造器必须推导枚举的类型参数
        if (candidateDescriptor is EnumConstructorDescriptor) {
            val enumDescriptor = candidateDescriptor.containingDeclaration as ClassDescriptor
            return enumDescriptor.declaredTypeParameters + candidateDescriptor.typeParameters
        }

        // 静态调用的情况
        if (resolvedCall.dispatchReceiverArgument != null &&
            resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

            val classValueReceiver = resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver
            val classDescriptor = classValueReceiver.classQualifier.descriptor
            val classType = classValueReceiver.type

            // 如果类型参数已显式给出，只收集方法的类型参数
            if (classType.arguments.isNotEmpty()) {
                return candidateDescriptor.original.typeParameters
            }

            // 否则需要推导类的类型参数
            return classDescriptor.declaredTypeParameters +
                   candidateDescriptor.original.typeParameters
        }

        // 普通方法调用，只收集方法的类型参数
        return candidateDescriptor.original.typeParameters
    }

    private fun ResolutionCandidate.extractKnownTypeArguments(): Map<TypeParameterDescriptor, UnwrappedType> {
        val receiver = resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue

        if (receiver is ClassValueReceiver) {
            val classDescriptor = receiver.classQualifier.descriptor
            val classType = receiver.type

            return classDescriptor.declaredTypeParameters.zip(classType.arguments)
                .mapNotNull { (param, arg) ->
                    val argType = arg.type as? UnwrappedType
                    if (argType != null) param to argType else null
                }
                .toMap()
        }

        return emptyMap()
    }

    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()

        val typeParameters = getTypeParameters()
        val knownTypeArguments = extractKnownTypeArguments()

        val (toFreshVariables, freshTypeVariables) =
            if (typeParameters.isEmpty())
                ComposableTypeSubstitutor.EMPTY to emptyList()
            else
                createToFreshVariableSubstitutorAndAddInitialConstraints(
                    candidateDescriptor,
                    resolvedCall.atom,
                    csBuilder,
                    typeParameters
                )

        // 为显式类型参数添加相等约束
        for ((typeParam, explicitType) in knownTypeArguments) {
            val freshVar = freshTypeVariables.find { it.originalTypeParameter == typeParam }
            if (freshVar != null) {
                csBuilder.addEqualityConstraint(
                    freshVar.defaultType,
                    explicitType,
                    ExplicitTypeParameterConstraintPositionImpl(
                        SimpleTypeArgument(explicitType, /* variance */ Variance.INVARIANT)
                    )
                )
            }
        }

        val knownTypeParametersSubstitutor = knownTypeParametersResultingSubstitutor?.let {
            createKnownParametersFromFreshVariablesSubstitutor(freshTypeVariables, it)
        } ?: ComposableTypeSubstitutor.EMPTY

        resolvedCall.freshVariablesSubstitutor = toFreshVariables
        resolvedCall.freshVariables = freshTypeVariables
        resolvedCall.knownParametersSubstitutor = knownTypeParametersSubstitutor

        if (typeParameters.isEmpty()) {
            return
        }

        // 检查是否有矛盾
        if (csBuilder.hasContradiction) return

        // 优化：如果没有显式类型参数且没有已知替换器，直接返回
        if (resolvedCall.typeArgumentMappingByOriginal == TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
            && knownTypeParametersResultingSubstitutor == null
            && knownTypeArguments.isEmpty()) {
            return
        }

        // 处理方法级别的显式类型参数
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]

            // 如果已在 knownTypeArguments 中处理过，跳过
            if (knownTypeArguments.containsKey(typeParameter)) {
                continue
            }

            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.safeSubstitute(typeParameter.defaultType.unwrap())
            if (knownTypeArgument != null) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(knownTypeArgument, freshVariable),
                    KnownTypeParameterConstraintPositionImpl(knownTypeArgument)
                )
                continue
            }

            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)

            if (typeArgument is SimpleTypeArgument) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(typeArgument.type, freshVariable),
                    ExplicitTypeParameterConstraintPositionImpl(typeArgument)
                )
            } else {
                assert(typeArgument == TypeArgumentPlaceholder) {
                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
                }
            }
        }
    }
}
```

---

## 测试用例

### 测试场景 1：显式类型参数

```cangjie
class a<T> {
    static func a1() {}
}

// 测试用例
a<Int64>.a1()
```

**预期行为**：
- 收集类型参数：无（T 已显式给出）
- 提取显式类型：`{ T -> Int64 }`
- 约束：无需推导
- **结果**：✓ 成功

**当前行为**：
- 收集类型参数：`[T]`
- 创建新鲜变量：`T'`
- 约束：无
- **结果**：✗ 推导失败（错误）

### 测试场景 2：无显式类型参数，无约束

```cangjie
a.a1()
```

**预期行为**：
- 收集类型参数：`[T]`
- 创建新鲜变量：`T'`
- 约束：无
- **结果**：✗ 推导失败（正确）

**当前行为**：
- 同预期
- **结果**：✗ 推导失败（正确）

### 测试场景 3：枚举构造器

```cangjie
enum Option<T> {
    Some(T) | None
}

// 测试用例
Option<String>.None
```

**预期行为**：
- 收集类型参数：无（T 已显式给出）
- 提取显式类型：`{ T -> String }`
- **结果**：✓ 成功

**当前行为**：
- 收集类型参数：`[T]`
- 约束：无
- **结果**：✗ 推导失败（错误）

### 测试场景 4：从参数推导

```cangjie
a.a2(1)  // a2 定义为 static func a2(a: T) {}
```

**预期行为**：
- 收集类型参数：`[T]`
- 约束：`Int64 <: T`
- **结果**：✓ 成功，T = Int64

**当前行为**：
- 同预期
- **结果**：✓ 成功（正确）

### 测试场景 5：显式类型与参数冲突

```cangjie
a<String>.a2(1)
```

**预期行为**：
- 提取显式类型：`{ T -> String }`
- 参数约束：`Int64 <: String`
- **结果**：✗ 类型错误（正确）

**当前行为**：
- 收集类型参数：`[T]`
- 约束：`Int64 <: T`
- **结果**：✓ 成功，T = Int64（错误！应该检测冲突）

### 完整对比表

| 场景 | 当前行为 | 改进后行为 | 正确性 |
|------|---------|-----------|--------|
| `a.a1()` | ✗ 推导失败 | ✗ 推导失败 | ✓ 正确 |
| `a<Int64>.a1()` | ✗ 推导失败 | ✓ 成功 | ✓ **修复** |
| `Option.None` | ✗ 推导失败 | ✗ 推导失败 | ✓ 正确 |
| `Option<String>.None` | ✗ 推导失败 | ✓ 成功 | ✓ **修复** |
| `a.a2(1)` | ✓ T=Int64 | ✓ T=Int64 | ✓ 正确 |
| `a<String>.a2(1)` | ✓ T=Int64（忽略显式） | ✗ 类型错误 | ✓ **修复** |
| `Some(1)` | ✓ T=Int64 | ✓ T=Int64 | ✓ 正确 |
| `Some<Int64>(1)` | ✓ T=Int64 | ✓ T=Int64 | ✓ 正确 |

---

## 实现路线图

### Phase 1: 核心改进
1. 修改 `getTypeParameters()` 检查显式类型参数
2. 实现 `extractKnownTypeArguments()`
3. 在 `process()` 中添加显式类型约束

### Phase 2: 测试覆盖
1. 添加单元测试覆盖所有场景
2. 回归测试确保不破坏现有功能
3. 边界情况测试（嵌套泛型、多个类型参数）

### Phase 3: 错误诊断
1. 为无约束推导失败提供清晰错误信息
2. 提示用户显式指定类型参数
3. 改进类型冲突的错误消息

### Phase 4: 性能优化
1. 避免创建不必要的新鲜类型变量
2. 缓存显式类型参数提取结果
3. 优化约束系统构建

---

## 相关文件

### 需要修改的文件
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/components/ResolutionParts.kt`
  - `CreateFreshVariablesSubstitutor.getTypeParameters()`
  - `CreateFreshVariablesSubstitutor.process()`

### 相关的文件（参考）
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/qualified/resolvers/ExpressionQualifierResolver.kt`
  - 限定符解析和类型参数处理
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/Qualifier.kt`
  - `ClassValueReceiver` 定义
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/model/CangJieCallKind.kt`
  - 不同调用类型的定义

### 测试文件
- `analysis/src/test/kotlin/org/cangnova/cangjie/resolve/calls/inference/TypeParameterInferenceTest.kt`（新增）

---

## 附录

### A. 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| 类型参数 | Type Parameter | 泛型声明中的参数（如 `T`） |
| 类型实参 | Type Argument | 具体的类型（如 `Int64`） |
| 新鲜类型变量 | Fresh Type Variable | 推导过程中创建的临时类型变量 |
| 约束 | Constraint | 类型之间的关系（子类型、相等等） |
| 限定符 | Qualifier | 类型名称前缀（如 `Option` in `Option.Some`） |
| 接收器 | Receiver | 方法调用的对象 |

### B. 参考资料

1. [Kotlin 类型推导](https://kotlinlang.org/spec/type-inference.html)
2. [仓颉语言规范](https://cangjie-lang.cn/)
3. [约束求解算法](./docs/constraint-solving.md)

---

**文档版本**: 1.0
**创建日期**: 2026-01-18
**作者**: Claude Code Analysis
**状态**: 设计提案