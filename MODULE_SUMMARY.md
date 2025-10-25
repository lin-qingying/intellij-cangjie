# 仓颉IntelliJ插件模块重构总结

## 📊 模块概览

### 新增模块 (4个)

1. **cangjie-project** - 项目模型核心
2. **cangjie-dependency** - 依赖管理核心
3. **cjpm** - cjpm完整实现
4. **build-system** (重构) - 构建系统核心

### 重构模块 (2个)

1. **build-system** - 重构为扩展点架构
2. **toolchain** - SDK管理(已完成重构)

### 未来扩展 (1个)

1. **kcjpm** - kcjpm完整实现

---

## 🎯 核心设计原则

### 1. 关注点分离

- **项目模型** (cangjie-project): 定义"项目是什么"
- **依赖管理** (cangjie-dependency): 管理"依赖如何解析"
- **构建系统** (build-system): 定义"如何编译"的抽象
- **SDK管理** (toolchain): 管理"SDK在哪里"
- **具体实现** (cjpm/kcjpm): 实现所有扩展点

### 2. 扩展点机制

所有具体实现通过扩展点注册,支持多种包管理器:

- **cjpm** (当前): 实现项目、依赖、构建三大扩展点
- **kcjpm** (未来): 同样实现三大扩展点
- 其他第三方实现

### 3. 依赖层次清晰

```
┌─────────────────────────────────────────┐
│            plugin (入口)                 │
└────────────┬────────────────────────────┘
             │
      ┌──────┴──────────────┬────────────┐
      ↓                     ↓            ↓
┌───────────┐      ┌────────────┐  ┌──────────┐
│   cjpm    │      │  kcjpm     │  │toolchain │
│  (扩展)   │      │  (扩展)    │  │  (SDK)   │
└─────┬─────┘      └──────┬─────┘  └──────────┘
      │                   │
  ┌───┴───────┬───────────┴───┬──────────┐
  ↓           ↓               ↓          ↓
┌────────┐ ┌──────────┐ ┌────────────┐
│cangjie-│ │ cangjie- │ │build-system│
│project │ │dependency│ │  (核心)    │
│(核心)  │ │  (核心)  │ └────────────┘
└────────┘ └──────────┘
```

---

## 📦 模块详细规划

### 1. cangjie-project (项目模型核心)

#### 职责范围

✅ 定义项目抽象接口
✅ 提供项目管理服务
✅ 项目发现和索引
✅ 项目事件发布
❌ 不包含具体项目实现
❌ 不包含依赖解析逻辑

#### 核心接口

```kotlin
interface CjProject {
    val name: String
    val rootDir: VirtualFile
    val modules: List<CjModule>
    val workspace: CjWorkspace?
}

interface CjProjectProvider {
    fun canHandle(dir: VirtualFile): Boolean
    fun createProject(dir: VirtualFile, project: Project): CjProject?
}
```

#### 扩展点

- `org.cangnova.cangjie.project.projectProvider`
- `org.cangnova.cangjie.project.configParser`

#### 依赖

- util
- messages
- notifications
- psi
- toolchain

---

### 2. cangjie-dependency (依赖管理核心)

#### 职责范围

✅ 定义依赖抽象模型
✅ 提供依赖解析服务
✅ 依赖图构建和管理
✅ 包缓存和注册表
❌ 不包含具体解析实现
❌ 不包含仓库访问实现

#### 核心接口

```kotlin
interface CjDependency {
    val name: String
    val version: CjVersion
    val scope: CjDependencyScope
    val type: CjDependencyType
}

interface CjDependencyResolver {
    val priority: Int
    fun canResolve(dependency: CjDependency): Boolean
    fun resolve(dependency: CjDependency, project: Project): CjResolvedDependency?
}

interface CjPackageManager {
    val name: String
    fun downloadPackage(dependency: CjDependency, targetDir: Path): Boolean
    fun isPackageDownloaded(dependency: CjDependency): Boolean
}

interface CjRepositoryProvider {
    val repositoryName: String
    val repositoryUrl: String
    fun searchPackage(name: String): List<CjPackage>
    fun downloadPackage(pkg: CjPackage, targetDir: Path): Boolean
}
```

#### 扩展点

- `org.cangnova.cangjie.dependency.dependencyResolver`
- `org.cangnova.cangjie.dependency.packageManager`
- `org.cangnova.cangjie.dependency.repositoryProvider`

#### 服务

- `CjDependencyService` (应用级)
- `CjPackageRegistry` (项目级)

