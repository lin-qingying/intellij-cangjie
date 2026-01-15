# 仓颉函数类型系统重构方案

## 现状分析

### 1. 仓颉函数类型系统架构

根据代码分析,仓颉语言支持两种可调用类型:

#### 1.1 内置函数类型 (Built-in Function Types)

**定义位置**: `org.cangnova.cangjie.builtins`

**类型结构**:
```cangjie
Function0<R>                    // () -> R
Function1<P1, R>                // (P1) -> R
Function2<P1, P2, R>            // (P1, P2) -> R
...
FunctionN<P1, ..., PN, R>       // (P1, ..., PN) -> R
```

**特点**:
- 编译器原生支持
- 有专门的语法糖: `(参数类型) -> 返回类型`
- 可以直接调用: `f(args)`
- 由 `CangJieBuiltIns` 系统管理

**C 互操作函数类型**:
```cangjie
CFunc0<R>                       // C 函数指针
CFunc1<P1, R>
...
```

#### 1.2 用户自定义可调用类型 (User-defined Callable Types)

**定义方式**: 实现 `operator invoke` 方法

```cangjie
class MyCallable {
    public operator func invoke(x: Int64): String {
        return "Result: ${x}"
    }
}

let obj = MyCallable()
obj(42)  // ✅ 必须使用 () 语法,编译器会自动调用 invoke 操作符
// obj.invoke(42)  // ❌ 不能显式调用,这是操作符而非普通方法
 ```

**调用解析流程**:
```
表达式: obj(args)
    ↓
1. 解析 obj (VariableDescriptor)
    ↓
2. 查找 obj 类型的 invoke 操作符 (FunctionDescriptor)
    ↓
3. 创建 VariableAsFunctionResolvedCall
    ↓
   - variableCall: obj 的解析结果
   - functionCall: invoke 的解析结果
```

**特殊规则** (`ResolutionParts.kt:108-111`):
- `invoke` 和 `get` 操作符**只能**使用操作符语法（`obj()` 或 `obj[key]`）
- 不能显式调用 `obj.invoke()` 或 `obj.get(key)`
- 这与普通方法的调用方式相反（普通方法只能用 `.` 调用）

### 2. 与 Kotlin 的差异

| 特性 | Kotlin | 仓颉 | 说明 |
|------|--------|------|------|
| 内置函数类型 | ✅ | ✅ | 都有 FunctionN 族 |
| 用户自定义 invoke | ✅ | ✅ | 都支持操作符重载 |
| 扩展函数类型 | ✅ | ❌ | 仓颉无扩展函数 |
| 上下文接收者 | ✅ (实验性) | ❌ | 仓颉不支持 |
| 挂起函数类型 | ✅ | ❌ | 仓颉无协程 |
| 带接收者函数类型 | ✅ `A.(B) -> C` | ❌ | 仓颉简化设计 |

**仓颉的简化设计哲学**:
- 不支持扩展函数 → 无需扩展函数类型
- 不支持接收者 → 函数类型更简单
- 不支持协程 → 无需挂起函数类型
- 专注于核心功能 + C 互操作

### 3. 当前代码结构

#### 3.1 类型检查扩展属性

**文件**: `descriptors/src/main/kotlin/org/cangnova/cangjie/types/functionTypes.kt`

```kotlin
// 检查是否是内置函数类型
val CangJieType.isBuiltinFunctionalType: Boolean
    get() = constructor.declarationDescriptor?.isBuiltinFunctionalClassDescriptor == true

// 检查是否是内置函数类型或其子类型
val CangJieType.isBuiltinFunctionalTypeOrSubtype: Boolean
    get() = isTypeOrSubtypeOf { it.isBuiltinFunctionalType }

// 检查描述符是否是内置函数类
val DeclarationDescriptor.isBuiltinFunctionalClassDescriptor: Boolean
    get() {
        val functionalClassKind = getFunctionTypeKind()
        return functionalClassKind == FunctionTypeKind.Function
    }
```

**问题**: 命名不够清晰,容易与 invoke 操作符混淆

#### 3.2 函数类型工具函数

```kotlin
// 获取参数类型列表 (不含返回类型)
fun CangJieType.getValueParameterTypesFromFunctionType(): List<TypeArgument>

// 获取返回类型
fun CangJieType.getReturnTypeFromFunctionType(): CangJieType

// 从父类型中提取函数类型
fun CangJieType.extractFunctionalTypeFromSupertypes(): CangJieType

// 创建函数类型
fun createFunctionType(
    builtIns: CangJieBuiltIns,
    annotations: Annotations,
    receiverType: CangJieType?,  // 仓颉中始终为 null
    parameterTypes: List<CangJieType>,
    parameterNames: List<Name>?,
    returnType: CangJieType,
): SimpleType
```

## 重构目标

