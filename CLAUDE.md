# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个为 IntelliJ 平台开发的仓颉语言插件，采用多模块 Gradle 架构。项目包含 22 个子模块，从 PSI 解析到调试器支持的完整语言工具链。

**关键技术栈**:

- Kotlin 2.2.0 + Gradle Kotlin DSL
- IntelliJ Platform SDK (支持版本 242-253)
- JFlex 词法分析器生成
- 手写递归下降语法分析器

## 常用开发命令

### 构建和运行

```bash
# 完整构建项目
./gradlew build

# 在 IDE 沙箱中运行插件（开发测试）
./gradlew :plugin:runIde

# 构建插件分发包
./gradlew :plugin:buildPlugin

# 验证插件兼容性
./gradlew :plugin:verifyPlugin

# 清理构建产物
./gradlew clean
```

### 词法分析器生成

**重要**: 修改 `.flex` 文件后必须重新生成词法分析器

```bash
# 生成所有词法分析器
./gradlew :psi:generateLexers

# 单独生成主词法分析器
./gradlew :psi:generateCangJieLexer

# 单独生成文档注释词法分析器
./gradlew :psi:generateCDocLexer
```

生成的文件位置: `psi/src/gen/org/cangnova/cangjie/lexer/`

### 测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :psi:test
./gradlew :cangjie-project:test
./gradlew :debugger:test

# 运行特定测试类
./gradlew test --tests "org.cangnova.cangjie.psi.CjPsiFactoryTest"

# 显示测试输出（调试用）
./gradlew test -PshowStandardStreams=true

# CI 模式（启用失败重试）
CI=true ./gradlew test
```

### 编译特定模块

```bash
# 编译核心模块
./gradlew :compileKotlin

# 编译 PSI 模块（依赖 Lexer 生成）
./gradlew :psi:compileKotlin

# 编译调试器模块
./gradlew :debugger:common:compileKotlin
./gradlew :debugger:protobuf:compileKotlin
./gradlew :debugger:dap:compileKotlin
```

## 架构设计

### 模块分层架构

```
plugin (分发层)
  ↓
lsp4ij, debugger, cjpm, cangjie-project (功能层)
  ↓
psi, analysis, descriptors, metadata (核心语言层)
  ↓
util, common, icon, messages, notifications (基础设施层)
```

### 核心模块职责

**psi 模块** - 程序结构接口

- 词法分析: `CangJieLexer.flex` → JFlex 生成 → `CangJieLexer.java`
- 语法分析: `CangJieParsing.kt` (4600+ 行手写递归下降解析器)
- PSI 树构建: `CangJieParserDefinition.kt`
- Stub 索引: 通过 `CjStubElementTypes` 加速符号解析
- 关键文件:
    - `psi/src/main/kotlin/org/cangnova/cangjie/parsing/CangJieParsing.kt`
    - `psi/src/main/kotlin/org/cangnova/cangjie/psi/CjElement.kt`
    - `psi/src/main/kotlin/org/cangnova/cangjie/psi/stubs/`

**cangjie-project 模块** - 项目管理

- 项目模型: `CjProject`, `CjModule`, `CjLibrary`
- 依赖解析: `CjDependencyResolver`
- Workspace Model 集成
- 项目同步任务: `CangJieSyncTask`

**cjpm 模块** - 包管理器集成

- TOML 配置解析: `CjpmTomlConfig`
- 依赖解析: `CjpmDependencyResolver`
- 包管理: `CjpmPackageManager`
- 仓库提供: `CjpmRepositoryProvider`

**debugger 模块** - 调试支持

- `debugger:common` - 公共调试基础设施
- `debugger:dap` - Debug Adapter Protocol 实现
- `debugger:protobuf` - Protobuf 协议调试支持

**lsp4ij 模块** - LSP 客户端

- LSP 服务器管理: `CangJieLspServerManager`
- 进程连接: `CangJieOSProcessStreamConnectionProvider`
- 依赖 RedHat LSP4IJ 插件 (版本 0.18.0)

**analysis 模块** - 代码分析

- 语义分析和类型推导
- `decompiler-to-psi` 子模块: 反编译器支持
- 内置库虚拟文件: `BuiltinsVirtualFileProvider`

### 关键工作流

#### 文件解析流程

```
.cj 源文件
  ↓
CangJieLexer (词法分析) → Token 流
  ↓
CangJieParser (语法分析) → AST
  ↓
ParserDefinition.createElement() → PSI Tree
  ↓
Stub Index 构建 (符号索引)
  ↓
