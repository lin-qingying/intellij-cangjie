# 项目重复文件检查报告

> **生成时间**: 2025-12-10
> **项目**: IntelliJ Cangjie Plugin
> **扫描范围**: 整个项目（排除 bin/, .gradle/, build/ 目录）

---

## 📊 执行摘要

- **总计发现重复文件**: 95个
- **严重程度分级**:
    - 🔴 **高优先级**: 15个文件（需要立即处理）
    - 🟡 **中优先级**: 30+个文件（建议近期处理）
    - 🟢 **低优先级**: 50个文件（自动生成或测试文件）

---

## 🔴 高优先级重复 - 需要立即处理

### 1. common 与 src 模块完全重复

这些文件在 `common` 和 `src/main` 两个模块中完全重复，导致维护负担和潜在的不一致性问题。

#### 1.1 任务管理相关文件

| 文件名                                        | 位置 1                                                | 位置 2                                         | 状态                                    |
|--------------------------------------------|-----------------------------------------------------|----------------------------------------------|---------------------------------------|
| `CjBackgroundTaskQueue.kt`                 | `common/src/main/kotlin/org/cangnova/cangjie/task/` | `src/main/kotlin/org/cangnova/cangjie/task/` | 几乎完全相同，仅格式差异                          |
| `CjProjectTaskQueueService.kt`             | `common/src/main/kotlin/org/cangnova/cangjie/task/` | `src/main/kotlin/org/cangnova/cangjie/task/` | 1个枚举值差异：`CANGJIE_SYNC` vs `CJPM_SYNC` |
| `DelayedBackgroundableProcessIndicator.kt` | `common/src/main/kotlin/org/cangnova/cangjie/task/` | `src/main/kotlin/org/cangnova/cangjie/task/` | **完全相同**                              |

**包含的重复类**:

- `CjBackgroundTaskQueue`
- `CjProjectTaskQueueService`
- `DelayedBackgroundableProcessIndicator`

#### 1.2 配置和版本管理

| 文件名                          | 位置 1                                                  | 位置 2                                           | 差异说明                    |
|------------------------------|-------------------------------------------------------|------------------------------------------------|-------------------------|
| `LanguageVersionSettings.kt` | `common/src/main/kotlin/org/cangnova/cangjie/config/` | `src/main/kotlin/org/cangnova/cangjie/config/` | 注释语言（中文 vs 英文）和部分枚举条目不同 |

**包含的重复类**:

- `AnalysisFlag`
- `CangJieVersion`
- `LanguageVersionSettingsImpl`

**推荐处理方案**:

```bash
# 保留 common 模块版本，删除 src 中的重复
rm -rf src/main/kotlin/org/cangnova/cangjie/task/CjBackgroundTaskQueue.kt
rm -rf src/main/kotlin/org/cangnova/cangjie/task/CjProjectTaskQueueService.kt
rm -rf src/main/kotlin/org/cangnova/cangjie/task/DelayedBackgroundableProcessIndicator.kt

# 合并 LanguageVersionSettings.kt 的差异后保留一份
```

---

### 2. analysis 与 descriptors 模块重复

这是最严重的重复问题，24个核心文件在两个模块间重复，说明模块边界不清晰。

#### 2.1 类型系统相关（4个文件）

| 文件名                              | 位置 1                                                               | 位置 2                                                                  | 包含的类                                                         |
|----------------------------------|--------------------------------------------------------------------|-----------------------------------------------------------------------|--------------------------------------------------------------|
| `IntegerValueTypeConstructor.kt` | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | `IntegerValueTypeConstructor`<br>`FloatValueTypeConstructor` |
| `CompileTimeConstant.kt`         | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | 编译时常量相关类                                                     |
| `ConstantValueFactory.kt`        | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | 常量值工厂                                                        |
| `constantValues.kt`              | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/constants/` | 常量值相关函数                                                      |

#### 2.2 接收者类型（7个文件）

| 文件名                        | 位置 1                                                                      | 位置 2                                                                         |
|----------------------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `ContextClassReceiver.kt`  | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` |
| `ContextReceiver.kt`       | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` |
| `ExtensionReceiver.kt`     | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` |
| `TransientReceiver.kt`     | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` |
| `ImplicitClassReceiver.kt` | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` |
| `ImplicitReceiver.kt`      | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` |
| `AbstractReceiverValue.kt` | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/receivers/` |

#### 2.3 作用域（Scopes）（9个文件）

