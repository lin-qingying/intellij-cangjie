# 多版本编译指南

本项目支持为多个 IntelliJ Platform 版本构建插件，以应对不同版本间的 API 变更。

## 🎯 支持的平台版本

| 版本 | IDE 版本 | API 类型 | 说明 |
|------|---------|---------|------|
| 242  | IntelliJ IDEA 2024.2 | Legacy API | WorkspaceModel 旧 API |
| 243  | IntelliJ IDEA 2024.3 | Legacy API | WorkspaceModel 旧 API |
| 251  | IntelliJ IDEA 2025.1 | Legacy API | WorkspaceModel 旧 API |
| 252  | IntelliJ IDEA 2025.2 | Legacy API | WorkspaceModel 旧 API |
| 253  | IntelliJ IDEA 2025.3 | Builder API | WorkspaceModel 新 Builder API |

## 📦 构建任务

### 1️⃣ 构建所有版本

一次性构建所有支持的平台版本（242, 243, 251, 252, 253）：

```bash
./gradlew buildAllVersions
```

**输出示例**：
```
================================================================================
开始构建所有平台版本的插件
================================================================================

>>> 正在构建平台版本 242...
    使用: Legacy API
<<< 平台版本 242 构建完成 (耗时: 120s)
    产物: plugin/build/distributions/intellij-cangjie-242.zip

>>> 正在构建平台版本 243...
    使用: Legacy API
<<< 平台版本 243 构建完成 (耗时: 115s)
    产物: plugin/build/distributions/intellij-cangjie-243.zip

...

================================================================================
所有版本构建完成！
================================================================================
构建产物位于: plugin/build/distributions
  - intellij-cangjie-242.zip (45 MB)
  - intellij-cangjie-243.zip (46 MB)
  - intellij-cangjie-251.zip (47 MB)
  - intellij-cangjie-252.zip (48 MB)
  - intellij-cangjie-253.zip (49 MB)
```

### 2️⃣ 快速构建（推荐用于测试）

仅构建 Legacy (242) 和 Builder (253) 两个代表版本：

```bash
./gradlew buildLegacyAndBuilder
```

这个任务构建速度更快，适合用于：
- 验证 API 兼容性
- CI/CD 流水线
- 快速测试

### 3️⃣ 构建单个版本

构建指定的平台版本：

```bash
# 构建 242 版本
./gradlew buildPlugin242

# 构建 253 版本
./gradlew buildPlugin253
```

或者手动指定版本参数：

```bash
./gradlew clean :plugin:buildPlugin -PplatformVersion=242
./gradlew clean :plugin:buildPlugin -PplatformVersion=253
```

## 🔍 构建产物说明

所有构建产物位于 `plugin/build/distributions/` 目录：

```
plugin/build/distributions/
├── intellij-cangjie-242.zip    # IntelliJ 2024.2 版本
├── intellij-cangjie-243.zip    # IntelliJ 2024.3 版本
├── intellij-cangjie-251.zip    # IntelliJ 2025.1 版本
├── intellij-cangjie-252.zip    # IntelliJ 2025.2 版本
└── intellij-cangjie-253.zip    # IntelliJ 2025.3 版本
```

## 🛠️ API 兼容性实现

### WorkspaceModel API

**问题**：
- **242-252**: 使用 `ExcludeUrlEntity` 和 `SourceRootEntity` 类构造函数
- **253+**: 使用 `ExcludeUrlEntityBuilder` 和 `SourceRootEntityBuilder`

**解决方案**：版本特定源码目录

```
cangjie-project/src/main/
├── workspace-model-legacy/kotlin/     # 242-252 使用
│   └── org/cangnova/cangjie/project/workspace/compat/
│       └── WorkspaceModelCompat.kt
└── workspace-model-builder/kotlin/    # 253+ 使用
    └── org/cangnova/cangjie/project/workspace/compat/
        └── WorkspaceModelCompat.kt
```

### BuildEvents API

**问题**：
- **242-252**: 使用具体实现类 (`StartEventImpl`, `FinishEventImpl` 等)
- **253+**: 使用 `BuildEvents.getInstance()` Builder 模式

**解决方案**：兼容层函数

```kotlin
// 主代码统一使用
createStartEvent(id, parentId, eventTime, message)
createFinishEvent(id, parentId, eventTime, message, result)
createFileMessageEvent(parentId, kind, group, message, detailedMessage, filePosition)
createOutputBuildEvent(parentId, message, stdOut)
```

## 📋 Gradle 配置说明

根据 `platformVersion` 参数自动选择对应的源码目录：

```kotlin
// cangjie-project/build.gradle.kts
val platformVersion = providers.gradleProperty("platformVersion").getOrElse("242")

sourceSets {
    main {
        kotlin {
            if (platformVersion.toInt() >= 253) {
                srcDir("src/main/workspace-model-builder/kotlin")  // 253+
            } else {
                srcDir("src/main/workspace-model-legacy/kotlin")   // 242-252
            }
        }
    }
}
```

## 🚀 CI/CD 集成

### GitHub Actions 示例

```yaml
name: Build All Versions

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Build all versions
        run: ./gradlew buildAllVersions

      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: intellij-cangjie-plugins
          path: plugin/build/distributions/*.zip
```

### 仅测试关键版本

```yaml
- name: Build legacy and builder versions
  run: ./gradlew buildLegacyAndBuilder
```

## 📝 开发建议

1. **日常开发**：使用默认版本 (242)
   ```bash
   ./gradlew build
   ```

2. **测试兼容性**：构建 Legacy + Builder
   ```bash
   ./gradlew buildLegacyAndBuilder
   ```

3. **发布前**：构建所有版本
   ```bash
   ./gradlew buildAllVersions
   ```

## ❓ 常见问题

### Q: 为什么需要多版本构建？
A: IntelliJ Platform 在不同版本间存在 API 变更，为了支持更广泛的用户群体，需要为不同版本构建专用的插件。

### Q: 如何添加新的平台版本支持？
A:
1. 在 `build.gradle.kts` 的 `supportedPlatformVersions` 列表中添加版本号
2. 根据 API 变化决定是否需要新的兼容层实现
3. 运行 `./gradlew buildAllVersions` 验证

### Q: 构建失败怎么办？
A:
1. 检查是否有语法错误或未兼容的 API 调用
2. 查看具体版本的编译日志
3. 确认版本特定的兼容层实现是否正确

## 🔗 相关文档

- [build.gradle.kts](../build.gradle.kts) - 构建任务定义
- [WorkspaceModelCompat.kt](../cangjie-project/src/main/workspace-model-legacy/kotlin/org/cangnova/cangjie/project/workspace/compat/WorkspaceModelCompat.kt) - WorkspaceModel 兼容层
- [BuildEventsCompat.kt](../cangjie-project/src/main/workspace-model-legacy/kotlin/org/cangnova/cangjie/run/compat/BuildEventsCompat.kt) - BuildEvents 兼容层