IDE 服务 (高亮、补全、导航等)
```

#### 插件加载流程

```
IDE 启动
  ↓
加载 plugin.xml (包含 xi:include 引用)
  ↓
加载所有 cangjie-*.xml 配置 (30+ 个模块化配置)
  ↓
注册扩展点和服务
  ↓
执行 CangJieStartupActivity
  ↓
项目打开 → CjpmProjectProvider 解析项目
  ↓
构建索引 → 准备就绪
```

## 代码生成和配置

### JFlex 词法分析器

**定义文件**: `psi/src/main/kotlin/org/cangnova/cangjie/lexer/CangJieLexer.flex`

**生成配置**: 使用 GrammarKit 插件的 `GenerateLexerTask`

**重要**: 编译任务自动依赖 Lexer 生成:

```kotlin
tasks.compileKotlin { dependsOn("generateLexers") }
tasks.compileJava { dependsOn("generateLexers") }
```

### 手写语法分析器

**核心文件**: `psi/src/main/kotlin/org/cangnova/cangjie/parsing/CangJieParsing.kt`

修改语法分析逻辑:

1. 直接编辑 `CangJieParsing.kt`
2. 无需运行生成任务
3. 重新编译即可

### Stub 索引系统

**作用**: 加速符号解析，避免完整解析大文件

**配置位置**: `src/main/resources/META-INF/cangjie-stubindex.xml`

**关键索引**:

- `CangJieClassShortNameIndex` - 类名索引
- `CangJieFunctionShortNameIndex` - 函数名索引
- `CangJieTopLevelFunctionByPackageIndex` - 包级函数索引
- `CangJieSuperClassIndex` - 继承关系索引

**添加新索引**:

1. 创建索引类继承 `StringStubIndexExtension`
2. 在 `cangjie-stubindex.xml` 中注册
3. 创建对应的 `StubElementType`

## 插件配置系统

### XML 模块化配置

**主入口**: `plugin/src/main/resources/META-INF/plugin.xml`

**配置聚合**: `src/main/resources/META-INF/cangjie-all.xml` 通过 XInclude 引入 30+ 个子配置

**关键配置文件**:

- `cangjie-core.xml` - 核心服务和启动活动
- `cangjie-psi.xml` - PSI 定义、折叠、括号匹配
- `cangjie-highlighting.xml` - 语法高亮配置
- `cangjie-stubindex.xml` - Stub 索引注册
- `cangjie-actions.xml` - 菜单和快捷键
- `cangjie-run.xml` - 运行配置
- `cangjie-debugger.xml` - 调试器集成
- `cangjie-extensionPoints.xml` - 自定义扩展点

### 扩展点和服务

**应用级服务** (全局单例):

```xml

<applicationService
        serviceInterface="..."
        serviceImplementation="..."/>
```

**项目级服务** (每个项目一个实例):

```xml

<projectService
        serviceInterface="..."
        serviceImplementation="..."/>
```

**自定义扩展点**:

```xml

<extensionPoint qualifiedName="org.cangnova.cangjie.toolchainProvider"
                interface="org.cangnova.cangjie.toolchain.CjToolchainProvider"
                dynamic="true"/>
```

## 多版本支持

### 平台版本配置

项目支持 IntelliJ Platform 242-253，通过不同的 `gradle-*.properties` 文件配置:

```
gradle.properties          # 默认配置 (platformVersion=242)
gradle-242.properties      # IDE 2024.2 特定配置
gradle-243.properties      # IDE 2024.3 特定配置
gradle-251.properties      # IDE 2025.1 特定配置
gradle-252.properties      # IDE 2025.2 特定配置
gradle-253.properties      # IDE 2025.3 特定配置
```

### 编译特定版本

```bash
# 编译 IDE 242 版本
./gradlew build -PplatformVersion=242

