# IntelliJ 仓颉插件架构设计

## 模块职责划分

### 核心模块层���

```
plugin (插件入口)
  ├─ cangjie-project (项目模型核心) [NEW]
  │   ├─ 项目抽象接口
  │   ├─ 项目服务
  │   ├─ 项目模型
  │   └─ 扩展点定义
  │       ├─ CjProjectProvider (扩展点)
  │       └─ CjProjectConfigParser (扩展点)
  │
  ├─ cangjie-dependency (依赖管理核心) [NEW]
  │   ├─ 依赖抽象模型
  │   ├─ 依赖解析服务
  │   ├─ 包管理接口
  │   └─ 扩展点定义
  │       ├─ CjDependencyResolver (扩展点)
  │       ├─ CjPackageManager (扩展点)
  │       └─ CjRepositoryProvider (扩展点)
  │
  ├─ build-system (构建系统核心) [REFACTOR]
  │   ├─ 构建抽象接口
  │   ├─ 构建任务管理
  │   ├─ Build事件系统
  │   └─ 扩展点定义
  │       ├─ CjBuildSystemProvider (扩展点)
  │       ├─ CjBuildTaskExecutor (扩展点)
  │       └─ CjBuildConfigurationProvider (扩展点)
  │
  ├─ cjpm (cjpm实现) [Extension]
  │   ├─ CjpmProject (实现)
  │   ├─ TOML解析
  │   ├─ cjpm依赖解析器
  │   ├─ cjpm包管理器
  │   ├─ cjpm构建系统 (实现)
  │   └─ cjpm工具窗口
  │
  ├─ kcjpm (kcjpm实现) [Extension - 未来]
  │   ├─ KcjpmProject (实现)
  │   ├─ 配置解析
  │   ├─ kcjpm依赖解析器
  │   ├─ kcjpm包管理器
  │   └─ kcjpm构建系统 (实现)
  │
  ├─ toolchain (SDK管理)
  │   ├─ SDK注册中心
  │   ├─ SDK检测和发现
  │   └─ 版本管理
  │
  ├─ psi (语法树)
  ├─ descriptors (类型系统)
  ├─ analysis (代码分析)
  ├─ lsp4ij (LSP集成)
  └─ dap-debugger (调试支持)
```

---

## 模块详细说明

### 1. cangjie-project 模块 (新建)

**职责**: 定义仓颉项目的核心抽象和扩展点机制

#### 核心功能

1. **项目抽象接口**
    - `CjProject` - 项目抽象接口
    - `CjWorkspace` - 工作空间抽象
    - `CjProjectsService` - 项目管理服务

2. **依赖模型**
    - `CjDependency` - 依赖抽象
    - `CjPackage` - 包抽象
    - `CjModule` - 模块抽象

3. **扩展点定义**
    - `CjProjectProvider` - 项目提供者扩展点
    - `CjDependencyResolver` - 依赖解析器扩展点
    - `CjProjectConfigParser` - 配置解析器扩展点
    - `CjProjectOpenProcessor` - 项目打开处理器扩展点

4. **项目服务**
    - 项目发现和创建
    - 项目缓存和索引
    - 项目事件发布

#### 目录结构

```
cangjie-project/
├── src/main/kotlin/org/cangnova/cangjie/project/
│   ├── model/
│   │   ├── CjProject.kt              # 项目抽象接口
│   │   ├── CjWorkspace.kt            # 工作空间抽象
│   │   ├── CjModule.kt               # 模块抽象
│   │   ├── CjTarget.kt               # 构建目标抽象
│   │   ├── CjSourceSet.kt            # 源码集抽象
│   │   └── CjProjectType.kt          # 项目类型
│   ├── service/
│   │   ├── CjProjectsService.kt      # 项目服务接口
│   │   └── impl/
│   │       └── CjProjectsServiceImpl.kt
│   ├── extension/
│   │   ├── CjProjectProvider.kt      # 项目提供者扩展点
│   │   ├── CjProjectConfigParser.kt  # 配置解析器扩展点
│   │   └── CjProjectOpenProcessor.kt # 项目打开处理器扩展点
│   ├── event/
│   │   ├── CjProjectEvent.kt         # 项目事件
│   │   └── CjProjectListener.kt      # 项目监听器
│   └── index/
│       ├── CjProjectIndex.kt         # 项目索引
│       └── CjPackageIndex.kt         # 包索引
├── src/main/resources/
│   └── META-INF/
│       └── cangjie-project.xml
└── build.gradle.kts
```

