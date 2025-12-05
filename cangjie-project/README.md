# cangjie-project 模块

cangjie-project 是仓颉语言 IntelliJ 插件的核心项目管理模块，负责项目模型定义、项目解析、依赖管理和构建系统集成。

## 一、仓颉项目模型设计

### 1.1 核心模型层次

```mermaid
classDiagram
    class CjProject {
        <<interface>>
        +name: String
        +rootDir: VirtualFile
        +isWorkspace: Boolean
        +module: CjModule
        +workspace: CjWorkspace?
        +refresh()
        +findModule(name): CjModule?
    }

    class CjWorkspace {
        <<interface>>
        +name: String
        +members: List~CjModule~
        +buildMembers: List~CjModule~
        +testMembers: List~CjModule~
    }

    class CjModule {
        <<interface>>
        +name: String
        +rootDir: VirtualFile
        +configFile: VirtualFile?
        +sourceSets: List~CjSourceSet~
        +dependencies: List~CjDependency~
        +testDependencies: List~CjDependency~
        +metadata: CjPackageMetadata
    }

    class CjSourceSet {
        <<interface>>
        +name: String
        +sourceRoots: List~VirtualFile~
        +resourceRoots: List~VirtualFile~
        +isTest: Boolean
    }

    CjProject "1" --> "0..1" CjWorkspace : workspace
    CjProject "1" --> "1" CjModule : module
    CjWorkspace "1" --> "*" CjModule : members
    CjModule "1" --> "*" CjSourceSet : sourceSets
```

### 1.2 依赖模型

```mermaid
classDiagram
    class CjDependency {
        <<sealed>>
        +name: String
        +scope: CjDependencyScope
        +optional: Boolean
        +transitive: Boolean
        +features: Set~String~
    }

    class Library {
        +versionReq: VersionRequirement
        +registry: String?
    }

    class Path {
        +path: Path
        +versionReq: VersionRequirement
    }

    class Git {
        +url: String
        +ref: GitRef
        +versionReq: VersionRequirement
    }

    class Stdlib {
        +versionReq: VersionRequirement
    }

    class Binary {
        +cjoPath: Path
        +libPath: Path?
        +target: String?
    }

    CjDependency <|-- Library
    CjDependency <|-- Path
    CjDependency <|-- Git
    CjDependency <|-- Stdlib
    CjDependency <|-- Binary
```

```mermaid
classDiagram
    class CjDependencyScope {
        <<enumeration>>
        COMPILE
        TEST
        RUNTIME
        PROVIDED
    }

    class GitRef {
        <<sealed>>
    }

    class Branch {
        +name: String
    }

    class Tag {
        +name: String
    }

    class Rev {
        +hash: String
    }

    GitRef <|-- Branch
    GitRef <|-- Tag
    GitRef <|-- Rev
```

### 1.3 包模型

```mermaid
classDiagram
    class CjPackage {
        <<sealed>>
        +id: PackageId
        +metadata: CjPackageMetadata
        +dependencies: List~CjDependency~
        +features: Map~String, Feature~
    }

    class LocalModule {
        +module: CjModule
    }

    class LibraryPkg {
        +registryUrl: String?
    }

    class GitPkg {
        +url: String
        +commitId: String
        +checkoutPath: Path
    }

    class PathPkg {
        +localPath: Path
    }

    class StdlibPkg {
        +sdkPath: Path
    }

    class BinaryPkg {
        +cjoPath: Path
        +libPath: Path?
        +target: String?
    }

    class Failed {
        +reason: String
    }

    CjPackage <|-- LocalModule
    CjPackage <|-- LibraryPkg
    CjPackage <|-- GitPkg
    CjPackage <|-- PathPkg
    CjPackage <|-- StdlibPkg
    CjPackage <|-- BinaryPkg
    CjPackage <|-- Failed
```

### 1.4 依赖图模型

