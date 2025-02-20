# Cangjie language plugin for intellij platform

## 适用于intellij平台的仓颉语言插件

[从 Jetbrains Marketplace 获取](https://plugins.jetbrains.com/plugin/24984-cangjie)

### 仓库

[码云 ![star](https://gitee.com/Lin_Qing_Ying/intellij-cangjie/badge/star.svg?theme=dark)](https://gitee.com/Lin_Qing_Ying/intellij-cangjie)

[GitCode Cangjie-SIG ![star](https://gitcode.com/Cangjie-SIG/intellij-cangjie/star/badge.svg)](https://gitcode.com/Cangjie-SIG/intellij-cangjie)

[GitCode 开源仓颉第三方开发者社区 ![star](https://gitcode.com/OpenCangjieCommunity/intellij-cangjie/star/badge.svg)](https://gitcode.com/OpenCangjieCommunity/intellij-cangjie)

[CangNova  基于Intellij集成的 IDE ![star](https://gitcode.com/OpenCangjieCommunity/CangNova/star/badge.svg)](https://gitcode.com/OpenCangjieCommunity/CangNova)

### 安装和使用

```
在intellij平台的插件界面， 搜索CangJie 安装即可
```

![img_2.png](img%2Fimg_2.png)
要创建项目，请使用 **CangJie** 模板。

### 支持的功能

| 功能   | 状态 | 说明                                            |
|------|------|-----------------------------------------------|
| 语法解析 | ✓    | 支持基本语法解析，包括类、函数、变量声明等                         |
| 语法错误检查 | ±    | 实时检测语法错误，包括：<br>- 导入语句检查<br>- 修饰符检查<br>- 包名检查 |
| 语法高亮 | ✓    | 支持两级语法高亮：<br>- 基本词法高亮<br>- 语义分析后的智能高亮         |
| 代码补全 | ±    | 部分支持，计划完善                                     |
| 代码格式化 | ✓    | 支持                                            |
| 重构工具 | ±    | 部分支持，计划完善                                     |
| 调试支持 | ✓    | 支持DAP调试，计划支持lldb调试                            |
| 运行目标 | ±    | 部分支持                                          |
| LSP  | ✓    | 支持                                            |

说明：
- ✓ 表示已实现
- ± 表示部分支持
- \- 表示计划支持

### 贡献

如果您发现任何问题或缺少功能，欢迎您为插件做出贡献。


<br>
感谢您的支持

# 前端分析器

**随时添加**

## 快速修复

### 已实现修复

| 功能                     |
|------------------------|
| class类型添加 abstract 关键字 |
| 类型声明快速生成成员抽象方法         |
| 扩展类型快速生成成员抽象方法         |
| 为解析引用的导入               |

### 未实现修复

| 功能          |
|-------------|
| 创建class     |
| 创建struct    |
| 创建interface |
| 创建顶层函数声明    |
| 创建成员函数声明    |
| 创建顶层变量声明    |
| 创建成员变量声明    |
| 修饰符快速添加     |

## 分析器

### 已实现分析

| 功能              | 
|-----------------|  
| 顶层重复声明          |
| 类型声明            |
| 为解析引用           |
| 抽象方法实现与抽象类型     |
| 扩展的抽象方法实现       |
| 导入检查            |
| 包名检查            |
| 类型别名声明          |
| 访问控制检查          |
| 方法参数检查          |
| 数值常量检查          |
| 字符常量检查          |
| 类型检查            |
| 类型边界检查          |
| 超类型与泛型检查        |
| where边界检查       |
| 表达式解析      (部分) |
| 类型推导            |
| 字符串常量检查         |
| 命名方法检查          |
| 返回值检查           |

### 未实现分析

| 功能                      |
|-------------------------|
| 宏的解析                    |
| 被宏修饰的声明解析               |
| 类型声明的作用域检查（部分）          |
| 扩展类型的作用域检查（主要在于private） |

## 引用与重构

| 功能   | 
|------|  
| 转到声明 |
| 查找用法 |

## IDE功能

| 功能       |
|----------|
| 关键字补全触发  |
| 重写方法补全触发 |
| 代码格式化    |
| 代码折叠     |

## 编辑器配色

| 功能   |
|------|
| 代码高亮 |

## 致谢
 
- [Kotlin](https://github.com/JetBrains/kotlin) - Apache License 2.0
  - Kotlin 编程语言为本插件提供了强大的语言支持和开发工具。
  - 本项目的部分代码基于或改编自Kotlin源代码。
  - Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
  - Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  - 完整的许可证文本可在 [licenses/LICENSE-KOTLIN](licenses/LICENSE-KOTLIN) 中找到。
 
- [intellij-kotlin](https://github.com/JetBrains/intellij-kotlin) - Apache License 2.0
 