### 1. 澄清概念

**核心问题**: "可调用类型" 有两种,需要明确区分

- **内置函数类型**: 编译器内置的 FunctionN 类型
- **可调用类型**: 任何可以用 `obj()` 语法调用的类型(包括内置函数类型 + 自定义 invoke)

### 2. 改进命名

#### 2.1 类型检查属性重命名

```kotlin
// 当前命名 (容易误解)
val CangJieType.isBuiltinFunctionalType: Boolean

// 建议命名 (更清晰)
val CangJieType.isBuiltinFunctionType: Boolean

// 或者更明确的名称
val CangJieType.isFunctionNType: Boolean  // 强调是 FunctionN 族
```

**理由**:
- "Functional" 在函数式编程中有特殊含义(高阶函数、不可变等)
- "Function" 更直接地表示函数类型
- "FunctionN" 明确指代内置的 Function0..FunctionN 类型

#### 2.2 新增可调用类型检查

```kotlin
// 检查是否可以被调用 (内置函数类型 OR 有 invoke 操作符)
val CangJieType.isCallable: Boolean
    get() = isBuiltinFunctionType || hasInvokeOperator()

// 检查是否有 invoke 操作符
fun CangJieType.hasInvokeOperator(): Boolean {
    // 查找是否有名为 OperatorNameConventions.INVOKE 的操作符函数
}
```

**注意**: invoke 操作符只能通过 `obj()` 语法调用,不能显式调用 `obj.invoke()`

### 3. 文档改进

#### 3.1 添加详细文档说明

```kotlin
/**
 * 检查类型是否是仓颉内置函数类型
 *
 * 仓颉的内置函数类型包括:
 * - `Function0<R>`, `Function1<P1, R>`, ..., `FunctionN<P1, ..., PN, R>`
 * - 对应语法糖: `() -> R`, `(P1) -> R`, `(P1, ..., PN) -> R`
 * - C 互操作函数: `CFunc0<R>`, `CFunc1<P1, R>`, ...
 *
 * **注意**: 这与用户自定义的 `invoke` 操作符不同:
 * ```cangjie
 * // 内置函数类型
 * let f: (Int64) -> String = { x -> "Result: ${x}" }
 * f(42)  // ✅ 这是内置函数类型
 *
 * // 用户自定义 invoke
 * class MyCallable {
 *     operator func invoke(x: Int64): String { ... }
 * }
 * let obj = MyCallable()
 * obj(42)  // ❌ 这不是内置函数类型,而是 invoke 操作符调用
 *           // 注意: 不能写 obj.invoke(42),操作符只能用操作符语法
 * ```
 *
 * @see hasInvokeOperator 检查是否有自定义 invoke 操作符
 * @see isCallable 检查是否可调用(包括两种情况)
 */
val CangJieType.isBuiltinFunctionType: Boolean
    get() = constructor.declarationDescriptor?.isBuiltinFunctionalClassDescriptor == true
```

#### 3.2 添加使用场景说明

在关键位置添加注释说明为何需要区分:

```kotlin
// 类型推导时
fun inferType(expr: CjCallExpression): CangJieType {
    val callee = expr.calleeExpression
    val calleeType = getType(callee)

    when {
        // 内置函数类型: 直接从类型参数获取返回类型
        calleeType.isBuiltinFunctionType -> {
            return calleeType.getReturnTypeFromFunctionType()
        }

        // 自定义 invoke: 需要解析 invoke 函数的返回类型
        calleeType.hasInvokeOperator() -> {
            val invokeDescriptor = resolveInvokeOperator(calleeType)
            return invokeDescriptor.returnType
        }

        else -> error("Type $calleeType is not callable")
    }
}
```

## 重构步骤

### 阶段 1: 添加新 API (向后兼容)

#### 1.1 在 `functionTypes.kt` 中添加

```kotlin
/**
 * 检查类型是否有 invoke 操作符
 *
 * 仓颉允许用户定义 `operator func invoke` 使类型可调用:
 * ```cangjie
 * class MyCallable {
 *     public operator func invoke(x: Int64): String {
 *         return "Result: ${x}"
 *     }
 * }
 * ```
 *
 * @return 如果类型定义了至少一个 invoke 操作符则返回 true
 */
fun CangJieType.hasInvokeOperator(): Boolean {
    val memberScope = memberScope
    val invokeName = OperatorNameConventions.INVOKE
    return memberScope.getContributedFunctions(invokeName, NoLookupLocation.FROM_BUILTINS)
        .any { it.isOperator }
}

/**
 * 检查类型是否可调用
 *
 * 可调用类型包括:
 * 1. 内置函数类型: `() -> R`, `(T) -> R` 等
 * 2. 带 invoke 操作符的类型
 *
 * @see isBuiltinFunctionType
 * @see hasInvokeOperator
 */
val CangJieType.isCallable: Boolean
    get() = isBuiltinFunctionType || hasInvokeOperator()

/**
 * 获取所有 invoke 操作符描述符
 *
 * @return invoke 操作符列表,如果没有则返回空列表
 */
fun CangJieType.getInvokeOperators(): Collection<FunctionDescriptor> {
    val memberScope = memberScope
    val invokeName = OperatorNameConventions.INVOKE
    return memberScope.getContributedFunctions(invokeName, NoLookupLocation.FROM_BUILTINS)
        .filter { it.isOperator }
}
```

