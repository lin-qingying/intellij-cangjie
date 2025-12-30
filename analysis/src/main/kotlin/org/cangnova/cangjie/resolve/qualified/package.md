# 限定表达式解析包

## 包概述

本包提供仓颉语言中限定表达式（Qualified Expression）的完整解析功能，是名称解析系统的核心组件之一。限定表达式是使用点号（`.`）连接多个标识符的表达式形式，例如 `a.b.c`、`com.example.MyClass`、`package.Class.member` 等。

## 核心功能

- **限定名称解析** - 将 `a.b.c` 形式的限定名称解析为包、类或成员
- **导入语句处理** - 解析 `import` 语句中的限定名称，建立导入作用域
- **类型引用解析** - 处理类型位置的限定名称（如变量类型、函数返回类型）
- **表达式解析** - 处理表达式位置的限定名称，区分值和类型
- **限定符提取** - 识别限定表达式中的限定符部分和成员部分
- **可见性检查** - 根据不同位置应用相应的可见性规则

## 文件结构

### 核心解析器
- **[QualifiedExpressionResolver.kt](QualifiedExpressionResolver.kt)** - 限定表达式解析器核心实现
  - 提供所有限定名称解析功能
  - 处理包、类、类型别名的解析
  - 执行可见性检查和错误报告

### 工具类
- **[QualifiedExpressionResolveUtil.kt](QualifiedExpressionResolveUtil.kt)** - 解析工具类
  - 限定符作为接收器的解析
  - 限定符作为独立表达式的解析
  - 引用目标的确定

### 数据类与结果
- **[QualifierPart.kt](QualifierPart.kt)** - 限定符部分相关类
  - `QualifierPart` - 限定符部分基类
  - `ExpressionQualifierPart` - 表达式限定符部分
  - `TypeQualifierResolutionResult` - 类型解析结果
  - `QualifiedExpressionResolveResult` - 限定表达式解析结果

### 位置与可见性
- **[QualifierPosition.kt](QualifierPosition.kt)** - 位置枚举和可见性判断
  - `QualifierPosition` 枚举 - 限定符出现的位置
  - `isVisible()` 函数 - 可见性检查逻辑

### 扩展功能
- **[QualifiedExpressionExtensions.kt](QualifiedExpressionExtensions.kt)** - 扩展函数
  - `CjExpression.asQualifierPartList()` - 将表达式转换为限定符列表
  - `CjImportInfo.ImportContent.asQualifierPartList()` - 导入内容转换

### 调试支持
- **[DebugModeSupport.kt](DebugModeSupport.kt)** - 调试模式相关支持
  - 调试模式诊断抑制
  - 包片段自定义源包装

### 分析文档
- **[QualifiedExpressionResolutionAnalysis.md](QualifiedExpressionResolutionAnalysis.md)** - 解析规则详细分析
  - 仓颉语言模块与包的约定
  - 当前实现与语言规范的对比
  - 已知问题和实现差距
  - 修复建议和优先级

- **[ScopeRefactoringProposal.md](ScopeRefactoringProposal.md)** - 作用域系统重构方案（聚焦版本）
  - 重构范围明确：仅作用域和限定表达式解析（描述符层次设计正确，不改动）
  - 4 大核心问题识别（重导出缺失、包别名不支持、模块名检查不完整、包重导出检查缺失）
  - 详细重构方案（3 个方向：重导出机制、包别名、错误检查）
  - 5 阶段实施计划（5-7 周，含代码示例）
  - 性能优化策略和风险评估

## 解析策略

限定表达式解析器采用**贪心策略**，从左到右尽可能多地识别限定符：

### 示例 1：包 + 类 + 成员
```kotlin
com.example.MyClass.foo()
-> 限定符: com.example.MyClass
-> 成员调用: foo()
```

### 示例 2：包 + 包 + 类
```kotlin
a.b.c.D
-> 限定符: a.b.c（包）
-> 类名: D
```

### 示例 3：类 + 嵌套类
```kotlin
OuterClass.InnerClass
-> 限定符: OuterClass
-> 嵌套类: InnerClass
```

## 位置敏感解析

解析行为根据限定名称出现的位置而变化：

### 包声明（PACKAGE_HEADER）
```kotlin
package com.example
```
只验证包路径的有效性。

### 导入语句（IMPORT）
```kotlin
import com.example.Foo
```
解析包、类、类型别名，检查可见性，禁止导入成员函数/变量。

### 类型位置（TYPE）
```kotlin
var x: com.example.Foo
```
只解析类型（类、接口、类型别名），不解析值。

### 表达式位置（EXPRESSION）
```kotlin
com.example.Foo.bar()
```
值优先于类型，需要区分限定符和成员访问。

## 特殊处理

