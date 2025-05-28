# 仓颉格式化器实现说明

## 核心类说明

### 基础构建类
1. `CangJieFormattingModelBuilder`
   - 格式化器的入口点
   - 创建格式化模型
   - 连接各个格式化组件

2. `CangJieBlock` & `CangJieCommonBlock`
   - 代码块的抽象表示
   - 处理缩进、对齐和换行
   - 维护AST节点的格式化信息

3. `CangJieSpacingBuilder`
   - 控制代码元素之间的空格
   - 定义空格规则
   - 处理特殊语法结构的空格

### 缩进控制
1. `NodeIndentStrategy`
   - 定义节点的缩进策略
   - 处理不同类型节点的缩进规则
   - 支持自定义缩进行为

2. `CangJieLineIndentProvider`
   - 提供行级别的缩进计算
   - 处理多行代码的缩进
   - 支持自动缩进功能

### 代码风格设置
1. `CangJieCodeStyleSettings`
   - 存储代码风格配置
   - 定义可配置的格式化选项
   - 管理用户自定义设置

2. `CangJieStyleGuideCodeStyle`
   - 实现标准代码风格指南
   - 提供默认格式化规则
   - 确保代码一致性

### 对齐策略
1. `NodeAlignmentStrategy`
   - 控制代码元素的对齐
   - 处理多行声明的对齐
   - 支持自定义对齐规则

### 特殊处理器
1. `CangJiePreFormatProcessor`
   - 格式化前的预处理
   - 准备AST节点
   - 处理特殊格式化需求

2. `TrailingCommaPostFormatProcessor`
   - 处理尾随逗号
   - 确保列表格式的一致性
   - 支持不同风格的逗号处理

## 缩进控制实现

### 基本原则
1. 缩进计算
   ```kotlin
   // NodeIndentStrategy中的缩进计算
   fun calculateIndent(node: ASTNode, indent: Indent): Int {
       // 基础缩进逻辑
       return when (node.elementType) {
           BLOCK -> indent.getIndentSize()
           STATEMENT -> indent.getIndentSize() + STATEMENT_INDENT
           else -> indent.getIndentSize()
       }
   }
   ```

2. 特殊情况处理
   - 多行表达式
   - 链式调用
   - 参数列表
   - 数组/集合初始化

### 缩进规则配置
1. 基础设置
   ```kotlin
   // CangJieCodeStyleSettings中的配置
   class CangJieCodeStyleSettings {
       var CONTINUATION_INDENT_SIZE: Int = 4
       var INDENT_SIZE: Int = 4
       var USE_TAB_CHARACTER: Boolean = false
   }
   ```

2. 自定义规则
   - 函数参数对齐
   - 链式调用对齐
   - 多行表达式对齐

## 使用示例

### 1. 基本缩进
```cangjie
func test() {
    statement1
    statement2
}
```

### 2. 链式调用
```cangjie
someObject
    .method1()
    .method2()
    .method3()
```

### 3. 参数对齐
```cangjie
func longFunction(
    param1: Type1,
    param2: Type2,
    param3: Type3
) {
    // 函数体
}
```

## 扩展和自定义

### 添加新的缩进规则
1. 在`NodeIndentStrategy`中添加新的处理逻辑
2. 更新`CangJieCodeStyleSettings`中的配置选项
3. 在`CangJieBlock`中实现新规则的应用

### 修改现有规则
1. 调整`CangJieSpacingBuilder`中的空格规则
2. 更新`NodeAlignmentStrategy`中的对齐策略
3. 修改`CangJieStyleGuideCodeStyle`中的默认设置

## 注意事项
1. 性能考虑
   - 缓存计算结果
   - 避免重复遍历
   - 优化大文件处理

2. 兼容性
   - 处理旧版本格式
   - 支持不同编辑器设置
   - 考虑特殊语法结构

3. 可扩展性
   - 模块化设计
   - 清晰的接口定义
   - 易于添加新规则 