#### 依赖

- util
- messages
- notifications

---

### 3. cjpm (cjpm实现)

#### 职责范围

✅ 实现CjProject接口(CjpmProject)
✅ 解析cjpm.toml配置
✅ 实现cjpm依赖解析器
✅ 实现cjpm包管理器
✅ 提供cjpm工具窗口UI
❌ 不定义核心抽象

#### 扩展实现

```xml
<!-- 项目相关 -->
<projectProvider implementation="CjpmProjectProvider"/>
<configParser implementation="CjpmConfigParser"/>

<!-- 依赖相关 -->
<dependencyResolver implementation="CjpmDependencyResolver"/>
<packageManager implementation="CjpmPackageManager"/>
<repositoryProvider implementation="CjpmRepositoryProvider"/>
```

#### 关键组件

- **CjpmProject**: 表示一个cjpm项目
- **CjpmWorkspace**: 表示一个多模块工作空间
- **CjpmTomlParser**: 解析cjpm.toml
- **CjpmDependencyResolver**: 解析cjpm依赖
- **CjpmPackageManager**: 管理cjpm包下载
- **CjpmToolWindow**: cjpm工具窗口

#### 依赖

- cangjie-project
- cangjie-dependency
- util
- messages
- notifications

---

### 4. build-system (构建系统核心)

#### 职责范围

✅ 定义构建系统抽象接口
✅ 提供构建任务管理服务
✅ 构建队列和会话管理
✅ Build事件系统
✅ IDE集成(Build Tool Window)
❌ 不包含具体构建实现
❌ 不执行实际编译命令

#### 核心接口

```kotlin
interface CjBuildSystem {
    val name: String
    val isAvailable: Boolean
    fun isApplicable(project: CjProject): Boolean
    fun createBuildTask(project: CjProject, config: CjBuildConfiguration): CjBuildTask
}

interface CjBuildSystemProvider {
    val priority: Int
    fun canProvideBuildSystem(project: CjProject): Boolean
    fun createBuildSystem(project: CjProject): CjBuildSystem?
}

interface CjBuildTaskExecutor {
    val name: String
    fun canExecute(task: CjBuildTask): Boolean
    suspend fun execute(
        task: CjBuildTask,
        context: CjBuildContext,
        progressReporter: CjBuildProgressReporter
    ): CjBuildResult
}
```

#### 扩展点

- `org.cangnova.cangjie.buildsystem.buildSystemProvider`
- `org.cangnova.cangjie.buildsystem.taskExecutor`
- `org.cangnova.cangjie.buildsystem.configurationProvider`

#### 服务

- `CjBuildTaskManager` (项目级)
- `CjBuildQueue` - 构建队列
- `CjBuildSessionManager` - 会话管理

#### 依赖

- cangjie-project
- cangjie-dependency
- toolchain
- util
- messages
- notifications

---

### 5. cjpm (cjpm完整实现)

#### 职责范围

✅ 执行编译任务
✅ 构建队列管理
✅ Build事件处理
✅ IDE工具窗口集成
✅ Before Run Tasks
❌ 不管理项目模型
❌ 不解析依赖

#### 核心组件(保留)

- `CjpmBuildTaskRunner` - 任务执行器
- `CjpmBuildManager` - 构建管理器
- `CjpmBuildContext` - 构建上下文
- `CjpmBuildSessionsQueueManager` - 队列管理
- `CjpmBuildAdapter` - Build事件适配
- `CjBuildEventsConverter` - 事件转换

#### 依赖变更

```diff
build-system 依赖:
+ - cangjie-project (获取项目信息)
+ - cangjie-dependency (获取依赖信息)
  - toolchain (获取编译器)
  - util
  - messages
  - notifications
```

---

### 5. toolchain (已重构)

#### 职责范围

✅ SDK注册和存储
✅ SDK检测和发现
✅ SDK版本管理
✅ 扩展点机制
❌ 不执行编译
❌ 不管理项目配置

#### 核心API

- `CjSdkRegistry` - SDK注册中心
- `CjSdk` - SDK数据模型
- `CjSdkDetector` - SDK检测器(扩展点)
- `CjSdkDiscoverer` - SDK发现器(扩展点)

#### 已完成的重构

✅ 删除ToolchainSettingsState
✅ 删除CjToolchainServices
✅ 更新CangJieProjectSettingsService
✅ 重构UI组件
✅ 更新build-system引用

---