#### 扩展点机制

```xml
<!-- cangjie-project.xml -->
<extensionPoints>
    <!-- 项目提供者:识别和创建项目 -->
    <extensionPoint name="projectProvider"
                    qualifiedName="org.cangnova.cangjie.project.projectProvider"
                    interface="org.cangnova.cangjie.project.extension.CjProjectProvider"
                    dynamic="true"/>

    <!-- 配置解析器:解析项目配置文件 -->
    <extensionPoint name="configParser"
                    qualifiedName="org.cangnova.cangjie.project.configParser"
                    interface="org.cangnova.cangjie.project.extension.CjProjectConfigParser"
                    dynamic="true"/>
</extensionPoints>
```

#### 依赖关系

```
cangjie-project 依赖:
  - util (工具类)
  - messages (国际化)
  - notifications (通知)
  - psi (语法树,用于索引)
  - toolchain (获取SDK信息)
```

---

### 2. cangjie-dependency 模块 (新建)

**职责**: 定义依赖管理的核心抽象和扩展点机制

#### 核心功能

1. **依赖抽象模型**
    - `CjDependency` - 依赖抽象接口
    - `CjPackage` - 包抽象
    - `CjVersion` - 版本模型
    - `CjDependencyScope` - 依赖范围(compile/runtime/test)

2. **依赖解析服务**
    - `CjDependencyService` - 依赖管理服务
    - `CjDependencyGraph` - 依赖图
    - `CjDependencyCache` - 依赖缓存

3. **包管理接口**
    - `CjPackageRegistry` - 包注册表
    - `CjLibrary` - 库抽象
    - `CjArtifact` - 产物抽象

4. **扩展点定义**
    - `CjDependencyResolver` - 依赖解析器扩展点
    - `CjPackageManager` - 包管理器扩展点
    - `CjRepositoryProvider` - 仓库提供者扩展点

#### 目录结构

```
cangjie-dependency/
├── src/main/kotlin/org/cangnova/cangjie/dependency/
│   ├── model/
│   │   ├── CjDependency.kt           # 依赖抽象接口
│   │   ├── CjPackage.kt              # 包抽象
│   │   ├── CjVersion.kt              # 版本模型
│   │   ├── CjDependencyScope.kt      # 依赖范围
│   │   ├── CjLibrary.kt              # 库抽象
│   │   ├── CjArtifact.kt             # 产物抽象
│   │   └── CjDependencyType.kt       # 依赖类型
│   ├── service/
│   │   ├── CjDependencyService.kt    # 依赖服务接口
│   │   └── impl/
│   │       └── CjDependencyServiceImpl.kt
│   ├── graph/
│   │   ├── CjDependencyGraph.kt      # 依赖图
│   │   ├── CjDependencyNode.kt       # 依赖节点
│   │   └── CjDependencyEdge.kt       # 依赖边
│   ├── cache/
│   │   ├── CjDependencyCache.kt      # 依赖缓存
│   │   └── CjPackageCache.kt         # 包缓存
│   ├── registry/
│   │   ├── CjPackageRegistry.kt      # 包注册表
│   │   └── impl/
│   │       └── CjPackageRegistryImpl.kt
│   ├── extension/
│   │   ├── CjDependencyResolver.kt   # 依赖解析器扩展点
│   │   ├── CjPackageManager.kt       # 包管理器扩展点
│   │   └── CjRepositoryProvider.kt   # 仓库提供者扩展点
│   ├── event/
│   │   ├── CjDependencyEvent.kt      # 依赖事件
│   │   └── CjDependencyListener.kt   # 依赖监听器
│   └── util/
│       ├── VersionComparator.kt      # 版本比较器
│       └── DependencyConflictResolver.kt  # 冲突解决器
├── src/main/resources/
│   └── META-INF/
│       └── cangjie-dependency.xml
└── build.gradle.kts
```

#### 扩展点机制

