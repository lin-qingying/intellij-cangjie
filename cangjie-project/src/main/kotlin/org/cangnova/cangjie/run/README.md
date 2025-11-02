# CangJie Run Configuration Framework

## 架构概述

`cangjie-project` 模块提供了完整的运行配置实现，但通过扩展点将UI编辑器和命令执行等具体功能委托给子系统（如 CJPM、CJC）。

### 设计原则

- **cangjie-project**: 提供运行配置的核心框架和基础实现
- **子系统**: 通过扩展点提供UI编辑器、命令执行器和构建系统检测

## 核心组件

### cangjie-project 提供的组件

1. **CangJieCommandConfiguration** - 抽象基类
   - 提供命令和工作目录字段
   - XML 序列化/反序列化
   - 基本配置验证

2. **CangJieRunConfiguration** - 具体实现
   - 完整的运行配置实现
   - 自动检测构建系统
   - 委托UI和执行给子系统

3. **CangJieCommandRunConfigurationType** - 命令配置类型
   - 注册到 IntelliJ 平台，用于运行构建命令
   - 提供配置工厂

4. **CangJieProgramRunConfigurationType** - 程序配置类型
   - 注册到 IntelliJ 平台，用于运行编译后的程序
   - 提供配置工厂

5. **CangJieRunConfigurationProducer** - 配置生成器
   - 从上下文自动创建配置
   - 自动检测构建系统

6. **CangJieCommandRunner** - 程序运行器
   - 执行运行配置

7. **CangJieRunState** - 运行状态
   - 委托命令行创建给子系统

8. **CangJieRunConfigurationDefaultEditor** - 默认编辑器
   - 当子系统未提供编辑器时使用

### 子系统需要实现的扩展点

1. **CangJieRunConfigurationEditorProvider** - UI编辑器提供者
2. **CangJieCommandExecutor** - 命令执行器
3. **CangJieBuildSystemDetector** - 构建系统检测器

## 扩展点

### 1. editorProvider - UI编辑器提供者

```xml
<extensionPoint
    name="runConfigurationEditorProvider"
    qualifiedName="org.cangnova.cangjie.run.editorProvider"
    interface="org.cangnova.cangjie.run.CangJieRunConfigurationEditorProvider"
    dynamic="true"/>
```

### 2. commandExecutor - 命令执行器

```xml
<extensionPoint
    name="commandExecutor"
    qualifiedName="org.cangnova.cangjie.run.commandExecutor"
    interface="org.cangnova.cangjie.run.CangJieCommandExecutor"
    dynamic="true"/>
```

### 3. buildSystemDetector - 构建系统检测器

```xml
<extensionPoint
    name="buildSystemDetector"
    qualifiedName="org.cangnova.cangjie.run.buildSystemDetector"
    interface="org.cangnova.cangjie.run.CangJieBuildSystemDetector"
    dynamic="true"/>
```

## 子系统实现指南

子系统（如 CJPM、CJC）需要实现三个扩展点来提供完整功能。

### 步骤 1: 实现构建系统检测器

```kotlin
class CjpmBuildSystemDetector : CangJieBuildSystemDetector {
    override fun getBuildSystemId(): String = "cjpm"

    override fun detectBuildSystem(project: Project): Boolean {
        // 检测项目是否使用 CJPM (例如检查 cjpm.toml 文件)
        val basePath = project.basePath ?: return false
        return File(basePath, "cjpm.toml").exists()
    }

    override fun getPriority(): Int = 100
}
```

### 步骤 2: 实现命令执行器

```kotlin
class CjpmCommandExecutor : CangJieCommandExecutor {
    override fun getBuildSystemId(): String = "cjpm"

    override fun createCommandLine(configuration: CangJieRunConfiguration): GeneralCommandLine? {
        val workingDir = configuration.workingDirectory ?: return null

        return GeneralCommandLine()
            .withExePath("cjpm")
            .withParameters(configuration.command)
            .withParameters(configuration.args?.split(" ") ?: emptyList())
            .withWorkDirectory(workingDir.toFile())
            .withEnvironment(configuration.env.envs)
            .withParentEnvironmentType(
                if (configuration.env.isPassParentEnvs) {
                    GeneralCommandLine.ParentEnvironmentType.CONSOLE
                } else {
                    GeneralCommandLine.ParentEnvironmentType.NONE
                }
            )
    }

    override fun validateConfiguration(configuration: CangJieRunConfiguration): String? {
        if (configuration.command.isBlank()) {
            return "Command cannot be empty"
        }
        // 可以添加更多验证逻辑
        return null
    }

    override fun getPriority(): Int = 100
}
```

