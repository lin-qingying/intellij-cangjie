# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 语言偏好

- 始终使用中文回复用户
- 代码注释可以使用中文或英文
- Git commit 信息使用中文

## ⚠️ 严格禁止的操作

**绝对禁止使用任何 Git 回滚命令：**

- 严禁执行 `git reset`
- 严禁执行 `git checkout -- <file>`
- 严禁执行 `git restore`
- 严禁执行任何会撤销用户手动修改的 Git 命令

**原因**：用户可能在 Claude 工作期间手动编辑文件，任何 Git 回滚操作都会导致用户的工作丢失。

**如果发现文件被修改**：这些修改可能来自用户手动编辑、IDE 自动格式化 / Linter、其他工具自动处理。**正确做法**：永远不要试图"恢复"文件到之前的状态，而是在当前状态的基础上继续工作。

## 项目概述

这是基于 IntelliJ Platform Gradle Plugin 2.x 的仓颉语言 **host plugin**（IDE 宿主插件）。
仓库**不再承载本地 PSI / Parser / Analysis 实现**——这些能力由主仓库 `..`（即 `D:\code\intellij\cangjie` 根工程）的一方模块提供，本仓库通过 `includeBuild("../")` 与 dependency substitution 接入，只负责 IDE 集成壳、产品装配、扩展点接线。

**关键技术栈**：

- Kotlin + Gradle Kotlin DSL（构建逻辑通过 `build-logic/` 中的 convention plugin 统一）
- IntelliJ Platform Gradle Plugin 2.10.5
- 默认基线平台 `platformVersion=253`，向下兼容 4 个版本（242 / 243 / 251 / 252 / 253）
- Java 工具链由 `foojay-resolver-convention` 自动 provisioning

**架构原则**（见 `docs/architecture-host-plugin.md` 与 `docs/host-upstream-responsibility-matrix.md`）：

- Host：插件装配、IntelliJ 扩展接线、项目集成、调试器 / LSP / formatter / highlighting 集成壳、消息 / 通知 / 遥测 / 图标支撑
- Upstream（主仓库）：PSI 实现、Parser 实现、Analysis API 实现、stubs / decompiled / light declarations
- **禁止**将上游能力 reintroduce 为本仓库的本地模块（不要新建本地 `analysis/`、`psi/`、`descriptors/`、`common/`、`util/`、`metadata/`、`macro/`）

## 模块结构

模块清单以 `settings.gradle.kts` 为准。当前接入构建的模块：

**产品装配**
- `:product:idea-plugin` — 唯一插件入口，承载 `plugin.xml`、`META-INF/cangjie-all.xml` 与最终打包

**Foundation**
- `:modules:foundation` — 基础设施与公共能力（含历史 `common` / `util` / `messages` / `namedpipe` 已收敛能力）

**Domain（领域能力）**
- `:modules:domain:toolchain` — 工具链管理
- `:modules:domain:project-model` — 仓颉项目模型与依赖解析
- `:modules:domain:package-manager` — CJPM 集成（TOML 配置、依赖、仓库提供）
- `:modules:domain:telemetry` — 遥测数据收集

**IDE（IDE 集成层）**
- `:modules:ide:base` — IDE 基础壳（含历史 `core` / `formatter` / `highlighter` 主链路）
- `:modules:ide:project` — 项目交互（项目识别、Workspace Model 同步、视图）
- `:modules:ide:run` — 运行 / 调试运行配置
- `:modules:ide:lsp` — LSP 客户端集成（基于 LSP4IJ）
- `:modules:ide:debugger-api` — 调试器 API
- `:modules:ide:debugger-dap` — DAP 调试实现
- `:modules:ide:debugger-proto` — Protobuf 协议调试实现
- `:modules:ide:macro` — 宏相关 IDE 能力
- `:modules:ide:ux` — UX（图标、通知等）

**测试支撑**
- `:modules:test-support` — 共享测试基建

**构建逻辑**
- `build-logic/` — Gradle convention plugins（独立 included build）

## 主仓库接入

`settings.gradle.kts` 通过 `includeBuild("../")` 把主仓库源码接入，并对若干工件做 dependency substitution：

- `org.cangnova.cangjie:cangjie-frontend-common-for-ide` → `:prepare:ide-plugin-dependencies-module:cangjie-frontend-common-for-ide-module`
- `cangjie-frontend-psi-for-ide` → 对应的 `-module` 工件
- `cangjie-frontend-cfir-for-ide` → 对应的 `-module` 工件
- `cangjie-frontend-analysis-api-for-ide` / `cangjie-frontend-analysis-api-cfir-for-ide` / `cangjie-frontend-analysis-api-standalone-for-ide` → 对应的 `-module` 工件

这意味着：**修改主仓库 PSI / CFIR / Analysis API 代码后，不需要先发布工件，本仓库 Gradle 同步会直接拿到最新源码产物**。

## 常用开发命令

### 构建和运行

```bash
./gradlew build                              # 完整构建
./gradlew :product:idea-plugin:runIde        # 在 IDE 沙箱中运行插件
./gradlew :product:idea-plugin:buildPlugin   # 构建插件分发包
./gradlew :product:idea-plugin:verifyPlugin  # 验证插件兼容性
./gradlew prepareSandbox                      # 仅准备沙箱
./gradlew clean
```

不要主动执行编译测试，除非明确要求。

### 多版本平台支持

通过 `gradle-*.properties` 切换基线平台（242 / 243 / 251 / 252 / 253）：

```bash
./gradlew build -PplatformVersion=253    # 默认
./gradlew build -PplatformVersion=242
```

版本特定的兼容层位于 `platform/<version>/`（当前仅 `platform/253/`）；业务模块内**不应**散落 if-version 分支，新增兼容代码统一放在 `platform/<version>/` 下。