```mermaid
classDiagram
    class ResolvedGraph {
        +packages: Map~PackageId, CjPackage~
        +dependencies: Map~PackageId, List~ResolvedDependency~~
        +root: PackageId
        +enabledFeatures: Map~PackageId, Set~String~~
        +validate(): Pair~Boolean, String?~
        +topologicalSort(): List~PackageId~
    }

    class PackageId {
        +name: String
        +version: CjVersion
        +sourceId: SourceId
    }

    class ResolvedDependency {
        +declaration: CjDependency
        +resolvedTo: PackageId
        +enabledFeatures: Set~String~
        +scope: CjDependencyScope
    }

    ResolvedGraph "1" --> "*" PackageId
    ResolvedGraph "1" --> "*" ResolvedDependency
    ResolvedDependency --> PackageId : resolvedTo
```

---

## 二、项目解析流程

### 2.1 项目发现流程

```mermaid
flowchart TD
    A[IDE 打开目录] --> B[CjProjectOpenedActivity.runActivity]
    B --> C[CjProjectsService.discoverProject]
    C --> D{遍历 CjProjectProvider 扩展点}
    D --> E[CjpmProjectProvider]
    D --> F[其他构建系统...]
    E --> G{存在 cjpm.toml?}
    G -->|是| H[创建 CjProject 实例]
    G -->|否| I[尝试下一个 Provider]
    F --> I
    I --> D
    H --> J[注册到 CjProjectsService]
    J --> K[发布 CjProjectEvent.CREATED]
```

### 2.2 配置解析流程

```mermaid
flowchart TD
    A[CjProjectProvider.createProject] --> B[CjProjectConfigParser.parse]
    B --> C[解析 cjpm.toml]
    C --> D{判断项目类型}
    D -->|单模块| E[解析 package 配置]
    D -->|工作空间| F[解析 workspace 配置]

    E --> G[PackageConfig]
    F --> H[WorkspaceConfig]
    H --> I[遍历 members]
    I --> J[递归解析子模块]

    G --> K[创建 CjModule]
    J --> K

    K --> L[解析 dependencies]
    K --> M[解析 test-dependencies]
    K --> N[解析 target.platform 配置]

    L --> O[CjDependency 列表]
    M --> O
    N --> P[平台特定依赖]
    P --> O

    O --> Q[CjpmModuleImpl]
```

### 2.3 依赖解析流程

```mermaid
flowchart TD
    A[CjDependencyService.resolveGraph] --> B[提取 replace 规则]
    B --> C[初始化 BFS 队列]
    C --> D[添加根包到队列]

    D --> E{队列非空?}
    E -->|否| R[构建 ResolvedGraph]
    E -->|是| F[取出包]

    F --> G[遍历依赖声明]
    G --> H{是间接依赖?}
    H -->|是| I{有 replace 规则?}
    I -->|是| J[应用替换]
    I -->|否| K[使用原依赖]
    H -->|否| K
    J --> K

    K --> L{目标平台匹配?}
    L -->|否| G
    L -->|是| M{可选且未启用?}
    M -->|是| G
    M -->|否| N[CjDependencyResolver.resolve]

    N --> O{解析成功?}
    O -->|否| G
    O -->|是| P{已解析过?}
    P -->|是| Q[检查版本冲突]
    P -->|否| S[添加到图中]

    Q --> G
    S --> T{传递依赖?}
    T -->|是| U[加入队列]
    T -->|否| G
    U --> E

    R --> V[验证依赖图]
    V --> W[返回 Result]
```

### 2.4 依赖解析器选择

```mermaid
flowchart TD
    A[CjDependencyResolver.resolve] --> B{依赖类型}

    B -->|Stdlib| C[resolveStdlibDependency]
    C --> D[从 SDK 获取标准库路径]
    D --> E[CjPackage.Stdlib]

    B -->|Binary| F[resolveBinaryDependency]
    F --> G[解析 .cjo 文件路径]
    G --> H[查找对应 .so/.a 库文件]
    H --> I[CjPackage.Binary]

    B -->|Git/Path/Library| J[查找 cjpm.lock]
    J --> K{lock 文件存在?}
    K -->|是| L[resolveFromLockFile]
    K -->|否| M[CjPackage.Failed]

    L --> N{锁定类型}
    N -->|Git| O[resolveLockedGitDependency]
    N -->|Path| P[resolveLockedPathDependency]
    N -->|Registry| Q[resolveLockedRegistryDependency]

    O --> R[~/.cjpm/git/name/commitId]
    P --> S[项目相对路径]
    Q --> T[暂不支持]

    R --> U[解析 cjpm.toml]
    S --> U
    U --> V[CjPackage.Git/Path]
```