```xml
<!-- cangjie-dependency.xml -->
<extensionPoints>
    <!-- 依赖解析器:解析和获取依赖 -->
    <extensionPoint name="dependencyResolver"
                    qualifiedName="org.cangnova.cangjie.dependency.dependencyResolver"
                    interface="org.cangnova.cangjie.dependency.extension.CjDependencyResolver"
                    dynamic="true"/>

    <!-- 包管理器:管理包的下载和安装 -->
    <extensionPoint name="packageManager"
                    qualifiedName="org.cangnova.cangjie.dependency.packageManager"
                    interface="org.cangnova.cangjie.dependency.extension.CjPackageManager"
                    dynamic="true"/>

    <!-- 仓库提供者:提供包仓库访问 -->
    <extensionPoint name="repositoryProvider"
                    qualifiedName="org.cangnova.cangjie.dependency.repositoryProvider"
                    interface="org.cangnova.cangjie.dependency.extension.CjRepositoryProvider"
                    dynamic="true"/>
</extensionPoints>

        <!-- 服务注册 -->
<extensions defaultExtensionNs="com.intellij">
<applicationService
        serviceImplementation="org.cangnova.cangjie.dependency.service.impl.CjDependencyServiceImpl"/>

<projectService
        serviceInterface="org.cangnova.cangjie.dependency.registry.CjPackageRegistry"
        serviceImplementation="org.cangnova.cangjie.dependency.registry.impl.CjPackageRegistryImpl"/>
</extensions>
```

#### 核心接口设计

##### CjDependency

```kotlin
interface CjDependency {
    val name: String
    val group: String?
    val version: CjVersion
    val scope: CjDependencyScope
    val type: CjDependencyType
    val transitive: Boolean
    val excludes: List<String>
}

enum class CjDependencyScope {
    COMPILE,    // 编译时依赖
    RUNTIME,    // 运行时依赖
    TEST,       // 测试依赖
    PROVIDED    // 已提供依赖
}

enum class CjDependencyType {
    LIBRARY,    // 外部库
    MODULE,     // 项目内模块
    SYSTEM      // 系统依赖(如标准库)
}
```

##### CjDependencyResolver (扩展点)

```kotlin
interface CjDependencyResolver {
    /**
     * 解析器优先级(数值越小优先级越高)
     */
    val priority: Int

    /**
     * 判断是否可以解析该依赖
     */
    fun canResolve(dependency: CjDependency): Boolean

    /**
     * 解析依赖,返回依赖的详细信息
     */
    fun resolve(dependency: CjDependency, project: Project): CjResolvedDependency?

    /**
     * 解析传递依赖
     */
    fun resolveTransitive(dependency: CjDependency, project: Project): List<CjDependency>
}
```

##### CjPackageManager (扩展点)

```kotlin
interface CjPackageManager {
    /**
     * 包管理器名称
     */
    val name: String

    /**
     * 下载包
     */
    fun downloadPackage(dependency: CjDependency, targetDir: Path): Boolean

    /**
     * 检查包是否已下载
     */
    fun isPackageDownloaded(dependency: CjDependency): Boolean

    /**
     * 获取包的本地路径
     */
    fun getPackagePath(dependency: CjDependency): Path?

    /**
     * 清除缓存
     */
    fun clearCache()
}
```

##### CjRepositoryProvider (扩展点)

```kotlin
interface CjRepositoryProvider {
    /**
     * 仓库名称
     */
    val repositoryName: String

    /**
     * 仓库URL
     */
    val repositoryUrl: String

    /**
     * 搜索包
     */
    fun searchPackage(name: String): List<CjPackage>

    /**
     * 获取包信息
     */
    fun getPackageInfo(name: String, version: CjVersion): CjPackage?

    /**
     * 下载包
     */
    fun downloadPackage(pkg: CjPackage, targetDir: Path): Boolean
}
```

#### 依赖关系

```
cangjie-dependency 依赖:
  - util (工具类)
  - messages (国际化)
  - notifications (通知)
```

---

### 3. cjpm 模块 (新建)

**职责**: cjpm项目的具体实现

#### 核心功能

1. **项目实现**
    - `CjpmProject` - 实现 `CjProject` 接口
    - `CjpmWorkspace` - 实现 `CjWorkspace` 接口
    - `CjpmProjectProvider` - 实现 `CjProjectProvider` 扩展点

2. **配置解析**
    - `CjpmTomlParser` - 解析 manifest.toml
    - `CjpmTomlConfig` - TOML配置模型
    - `CjpmConfigParser` - 实现 `CjProjectConfigParser` 扩展点