- **IDE 模式** - 支持 `_root_ide_package_` 前缀避免解析歧义
- **调试模式** - 在调试器上下文中禁用某些诊断检查
- **导入路径解析** - 优先通过导入路径解析，提高 IDE 补全性能
- **废弃标记** - 跟踪并报告通过废弃路径访问的符号
- **模块名限定** - 禁止使用模块名作为限定符前缀

## 解析流程

```
1. 解析入口
   ├─ resolveDescriptorForType()        // 类型位置
   ├─ resolveQualifierInExpressionAndUnroll()  // 表达式位置
   ├─ processImportReference()          // 导入语句
   └─ resolvePackageHeader()            // 包声明

2. 路径提取
   └─ mapToQualifierParts()
      将嵌套的限定表达式展开为线性路径

3. 前缀解析
   └─ resolveToPackageOrClassPrefix()
      ├─ 检查 IDE 模式前缀
      ├─ 检查第一部分是否为值（表达式位置）
      ├─ 在词法作用域中查找类型
      ├─ quickResolveToPackage() - 快速解析包前缀
      └─ 逐个解析剩余部分

4. 结果存储
   └─ storeResult()
      ├─ 记录绑定信息
      ├─ 检查可见性
      ├─ 报告错误（如有）
      └─ 创建限定符接收器（如适用）
```

## 可见性规则

不同位置应用不同的可见性检查：

- **导入位置** - private 符号只能在同一文件内导入
- **类型/表达式位置** - 标准可见性规则（public/protected/internal/private）
- **跨模块** - 检查模块间的友好关系（friend module）

## 错误诊断

解析器会报告以下错误：

- `UNRESOLVED_REFERENCE` - 无法解析的引用
- `INVISIBLE_REFERENCE` - 不可见的引用（可见性冲突）
- `AMBIGUOUS_REFERENCE_TARGET` - 歧义引用（多个候选）
- `CANNOT_BE_IMPORTED` - 无法导入的符号（如成员函数）
- `CANNOT_ALL_UNDER_IMPORT_FROM_ENUM` - 禁止从枚举类全导入（所有类都不能使用 `.*` 全导入）
- `MODULE_PACKAGE_CANNOT_BE_IMPORTED` - 禁止导入模块名包
- `ENUM_ENTRY_AS_TYPE` - 枚举条目不能作为类型使用

## 性能优化

- **快速包解析** - 使用贪心算法批量匹配包前缀
- **导入路径优先** - 优先通过导入作用域解析，避免全局搜索
- **短路评估** - 在表达式位置，发现值立即返回，不继续解析类型
- **缓存利用** - 通过 BindingContext 缓存解析结果

## 与其他组件的关系

- **TypeResolver** - 类型解析器，处理类型构造和泛型参数
- **BindingContext/BindingTrace** - 存储解析结果和诊断信息
- **LexicalScope** - 词法作用域，提供名称查找功能
- **ModuleDescriptor** - 模块描述符，提供包查找功能
- **CallResolver** - 调用解析器，处理成员调用和重载解析

## 使用示例

### 解析类型引用
```kotlin
val resolver = QualifiedExpressionResolver(languageVersionSettings)
val result = resolver.resolveDescriptorForType(
    userType,
    scope,
    trace,
    isDebuggerContext = false
)
val classDescriptor = result.classifierDescriptor
```

### 解析导入语句
```kotlin
val importScope = resolver.processImportReference(
    importDirective,
    module,
    trace,
    excludedNames,
    packageFragment
)
```

### 解析表达式中的限定符
```kotlin
val callChain = resolver.resolveQualifierInExpressionAndUnroll(
    qualifiedExpression,
    context,
    isValue = { expr -> /* 检查是否为值 */ }
)
```

## 参考资料

- [QualifiedExpressionResolver](QualifiedExpressionResolver.kt) - 解析器核心实现
- [QualifiedExpressionResolveUtil](QualifiedExpressionResolveUtil.kt) - 解析工具类
- [QualifierPart](QualifierPart.kt) - 限定符部分数据类
- [QualifierPosition](QualifierPosition.kt) - 位置枚举和可见性
- [QualifiedExpressionExtensions](QualifiedExpressionExtensions.kt) - 扩展函数
- [DebugModeSupport](DebugModeSupport.kt) - 调试支持
- [TypeResolver](../TypeResolver.kt) - 类型解析器
- [LexicalScope](../scopes/LexicalScope.kt) - 词法作用域


## 解析策略

限定表达式解析器采用**贪心策略**，从左到右尽可能多地识别限定符：

### 示例 1：包 + 类 + 成员
```kotlin
com.example.MyClass.foo()
-> 限定符: com.example.MyClass
-> 成员调用: foo()
```

### 示例 2：包 + 包 + 类
```kotlin
a.b.c.D
-> 限定符: a.b.c（包）
-> 类名: D
```

### 示例 3：类 + 嵌套类
```kotlin
OuterClass.InnerClass
-> 限定符: OuterClass
-> 嵌套类: InnerClass
```

