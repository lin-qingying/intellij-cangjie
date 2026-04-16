# IntelliJ Platform Gradle Plugin 2.x 重构说明

## 当前状态

本项目已经切换到新的多模块主结构：

- `build-logic/`：统一承载 Gradle convention plugins。
- `product/idea-plugin/`：唯一产品插件入口。
- `modules/foundation`：基础设施与公共能力。
- `modules/domain/*`：工具链、项目模型、包管理、遥测等领域能力。
- `modules/ide/*`：IDE 注册、项目交互、运行、LSP、调试、UX 等能力。
- `modules/test-support`：测试基建。

旧工程代码已经不再参与主构建，也不再作为运行时或编译时输入。

## 第一阶段重构结果

- 构建体系已从旧根工程脚本式拼装切换到 `build-logic`。
- 产品层已改为单插件发布模型，内部通过 Gradle 多模块组织代码。
- `foundation / toolchain / project-model / package-manager / telemetry / ide-base / ide-project / ide-run / ide-lsp / debugger-* / ux` 已纳入新主架构。
- `ide-macro` 仍按迁移策略暂时排除，不进入当前产品基线。
- 旧的聚合式运行时装配问题已经移除，当前以单插件类加载器稳定运行。

## 已完成的迁移收敛

- 旧 `common / util / messages / namedpipe` 能力已收敛到 `modules/foundation`。
- 旧 `icon / notifications` 能力已收敛到 `modules/ide/ux`。
- 旧 `toolchain / telemetry / cjpm / lsp4ij / debugger` 已按新边界进入 `domain` 与 `ide` 层。
- 旧 `cangjie-project` 已拆分收敛到 `domain:project-model`、`ide:project`、`ide:run`。
- 旧 `core / formatter / highlighter` 的主链路能力已收敛到 `ide:base` 等模块。
- 旧测试基建已收敛到 `modules/test-support`。

## 运行链路状态

- 当前基线平台为 `253`。
- `prepareSandbox` 可通过。
- `runIde` 已可启动。
- 仓颉项目识别、CJPM 刷新、依赖解析、Workspace Model 同步、LSP 重启链路已跑通。
- 自家运行日志中的关键异常已清除。

## 后续边界

- `ide-macro` 后续单独迁入，不与当前主链路混合处理。
- 若未来恢复反编译或其他非主链路能力，必须按新模块边界重新纳管，不能回退到旧工程结构。