### 步骤 3: 实现UI编辑器提供者

```kotlin
class CjpmRunConfigurationEditorProvider : CangJieRunConfigurationEditorProvider {
    override fun getBuildSystemId(): String = "cjpm"

    override fun createEditor(
        project: Project,
        configuration: CangJieRunConfiguration
    ): SettingsEditor<CangJieRunConfiguration> {
        return CjpmRunConfigurationEditor(project)
    }

    override fun getPriority(): Int = 100
}

class CjpmRunConfigurationEditor(private val project: Project) :
    SettingsEditor<CangJieRunConfiguration>() {

    private val commandField = RawCommandLineEditor()
    private val argsField = JBTextField()
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val envVarsComponent = EnvironmentVariablesComponent()

    init {
        workingDirectoryField.addBrowseFolderListener(
            "Select Working Directory",
            "Select the working directory for the command",
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }

    override fun resetEditorFrom(configuration: CangJieRunConfiguration) {
        commandField.text = configuration.command
        argsField.text = configuration.args ?: ""
        workingDirectoryField.text = configuration.workingDirectory?.toString() ?: ""
        envVarsComponent.envData = configuration.env
    }

    override fun applyEditorTo(configuration: CangJieRunConfiguration) {
        configuration.command = commandField.text
        configuration.args = argsField.text.takeIf { it.isNotBlank() }
        configuration.workingDirectory = workingDirectoryField.text
            .takeIf { it.isNotBlank() }?.let { Paths.get(it) }
        configuration.env = envVarsComponent.envData
    }

    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Command:", commandField)
            .addLabeledComponent("Arguments:", argsField)
            .addLabeledComponent("Working directory:", workingDirectoryField)
            .addComponent(envVarsComponent)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}
```

### 步骤 4: 注册到 plugin.xml

在子系统的 `plugin.xml` 中注册扩展点实现：

```xml
<extensions defaultExtensionNs="org.cangnova.cangjie.run">
    <!-- 构建系统检测器 -->
    <buildSystemDetector implementation="com.example.cjpm.run.CjpmBuildSystemDetector"/>

    <!-- 命令执行器 -->
    <commandExecutor implementation="com.example.cjpm.run.CjpmCommandExecutor"/>

    <!-- UI编辑器提供者 -->
    <editorProvider implementation="com.example.cjpm.run.CjpmRunConfigurationEditorProvider"/>
</extensions>
```

## 工作流程

1. **创建配置**: 用户通过 Run → Edit Configurations 创建 CangJie 运行配置
2. **检测构建系统**: `CangJieBuildSystemDetector` 自动检测项目使用的构建系统（CJPM/CJC）
3. **显示UI**: 根据检测到的构建系统，加载对应的 `CangJieRunConfigurationEditorProvider`
4. **配置验证**: 使用 `CangJieCommandExecutor.validateConfiguration()` 验证配置
5. **执行命令**: 使用 `CangJieCommandExecutor.createCommandLine()` 创建命令行并执行

## 优势

1. **统一入口**: 用户只需要一个 "CangJie" 运行配置类型
2. **自动适配**: 根据项目类型自动选择合适的子系统
3. **灵活扩展**: 子系统可以独立开发和部署
4. **关注点分离**: cangjie-project 负责框架，子系统负责具体实现
5. **降低耦合**: 子系统之间互不依赖

## 示例子系统

- **CJPM**: 仓颉包管理器，提供 `cjpm run`、`cjpm build`、`cjpm test` 等命令
- **CJC**: 仓颉编译器，提供 `cjc compile` 等命令

每个子系统只需实现三个扩展点即可完整集成到运行配置系统中。