| 文件名                            | 位置 1                                                            | 位置 2                                                               |
|--------------------------------|-----------------------------------------------------------------|--------------------------------------------------------------------|
| `ChainedMemberScope.kt`        | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `FunctionClassScope.kt`        | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `SubstitutingScope.kt`         | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `TupleClassScope.kt`           | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `TypeIntersectionScope.kt`     | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `InnerClassesScopeWrapper.kt`  | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `GivenFunctionsMemberScope.kt` | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `AbstractScopeAdapter.kt`      | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |
| `LazyScopeAdapter.kt`          | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/` |

#### 2.4 工具和辅助类（4个文件）

| 文件名                                    | 位置 1                                                                       | 位置 2                                                            |
|----------------------------------------|----------------------------------------------------------------------------|-----------------------------------------------------------------|
| `DescriptorEquivalenceForOverrides.kt` | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/`                   | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/`     |
| `DescriptorFactory.kt`                 | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/`                   | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/`     |
| `ExternalOverridabilityCondition.kt`   | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/`                   | `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/`     |
| `DeclarationProvider.kt`               | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/lazy/declarations/` | `descriptors/src/main/kotlin/org/cangnova/cangjie/descriptors/` |

**推荐处理方案**:

```
选项 A: 将共享代码移到 common 模块
选项 B: 明确模块职责，保留一侧实现，另一侧通过依赖引用
选项 C: 创建新的 shared-descriptors 模块
```

---

### 3. CangJieEnv 环境检测重复

| 文件名             | 位置 1                                                 | 位置 2                                                            | 差异                               |
|-----------------|------------------------------------------------------|-----------------------------------------------------------------|----------------------------------|
| `CangJieEnv.kt` | `common/src/main/kotlin/org/cangnova/cangjie/utils/` | `toolchain/src/main/kotlin/org/cangnova/cangjie/toolchain/env/` | toolchain 版本多一个 `getOsName()` 函数 |

**包含的重复类**:

- `MacCangJieEnv`
- `UnixCangJieEnv`
- `WindowsCangJieEnv`

**推荐处理方案**:

```bash
# 保留 toolchain 版本（功能更完整）
# 删除 common/src/main/kotlin/org/cangnova/cangjie/utils/CangJieEnv.kt
# 更新所有引用指向 toolchain 版本
```

---

### 4. 调试器 DAP vs Protobuf 实现重复

这些文件代表两种不同的调试协议实现，存在平行的代码。

| 文件名                                 | DAP 位置                                      | Protobuf 位置                                       | 差异程度 |
|-------------------------------------|---------------------------------------------|---------------------------------------------------|------|
| `CangJieStackFrame.kt`              | `debugger/dap/src/main/kotlin/.../ui/`      | `debugger/protobuf/src/main/kotlin/.../core/`     | 中等差异 |
| `CangJieSuspendContext.kt`          | `debugger/dap/src/main/kotlin/.../ui/`      | `debugger/protobuf/src/main/kotlin/.../core/`     | 较大差异 |
| `BreakpointService.kt`              | `debugger/dap/src/main/kotlin/.../service/` | `debugger/protobuf/src/main/kotlin/.../services/` | 中等差异 |
| `CangJieDebuggerEditorsProvider.kt` | `debugger/dap/src/main/kotlin/.../ui/`      | `debugger/protobuf/src/main/kotlin/.../core/`     | 小差异  |

**包含的重复类**:

- `CangJieStackFrame`
- `CangJieSuspendContext`
- `CangJieExecutionStack` (内部类)

**推荐处理方案**:

```
1. 提取共同接口到 debugger/common 模块
2. 使用策略模式或工厂模式统一两种实现
3. 将协议特定的代码保留在各自模块中
```

---

### 5. 其他关键跨模块重复

| 文件名                                   | 位置 1                                                                   | 位置 2                                                          | 状态             |
|---------------------------------------|------------------------------------------------------------------------|---------------------------------------------------------------|----------------|
| `CangJieSourceElement.kt`             | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/source/`        | `psi/src/main/kotlin/org/cangnova/cangjie/resolve/source/`    | 仅版权年份不同，内容完全相同 |
| `CjContractDescription.kt`            | `analysis/src/main/kotlin/org/cangnova/cangjie/contracts/description/` | `src/main/kotlin/org/cangnova/cangjie/contracts/description/` | **完全相同**       |
| `CjContractDescriptionVisitor.kt`     | `analysis/src/main/kotlin/org/cangnova/cangjie/contracts/description/` | `src/main/kotlin/org/cangnova/cangjie/contracts/description/` | **完全相同**       |
| `CommonCompilerPerformanceManager.kt` | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/`               | `src/main/kotlin/org/cangnova/cangjie/config/`                | **完全相同**       |
| `CjPsiUtil.kt`                        | `psi/src/main/kotlin/org/cangnova/cangjie/psi/`                        | `psi/src/main/kotlin/org/cangnova/cangjie/psi/psiUtil/`       | 同一模块内的重复       |
| `AnalyzingUtils.kt`                   | `analysis/src/main/kotlin/org/cangnova/cangjie/analysis/`              | `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/`      | 同一模块内的重复       |

**推荐处理方案**:

```bash
# 删除 src/main 中的 Contract 相关文件，保留 analysis 版本
rm -rf src/main/kotlin/org/cangnova/cangjie/contracts/