3. **依赖管理**
    - `CjpmDependencyResolver` - 实现 `CjDependencyResolver` 扩展点
    - `CjpmPackageManager` - 实现 `CjPackageManager` 扩展点
    - `CjpmRepositoryProvider` - 实现 `CjRepositoryProvider` 扩展点
    - cjpm依赖图构建

4. **UI集成**
    - `CjpmToolWindow` - cjpm工具窗口
    - `CjpmProjectTree` - 项目树视图
    - `CjpmConfigurable` - 配置页面

#### 目录结构

```
cjpm/
├── src/main/kotlin/org/cangnova/cangjie/cjpm/
│   ├── project/
│   │   ├── CjpmProject.kt            # CjProject实现
│   │   ├── CjpmWorkspace.kt          # CjWorkspace实现
│   │   ├── CjpmProjectProvider.kt    # 项目提供者实现
│   │   └── impl/
│   │       ├── CjpmProjectImpl.kt
│   │       └── ...
│   ├── config/
│   │   ├── CjpmTomlParser.kt         # TOML解析器
│   │   ├── CjpmTomlConfig.kt         # TOML配置模型
│   │   ├── CjpmConfigParser.kt       # 配置解析器实现
│   │   ├── DependencyConfig.kt
│   │   ├── TargetConfig.kt
│   │   └── ...
│   ├── dependency/
│   │   ├── CjpmDependencyResolver.kt # 依赖解析器实现
│   │   ├── CjpmPackageManager.kt     # 包管理器实现
│   │   ├── CjpmRepositoryProvider.kt # 仓库提供者实现
│   │   ├── CjpmDependencyGraph.kt    # cjpm依赖图
│   │   └── ...
│   ├── build/
│   │   ├── CjpmBuildSystem.kt        # 构建系统实现
│   │   ├── CjpmBuildSystemProvider.kt # 构建系统提供者
│   │   ├── CjpmBuildTaskExecutor.kt  # 任务执行器实现
│   │   ├── CjpmBuildTask.kt          # cjpm构建任务
│   │   ├── CjpmBuildContext.kt       # cjpm构建上下文
│   │   └── ...
│   ├── toolwindow/
│   │   ├── CjpmToolWindow.kt
│   │   ├── CjpmProjectTree.kt
│   │   └── ...
│   └── watcher/
│       └── CjpmTomlWatcher.kt        # 文件监听
├── src/main/resources/
│   ├── messages/
│   │   └── CjpmBundle.properties
│   └── META-INF/
│       └── cjpm.xml
└── build.gradle.kts
```

#### 扩展实现注册

```xml
<!-- cjpm.xml -->
<extensions defaultExtensionNs="org.cangnova.cangjie.project">
    <!-- 注册cjpm项目提供者 -->
    <projectProvider implementation="org.cangnova.cangjie.cjpm.project.CjpmProjectProvider"/>

    <!-- 注册cjpm配置解析器 -->
    <configParser implementation="org.cangnova.cangjie.cjpm.config.CjpmConfigParser"/>
</extensions>

<extensions defaultExtensionNs="org.cangnova.cangjie.dependency">
<!-- 注册cjpm依赖解析器 -->
<dependencyResolver implementation="org.cangnova.cangjie.cjpm.dependency.CjpmDependencyResolver"/>

<!-- 注册cjpm包管理器 -->
<packageManager implementation="org.cangnova.cangjie.cjpm.dependency.CjpmPackageManager"/>

<!-- 注册cjpm仓库提供者 -->
<repositoryProvider implementation="org.cangnova.cangjie.cjpm.dependency.CjpmRepositoryProvider"/>
</extensions>

<extensions defaultExtensionNs="org.cangnova.cangjie.buildsystem">
<!-- 注册cjpm构建系统提供者 -->
<buildSystemProvider implementation="org.cangnova.cangjie.cjpm.build.CjpmBuildSystemProvider"/>

<!-- 注册cjpm任务执行器 -->
<taskExecutor implementation="org.cangnova.cangjie.cjpm.build.CjpmBuildTaskExecutor"/>
</extensions>
```

#### 依赖关系

```
cjpm 依赖:
  - cangjie-project (项目核心抽象)
  - cangjie-dependency (依赖管理核心)
  - build-system (构建系统核心)
  - toolchain (SDK管理)
  - util (工具类)
  - messages (国际化)
  - notifications (通知)
```

