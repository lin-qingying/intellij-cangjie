# 类型系统架构重构方案

## 当前问题分析

### 1. 类型检查系统碎片化

当前存在多个类型检查入口，职责不清：

```
TypeCheckingProcedure          ← 用于约束系统的类型检查
CangJieTypeChecker             ← 统一的类型检查接口
  ├─ CangJieSubtypeChecker     ← 子类型检查实现
  ├─ CangJieTypeEquality       ← 类型相等性检查
  ├─ ClassTypeChecker          ← 类类型检查
  ├─ FunctionTypeChecker       ← 函数类型检查
  ├─ TupleTypeChecker          ← 元组类型检查
  ├─ PrimitiveTypeChecker      ← 基本类型检查
  └─ SpecialTypeChecker        ← 特殊类型检查
```

**问题**：
- `TypeCheckingProcedure` 与 `CangJieTypeChecker` 功能重复
- 约束系统使用 `TypeCheckingProcedure` + 回调模式，复杂且难维护
- 类型检查逻辑分散在多处，不利于统一优化

### 2. TypeProjection 残留

虽然已删除 `TypeProjection` 接口，但相关概念仍在：
- `CapturedTypeApproximation.kt` 仍使用 Variance 和投影逻辑
- 内部定义的 `TypeArgument` 类与公共 `TypeArgument` 接口重复
- `approximateCapturedTypes` 等函数仍基于 Kotlin 的协变/逆变概念

### 3. 约束系统耦合

`ConstraintSystemBuilderImpl` 通过 `TypeCheckingProcedureCallbacks` 与类型检查耦合：
- 每次添加约束都创建新的 `TypeCheckingProcedure` 实例
- 递归调用通过回调实现，调用栈深且不直观
- 难以追踪约束添加的完整流程

## 重构目标

### 1. 统一类型检查入口

**目标**：所有类型比较都通过 `CangJieTypeChecker` 进行

**好处**：
- 单一职责，易于维护和测试
- 便于添加缓存、性能优化
- 便于统一错误处理和调试

### 2. 简化约束系统

**目标**：约束系统直接使用 `CangJieTypeChecker`，移除 `TypeCheckingProcedure`

**好处**：
- 减少间接层，代码更直观
- 降低复杂度，易于理解和修改
- 提高性能（减少函数调用开销）

### 3. 彻底移除 Variance 概念

**目标**：基于仓颉语言的不变性设计重写类型近似和捕获逻辑

**好处**：
- 代码更符合仓颉语言特性
- 避免 Kotlin 概念的混淆
- 简化实现，提高可维护性

## 重构步骤

### 阶段一：重构 CapturedTypeApproximation.kt

**任务**：
1. 移除所有 Variance 相关逻辑
2. 移除内部 `TypeArgument` 类定义
3. 基于不变性重写 `approximateCapturedTypes`
4. 简化 `approximateCapturedTypesIfNecessary`

**影响文件**：
- `descriptors/src/main/kotlin/org/cangnova/cangjie/types/CapturedTypeApproximation.kt`

### 阶段二：统一类型检查系统

**任务**：
1. 在 `CangJieTypeChecker` 中添加约束系统需要的方法：
   ```kotlin
   interface CangJieTypeChecker {
       // 现有方法
       fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean
       fun equalTypes(a: CangJieType, b: CangJieType): Boolean

       // 新增：用于约束系统的类型检查
       fun checkSubtypeConstraint(
           subtype: CangJieType,
           supertype: CangJieType,
           onTypeArgumentMismatch: (TypeArgument, TypeArgument) -> Unit
       ): Boolean

       fun checkEqualityConstraint(
           type1: CangJieType,
           type2: CangJieType,
           onTypeArgumentMismatch: (TypeArgument, TypeArgument) -> Unit
       ): Boolean
   }
   ```

2. 重构 `ConstraintSystemBuilderImpl` 使用 `CangJieTypeChecker`：
   ```kotlin
   // 旧实现
   val typeCheckingProcedure = TypeCheckingProcedure(callbacks)
   typeCheckingProcedure.isSubtypeOf(subType, superType)

   // 新实现
   val typeChecker = CangJieTypeChecker.DEFAULT
   typeChecker.checkSubtypeConstraint(subType, superType) { arg1, arg2 ->
       // 处理类型参数不匹配
       addNestedConstraint(arg1.type, arg2.type)
   }
   ```