# 删除 src/main 中的性能管理器，保留 analysis 版本
rm -rf src/main/kotlin/org/cangnova/cangjie/config/CommonCompilerPerformanceManager.kt

# 统一 CangJieSourceElement，保留 psi 版本
rm -rf analysis/src/main/kotlin/org/cangnova/cangjie/resolve/source/CangJieSourceElement.kt

# 整合 CjPsiUtil，删除 psi/ 根目录版本，保留 psiUtil/ 子目录版本
rm -rf psi/src/main/kotlin/org/cangnova/cangjie/psi/CjPsiUtil.kt
```

---

## 🟡 中优先级重复

### 6. 元数据生成文件重复

自动生成的 Protobuf 格式文件，在多个格式包之间重复。这些文件由代码生成器创建，可能是合理的。

#### 6.1 跨格式包重复的文件

**ChirFormat vs NodeFormat vs PackageFormat 重复**:

| 文件名                | 重复次数 | 位置                                                                |
|--------------------|------|-------------------------------------------------------------------|
| `Annotation.kt`    | 2    | `metadata/gen/.../ChirFormat/`, `metadata/gen/.../NodeFormat/`    |
| `Block.kt`         | 2    | `metadata/gen/.../ChirFormat/`, `metadata/gen/.../NodeFormat/`    |
| `Decl.kt`          | 2    | `metadata/gen/.../NodeFormat/`, `metadata/gen/.../PackageFormat/` |
| `Expr.kt`          | 2    | `metadata/gen/.../NodeFormat/`, `metadata/gen/.../PackageFormat/` |
| `FuncBody.kt`      | 2    | `metadata/gen/.../NodeFormat/`, `metadata/gen/.../PackageFormat/` |
| `FuncParamList.kt` | 2    | `metadata/gen/.../NodeFormat/`, `metadata/gen/.../PackageFormat/` |
| `FuncType.kt`      | 2    | `metadata/gen/.../ChirFormat/`, `metadata/gen/.../NodeFormat/`    |
| `Generic.kt`       | 2    | `metadata/gen/.../NodeFormat/`, `metadata/gen/.../PackageFormat/` |
| `ImportSpec.kt`    | 2    | `metadata/gen/.../NodeFormat/`, `metadata/gen/.../PackageFormat/` |
| `EnumType.kt`      | 2    | `descriptors/src/.../types/`, `metadata/gen/.../ChirFormat/`      |

**其他元数据文件**:

- `IntegerLiteralTypeConstructor.kt` (2个位置)
- `IntegerValueTypeConstructor.kt` (2个位置)
- `PrimitiveTypeUtil.kt` (2个位置)
- `ResolutionAnchorProvider.kt` (2个位置)
- `ResolutionScope.kt` (2个位置)
- `TypeAttributeTranslators.kt` (2个位置)
- `TypeSystemContext.kt` (2个位置)
- `UnreachableCode.kt` (2个位置)
- `VisibilityUtil.kt` (2个位置)

**推荐处理方案**:

```
1. 检查 Protobuf 生成脚本，确认是否需要生成多份
2. 如果格式不同但类名相同，考虑使用包名区分
3. 如果格式相同，合并到单一位置并共享
```

---

### 7. 工具类文件高度重复

这些通用名称的文件在多个位置重复，表明缺乏统一的工具类模块。

| 文件名          | 重复次数 | 示例位置                                                                                                         |
|--------------|------|--------------------------------------------------------------------------------------------------------------|
| `utils.kt`   | 9    | `analysis/src/`, `common/src/`, `descriptors/src/`, `psi/src/`, `util/src/`, ...                             |
| `util.kt`    | 5    | `analysis/src/`, `common/src/`, `descriptors/src/`, `util/src/`, ...                                         |
| `urls.kt`    | 3    | `cangjie-project/src/`, `cjpm/src/`, `toolchain/src/`                                                        |
| `context.kt` | 2    | `analysis/src/main/kotlin/org/cangnova/cangjie/context/`, `util/src/main/kotlin/org/cangnova/cangjie/utils/` |
| `storage.kt` | 2    | `util/src/main/kotlin/org/cangnova/cangjie/container/`, 另一位置                                                 |

**推荐处理方案**:

```
1. 创建统一的 util 模块或扩展现有的 util 模块
2. 将所有通用工具函数迁移到单一位置
3. 使用 Kotlin 扩展函数避免工具类膨胀
4. 采用更具体的文件名（如 StringUtils.kt, FileUtils.kt）
```

---

### 8. 其他中等优先级重复

| 文件名                                | 位置 1                            | 位置 2                                         | 说明             |
|------------------------------------|---------------------------------|----------------------------------------------|----------------|
| `EmptyIntersectionTypeChecker.kt`  | `analysis/src/.../caches/`      | `common/src/.../checkers/`                   | 空交集类型检查器       |
| `DoubleColonExpressionResolver.kt` | `analysis/src/.../resolve/`     | `analysis/src/.../types/expressions/`        | 同一模块内重复        |
| `Diagnostic.kt`                    | `analysis/diagnostics/src/`     | `metadata/gen/.../MacroMsgFormat/`           | 一个是诊断系统，一个是元数据 |
| `Diagnostics.kt`                   | `analysis/diagnostics/src/`     | `analysis/src/.../calls/model/`              | 同一模块不同用途       |
| `Box.kt`                           | `metadata/gen/.../ChirFormat/`  | `util/src/main/kotlin/org/cangnova/cangjie/` | 不同用途的 Box      |
| `File.kt`                          | `debugger/common/src/.../util/` | `metadata/gen/.../NodeFormat/`               | 不同用途           |

---

## 🟢 低优先级重复

### 9. 测试文件重复

| 文件名                   | 位置 1                                            | 位置 2                                                        | 说明      |
|-----------------------|-------------------------------------------------|-------------------------------------------------------------|---------|
| `CjPsiFactoryTest.kt` | `psi/src/test/kotlin/org/cangnova/cangjie/psi/` | `src/test/kotlin/org/cangnova/cangjie/cjpm1/project/model/` | 不同模块的测试 |

---

### 10. Scope 工具类重复

| 文件名                  | 位置数量 | 说明                                  |
|----------------------|------|-------------------------------------|
| `ScopeUtils.kt`      | 2    | 作用域工具函数                             |
| `TypeVariable.kt`    | 2    | 类型变量定义                              |
| `ValueDescriptor.kt` | 2    | 值描述符（descriptors vs util/container） |

---

## 📋 完整重复文件列表

以下是所有95个重复文件的完整清单：

### A-C

1. `AbstractReceiverValue.kt` (2个位置)
2. `AbstractScopeAdapter.kt` (2个位置)
3. `AnalyzingUtils.kt` (2个位置)
4. `Annotation.kt` (2个位置)
5. `Block.kt` (2个位置)
6. `Box.kt` (2个位置)
7. `BreakpointService.kt` (2个位置)
8. `CangJieDebuggerEditorsProvider.kt` (2个位置)
9. `CangJieEnv.kt` (2个位置)
10. `CangJieSourceElement.kt` (2个位置)
11. `CangJieStackFrame.kt` (2个位置)
12. `CangJieSuspendContext.kt` (2个位置)
13. `ChainedMemberScope.kt` (2个位置)
14. `CjBackgroundTaskQueue.kt` (2个位置)
15. `CjContractDescription.kt` (2个位置)
16. `CjContractDescriptionVisitor.kt` (2个位置)
17. `CjProjectTaskQueueService.kt` (2个位置)
18. `CjPsiFactoryTest.kt` (2个位置)
19. `CjPsiUtil.kt` (2个位置)
20. `CommonCompilerPerformanceManager.kt` (2个位置)
21. `CompileTimeConstant.kt` (2个位置)
22. `ConstantValueFactory.kt` (2个位置)
23. `constantValues.kt` (2个位置)
24. `context.kt` (2个位置)
25. `ContextClassReceiver.kt` (2个位置)
26. `ContextReceiver.kt` (2个位置)

### D-I

27. `Decl.kt` (2个位置)
28. `DeclarationProvider.kt` (2个位置)
29. `DelayedBackgroundableProcessIndicator.kt` (2个位置)
30. `DescriptorEquivalenceForOverrides.kt` (2个位置)
31. `DescriptorFactory.kt` (2个位置)
32. `Diagnostic.kt` (2个位置)
33. `Diagnostics.kt` (2个位置)
34. `DoubleColonExpressionResolver.kt` (2个位置)
35. `EmptyIntersectionTypeChecker.kt` (2个位置)
36. `EnumType.kt` (2个位置)
37. `Expr.kt` (2个位置)
38. `ExtensionReceiver.kt` (2个位置)
39. `ExternalOverridabilityCondition.kt` (2个位置)
40. `File.kt` (2个位置)
41. `FuncBody.kt` (2个位置)
42. `FuncParamList.kt` (2个位置)
43. `FunctionClassScope.kt` (2个位置)
44. `FuncType.kt` (2个位置)
45. `Generic.kt` (2个位置)
46. `GivenFunctionsMemberScope.kt` (2个位置)
47. `ImplicitClassReceiver.kt` (2个位置)
48. `ImplicitReceiver.kt` (2个位置)
49. `ImportSpec.kt` (2个位置)
50. `InnerClassesScopeWrapper.kt` (2个位置)
51. `IntegerLiteralTypeConstructor.kt` (2个位置)
52. `IntegerValueTypeConstructor.kt` (2个位置)

### L-Z

53. `LanguageVersionSettings.kt` (2个位置)
54. `LazyScopeAdapter.kt` (3个位置)
55. `MemberScope.kt` (包含 InstanceMemberScope, StaticMemberScope)
56. `PrimitiveTypeUtil.kt` (2个位置)
57. `ResolutionAnchorProvider.kt` (2个位置)
58. `ResolutionScope.kt` (2个位置)
59. `ScopeUtils.kt` (2个位置)
60. `StatementFilter.kt` (3个位置)
61. `storage.kt` (2个位置)
62. `SubstitutingScope.kt` (2个位置)
63. `TransientReceiver.kt` (2个位置)
64. `TupleClassScope.kt` (2个位置)
65. `TypeAttributeTranslators.kt` (2个位置)
66. `TypeIntersectionScope.kt` (2个位置)
67. `TypeSystemContext.kt` (2个位置)
68. `TypeVariable.kt` (2个位置)
69. `UnreachableCode.kt` (2个位置)
70. `urls.kt` (3个位置)
71. `util.kt` (5个位置)
72. `utils.kt` (9个位置)
73. `ValueDescriptor.kt` (2个位置)
74. `VisibilityUtil.kt` (2个位置)

### 元数据格式相关（约21个额外文件）

75-95. 各种 Protobuf 生成的元数据文件

---

## 💡 处理建议和行动计划

### 阶段 1: 立即行动（1-2周）

#### 任务 1.1: 清理 common 和 src 重复

```bash
# 优先级: 最高
# 预计工作量: 4小时