## 🔄 迁移计划

### Phase 1: 创建核心模块 ⏳

1. 创建cangjie-project模块
2. 创建cangjie-dependency模块
3. 重构build-system为扩展点架构
4. 定义核心接口和扩展点
5. 编写README和文档

### Phase 2: 创建cjpm模块 ⏳

1. 创建cjpm模块结构
2. 从plugin迁移代码:
    - `org/cangnova/cangjie/cjpm/project/` → cjpm模块
    - `org/cangnova/cangjie/cjpm/config/` → cjpm模块
    - `org/cangnova/cangjie/cjpm/dependency/` → cjpm模块
    - `org/cangnova/cangjie/ide/run/cjpm/runconfig/buildtool/` → cjpm模块

### Phase 3: 重构现有代码 ⏳

1. 将现有CjpmBuildTaskRunner等迁移到cjpm模块
2. 实现新的扩展点接口
3. 更新依赖关系

### Phase 4: 更新配置和测试 ⏳

1. 更新settings.gradle.kts
2. 更新各模块build.gradle.kts
3. 更新plugin.xml
4. 编写单元测试
5. 集成测试

---

## ✨ 架构优势

### 1. 可扩展性

- ✅ 轻松添加新的包管理器(kcjpm等)
- ✅ 支持第三方扩展
- ✅ 模块化设计便于维护

### 2. 职责清晰

- ✅ 每个模块职责单一
- ✅ 模块间耦合度低
- ✅ 易于理解和修改

### 3. 代码复用

- ✅ 核心抽象可被多种实现共享
- ✅ 依赖管理逻辑统一
- ✅ 减少重复代码

### 4. 性能优化

- ✅ 独立缓存机制
- ✅ 并行解析依赖
- ✅ 异步SDK检测

---

## 📈 模块依赖图

```
                    plugin
                      │
        ┌─────────────┼──────────────┐
        │             │              │
      cjpm        kcjpm(未来)    toolchain
        │             │              │
   ┌────┴────┐   ┌────┴────┐        │
   │         │   │         │        │
cangjie-  cangjie- build-  │        │
project  dependency system │        │
   │         │       │     │        │
   └─────┬───┴───────┴─────┴────────┘
         │
    ┌────┴────┬────────┬────────┐
    │         │        │        │
   util   messages  notifications  psi
```

---

## 🎓 最佳实践

### 1. 扩展点使用

```kotlin
// 定义扩展点
interface CjProjectProvider {
    fun canHandle(dir: VirtualFile): Boolean
    fun createProject(dir: VirtualFile, project: Project): CjProject?
}

// 实现扩展点
class CjpmProjectProvider : CjProjectProvider {
    override fun canHandle(dir: VirtualFile) =
        dir.findChild("cjpm.toml") != null

    override fun createProject(dir: VirtualFile, project: Project) =
        CjpmProject(dir, project)
}
```

### 2. 服务访问

```kotlin
// 获取项目服务
val projectService = project.service<CjProjectsService>()
val projects = projectService.allProjects

// 获取依赖服务
val depService = CjDependencyService.getInstance()
val dependencies = depService.resolveDependencies(project)
```

### 3. 事件监听

```kotlin
// 监听项目变化
project.messageBus.connect().subscribe(
    CjProjectListener.TOPIC,
    object : CjProjectListener {
        override fun projectUpdated(project: CjProject) {
            // 处理项目更新
        }
    }
)
```

---

## 📝 注意事项

1. **向后兼容**: 迁移过程保持API兼容性
2. **渐进式重构**: 分阶段实施,每个阶段都能编译
3. **文档同步**: 及时更新README和注释
4. **测试覆盖**: 确保每个模块都有充分测试
5. **性能监控**: 关注模块加载和服务初始化性能

---

## 🚀 下一步行动

### 立即执行

1. ✅ 完成架构设计文档
2. ⏳ 修复当前编译错误
3. ⏳ 创建cangjie-project模块
4. ⏳ 创建cangjie-dependency模块

### 后续计划

1. 迁移现有代码到新模块
2. 重构build-system
3. 创建cjpm模块
4. 编写单元测试
5. 集成测试和性能优化

---

## 📚 相关文档

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 详细架构设计
- [CLAUDE.md](./CLAUDE.md) - 开发指南
- [toolchain/README.md](./toolchain/README.md) - Toolchain模块文档

---

**生成时间**: 2025-10-24
**架构版本**: v2.0
**状态**: 设计完成,待实施