**影响文件**：
- `descriptors/src/main/kotlin/org/cangnova/cangjie/types/checker/CangJieTypeChecker.kt`
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ConstraintSystemBuilderImpl.kt`

### 阶段三：移除 TypeCheckingProcedure

**任务**：
1. 确认所有使用点已迁移到 `CangJieTypeChecker`
2. 删除 `TypeCheckingProcedure.kt`
3. 删除 `TypeCheckingProcedureCallbacks.kt`
4. 删除 `TypeCheckerProcedureCallbacksImpl.kt`

**影响文件**：
- `descriptors/src/main/kotlin/org/cangnova/cangjie/types/checker/TypeCheckingProcedure.kt` (删除)
- `descriptors/src/main/kotlin/org/cangnova/cangjie/types/checker/TypeCheckingProcedureCallbacks.kt` (删除)
- `descriptors/src/main/kotlin/org/cangnova/cangjie/types/checker/TypeCheckerProcedureCallbacksImpl.kt` (删除)

### 阶段四：清理和优化

**任务**：
1. 移除未使用的 import 和方法
2. 更新文档和注释
3. 添加单元测试验证重构
4. 性能测试对比

## 详细设计

### CangJieTypeChecker 扩展接口

```kotlin
/**
 * 仓颉类型检查器接口
 *
 * 提供类型相等性与子类型判断的统一入口
 */
interface CangJieTypeChecker {
    // === 现有方法 ===
    fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean
    fun equalTypes(a: CangJieType, b: CangJieType): Boolean
    fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean

    // === 约束系统专用方法 ===

    /**
     * 检查子类型约束，并在遇到嵌套约束时调用回调
     *
     * 用于约束系统中递归添加嵌套约束
     *
     * @param subtype 子类型
     * @param supertype 父类型
     * @param context 约束检查上下文，用于嵌套约束的回调
     * @return true 如果约束满足
     */
    fun checkSubtypeConstraint(
        subtype: CangJieType,
        supertype: CangJieType,
        context: ConstraintCheckContext
    ): Boolean

    /**
     * 检查类型相等约束，并在遇到嵌套约束时调用回调
     *
     * @param type1 第一个类型
     * @param type2 第二个类型
     * @param context 约束检查上下文
     * @return true 如果约束满足
     */
    fun checkEqualityConstraint(
        type1: CangJieType,
        type2: CangJieType,
        context: ConstraintCheckContext
    ): Boolean

    /**
     * 尝试捕获类型参数
     *
     * 用于处理泛型类型的捕获
     *
     * @param type 待捕获的类型
     * @param typeArgument 类型参数
     * @param context 约束检查上下文
     * @return true 如果成功捕获
     */
    fun tryCaptureTypeArgument(
        type: CangJieType,
        typeArgument: TypeArgument,
        context: ConstraintCheckContext
    ): Boolean
}

/**
 * 约束检查上下文
 *
 * 在类型检查过程中收集嵌套约束
 */
interface ConstraintCheckContext {
    /**
     * 添加子类型约束
     */
    fun addSubtypeConstraint(subtype: CangJieType, supertype: CangJieType)

    /**
     * 添加相等约束
     */
    fun addEqualityConstraint(type1: CangJieType, type2: CangJieType)

    /**
     * 尝试捕获类型参数
     */
    fun tryCaptureTypeArgument(type: CangJieType, typeArgument: TypeArgument): Boolean

    /**
     * 报告约束不满足
     */
    fun reportConstraintError()
}
```

### 约束系统重构示例

**旧实现**：
```kotlin
private fun doAddConstraint(
    constraintKind: ConstraintKind,
    subType: CangJieType?,
    superType: CangJieType?,
    constraintContext: ConstraintContext,
    typeCheckingProcedure: TypeCheckingProcedure
) {
    // 创建回调对象
    val callbacks = object : TypeCheckingProcedureCallbacks {
        override fun assertEqualTypes(...) { ... }
        override fun assertSubtype(...) { ... }
        override fun capture(...) { ... }
    }

    // 创建类型检查过程
    val procedure = TypeCheckingProcedure(callbacks)

    // 执行检查
    if (constraintKind == EQUAL) {
        procedure.equalTypes(subType, superType)
    } else {
        procedure.isSubtypeOf(subType, superType)
    }
}
```

**新实现**：
```kotlin
private fun doAddConstraint(
    constraintKind: ConstraintKind,
    subType: CangJieType?,
    superType: CangJieType?,
    constraintContext: ConstraintContext
) {
    // 创建上下文对象
    val context = object : ConstraintCheckContext {
        override fun addSubtypeConstraint(subtype: CangJieType, supertype: CangJieType) {
            doAddConstraint(ConstraintKind.SUB_TYPE, subtype, supertype, newContext)
        }

        override fun addEqualityConstraint(type1: CangJieType, type2: CangJieType) {
            doAddConstraint(ConstraintKind.EQUAL, type1, type2, newContext)
        }

        override fun tryCaptureTypeArgument(type: CangJieType, typeArgument: TypeArgument): Boolean {
            if (isMyTypeVariable(typeArgument.type)) return false
            val myVar = getMyTypeVariable(type) ?: return false
            generateTypeParameterCaptureConstraint(myVar, typeArgument, newContext, type.isOption)
            return true
        }

        override fun reportConstraintError() {
            errors.add(newTypeInferenceOrParameterConstraintError(constraintContext.position))
        }
    }

    // 使用统一的类型检查器
    val typeChecker = CangJieTypeChecker.DEFAULT

    // 执行检查
    val result = if (constraintKind == ConstraintKind.EQUAL) {
        typeChecker.checkEqualityConstraint(subType!!, superType!!, context)
    } else {
        typeChecker.checkSubtypeConstraint(subType!!, superType!!, context)
    }
}
```

### CapturedTypeApproximation 重构

**旧实现**（基于 Variance）：
```kotlin
private fun TypeProjection.toTypeArgument(typeParameter: TypeParameterDescriptor) =
    when (TypeSubstitutor.combine(typeParameter.variance, this)) {
        Variance.INVARIANT -> TypeArgument(typeParameter, type, type)
        Variance.IN_VARIANCE -> TypeArgument(typeParameter, type, upper)
        Variance.OUT_VARIANCE -> TypeArgument(typeParameter, lower, type)
    }