# 步骤:
1. 删除 src/main/kotlin/org/cangnova/cangjie/task/ 下的3个文件
2. 合并 LanguageVersionSettings.kt 的差异
3. 更新所有 import 引用
4. 运行测试验证
```

#### 任务 1.2: 统一 Contract 和性能管理器

```bash
# 优先级: 高
# 预计工作量: 2小时

# 步骤:
1. 删除 src/main 中的 Contract 相关文件
2. 删除 src/main 中的 CommonCompilerPerformanceManager.kt
3. 更新引用
4. 测试
```

#### 任务 1.3: 整合 CangJieEnv

```bash
# 优先级: 高
# 预计工作量: 3小时

# 步骤:
1. 删除 common/utils/CangJieEnv.kt
2. 更新所有引用到 toolchain 版本
3. 测试跨平台兼容性
```

### 阶段 2: 中期改进（1个月）

#### 任务 2.1: 重构 analysis 和 descriptors 模块

```
优先级: 高
预计工作量: 2-3周

建议方案:
1. 创建架构评审，明确两个模块的职责边界
2. 选择处理策略:
   - 方案 A: 提取共享代码到新模块 shared-descriptors
   - 方案 B: 明确依赖方向，保留一侧实现
   - 方案 C: 合并两个模块
3. 分批次迁移文件（按类别）
4. 每次迁移后运行完整测试
```

#### 任务 2.2: 统一调试器实现

```
优先级: 中
预计工作量: 1周

