# 项目结构说明

## 项目概述

intellij-cangjie 是一个为 IntelliJ 平台开发的仓颉语言插件，为仓颉语言提供完整的 IDE 支持，包括语法高亮、代码补全、调试、LSP
集成等功能。

## 模块架构

项目采用多模块架构，各模块职责清晰，便于维护和扩展。

### 核心语言支持模块

#### psi

**程序结构接口（Program Structure Interface）**

- 负责仓颉语言的 PSI 树构建和管理
- 包含词法分析器（Lexer）和语法分析器（Parser）生成
- 定义语言元素的 PSI 节点类型
- 提供代码结构的抽象语法树表示

#### analysis

**代码分析模块**

- 提供语义分析和类型推导功能
- 包含 `decompiler-to-psi` 子模块，负责反编译到 PSI 的转换
- 实现代码检查和智能高亮
- 支持跨模块的引用解析

#### descriptors

**描述符系统**

- 定义语言元素的描述符（Descriptor）
- 包含 `deserialization` 子模块，负责描述符的序列化和反序列化
- 提供类型系统的抽象表示
- 支持符号解析和作用域管理

#### metadata

**元数据管理**

- 处理编译后的元数据信息
- 支持库和依赖的元数据读取
- 提供二进制格式的元数据解析

### 项目管理模块

#### cangjie-project

**仓颉项目支持**

- 提供仓颉项目的创建、导入和管理功能
- 管理项目结构和模块配置
- 集成 Workspace Model API
- 提供项目依赖管理和同步
- 包含项目视图和工具窗口

#### cjpm

**仓颉包管理器集成**

- 集成 cjpm（CangJie Package Manager）
- 处理 cjpm.toml 配置文件
- 管理依赖下载和解析
- 提供包仓库访问

### 调试模块

#### debugger

**调试器核心**

- 提供仓颉程序的调试支持
- 包含以下子模块：
    - `common`: 调试器公共组件
    - `protobuf`: 基于 Protobuf 协议的调试实现
    - `dap`: Debug Adapter Protocol 支持

### 工具链和 LSP 模块

#### toolchain

**工具链集成**

- 管理仓颉编译器和工具链版本
- 提供工具链的下载和配置
- 支持多版本工具链切换

#### lsp4ij

**LSP 客户端支持**

- 集成 Language Server Protocol
- 提供与仓颉语言服务器的通信
- 支持 LSP 特性：跳转、补全、重命名等

### 工具和资源模块

#### util

**工具类库**

- 提供通用工具函数和扩展方法
- 包含项目级别的帮助类
- 提供跨模块共享的实用功能

#### common

**公共模块**

- 定义跨模块共享的接口和抽象
- 提供基础数据结构和常量定义
- 包含公共的扩展功能

#### icon

**图标资源**

- 管理插件使用的所有图标资源
- 提供统一的图标访问接口
- 包含仓颉语言和文件类型的图标

#### messages

**国际化消息**

- 提供插件的国际化支持
- 管理多语言资源文件
- 定义消息 Bundle 访问接口

#### notifications

**通知系统**

- 提供用户通知和提示功能
- 管理各类通知的显示和交互
- 支持气泡通知和工具窗口通知

### 其他模块

#### telemetry

**遥测数据收集**

- 收集插件使用统计信息
- 提供用户行为分析支持
- 管理数据上报和隐私控制

#### plugin

**插件配置**

- 管理插件的整体配置
- 包含插件描述和依赖定义
- 提供插件入口点配置

#### test-common

**测试公共模块**

- 提供测试基础设施
- 包含测试工具类和 Mock 对象
- 支持跨模块的测试共享

## 构建配置

### 根目录配置文件

- `build.gradle.kts` - Gradle 主构建脚本
- `settings.gradle.kts` - 多模块项目配置
- `gradle.properties` - Gradle 属性配置
- `gradle-*.properties` - 针对不同平台版本的配置

### 语法定义

- `CangJie.g4` - ANTLR 语法定义文件
- `idea-flex.skeleton` - JFlex 词法分析器骨架文件

### 其他目录

- `.github` - GitHub Actions 工作流配置
- `config` - 代码风格和检查配置
- `docs` - 项目文档
- `licenses` - 第三方许可证
- `external` - 外部依赖

## 技术栈

- **语言**: Kotlin
- **构建工具**: Gradle (Kotlin DSL)
- **平台**: IntelliJ Platform SDK
- **词法/语法**: JFlex + Grammar-Kit / ANTLR
- **调试协议**: DAP (Debug Adapter Protocol)
- **LSP**: Language Server Protocol
- **版本控制**: Git

## 模块依赖关系

```
plugin (主模块)
├── psi (语言基础)
├── analysis (依赖 psi, descriptors, metadata)
├── descriptors (依赖 psi)
│   └── deserialization
├── metadata
├── cangjie-project (依赖 psi, analysis)
├── cjpm (依赖 cangjie-project)
├── debugger
│   ├── common
│   ├── protobuf (依赖 common)
│   └── dap (依赖 common)
├── toolchain
├── lsp4ij
├── telemetry
└── 工具模块
    ├── util
    ├── common
    ├── icon
    ├── messages
    └── notifications
```

## 支持的 IntelliJ 平台版本

项目从最新版本向下支持 4 个版本，通过 `gradle-*.properties` 配置不同平台版本的兼容性。

## 开发和贡献

详细的开发指南和贡献说明请参考项目根目录的 `README.md` 文件。