```

**新实现**（基于不变性）：
```kotlin
/**
 * 近似捕获类型
 *
 * 仓颉语言中所有用户自定义泛型都是不变的，简化了近似逻辑
 */
fun approximateCapturedTypes(type: CangJieType): ApproximationBounds<CangJieType> {
    // 处理 Flexible 类型
    if (type.isFlexible()) {
        val lowerBounds = approximateCapturedTypes(type.lowerIfFlexible())
        val upperBounds = approximateCapturedTypes(type.upperIfFlexible())

        return ApproximationBounds(
            CangJieTypeFactory.flexibleType(lowerBounds.lower, upperBounds.lower),
            CangJieTypeFactory.flexibleType(lowerBounds.upper, upperBounds.upper)
        )
    }

    // 处理捕获类型
    if (type.isCaptured()) {
        val capturedType = type as CapturedType
        val innerType = capturedType.lowerType ?: capturedType.constructor.projection.type
        return ApproximationBounds(innerType, innerType)
    }

    // 处理泛型类型参数（不变）
    if (type.arguments.isEmpty()) {
        return ApproximationBounds(type, type)
    }

    val approximatedArguments = type.arguments.map { arg ->
        approximateCapturedTypes(arg.type)
    }

    // 所有参数不变，上下界相同
    return ApproximationBounds(
        type.replace(approximatedArguments.map { TypeArgumentImpl(it.lower) }),
        type.replace(approximatedArguments.map { TypeArgumentImpl(it.upper) })
    )
}
```

## 实施计划

### 第一步：重构 CapturedTypeApproximation.kt

**目标**：移除 Variance 和 TypeProjection 残留

**工作量**：2-3 小时

**风险**：低（该文件使用点较少）

### 第二步：扩展 CangJieTypeChecker

**目标**：添加约束系统需要的方法

**工作量**：3-4 小时

**风险**：低（纯新增，不影响现有功能）

### 第三步：重构 ConstraintSystemBuilderImpl

**目标**：使用 CangJieTypeChecker 替代 TypeCheckingProcedure

**工作量**：4-6 小时

**风险**：中（核心推导逻辑，需要充分测试）

### 第四步：移除旧代码

**目标**：删除 TypeCheckingProcedure 相关文件

**工作量**：1-2 小时

**风险**：低（确认无使用后删除）

### 第五步：测试和优化

**目标**：全面测试，性能对比

**工作量**：3-4 小时

**风险**：低

**总工作量**：13-19 小时

## 预期收益

### 代码质量

- **减少代码行数**：预计减少 500-800 行
- **降低圈复杂度**：类型检查逻辑统一，平均圈复杂度降低 30%
- **提高可维护性**：单一入口，易于定位和修改

### 性能提升

- **减少函数调用**：移除回调层，减少 20-30% 的函数调用
- **便于缓存优化**：统一入口便于添加类型检查结果缓存
- **预计性能提升**：5-10%

### 开发体验

- **更易理解**：代码流程更直观
- **更易调试**：调用栈更清晰
- **更易扩展**：添加新的类型检查规则更简单

## 兼容性考虑

### API 兼容性

- `CangJieTypeChecker` 接口新增方法，不影响现有使用
- 内部实现重构，外部 API 保持不变

### 迁移策略

- 分阶段实施，每个阶段独立验证
- 保留旧实现直到新实现稳定
- 使用 `@Deprecated` 标记过时接口

## 总结

这次重构的核心目标是：

1. **统一类型检查**：所有类型比较通过 `CangJieTypeChecker` 完成
2. **简化约束系统**：移除 `TypeCheckingProcedure` 回调模式
3. **彻底清理 Variance**：基于仓颉语言不变性重写类型近似

重构后的系统将更加简洁、高效、易于维护。
