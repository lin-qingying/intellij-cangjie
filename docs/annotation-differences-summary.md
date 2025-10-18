# 注解位置差异分析 - 关键发现总结

## 核心发现

通过对比 C++ 编译器和 IntelliJ PSI 的实现，我们发现了一个重要事实：

**内置注解和自定义注解在允许出现的位置上确实存在显著差异。**

## 主要差异点

### 1. 导入语句限制 🚨

**最重要的差异**：
- **内置注解**：✅ 可以用于导入语句
- **自定义注解**：❌ 完全不能用于导入语句

```cangjie
// ✅ 正确：内置注解可用于导入
@Deprecated
import old.api

@When[DEBUG]
import debug.tools

// ❌ 错误：自定义注解不能用于导入
@MyCustomAnno  // 编译错误或被解析为宏
import my.module
```

### 2. Lambda 表达式限制 🚨

- **内置注解**：✅ 部分可用（仅 `@Overflow` 和 `@Mock`）
- **自定义注解**：❌ 不可用

```cangjie
// ✅ 正确：特定内置注解可用
let compute = @Overflow[Panic] { x, y -> x + y }

// ❌ 错误：自定义注解不可用
let transform = @CustomProcessor { data -> process(data) }
```

### 3. 编译时可见性语法 (`@!`) 🔍

- **内置注解**：❌ 不支持 `@!` 语法
- **自定义注解**：✅ 支持 `@!` 标记为编译时可见

```cangjie
// ❌ 错误：内置注解不能使用 @!
@!Deprecated  // 语法错误

// ✅ 正确：自定义注解可以使用 @!
@!MyCompileTimeAnno  // 编译时可见
class MyClass {}
```

### 4. 启用条件差异 ⚙️

- **内置注解**：始终启用，无需任何标志
- **自定义注解**：需要 `enableCustomAnno=true` 才能解析

当 `enableCustomAnno=false` 时，自定义注解标识符会被解析为**宏调用**！

## 位置权限总览

| 使用位置 | 内置注解 | 自定义注解 |
|---------|:--------:|:----------:|
| 顶层声明（类/函数/变量） | ✅ | ✅* |
| 导入语句 | ✅ | ❌ |
| 函数参数 | ✅ | ✅* |
| Lambda 表达式 | ⚠️ | ❌ |
| 类成员 | ✅ | ✅* |
| 属性 getter/setter | ✅ | ✅* |

*需要 `enableCustomAnno=true`

## IntelliJ PSI 实现的问题

PSI 当前实现**没有区分**内置和自定义注解，这会导致：

1. **误导性的代码提示**：IDE 可能允许在导入语句前使用自定义注解，但实际编译会失败
2. **缺少错误检查**：不会提示 Lambda 表达式上的自定义注解是错误的
3. **不支持 `@!` 语法**：无法识别编译时可见注解

## 建议的修复优先级

1. **高优先级**：
   - 实现导入语句的注解限制检查
   - 添加内置注解名称识别（已部分实现）

2. **中优先级**：
   - 实现 Lambda 表达式的注解限制
   - 支持 `@!` 编译时可见语法

3. **低优先级**：
   - 添加 `@ConstSafe` 的模块限制
   - 实现 `@Deprecated` 参数验证

## 实际影响

这些差异意味着：

1. 开发者可能在 IntelliJ 中编写了"看起来正确"但实际无法编译的代码
2. 代码迁移时可能遇到意外的编译错误
3. IDE 的代码补全和错误提示可能不准确

## 已知的内置注解列表

```kotlin
companion object {
    val BUILT_IN_ANNOTATIONS = setOf(
        "Attribute",
        "Overflow",
        "When",
        "Deprecated",
        "ConstSafe"  // 仅限 std 模块
    )
}
```

---

**结论**：自定义注解的位置限制比内置注解严格得多，这是设计上的有意区分，而不是实现缺陷。IntelliJ PSI 需要相应地更新以反映这些限制。