# 编译 IDE 253 版本
./gradlew build -PplatformVersion=253
```

### 版本特定源码

可以在模块中创建版本特定的源码目录:

```
src/main/242/     # 仅用于 IDE 242
src/main/253/     # 仅用于 IDE 253
```

## 开发工作流建议

### 添加新的 PSI 元素

1. 在 `CjNodeTypes` 或 `CjStubElementTypes` 中定义元素类型
2. 在 `CangJieParsing.kt` 中添加解析逻辑
3. 创建 PSI 类实现 (继承 `CjElement`)
4. 如需索引加速，创建对应的 Stub 类
5. 在 `ParserDefinition.createElement()` 中处理新类型

### 修改词法规则

1. 编辑 `psi/src/main/kotlin/org/cangnova/cangjie/lexer/CangJieLexer.flex`
2. 运行 `./gradlew :psi:generateLexers`
3. 检查生成的 `psi/src/gen/.../CangJieLexer.java`
4. 重新编译和测试

### 添加新的语言功能

1. **语法扩展**: 修改 `CangJieParsing.kt` 添加解析逻辑
2. **语义分析**: 在 `analysis` 模块中添加类型推导和检查
3. **IDE 功能**: 在相应配置文件中注册扩展
    - 代码补全: 实现 `CompletionContributor`
    - 引用解析: 实现 `PsiReferenceContributor`
    - 代码检查: 实现 `LocalInspectionTool`
4. **测试**: 在 `src/test/` 中添加单元测试

### 调试插件

```bash
# 启动调试模式的 IDE
./gradlew :plugin:runIde

# 在 IDE 中设置断点，然后从 IDE 中调试运行
# Help → Diagnostic Tools → Debug Log Settings
# 添加: #org.cangnova.cangjie
```

**查看内部日志**:

- Windows: `%APPDATA%\JetBrains\<IDE>\log\idea.log`
- Linux: `~/.local/share/JetBrains/<IDE>/log/idea.log`
- macOS: `~/Library/Logs/JetBrains/<IDE>/idea.log`

## 国际化

### 消息 Bundle

**位置**: `src/main/resources/messages/`

**主要 Bundle**:

- `CangJieBundle.properties` - 主要 UI 消息
- `CangJieAnalysisBundle.properties` - 代码分析消息
- `CjBuildBundle.properties` - 构建相关消息
- `CangJieParsingBundle.properties` - 解析错误消息

**使用方式**:

```kotlin
import org.cangnova.cangjie.CangJieBundle

val message = CangJieBundle.message("key.in.properties")
val formatted = CangJieBundle.message("key.with.param", arg1, arg2)
```

## 性能优化要点

### Lazy Parsing

Parser 使用延迟解析策略:

```kotlin
CangJieParsing.createForTopLevel(builder)  // isLazy = true
```

### Stub 索引使用

频繁访问的符号应该通过 Stub 索引:

```kotlin
CangJieClassShortNameIndex.getInstance()
    .get(className, project, scope)
```

避免直接遍历 PSI 树。

### 缓存机制

利用 IntelliJ 的缓存系统:

```kotlin
CachedValuesManager.getCachedValue(element) {
    CachedValueProvider.Result.create(computation(), dependencies)
}
```

## 项目结构约定

### 包命名

- 核心包: `org.cangnova.cangjie`
- PSI: `org.cangnova.cangjie.psi`
- IDE 功能: `org.cangnova.cangjie.ide.*`
- 项目管理: `org.cangnova.cangjie.project`
- 调试器: `org.cangnova.cangjie.debugger`

### 资源文件

```
src/main/resources/
├── META-INF/              # 插件配置
├── messages/              # 国际化消息
├── icons/                 # 图标资源
├── fileTemplates/         # 文件模板
├── intentionDescriptions/ # 意图描述
├── inspectionDescriptions/# 检查描述
└── liveTemplates/         # 实时模板
```

## 依赖管理

### 关键依赖

- **IntelliJ Platform**: 通过 `intellijPlatform` 配置块
- **Kotlin**: 2.2.0
- **LSP4J**: 0.21.0 (Debug Adapter Protocol)
- **RedHat LSP4IJ**: 0.18.0 (LSP 客户端插件)
- **Jackson**: 2.15.2 (TOML/JSON 解析)

### 添加依赖

在对应模块的 `build.gradle.kts` 中:

```kotlin
dependencies {
    implementation(project(":moduleName"))
    intellijPlatform {
        plugins("com.redhat.devtools.lsp4ij:0.18.0")
    }
}
```

## 常见问题排查

### Lexer 生成失败

- 检查 `.flex` 文件语法
- 确保 JFlex 版本兼容 (项目使用 1.9.2)
- 查看 `psi/build/` 目录下的生成日志

### 编译错误

- 清理缓存: `./gradlew clean`
- 删除 `.gradle/` 和 `build/` 目录
- 重新导入 Gradle 项目

### 插件无法加载

- 检查 `plugin.xml` 中的 `<id>` 和 `<dependencies>`
- 验证所有 `xi:include` 引用的文件都存在
- 运行 `./gradlew :plugin:verifyPlugin`

### 测试失败

- 检查是否在无头模式: `java.awt.headless=true`
- 确保测试资源文件位于 `src/test/resources/`
- 使用 `-PshowStandardStreams=true` 查看详细输出