---

### 7. kcjpm 模块 (未来扩展)

**职责**: kcjpm项目的具体实现

#### 核心功能

1. **项目实现**
    - `KcjpmProject` - 实现 `CjProject` 接口
    - `KcjpmProjectProvider` - 实现 `CjProjectProvider` 扩展点

2. **配置解析**
    - `KcjpmConfigParser` - 实现 `CjProjectConfigParser` 扩展点
    - 解析kcjpm特定的配置格式

3. **依赖管理**
    - `KcjpmDependencyResolver` - 实现 `CjDependencyResolver` 扩展点
    - `KcjpmPackageManager` - 实现 `CjPackageManager` 扩展点

4. **构建系统**
    - `KcjpmBuildSystem` - 实现 `CjBuildSystem` 接口
    - `KcjpmBuildSystemProvider` - 实现 `CjBuildSystemProvider` 扩展点
    - `KcjpmBuildTaskExecutor` - 实现 `CjBuildTaskExecutor` 扩展点

#### 扩展实现注册

```xml
<!-- kcjpm.xml -->
<extensions defaultExtensionNs="org.cangnova.cangjie.project">
    <projectProvider implementation="org.cangnova.cangjie.kcjpm.project.KcjpmProjectProvider"/>
    <configParser implementation="org.cangnova.cangjie.kcjpm.config.KcjpmConfigParser"/>
</extensions>

<extensions defaultExtensionNs="org.cangnova.cangjie.dependency">
<dependencyResolver implementation="org.cangnova.cangjie.kcjpm.dependency.KcjpmDependencyResolver"/>
<packageManager implementation="org.cangnova.cangjie.kcjpm.dependency.KcjpmPackageManager"/>
</extensions>

<extensions defaultExtensionNs="org.cangnova.cangjie.buildsystem">
<buildSystemProvider implementation="org.cangnova.cangjie.kcjpm.build.KcjpmBuildSystemProvider"/>
<taskExecutor implementation="org.cangnova.cangjie.kcjpm.build.KcjpmBuildTaskExecutor"/>
</extensions>
```

---

### 5. build-system 模块 (重构为扩展点架构)

**职责**: 定义构建系统的核心抽象和扩展点机制

#### 核心功能

1. **构建抽象接口**
    - `CjBuildSystem` - 构建系统抽象
    - `CjBuildTask` - 构建任务抽象
    - `CjBuildContext` - 构建上下文
    - `CjBuildResult` - 构建结果

2. **构建任务管理**
    - `CjBuildTaskManager` - 任务管理服务
    - `CjBuildQueue` - 构建队列
    - `CjBuildSessionManager` - 会话管理

3. **Build事件系统**
    - `CjBuildEvent` - 构建事件
    - `CjBuildListener` - 构建监听器
    - `CjBuildProgressReporter` - 进度报告

4. **扩展点定义**
    - `CjBuildSystemProvider` - 构建系统提供者扩展点
    - `CjBuildTaskExecutor` - 任务执行器扩展点
    - `CjBuildConfigurationProvider` - 构建配置提供者扩展点

#### 目录结构

```
build-system/
├── src/main/kotlin/org/cangnova/cangjie/buildsystem/
│   ├── model/
│   │   ├── CjBuildSystem.kt          # 构建系统抽象
│   │   ├── CjBuildTask.kt            # 构建任务抽象
│   │   ├── CjBuildContext.kt         # 构建上下文
│   │   ├── CjBuildResult.kt          # 构建结果
│   │   └── CjBuildConfiguration.kt   # 构建配置
│   ├── service/
│   │   ├── CjBuildTaskManager.kt     # 任务管理服务
│   │   └── impl/
│   │       └── CjBuildTaskManagerImpl.kt
│   ├── queue/
│   │   ├── CjBuildQueue.kt           # 构建队列
│   │   ├── CjBuildSessionManager.kt  # 会话管理
│   │   └── CjBuildQueueStrategy.kt   # 队列策略
│   ├── event/
│   │   ├── CjBuildEvent.kt           # 构建事件
│   │   ├── CjBuildListener.kt        # 构建监听器
│   │   └── CjBuildProgressReporter.kt # 进度报告
│   ├── extension/
│   │   ├── CjBuildSystemProvider.kt  # 构建系统提供者扩展点
│   │   ├── CjBuildTaskExecutor.kt    # 任务执行器扩展点
│   │   └── CjBuildConfigurationProvider.kt # 配置提供者扩展点
│   ├── adapter/
│   │   ├── IntellijBuildAdapter.kt   # IntelliJ Build窗口适配
│   │   └── BuildEventConverter.kt    # 事件转换器
│   └── task/
│       ├── BeforeRunTaskProvider.kt  # Before Run Task基类
│       └── BuildTaskRunnerBase.kt    # TaskRunner基类
├── src/main/resources/
│   ├── messages/
│   │   └── BuildSystemBundle.properties
│   └── META-INF/
│       └── build-system.xml
└── build.gradle.kts
```

