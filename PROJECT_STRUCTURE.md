# 项目结构说明

> 更新日期：2026-05-11
> 真相源：`settings.gradle.kts`

## 项目定位

`intellij-cangjie` 是基于 IntelliJ Platform Gradle Plugin 2.x 的仓颉语言 **host plugin**，为仓颉语言提供 IDE 集成壳（语法高亮、补全、调试、LSP、项目管理、运行配置等）。

仓库**不承载本地 PSI / Parser / Analysis 实现**——这些上游能力由主仓库（`../`，根 `cangjie` 工程）通过 `includeBuild` 与 dependency substitution 提供。

详见 [`PROJECT_RESTRUCTURE.md`](PROJECT_RESTRUCTURE.md)（重构进度）与 [`docs/architecture-host-plugin.md`](docs/architecture-host-plugin.md)（架构原则）。

## 顶层布局

```
intellij-ide/
├── build-logic/                 # Gradle convention plugins（独立 included build）
├── product/
│   └── idea-plugin/             # 唯一插件入口：plugin.xml、cangjie-all.xml、最终打包
├── modules/
│   ├── foundation/              # 基础设施与公共能力
│   ├── domain/
│   │   ├── toolchain/           # 工具链管理
│   │   ├── project-model/       # 仓颉项目模型与依赖解析
│   │   ├── package-manager/     # CJPM 集成
│   │   └── telemetry/           # 遥测数据收集
│   ├── ide/
│   │   ├── base/                # IDE 基础壳（core / formatter / highlighter 主链路）
│   │   ├── project/             # 项目交互、Workspace Model 同步、项目视图
│   │   ├── run/                 # 运行配置
│   │   ├── lsp/                 # LSP 客户端（基于 LSP4IJ）
│   │   ├── debugger-api/        # 调试器 API
│   │   ├── debugger-dap/        # DAP 调试实现
│   │   ├── debugger-proto/      # Protobuf 协议调试
│   │   ├── macro/               # 宏 IDE 能力
│   │   └── ux/                  # UX：图标、通知
│   └── test-support/            # 共享测试基建
├── platform/                    # 平台兼容层
│   └── 253/                     # 当前基线（platformVersion=253）
├── features/                    # 预留：未来产品级 feature 插件
├── docs/                        # 架构与设计文档
│   ├── architecture-host-plugin.md
│   ├── host-upstream-responsibility-matrix.md
│   ├── xml-wiring-layout.md
│   ├── macro-psi-replacement-design.md
│   ├── design/
│   │   ├── binary-stub-building-design.md
│   │   └── cjo-service-module-design.md
│   └── type/                    # 仓颉类型系统参考文档
├── external/                    # 外部参考源码（不参与构建）
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties            # 默认配置（platformVersion=253）
├── gradle-242.properties        # IDE 2024.2
├── gradle-243.properties        # IDE 2024.3
├── gradle-251.properties        # IDE 2025.1
├── gradle-252.properties        # IDE 2025.2
├── gradle-253.properties        # IDE 2025.3（默认）
├── CHANGELOG.md
├── CLAUDE.md / AGENTS.md        # AI 代理操作指南
├── README.md / README_zh.md     # 用户介绍
├── PROJECT_RESTRUCTURE.md       # 重构进度
└── PROJECT_STRUCTURE.md         # 本文件
```

## 模块清单

### 产品装配

| Gradle 路径 | 职责 |
|---|---|
| `:product:idea-plugin` | 唯一产品插件入口，承载 `plugin.xml`、`META-INF/cangjie-all.xml`，负责最终装配与发布 |

### Foundation 层

| Gradle 路径 | 职责 |
|---|---|
| `:modules:foundation` | 基础设施与公共能力，收敛了历史 `common` / `util` / `messages` / `namedpipe` 等模块 |

### Domain（领域能力）层

| Gradle 路径 | 职责 |
|---|---|
| `:modules:domain:toolchain` | 仓颉工具链版本管理、工具链下载与配置、多版本切换 |
| `:modules:domain:project-model` | 仓颉项目模型抽象（`CjProject` / `CjModule` / `CjLibrary`）、依赖解析、模块结构 |
| `:modules:domain:package-manager` | CJPM（仓颉包管理器）集成：`cjpm.toml` 解析、依赖管理、包仓库 |
| `:modules:domain:telemetry` | 遥测数据收集与上报、隐私控制 |

### IDE（IDE 集成）层

