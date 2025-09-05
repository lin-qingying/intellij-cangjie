# 仓颉语言高亮系统设计文档

## 1. 系统概述

仓颉语言高亮系统是一个多层次的代码高亮处理框架，用于为仓颉语言提供语法高亮支持。该系统基于 IntelliJ Platform，实现了从词法分析到语义高亮的完整处理流程。

### 1.1 主要特点

- 多层次高亮处理
- 可扩展的访问器架构
- 灵活的颜色配置
- 支持插件化扩展

### 1.2 核心组件

```
highlighter/
├── CangJieHighlighter.kt              # 基础词法高亮器
├── CangJieBeforeResolveHighlightingPass.kt  # 高亮处理器
├── BeforeResolveHighlightingVisitor.kt      # 具体访问器
├── HighlightingFactory.kt             # 高亮信息工厂
├── CangJieHighlightingColors.kt       # 颜色定义
└── CangJieHighlightInfoTypeSemanticNames.kt # 语义名称定义
```

## 2. 高亮处理流程

### 2.1 词法高亮阶段

由 `CangJieHighlighter` 处理基础词法标记：

- 关键字 (let, var, const 等)
- 运算符 (+, -, *, / 等)
- 标点符号 ({}, [], () 等)
- 字符串和注释

### 2.2 语法高亮阶段

由 `CangJieBeforeResolveHighlightingPass` 和相关访问器处理：

- 函数声明和调用
- 类型引用
- 变量声明和使用
- 特殊语言构造

### 2.3 语义高亮阶段

处理上下文相关的高亮：

- 未解析的引用
- 类型推导结果
- 错误标记
- 警告提示

## 3. 组件详解

### 3.1 CangJieHighlighter

基础词法高亮器，负责：
```kotlin
class CangJieHighlighter : SyntaxHighlighterBase() {
    // 处理基础词法标记
    // 管理高亮颜色映射
    // 提供词法分析器
}
```

### 3.2 CangJieBeforeResolveHighlightingPass

高亮处理协调器，负责：
```kotlin
class CangJieBeforeResolveHighlightingPass {
    // 协调访问器工作
    // 管理扩展点
    // 处理高亮信息
}
```

### 3.3 BeforeResolveHighlightingVisitor

具体访问器，负责：
```kotlin
class BeforeResolveHighlightingVisitor {
    // 访问具体语法元素
    // 创建高亮信息
    // 处理特殊情况
}
```

### 3.4 HighlightingFactory

高亮信息工厂，负责：
```kotlin
object HighlightingFactory {
    // 创建高亮信息
    // 配置高亮属性
    // 管理高亮范围
}
```

## 4. 颜色配置系统

### 4.1 基础颜色定义

```kotlin
class CangJieHighlightingColors {
    // 定义各类语法元素的默认颜色
    // 支持明暗主题切换
    // 提供颜色自定义接口
}
```

### 4.2 语义名称定义

```kotlin
class CangJieHighlightInfoTypeSemanticNames {
    // 定义语法元素的语义名称
    // 关联颜色属性
    // 提供描述信息
}
```

## 5. 扩展机制

### 5.1 访问器扩展

```kotlin
interface BeforeResolveHighlightingExtension {
    // 创建自定义访问器
    // 处理特定语言特性
    // 集成到现有系统
}
```

### 5.2 颜色方案扩展

```kotlin
class CangJieColorSettingsPage {
    // 自定义颜色方案
    // 配置界面支持
    // 主题集成
}
```

## 6. 使用示例

### 6.1 基本语法高亮

```cangjie
fun test(x: Int): String {
    let y = x + 1
    return "Result: $y"
}
```

### 6.2 特殊语言特性

```cangjie
@derive["Debug"]
struct Point {
    x: Int,
    y: Int
}
```

## 7. 最佳实践

### 7.1 高亮规则

- 保持视觉层次清晰
- 避免过度使用高亮
- 确保颜色对比度适当

### 7.2 性能优化

- 使用增量更新
- 避免重复创建对象
- 优化访问器逻辑

### 7.3 可维护性

- 遵循命名规范
- 添加完整注释
- 保持代码模块化

## 8. 常见问题

### 8.1 故障排除

- 高亮不生效的常见原因
- 性能问题诊断
- 扩展点冲突解决

### 8.2 配置调优

- 颜色方案优化
- 性能参数调整
- 自定义规则建议

## 9. 未来计划

### 9.1 待实现特性

- 语义分析增强
- 实时类型推导
- 更多语言特性支持

### 9.2 改进方向

- 性能优化
- 更好的扩展性
- 更丰富的配置选项

## 10. 参考资料

- IntelliJ Platform SDK 文档
- 仓颉语言规范
- 相关设计文档

 