#### 扩展点机制

```xml
<!-- build-system.xml -->
<extensionPoints>
    <!-- 构建系统提供者:识别和创建构建系统 -->
    <extensionPoint name="buildSystemProvider"
                    qualifiedName="org.cangnova.cangjie.buildsystem.buildSystemProvider"
                    interface="org.cangnova.cangjie.buildsystem.extension.CjBuildSystemProvider"
                    dynamic="true"/>

    <!-- 任务执行器:执行具体的构建任务 -->
    <extensionPoint name="taskExecutor"
                    qualifiedName="org.cangnova.cangjie.buildsystem.taskExecutor"
                    interface="org.cangnova.cangjie.buildsystem.extension.CjBuildTaskExecutor"
                    dynamic="true"/>

    <!-- 构建配置提供者:提供构建配置 -->
    <extensionPoint name="configurationProvider"
                    qualifiedName="org.cangnova.cangjie.buildsystem.configurationProvider"
                    interface="org.cangnova.cangjie.buildsystem.extension.CjBuildConfigurationProvider"
                    dynamic="true"/>
</extensionPoints>

        <!-- 服务注册 -->
<extensions defaultExtensionNs="com.intellij">
<!-- 项目级构建任务管理器 -->
<projectService
        serviceInterface="org.cangnova.cangjie.buildsystem.service.CjBuildTaskManager"
        serviceImplementation="org.cangnova.cangjie.buildsystem.service.impl.CjBuildTaskManagerImpl"/>

<!-- 项目任务运行器 -->
<projectTaskRunner
        implementation="org.cangnova.cangjie.buildsystem.task.BuildTaskRunnerBase"/>
</extensions>
```

#### 核心接口设计

##### CjBuildSystem

```kotlin
interface CjBuildSystem {
    /**
     * 构建系统名称 (如 "cjpm", "kcjpm")
     */
    val name: String

    /**
     * 构建系统版本
     */
    val version: String?

    /**
     * 是否可用
     */
    val isAvailable: Boolean

    /**
     * 判断是否适用于该项目
     */
    fun isApplicable(project: CjProject): Boolean

    /**
     * 创建构建任务
     */
    fun createBuildTask(project: CjProject, config: CjBuildConfiguration): CjBuildTask

    /**
     * 获取支持的构建类型
     */
    fun getSupportedBuildTypes(): List<String>  // ["debug", "release", "test"]
}
```

##### CjBuildSystemProvider (扩展点)

```kotlin
interface CjBuildSystemProvider {
    /**
     * 提供者优先级
     */
    val priority: Int

    /**
     * 判断是否可以为该项目提供构建系统
     */
    fun canProvideBuildSystem(project: CjProject): Boolean

    /**
     * 创建构建系统实例
     */
    fun createBuildSystem(project: CjProject): CjBuildSystem?
}
```

##### CjBuildTaskExecutor (扩展点)

```kotlin
interface CjBuildTaskExecutor {
    /**
     * 执行器名称
     */
    val name: String

    /**
     * 判断是否可以执行该任务
     */
    fun canExecute(task: CjBuildTask): Boolean

    /**
     * 执行构建任务
     */
    suspend fun execute(
        task: CjBuildTask,
        context: CjBuildContext,
        progressReporter: CjBuildProgressReporter
    ): CjBuildResult

    /**
     * 取消任务执行
     */
    fun cancel(task: CjBuildTask)
}
```

##### CjBuildTask