### 2.5 项目同步流程

```mermaid
sequenceDiagram
    participant User
    participant RefreshAction
    participant SyncTask
    participant ProjectsService
    participant DependencyService
    participant WorkspaceSync
    participant EventBus

    User->>RefreshAction: 点击刷新
    RefreshAction->>SyncTask: 启动同步任务
    SyncTask->>ProjectsService: discoverProject()
    ProjectsService->>ProjectsService: 解析项目配置

    SyncTask->>DependencyService: resolveGraph()
    DependencyService->>DependencyService: BFS 解析依赖
    DependencyService-->>SyncTask: ResolvedGraph

    SyncTask->>WorkspaceSync: sync()
    WorkspaceSync->>WorkspaceSync: 更新 IntelliJ 模块
    WorkspaceSync->>WorkspaceSync: 添加库依赖

    SyncTask->>EventBus: 发布 UPDATED 事件
    EventBus-->>User: UI 更新
```

---

## 三、层级架构

### 3.1 模块分层

```mermaid
graph TB
    subgraph UI["UI Layer"]
        TW[ToolWindow]
        PT[ProjectTree]
        Actions[Actions]
    end

    subgraph Service["Service Layer"]
        PS[CjProjectsService]
        DS[CjDependencyService]
        BS[CjBuildTaskManager]
    end

    subgraph Extension["Extension Layer"]
        PP[CjProjectProvider]
        DR[CjDependencyResolver]
        PM[CjPackageManager]
        RP[CjRepositoryProvider]
    end

    subgraph Model["Model Layer"]
        Project[CjProject]
        Module[CjModule]
        Dep[CjDependency]
        Pkg[CjPackage]
        Graph[ResolvedGraph]
    end

    subgraph Infra["Infrastructure Layer"]
        WS[WorkspaceModelSync]
        EV[Event System]
        CW[ConfigWatcher]
    end

    UI --> Service
    Service --> Extension
    Service --> Model
    Extension --> Model
    Infra --> Service
    Infra --> Model
```

### 3.2 包结构

```mermaid
graph LR
    subgraph cangjie-project
        subgraph project["project/"]
            model["model/"]
            extension["extension/"]
            service["service/"]
            task["task/"]
            workspace["workspace/"]
            listener["listener/"]
            event["event/"]
            ui["ui/"]
        end

        subgraph ext["extension/"]
            dr[CjDependencyResolver]
            pm[CjPackageManager]
            rp[CjRepositoryProvider]
        end

        subgraph registry["registry/"]
            pr[CjPackageRegistry]
        end

        subgraph run["run/"]
            cfg[RunConfiguration]
            build[BuildManager]
            target[TargetEnv]
        end
    end
```

### 3.3 扩展点机制

```mermaid
flowchart LR
    subgraph Core["cangjie-project (核心)"]
        EP1[CjProjectProvider EP]
        EP2[CjDependencyResolver EP]
        EP3[CjPackageManager EP]
        EP4[CjRepositoryProvider EP]
    end

    subgraph CJPM["cjpm (实现)"]
        I1[CjpmProjectProvider]
        I2[CjpmDependencyResolver]
        I3[CjpmPackageManager]
        I4[CjpmRepositoryProvider]
    end

    subgraph Future["未来构建系统"]
        F1[XxxProjectProvider]
        F2[XxxDependencyResolver]
    end

    I1 -.->|implements| EP1
    I2 -.->|implements| EP2
    I3 -.->|implements| EP3
    I4 -.->|implements| EP4

    F1 -.->|implements| EP1
    F2 -.->|implements| EP2
```

### 3.4 事件系统