### 测试

```bash
./gradlew test                                          # 全部测试
./gradlew :modules:ide:project:test                     # 单模块测试
./gradlew :modules:ide:lsp:test --tests "ClassName"     # 单测试类
./gradlew test -PshowStandardStreams=true               # 显示 stdout/stderr
CI=true ./gradlew test                                  # CI 模式（启用失败重试）
```

### 上游联动

由于 `includeBuild("../")`，主仓库的 Kotlin 修改会被 Gradle 同步自动捕获。如果遇到符号找不到 / API 未更新：

1. 先在主仓库定向编译相关模块：`./gradlew :cfir:cfir-tree:compileKotlin`（在主仓库根目录执行）
2. 再回本仓库 Gradle 同步或重新 `runIde`

## 插件配置系统

### XML 接线布局

主入口：`product/idea-plugin/src/main/resources/META-INF/plugin.xml`
通过 `cangjie-all.xml` 用 `xi:include` 聚合：

| 配置 | 职责 |
|---|---|
| `cangjie-product.xml` | startup、configurables、host product 服务 |
| `cangjie-language-shell.xml` | parser definition、language shell |
| `cangjie-editor.xml` | editor handlers、editor shell |
| `cangjie-ide-features.xml` | 高亮等 IDE 集成 |
| `cangjie-extensionPoints.xml` | host 自定义扩展点 |
| `cangjie-project.xml` / `cangjie-dependency.xml` / `cangjie-toolchain.xml` | 项目集成 |
| `cangjie-run.xml` | 运行集成 |
| `cangjie-debugger.xml` | 调试器集成 |
| `cangjie-analysis-entry.xml` | 仅 include 上游 analysis |

详见 `docs/xml-wiring-layout.md`。

### 规则

- 每个 XML 文件只描述一个责任域
- 不允许新增"catch-all"型集中注册文件
- 上游能力（PSI、Parser、Analysis）的扩展点接线允许，但**不允许**在本仓库重写实现

## 包命名约定

- 核心包：`org.cangnova.cangjie`
- IDE 功能：`org.cangnova.cangjie.ide.*`
- 项目管理：`org.cangnova.cangjie.project`
- 调试器：`org.cangnova.cangjie.debugger`
- LSP：`org.cangnova.cangjie.lsp`

## 国际化

消息 Bundle 位置：`<module>/src/main/resources/messages/`，主要 Bundle：

- `CangJieBundle.properties` — 主要 UI 消息
- `CjBuildBundle.properties` — 构建相关消息

使用：

```kotlin
import org.cangnova.cangjie.CangJieBundle
val message = CangJieBundle.message("key.in.properties")
```

## 资源文件约定

```
<module>/src/main/resources/
├── META-INF/              # 插件配置（仅 product/idea-plugin）
├── messages/              # 国际化消息
├── icons/                 # 图标资源
├── fileTemplates/         # 文件模板
├── intentionDescriptions/ # 意图描述
├── inspectionDescriptions/# 检查描述
└── liveTemplates/         # 实时模板
```

## 关键依赖

- **IntelliJ Platform**：通过 `intellijPlatform { ... }` 配置块
- **Kotlin**：来自 build-logic toolchain
- **LSP4J**：用于 DAP / LSP 协议
- **RedHat LSP4IJ**：LSP 客户端宿主插件
- **Jackson**：TOML / JSON 解析

主仓库工件通过 dependency substitution 接入（见上文）。

## 调试

```bash
./gradlew :product:idea-plugin:runIde   # 启动调试模式 IDE
```

启用日志：Help → Diagnostic Tools → Debug Log Settings → 添加 `#org.cangnova.cangjie`。

日志位置：
- Windows：`%APPDATA%\JetBrains\<IDE>\log\idea.log`
- Linux：`~/.local/share/JetBrains/<IDE>/log/idea.log`
- macOS：`~/Library/Logs/JetBrains/<IDE>/idea.log`

## 常见排查

### Gradle 同步失败

- 删除 `.gradle/` 和 `build/` 重新导入
- 检查 `gradle.properties` 中 `platformVersion` 是否与 `gradle-<version>.properties` 匹配
- GitHub Packages 凭据缺失：`GITHUB_PACKAGES_USERNAME` / `GITHUB_PACKAGES_TOKEN` 需要在 `~/.gradle/gradle.properties` 或环境变量中提供（用于解析上游发布工件）

### 主仓库变更未生效

- 在主仓库根目录定向编译相关模块
- 本仓库 Gradle 同步

### 插件无法加载

- 检查 `plugin.xml` 的 `<id>` 与 `<dependencies>`
- 验证所有 `xi:include` 引用的文件都存在
- `./gradlew :product:idea-plugin:verifyPlugin`

### 测试失败

- 确认在无头模式：`java.awt.headless=true`
- 测试资源在 `<module>/src/test/resources/`
- `-PshowStandardStreams=true` 查看详细输出

## 相关文档

- `README.md` / `README_zh.md` — 用户视角的插件介绍
- `PROJECT_STRUCTURE.md` — 详细项目结构说明
- `PROJECT_RESTRUCTURE.md` — IntelliJ Platform Gradle Plugin 2.x 重构进度
- `docs/architecture-host-plugin.md` — Host 插件架构原则
- `docs/host-upstream-responsibility-matrix.md` — Host / Upstream 职责矩阵
- `docs/xml-wiring-layout.md` — XML 接线约定
- `docs/macro-psi-replacement-design.md` — 宏 PSI 替换设计
- `docs/design/binary-stub-building-design.md`、`docs/design/cjo-service-module-design.md` — 子系统设计
- `docs/type/*` — 仓颉类型系统参考文档（基于官方实现）