```kotlin
interface CjBuildTask {
    /**
     * 任务ID
     */
    val id: String

    /**
     * 任务名称
     */
    val name: String

    /**
     * 所属项目
     */
    val project: CjProject

    /**
     * 构建配置
     */
    val configuration: CjBuildConfiguration

    /**
     * 构建类型 (debug/release/test)
     */
    val buildType: String

    /**
     * 任务状态
     */
    var status: BuildTaskStatus

    enum class BuildTaskStatus {
        PENDING,      // 等待中
        RUNNING,      // 执行中
        SUCCEEDED,    // 成功
        FAILED,       // 失败
        CANCELLED     // 已取消
    }
}
```

##### CjBuildContext

```kotlin
interface CjBuildContext {
    /**
     * 当前任务
     */
    val task: CjBuildTask

    /**
     * 工作目录
     */
    val workingDirectory: Path

    /**
     * 环境变量
     */
    val environment: Map<String, String>

    /**
     * 编译器路径
     */
    val compilerPath: Path?

    /**
     * 依赖列表
     */
    val dependencies: List<CjDependency>

    /**
     * 输出目录
     */
    val outputDirectory: Path

    /**
     * 用户数据存储
     */
    val userData: MutableMap<String, Any>
}
```

#### 依赖关系

```
build-system 依赖:
  - cangjie-project (获取项目信息)
  - cangjie-dependency (获取依赖信息)
  - toolchain (获取SDK路径)
  - util (工具类)
  - messages (国际化)
  - notifications (构建通知)
```

---

### 6. cjpm 模块 (扩展实现)

**职责**: cjpm项目的完整实现

#### 核心功能

1. **项目实现**
    - `CjpmProject` - 实现 `CjProject` 接口
    - `CjpmWorkspace` - 实现 `CjWorkspace` 接口
    - `CjpmProjectProvider` - 实现 `CjProjectProvider` 扩展点

2. **配置解析**
    - `CjpmTomlParser` - 解析 manifest.toml
    - `CjpmTomlConfig` - TOML配置模型
    - `CjpmConfigParser` - 实现 `CjProjectConfigParser` 扩展点

3. **依赖管理**
    - `CjpmDependencyResolver` - 实现 `CjDependencyResolver` 扩展点
    - `CjpmPackageManager` - 实现 `CjPackageManager` 扩展点
    - `CjpmRepositoryProvider` - 实现 `CjRepositoryProvider` 扩展点
    - cjpm依赖图构建

4. **构建系统** (新增)
    - `CjpmBuildSystem` - 实现 `CjBuildSystem` 接口
    - `CjpmBuildSystemProvider` - 实现 `CjBuildSystemProvider` 扩展点
    - `CjpmBuildTaskExecutor` - 实现 `CjBuildTaskExecutor` 扩展点
    - `CjpmBuildTask` - cjpm构建任务实现

5. **UI集成**
    - `CjpmToolWindow` - cjpm工具窗口
    - `CjpmProjectTree` - 项目树视图
    - `CjpmConfigurable` - 配置页面
1. **编译执行**
    - `CjpmBuildTaskRunner` - 任务执行器
    - `CjpmBuildManager` - 构建管理器
    - `CjpmBuildContext` - 构建上下文

2. **构建队列**
    - `CjpmBuildSessionsQueueManager` - 队列管理
    - 并发控制和任务调度

3. **IDE集成**
    - Build Tool Window集成
    - Progress Indicators
    - Build事件转换

4. **Before Run Tasks**
    - `CjpmBuildTaskProvider` - 运行前构建

#### 目录结构

```
build-system/
├── src/main/kotlin/org/cangnova/cangjie/buildsystem/
│   ├── api/
│   │   ├── CangJieBuildSystem.kt
│   │   ├── CangJieCompileContext.kt
│   │   └── CangJieProjectBuilder.kt
│   ├── impl/
│   │   ├── cjpm/
│   │   │   └── CjpmBuildSystem.kt
│   │   └── cjc/
│   │       └── CjcBuildSystem.kt
│   ├── task/
│   │   ├── CjpmBuildTaskRunner.kt
│   │   ├── CjpmBuildManager.kt
│   │   ├── CjpmBuildContext.kt
│   │   ├── CjpmBuildSessionsQueueManager.kt
│   │   └── CjpmBuildTaskProvider.kt
│   └── adapter/
│       ├── CjpmBuildAdapter.kt
│       └── CjBuildEventsConverter.kt
├── src/main/resources/
│   ├── messages/
│   │   ├── CangJieBuildSystemBundle.properties
│   │   └── CangJieBuildSystemBundle_zh_CN.properties
│   └── META-INF/
│       └── build-system.xml
└── build.gradle.kts
```