#### 1.2 添加废弃标记

```kotlin
/**
 * @deprecated 使用更清晰的名称 [isBuiltinFunctionType]
 */
@Deprecated(
    "Renamed to isBuiltinFunctionType for clarity",
    ReplaceWith("isBuiltinFunctionType")
)
val CangJieType.isBuiltinFunctionalType: Boolean
    get() = isBuiltinFunctionType
```

### 阶段 2: 渐进式迁移

#### 2.1 优先级 1: 公共 API

迁移所有公开的 API 和扩展函数:
- `descriptors/src/main/kotlin/org/cangnova/cangjie/types/functionTypes.kt`
- 公共工具类和扩展

#### 2.2 优先级 2: 内部使用

逐个模块迁移内部使用:
- `analysis` 模块
- `resolve` 模块
- `types` 模块

#### 2.3 优先级 3: 测试和文档

- 更新单元测试
- 更新文档和示例代码

### 阶段 3: 清理旧 API

在下一个主版本中移除废弃的 API:
- 移除 `isBuiltinFunctionalType`
- 移除相关的废弃函数

## 实施建议

### 1. 立即可做的改进 (无破坏性)

#### 1.1 添加文档注释

在现有的 `isBuiltinFunctionalType` 上添加详细文档:

```kotlin
/**
 * 检查类型是否是仓颉内置函数类型 (FunctionN 或 CFuncN)
 *
 * ## 仓颉函数类型系统
 *
 * 仓颉支持两种可调用类型:
 *
 * ### 1. 内置函数类型 (此属性检查的类型)
 *
 * - 编译器内置的 `Function0<R>`, `Function1<P1, R>`, ..., `FunctionN<...>`
 * - 语法糖: `() -> R`, `(T) -> R`, `(T1, T2) -> R` 等
 * - C 互操作: `CFunc0<R>`, `CFunc1<P1, R>` 等
 *
 * ```cangjie
 * // 内置函数类型示例
 * let add: (Int64, Int64) -> Int64 = { a, b -> a + b }
 * add(1, 2)  // 调用内置函数类型
 * ```
 *
 * ### 2. 自定义可调用类型 (此属性不检查)
 *
 * - 用户定义的类,实现了 `operator func invoke`
 * - 可以使用 `obj()` 语法调用,但不能显式调用 `obj.invoke()`
 * - 底层通过 VariableAsFunctionResolvedCall 机制解析
 *
 * ```cangjie
 * class Counter(var count: Int64) {
 *     public operator func invoke(): Int64 {
 *         count += 1
 *         return count
 *     }
 * }
 *
 * let counter = Counter(0)
 * counter()  // ✅ 调用 invoke 操作符,不是内置函数类型
 * // counter.invoke()  // ❌ 不能显式调用操作符
 * ```
 *
 * ## 使用场景
 *
 * 区分这两种类型的原因:
 * - **类型推导**: 内置函数类型的返回类型是类型参数的最后一个
 * - **调用约定**: 内置函数类型有优化的调用路径
 * - **互操作**: C 函数指针只能是 CFuncN 类型
 * - **序列化**: 内置函数类型有特殊的序列化格式
 *
 * @return 如果类型是 FunctionN 或 CFuncN 则返回 true
 * @see hasInvokeOperator 检查是否有自定义 invoke 操作符
 * @see VariableAsFunctionResolvedCall invoke 操作符的调用解析
 */
val CangJieType.isBuiltinFunctionalType: Boolean
    get() = constructor.declarationDescriptor?.isBuiltinFunctionalClassDescriptor == true
```

#### 1.2 添加辅助检查函数

在 `functionTypes.kt` 中添加新函数,不影响现有代码:

```kotlin
/**
 * 检查类型是否定义了 invoke 操作符
 */
fun CangJieType.hasInvokeOperator(): Boolean {
    val memberScope = memberScope
    val invokeName = OperatorNameConventions.INVOKE
    return memberScope.getContributedFunctions(invokeName, NoLookupLocation.FROM_BUILTINS)
        .any { it.isOperator }
}

/**
 * 检查类型是否可调用 (内置函数类型或有 invoke 操作符)
 */
val CangJieType.isCallable: Boolean
    get() = isBuiltinFunctionalType || hasInvokeOperator()
```

