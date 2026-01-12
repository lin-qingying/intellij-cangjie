# ECS (Existential Constraint System) 存在性约束系统

## 概述

ECS 是一套专门用于重载决议的约束系统，其核心目标是回答一个简单的问题：

> **是否存在一个类型代换 σ，使得给定的约束成立？**

与完整的类型推导系统不同，ECS **只返回 YES/NO**，不进行：
- 类型推断（不给出具体类型）
- 最优解选择
- 调用解析
- Lambda 推断
- Builder inference

## 核心应用场景

### 1. Overloadability（重载冲突检测）

判断两个函数声明是否可以共存（不冲突）。

```cangjie
func <T> f(x: T): Unit
func f(x: Any): Unit
```

问题：是否存在 T，使得 T <: Any？
答案：存在（例如 T = String）
结论：两个声明可以共存

### 2. Specificity（特异性比较）

判断一个函数是否比另一个更具体。

```cangjie
func <T> f(x: T): Unit      // A
func f(x: String): Unit     // B
```

问题：A 是否不比 B 更不具体？
即：是否存在 T，使得 T <: String？
答案：存在（T = String）
结论：A 至少和 B 一样具体

### 3. API 兼容性检查

```cangjie
// v1
func f(x: Number): Unit

// v2
func f(x: Int64): Unit
```

问题：是否存在调用在 v1 可用但 v2 不可用？
这也是存在性判断。

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     ECS Public API                          │
├─────────────────────────────────────────────────────────────┤
│  ExistentialConstraintSystem                                │
│    ├─ checkOverloadability(sig1, sig2): Boolean             │
│    ├─ isNotLessSpecific(specific, general): Boolean         │
│    └─ areSignaturesDistinguishable(sig1, sig2): Boolean     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    ECS Internal                             │
├─────────────────────────────────────────────────────────────┤
│  ExistentialConstraintSolver                                │
│    ├─ solve(constraints): ExistentialResult                 │
│    └─ checkSatisfiability(): Boolean                        │
├─────────────────────────────────────────────────────────────┤
│  ExistentialTypeVariable                                    │
│    └─ 轻量级类型变量，只用于存在性判断                        │
├─────────────────────────────────────────────────────────────┤
│  ExistentialConstraint                                      │
│    ├─ Subtype(lower, upper)                                 │
│    └─ Equality(left, right)                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Type System Integration                    │
├─────────────────────────────────────────────────────────────┤
│  AbstractTypeChecker (existing)                             │
│    ├─ isSubtypeOf()                                         │
│    └─ equalTypes()                                          │
└─────────────────────────────────────────────────────────────┘
```

## 与现有系统的对比

| 特性 | ConstraintSystemImpl (现有) | ECS (新) |
|------|---------------------------|----------|
| 目标 | 类型推导，给出具体类型 | 存在性判断，只给 YES/NO |
| 状态管理 | 复杂状态机 | 无状态/只读 |
| 事务支持 | 需要（回滚等） | 不需要 |
| Lambda 推断 | 支持 | 不支持 |
| 延迟变量 | 支持 | 不支持 |
| 错误收集 | 详细错误 | 只有 satisfiable/unsatisfiable |
| 性能 | 较重 | 轻量级 |

## 文件组织

```
analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ecs/
├── ExistentialConstraintSystem.kt      # 主入口 API
├── ExistentialConstraint.kt            # 约束定义
├── ExistentialTypeVariable.kt          # 类型变量
├── ExistentialConstraintSolver.kt      # 求解器
├── ExistentialResult.kt                # 结果类型和枚举
├── SignatureComparator.kt              # 签名比较器
└── ECS.md                              # 本文档
```

## 使用示例

```kotlin
// 创建 ECS 实例
val ecs = ExistentialConstraintSystem.create(builtIns, specificityComparator)

// 检查两个函数是否可以重载（不冲突）
val result = ecs.checkOverloadability(funcA, funcB)
if (result == OverloadabilityResult.OVERLOADABLE) {
    println("两个函数可以重载")
}

// 比较特异性
val isNotLessSpecific = ecs.isNotLessSpecific(
    specific = signatureA,
    general = signatureB
)

// 完整特异性比较
val specificityResult = ecs.compareSpecificity(signatureA, signatureB)
when (specificityResult) {
    SpecificityResult.MORE_SPECIFIC -> println("A 比 B 更具体")
    SpecificityResult.LESS_SPECIFIC -> println("A 比 B 更不具体")
    SpecificityResult.EQUALLY_SPECIFIC -> println("A 和 B 同样具体")
    SpecificityResult.INCOMPARABLE -> println("A 和 B 无法比较")
}

// 检查两个签名是否可区分
val distinguishable = ecs.areSignaturesDistinguishable(sig1, sig2)

// 底层约束 API
val solver = ecs.createSolver()
solver.registerTypeVariable(typeParameter)
solver.addSubtypeConstraint(lower, upper)
val satisfiable = solver.solve().isSatisfiable
```

## 集成到现有代码

### OverloadChecker

```kotlin
class OverloadChecker(
    val builtIns: CangJieBuiltIns,
    val specificityComparator: TypeSpecificityComparator,
) {
    private val ecs by lazy {
        ExistentialConstraintSystem.create(builtIns, specificityComparator)
    }

    private fun checkOverloadability(a: CallableDescriptor, b: CallableDescriptor): Boolean {
        val result = ecs.checkOverloadability(a, b)
        return result == OverloadabilityResult.OVERLOADABLE
    }
}
```

### ShadowedExtensionChecker

```kotlin
class ShadowedExtensionChecker(
    val builtIns: CangJieBuiltIns,
    val typeSpecificityComparator: TypeSpecificityComparator,
    val trace: DiagnosticSink,
) {
    private val ecs by lazy {
        ExistentialConstraintSystem.create(builtIns, typeSpecificityComparator)
    }

    private fun isSignatureNotLessSpecific(
        extensionSignature: FlatSignature<FunctionDescriptor>,
        memberSignature: FlatSignature<FunctionDescriptor>
    ): Boolean = ecs.isNotLessSpecific(extensionSignature, memberSignature)
}
```

## 实现细节

### 存在性判断的算法

对于约束集合 C，判断是否存在满足条件的代换：

1. **收集约束**：从签名对比中收集所有类型约束
2. **传播约束**：执行约束传播（但不执行完整推导）
3. **检测矛盾**：检查是否存在不可满足的约束组合
4. **返回结果**：无矛盾 → YES，有矛盾 → NO

### 矛盾检测

矛盾的情况：
- `T <: Int64` 且 `T <: String`（如果 Int64 和 String 无公共子类型）
- `Int64 <: String`（直接类型不兼容）
- 循环约束导致的不可满足

### 性能优化

1. **早期终止**：一旦检测到矛盾立即返回
2. **缓存**：对相同签名对的结果缓存
3. **简化约束**：对于不含类型参数的签名直接比较