## 位置敏感解析

解析行为根据限定名称出现的位置而变化：

### 包声明（PACKAGE_HEADER）
```kotlin
package com.example
```
只验证包路径的有效性。

### 导入语句（IMPORT）
```kotlin
import com.example.Foo
```
解析包、类、类型别名，检查可见性，禁止导入成员函数/变量。

### 类型位置（TYPE）
```kotlin
var x: com.example.Foo
```
只解析类型（类、接口、类型别名），不解析值。

### 表达式位置（EXPRESSION）
```kotlin
com.example.Foo.bar()
```
值优先于类型，需要区分限定符和成员访问。

## 特殊处理

- **IDE 模式** - 支持 `_root_ide_package_` 前缀避免解析歧义
- **调试模式** - 在调试器上下文中禁用某些诊断检查
- **导入路径解析** - 优先通过导入路径解析，提高 IDE 补全性能
- **废弃标记** - 跟踪并报告通过废弃路径访问的符号
- **模块名限定** - 禁止使用模块名作为限定符前缀

## 解析流程

```
1. 解析入口
   ├─ resolveDescriptorForType()        // 类型位置
   ├─ resolveQualifierInExpressionAndUnroll()  // 表达式位置
   ├─ processImportReference()          // 导入语句
   └─ resolvePackageHeader()            // 包声明

2. 路径提取
   └─ mapToQualifierParts()
      将嵌套的限定表达式展开为线性路径

3. 前缀解析
   └─ resolveToPackageOrClassPrefix()
      ├─ 检查 IDE 模式前缀
      ├─ 检查第一部分是否为值（表达式位置）
      ├─ 在词法作用域中查找类型
      ├─ quickResolveToPackage() - 快速解析包前缀
      └─ 逐个解析剩余部分

4. 结果存储
   └─ storeResult()
      ├─ 记录绑定信息
      ├─ 检查可见性
      ├─ 报告错误（如有）
      └─ 创建限定符接收器（如适用）
```

## 可见性规则

不同位置应用不同的可见性检查：

- **导入位置** - private 符号只能在同一文件内导入
- **类型/表达式位置** - 标准可见性规则（public/protected/internal/private）
- **跨模块** - 检查模块间的友好关系（friend module）

## 错误诊断

解析器会报告以下错误：

- `UNRESOLVED_REFERENCE` - 无法解析的引用
- `INVISIBLE_REFERENCE` - 不可见的引用（可见性冲突）
- `AMBIGUOUS_REFERENCE_TARGET` - 歧义引用（多个候选）
- `CANNOT_BE_IMPORTED` - 无法导入的符号（如成员函数）
- `CANNOT_ALL_UNDER_IMPORT_FROM_ENUM` - 禁止从枚举类全导入（所有类都不能使用 `.*` 全导入）
- `MODULE_PACKAGE_CANNOT_BE_IMPORTED` - 禁止导入模块名包
- `ENUM_ENTRY_AS_TYPE` - 枚举条目不能作为类型使用

## 性能优化

- **快速包解析** - 使用贪心算法批量匹配包前缀
- **导入路径优先** - 优先通过导入作用域解析，避免全局搜索
- **短路评估** - 在表达式位置，发现值立即返回，不继续解析类型
- **缓存利用** - 通过 BindingContext 缓存解析结果

## 与其他组件的关系

- **TypeResolver** - 类型解析器，处理类型构造和泛型参数
- **BindingContext/BindingTrace** - 存储解析结果和诊断信息
- **LexicalScope** - 词法作用域，提供名称查找功能
- **ModuleDescriptor** - 模块描述符，提供包查找功能
- **CallResolver** - 调用解析器，处理成员调用和重载解析

## 使用示例

### 解析类型引用
```kotlin
val resolver = QualifiedExpressionResolver(languageVersionSettings)
val result = resolver.resolveDescriptorForType(
    userType,
    scope,
    trace,
    isDebuggerContext = false
)
val classDescriptor = result.classifierDescriptor
```

### 解析导入语句
```kotlin
val importScope = resolver.processImportReference(
    importDirective,
    module,
    trace,
    excludedNames,
    packageFragment
)
```

### 解析表达式中的限定符
```kotlin
val callChain = resolver.resolveQualifierInExpressionAndUnroll(
    qualifiedExpression,
    context,
    isValue = { expr -> /* 检查是否为值 */ }
)
```

## 参考资料

- [QualifiedExpressionResolver](QualifiedExpressionResolver.kt) - 解析器核心实现
- [QualifiedExpressionResolveUtil](QualifiedExpressionResolveUtil.kt) - 解析工具类
- [TypeResolver](../TypeResolver.kt) - 类型解析器
- [LexicalScope](../scopes/LexicalScope.kt) - 词法作用域


 