```mermaid
stateDiagram-v2
    [*] --> CREATED: 项目识别成功
    CREATED --> OPENED: 打开项目
    OPENED --> UPDATED: 刷新/配置变更
    UPDATED --> UPDATED: 再次刷新
    OPENED --> CONFIG_CHANGED: cjpm.toml 变更
    CONFIG_CHANGED --> UPDATED: 触发刷新
    OPENED --> REMOVED: 关闭项目
    UPDATED --> REMOVED: 关闭项目
    REMOVED --> [*]
```

---

## 四、与其他模块的关系

```mermaid
graph TB
    subgraph Plugin["plugin (分发层)"]
        P[plugin.xml]
    end

    subgraph Features["功能层"]
        CJPM[cjpm]
        CP[cangjie-project]
        DBG[debugger]
        LSP[lsp4ij]
    end

    subgraph Core["核心层"]
        PSI[psi]
        ANALYSIS[analysis]
    end

    subgraph Base["基础层"]
        UTIL[util]
        COMMON[common]
        TC[toolchain]
    end

    P --> CJPM
    P --> CP
    P --> DBG
    P --> LSP

    CJPM -->|实现扩展点| CP
    DBG -->|使用项目信息| CP
    LSP -->|使用项目信息| CP

    CP --> PSI
    CP --> TC
    CJPM --> TC

    PSI --> COMMON
    CP --> UTIL
```

### 依赖关系说明

| 模块        | 关系  | 说明                                             |
|-----------|-----|------------------------------------------------|
| cjpm      | 实现者 | 实现 CjProjectProvider、CjDependencyResolver 等扩展点 |
| psi       | 依赖  | 使用语法解析支持                                       |
| debugger  | 消费者 | 使用项目信息进行调试配置                                   |
| lsp4ij    | 消费者 | 使用项目信息配置 LSP                                   |
| toolchain | 依赖  | 使用 SDK 和工具链信息                                  |

---

## 五、关键设计决策

### 5.1 Project Model 优先架构

```mermaid
graph LR
    subgraph Primary["主数据源"]
        PM[CjProject/CjModule]
    end

    subgraph Secondary["辅助"]
        WM[Workspace Model]
    end

    subgraph IDE["IDE 子系统"]
        IDX[索引]
        NAV[导航]
        REF[引用解析]
    end

    PM -->|单向同步| WM
    WM --> IDE
    PM -->|直接使用| IDE
```

### 5.2 依赖解析缓存策略

```mermaid
flowchart TD
    A[解析依赖请求] --> B{检查缓存}
    B -->|命中| C[返回缓存结果]
    B -->|未命中| D[执行解析]
    D --> E{解析成功?}
    E -->|是| F[缓存成功结果]
    E -->|否| G[缓存失败结果]
    F --> H[返回结果]
    G --> H

    subgraph CacheKey["缓存键"]
        K1[dependency.id]
        K2[enabledFeatures]
    end
```

### 5.3 Replace 规则应用

```mermaid
flowchart TD
    A[根模块] --> B[提取 replace 规则]
    B --> C[解析依赖 A]
    C --> D[A 的依赖 B]
    D --> E{B 在 replace 中?}
    E -->|是| F[使用替换后的 B']
    E -->|否| G[使用原始 B]
    F --> H[继续解析 B' 的依赖]
    G --> H

    style A fill:#f9f,stroke:#333
    style F fill:#9f9,stroke:#333
```

---

## 六、配置文件格式

### cjpm.toml 结构

```mermaid
graph TD
    subgraph TOML["cjpm.toml"]
        PKG["[package]"]
        WS["[workspace]"]
        DEP["[dependencies]"]
        TDEP["[test-dependencies]"]
        SDEP["[script-dependencies]"]
        REP["[replace]"]
        TGT["[target.platform]"]
        FFI["[ffi]"]
        PROF["[profile]"]
    end

    PKG --> |单模块| M1[name, version, output-type]
    WS --> |多模块| M2[members, build-members]
    DEP --> M3[源码依赖]
    TDEP --> M4[测试依赖]
    REP --> M5[依赖替换]
    TGT --> M6[平台特定配置]
    TGT --> M7[bin-dependencies]
```