| Gradle 路径 | 职责 |
|---|---|
| `:modules:ide:base` | IDE 基础壳（startup activity、core 服务、formatter、highlighter 主链路） |
| `:modules:ide:project` | 项目识别、Workspace Model 同步、项目视图、工具窗口 |
| `:modules:ide:run` | 运行配置、运行目标 |
| `:modules:ide:lsp` | LSP 客户端集成，基于 RedHat LSP4IJ 插件 |
| `:modules:ide:debugger-api` | 调试器 API 与公共组件 |
| `:modules:ide:debugger-dap` | 基于 Debug Adapter Protocol 的调试器实现 |
| `:modules:ide:debugger-proto` | 基于 Protobuf 协议的调试器实现 |
| `:modules:ide:macro` | 宏相关 IDE 能力 |
| `:modules:ide:ux` | UX：图标、通知 |

### 测试支撑

| Gradle 路径 | 职责 |
|---|---|
| `:modules:test-support` | 跨模块共享的测试基础设施、工具类、Mock 对象 |

### 构建逻辑

`build-logic/` 是独立的 included build，承载 Gradle convention plugins，统一所有模块的 Kotlin / Java / IntelliJ Platform 配置。

## 主仓库（上游）接入

`settings.gradle.kts` 通过：

```kotlin
includeBuild("../") {
    dependencySubstitution {
        substitute(module("org.cangnova.cangjie:cangjie-frontend-common-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-common-for-ide-module"))
        // … psi / cfir / analysis-api / analysis-api-cfir / analysis-api-standalone 同理
    }
}
```

把主仓库 `prepare:ide-plugin-dependencies-module:*` 模块源码直接接入构建。上游 6 个工件：

- `cangjie-frontend-common-for-ide`
- `cangjie-frontend-psi-for-ide`
- `cangjie-frontend-cfir-for-ide`
- `cangjie-frontend-analysis-api-for-ide`
- `cangjie-frontend-analysis-api-cfir-for-ide`
- `cangjie-frontend-analysis-api-standalone-for-ide`

修改主仓库源码后，Gradle 同步即可生效，**无需先发布工件**。

## 平台版本支持

从最新版本向下支持 4 个版本（实际配置 5 份 properties 文件）：

| 配置 | 平台 |
|---|---|
| `gradle.properties` | 默认（`platformVersion=253`） |
| `gradle-242.properties` | IDE 2024.2 |
| `gradle-243.properties` | IDE 2024.3 |
| `gradle-251.properties` | IDE 2025.1 |
| `gradle-252.properties` | IDE 2025.2 |
| `gradle-253.properties` | IDE 2025.3（默认基线） |

切换：`./gradlew build -PplatformVersion=242`

版本特定的兼容层应放在 `platform/<version>/`（当前仅 `platform/253/`），**不应**在业务模块内散落 if-version 分支。

## XML 接线（在 `product/idea-plugin`）

主入口：`product/idea-plugin/src/main/resources/META-INF/plugin.xml`
通过 `cangjie-all.xml` 用 `xi:include` 聚合：

```
cangjie-all.xml
├── cangjie-product.xml              # startup / configurables / host product
├── cangjie-language-shell.xml       # parser definition / language shell
├── cangjie-editor.xml               # editor handlers
├── cangjie-ide-features.xml         # 高亮等 IDE 集成
├── cangjie-extensionPoints.xml      # 自定义扩展点
├── cangjie-project.xml              # 项目集成
├── cangjie-dependency.xml           # 依赖集成
├── cangjie-toolchain.xml            # 工具链集成
├── cangjie-run.xml                  # 运行集成
├── cangjie-debugger.xml             # 调试器集成
└── cangjie-analysis-entry.xml       # 仅 include 上游 analysis
```

详细约束见 [`docs/xml-wiring-layout.md`](docs/xml-wiring-layout.md)。

## 技术栈

- **语言**：Kotlin
- **构建**：Gradle + Kotlin DSL，build-logic convention plugins
- **平台 SDK**：IntelliJ Platform Gradle Plugin 2.10.5
- **协议**：DAP（Debug Adapter Protocol）、LSP（Language Server Protocol）
- **解析**：Jackson（TOML / JSON）
- **LSP 宿主**：RedHat LSP4IJ

## 模块依赖大致方向

```
product:idea-plugin
  ↓
modules:ide:*  (project / run / lsp / debugger-* / macro / ux / base)
  ↓
modules:domain:*  (toolchain / project-model / package-manager / telemetry)
  ↓
modules:foundation
```

`modules:test-support` 横向支撑各模块测试；`build-logic` 在 settings 层提供 convention plugins。

## 开发指南

详见 [`README.md`](README.md) / [`README_zh.md`](README_zh.md)（用户视角）与 [`CLAUDE.md`](CLAUDE.md) / [`AGENTS.md`](AGENTS.md)（AI 代理视角）。