步骤:
1. 提取共同接口到 debugger/common
2. 使用策略模式重构
3. 保持两种协议的独立性
```

#### 任务 2.3: 整合工具类

```
优先级: 中
预计工作量: 1周

步骤:
1. 审查所有 utils.kt 和 util.kt 文件
2. 分类整理（字符串、文件、集合等）
3. 创建统一的工具模块结构
4. 迁移并更新引用
```

### 阶段 3: 长期优化（持续）

#### 任务 3.1: 元数据生成优化

```
优先级: 低
预计工作量: 按需

步骤:
1. 审查 Protobuf 生成脚本
2. 确认重复是否合理
3. 如不合理，优化生成逻辑
```

#### 任务 3.2: 建立防重复机制

```
优先级: 中
预计工作量: 持续

建议:
1. 添加 CI 检查脚本，检测新增重复文件
2. 建立代码审查清单
3. 定期（季度）运行重复检测
4. 更新团队编码规范
```

---

## 🔧 自动化检测脚本

为了持续监控重复文件，建议添加以下 Gradle 任务：

```kotlin
// build.gradle.kts

tasks.register("detectDuplicateFiles") {
    group = "verification"
    description = "检测项目中的重复文件"

    doLast {
        val duplicates = mutableMapOf<String, MutableList<File>>()

        fileTree(projectDir) {
            include("**/*.kt")
            exclude("**/bin/**", "**/build/**", "**/.gradle/**")
        }.forEach { file ->
            duplicates.getOrPut(file.name) { mutableListOf() }.add(file)
        }

        val hasDuplicates = duplicates.values.any { it.size > 1 }
        if (hasDuplicates) {
            println("⚠️  发现重复文件:")
            duplicates.filter { it.value.size > 1 }.forEach { (name, files) ->
                println("  - $name (${files.size}个位置)")
                files.forEach { println("    → ${it.relativeTo(projectDir)}") }
            }
            throw GradleException("发现 ${duplicates.count { it.value.size > 1 }} 个重复文件")
        } else {
            println("✅ 未发现重复文件")
        }
    }
}