#### 依赖关系

```
build-system 依赖:
  - cjpm-project (获取项目信息)
  - toolchain (获取编译器路径)
  - util (工具类)
  - messages (国际化)
  - notifications (构建通知)
```

---

### 3. toolchain 模块 (已重构)

**职责**: SDK路径管理和版本检测

#### 核心功能

1. **SDK注册**
    - `CjSdkRegistry` - SDK注册中心
    - SDK持久化存储

2. **SDK检测**
    - `CjSdkDetector` - 检测器接口
    - `CjSdkDiscoverer` - 自动发现

3. **版本管理**
    - 版本解析
    - 有效性检查

#### 特性

- ✅ 扩展点机制
- ✅ 多平台支持
- ✅ 自动发现SDK
- ✅ 持久化配置

---

## 数据流和依赖关系

### 编译流程

```
用户触发Build
    ↓
[build-system] CjpmBuildTaskRunner
    ↓
查询项目信息 → [cjpm-project] CjpmProjectsService
    ↓
获取SDK路径 → [toolchain] CjSdkRegistry
    ↓
[build-system] CjpmBuildManager 执行编译
    ↓
[build-system] CjpmBuildAdapter 处理输出
    ↓
显示结果到Build Tool Window
```

### 项目加载流程

```
打开项目
    ↓
[cjpm-project] CjpmProjectOpenProcessor
    ↓
解析manifest.toml → [cjpm-project] CjpmTomlParser
    ↓
创建项目模型 → [cjpm-project] CjpmProjectsServiceImpl
    ↓
索引依赖 → [cjpm-project] CjpmPackageIndex
    ↓
显示项目结构 → [cjpm-project] CjpmToolWindow
```

---

## 迁移计划

### Phase 1: 创建 cjpm-project 模块

1. 创建模块目录结构
2. 添加 build.gradle.kts
3. 创建 META-INF/cjpm-project.xml
4. 定义核心接口

### Phase 2: 迁移代码

1. 从 plugin 模块移动项目模型代码到 cjpm-project
    - `org/cangnova/cangjie/cjpm/project/model/` → `cjpm-project/`
    - `org/cangnova/cangjie/cjpm/project/toml/` → `cjpm-project/`
    - `org/cangnova/cangjie/cjpm/project/workspace/` → `cjpm-project/`
    - `org/cangnova/cangjie/cjpm/project/toolwindow/` → `cjpm-project/`

2. 从 build-system 移除项目模型依赖
    - 移除对 CjpmProject 的直接引用
    - 通过服务接口获取项目信息

### Phase 3: 清理 build-system

1. 移除 CangJieToolchain (已完成)
2. 只保留编译执行相关代码
3. 通过 cjpm-project 服务获取项目配置

### Phase 4: 更新依赖

1. 更新各模块的 build.gradle.kts
2. 更新 plugin.xml 的模块引用
3. 更新导入语句

### Phase 5: 测试验证

1. 单元测试
2. 集成测试
3. 构建插件测试

---

## 优势

### 职责清晰

- **cjpm-project**: 知道项目"是什么"
- **build-system**: 知道如何"编译"
- **toolchain**: 知道SDK"在哪里"

### 模块独立

- 各模块可以独立开发和测试
- 降低耦合度
- 提高可维护性

### 扩展性强

- 可以轻松添加新的构建系统实现
- 可以支持多种项目类型
- 可以集成外部构建工具

### 性能优化

- 项目模型可以独立缓存
- 构建可以并行执行
- SDK检测可以异步进行

---

## 注意事项

1. **向后兼容**: 迁移过程中保持API兼容
2. **渐进式重构**: 分阶段进行,每个阶段都能构建成功
3. **文档同步**: 及时更新README和代码注释
4. **测试覆盖**: 确保每个模块都有充分的测试

---

## 总结

通过这次重构,我们将:

- ✅ 提高代码的可维护性
- ✅ 降低模块间的耦合
- ✅ 提升系统的可扩展性
- ✅ 优化性能和用户体验