### 2. 建议的重命名 (破坏性,需要计划)

如果决定重命名,建议采用渐进式方法:

#### 步骤 1: 添加新名称

```kotlin
val CangJieType.isBuiltinFunctionType: Boolean
    get() = isBuiltinFunctionalType
```

#### 步骤 2: 标记旧名称为废弃

```kotlin
@Deprecated(
    "Renamed to isBuiltinFunctionType",
    ReplaceWith("isBuiltinFunctionType")
)
val CangJieType.isBuiltinFunctionalType: Boolean
    get() = isBuiltinFunctionType
```

#### 步骤 3: 逐步迁移

使用 IDE 的"查找用法"功能,逐个文件迁移

#### 步骤 4: 移除旧名称

在主版本更新时移除废弃的属性

### 3. 不建议做的改动

#### ❌ 不要改变 `isBuiltinFunctionalType` 的语义

```kotlin
// ❌ 错误: 改变了现有行为
val CangJieType.isBuiltinFunctionalType: Boolean
    get() = isBuiltinFunctionType || hasInvokeOperator()  // 不要这样做!
```

**原因**: 会破坏现有代码,导致不可预测的行为

#### ❌ 不要移除现有函数

在确保没有使用者之前,不要移除任何公共 API

## 测试计划

### 1. 单元测试

#### 1.1 内置函数类型测试

```kotlin
class BuiltinFunctionTypeTest {
    @Test
    fun `test function types are recognized`() {
        // Function0<Int>
        val f0 = createFunctionType(builtIns, emptyList(), intType)
        assertTrue(f0.isBuiltinFunctionalType)

        // (Int, String) -> Bool
        val f2 = createFunctionType(builtIns, listOf(intType, stringType), boolType)
        assertTrue(f2.isBuiltinFunctionalType)
    }

    @Test
    fun `test non-function types are not recognized`() {
        assertFalse(intType.isBuiltinFunctionalType)
        assertFalse(stringType.isBuiltinFunctionalType)
        assertFalse(arrayType.isBuiltinFunctionalType)
    }
}
```

#### 1.2 自定义 invoke 测试

```kotlin
class CustomInvokeTest {
    @Test
    fun `test custom invoke operator is detected`() {
        val code = """
            class MyCallable {
                public operator func invoke(x: Int64): String {
                    return "Result"
                }
            }
        """
        val type = analyzeAndGetType(code, "MyCallable")

        assertFalse(type.isBuiltinFunctionalType)
        assertTrue(type.hasInvokeOperator())
        assertTrue(type.isCallable)
    }
}
```

### 2. 集成测试

测试调用解析是否正确:

```kotlin
class CallResolutionTest {
    @Test
    fun `test builtin function call`() {
        val code = """
            let f: (Int64) -> String = { x -> "Result: ${x}" }
            f(42)
        """
        val call = analyzeAndGetCall(code, "f(42)")
        // 应该直接解析为函数调用,不是 VariableAsFunctionResolvedCall
    }

    @Test
    fun `test custom invoke call`() {
        val code = """
            class MyCallable {
                operator func invoke(x: Int64): String = "Result"
            }
            let obj = MyCallable()
            obj(42)
        """
        val call = analyzeAndGetCall(code, "obj(42)")
        assertTrue(call is VariableAsFunctionResolvedCall)
    }
}
```

## 总结

### 核心观点

1. **仓颉确实支持自定义 invoke**: 当前的 `isBuiltinFunctionalType` 逻辑是正确的
2. **命名可以改进**: "Functional" → "Function" 更清晰
3. **需要更好的文档**: 说明两种可调用类型的区别
4. **渐进式重构**: 先添加文档和新 API,再考虑重命名

### 推荐方案

**短期 (立即可做)**:
1. ✅ 为现有 API 添加详细文档注释
2. ✅ 添加 `hasInvokeOperator()` 和 `isCallable` 辅助函数
3. ✅ 在关键位置添加注释说明使用场景

**中期 (下个版本)**:
1. 考虑重命名 `isBuiltinFunctionalType` → `isBuiltinFunctionType`
2. 添加废弃标记
3. 逐步迁移内部使用

**长期 (主版本更新)**:
1. 移除废弃的 API
2. 统一命名风格

### 不需要改动的部分

- ✅ 核心逻辑正确,无需修改
- ✅ invoke 操作符支持符合仓颉语言设计
- ✅ 与 Kotlin 的差异是有意为之(简化设计)

### 需要改进的部分

- ⚠️ 文档不足,容易误解
- ⚠️ 缺少辅助函数来检查 invoke 操作符
- ⚠️ 命名可以更清晰

---

**文档创建时间**: 2026-01-15
**基于代码版本**: intellij-cangjie PSI 分支
**作者**: Claude Code Analysis