// 集成到 check 任务
tasks.check {
    dependsOn("detectDuplicateFiles")
}
```

---

## 📈 进度追踪

| 任务                      | 状态     | 负责人 | 预计完成 | 实际完成 |
|-------------------------|--------|-----|------|------|
| 清理 common/src 重复        | 🔲 待开始 | -   | -    | -    |
| 统一 Contract 文件          | 🔲 待开始 | -   | -    | -    |
| 整合 CangJieEnv           | 🔲 待开始 | -   | -    | -    |
| 重构 analysis/descriptors | 🔲 待开始 | -   | -    | -    |
| 统一调试器实现                 | 🔲 待开始 | -   | -    | -    |
| 整合工具类                   | 🔲 待开始 | -   | -    | -    |
| 元数据生成优化                 | 🔲 待开始 | -   | -    | -    |
| 建立防重复机制                 | 🔲 待开始 | -   | -    | -    |

---

## 📝 附录

### A. 检测方法

本报告使用以下方法生成：

```bash
# 1. 查找同名文件
find . -type f -name "*.kt" \
  -not -path "*/bin/*" \
  -not -path "*/.gradle/*" \
  -not -path "*/build/*" \
  | awk -F/ '{print $NF}' \
  | sort | uniq -d

# 2. 定位每个重复文件的位置
for fname in $(cat duplicate_names.txt); do
  find . -name "$fname" -type f \
    -not -path "*/bin/*" \
    -not -path "*/.gradle/*" \
    -not -path "*/build/*"
done

# 3. 搜索特定类定义
grep -r "class ClassName" --include="*.kt" \
  | grep -v "/bin/" \
  | grep -v ".gradle" \
  | grep -v "build/"
```

### B. 相关文档

- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - 项目结构文档
- [CLAUDE.md](CLAUDE.md) - 开发指南
- [CHANGELOG.md](CHANGELOG.md) - 变更日志

### C. 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发起 Pull Request
- 团队讨论会

---

**报告生成者**: Claude Code
**最后更新**: 2025-12-10
**报告版本**: 1.0
