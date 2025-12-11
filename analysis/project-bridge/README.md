# Analysis Project Bridge 模块

## 概述

这是一个桥接模块，负责将 `cangjie-project` 的项目模型适配为 `analysis` 模块所需的分析上下文。

## 架构设计

```
┌──────────────────────────────────────┐
│         analysis 模块                 │
│  ┌────────────────────────────────┐  │
│  │  AnalysisContext (接口)         │  │
│  │  - contextId: String           │  │
│  │  - scope: GlobalSearchScope    │  │
│  │  - dependencies: List          │  │
│  │  - isSourceContext: Boolean    │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
                  ▲
                  │ 实现
                  │
┌──────────────────────────────────────┐
│   analysis:project-bridge 模块        │
│  ┌────────────────────────────────┐  │
│  │  CjModuleAnalysisContext       │  │
│  │  CjLibraryAnalysisContext      │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
                  │
                  │ 适配
                  ▼
┌──────────────────────────────────────┐
│      cangjie-project 模块             │
│  ┌────────────────────────────────┐  │
│  │  CjModule (项目模型)            │  │
│  │  CjDependency (依赖模型)        │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

## 核心类

### CjModuleAnalysisContext

将 `CjModule` 适配为 `AnalysisContext`，用于分析项目源码模块。

**特性：**
- 基于 CjModule 的 sourceSets 构建文件作用域
- 支持多源码集（main, test 等）
- 标记为源码上下文（isSourceContext = true）

**使用示例：**
```kotlin
val cjModule: CjModule = project.cjProject.findModule("my-module")
val context: AnalysisContext = cjModule.asAnalysisContext()

// 传递给分析器
val resolver = analyzerFacade.resolverFor(context)
```

### CjLibraryAnalysisContext

将 `CjDependency` 适配为 `AnalysisContext`，用于分析外部库依赖。

**支持的依赖类型：**
- `CjDependency.Library`：远程仓库依赖
- `CjDependency.Path`：本地路径依赖
- `CjDependency.Git`：Git 仓库依赖
- `CjDependency.Stdlib`：标准库
- `CjDependency.Binary`：二进制依赖

**使用示例：**
```kotlin
val dependency: CjDependency.Library = ...
val context: AnalysisContext = dependency.asAnalysisContext(project)
```

## 依赖关系

本模块依赖：
- `analysis`：提供 AnalysisContext 接口定义
- `cangjie-project`：提供 CjModule、CjDependency 等项目模型
- `psi`：提供 PSI 相关工具
- `util`、`common`：基础设施

## 使用场景

### 1. 代码分析

```kotlin
// 获取当前文件所属模块
val file: CjFile = ...
val cjModule = CjProjectsService.getInstance(project).findModuleForFile(file)

// 转换为分析上下文
val context = cjModule?.asAnalysisContext()

// 执行分析
if (context != null) {
    val resolutionFacade = ProjectResolutionFacade.getInstance(project)
    val analysisResult = resolutionFacade.analyzeInContext(file, context)
}
```

### 2. 依赖解析

```kotlin
// 获取模块的所有依赖上下文
fun getAllDependencyContexts(module: CjModule): List<AnalysisContext> {
    return module.dependencies.map { dep ->
        dep.asAnalysisContext(module.project.intellijProject)
    }
}
```

### 3. 符号查找

```kotlin
// 在模块及其依赖中查找符号
fun findSymbol(symbolName: String, module: CjModule): Symbol? {
    val context = module.asAnalysisContext()

    // 在当前模块中查找
    val localSymbol = findInScope(symbolName, context.scope)
    if (localSymbol != null) return localSymbol

    // 在依赖中查找
    for (depContext in context.dependencies) {
        val symbol = findInScope(symbolName, depContext.scope)
        if (symbol != null) return symbol
    }

    return null
}
```

## 设计原则

### 1. 最小化接口

`AnalysisContext` 只包含分析器必需的信息：
- `contextId`：标识符
- `scope`：文件范围
- `dependencies`：依赖列表
- `isSourceContext`：类型标识

### 2. 延迟计算

作用域和依赖信息使用 `lazy` 延迟计算，避免不必要的性能开销。

### 3. 解耦设计

- `analysis` 模块不知道 `CjModule` 的存在
- `cangjie-project` 模块不知道 `AnalysisContext` 的存在
- 本桥接模块是唯一知道两者的地方

## TODO

当前为初始版本，以下功能待实现：

### 1. 完整的依赖解析

```kotlin
// CjModuleAnalysisContext.dependencies
override val dependencies: List<AnalysisContext> by lazy {
    cjModule.dependencies.mapNotNull { dep ->
        when (dep) {
            is CjDependency.Library -> resolveLibraryDependency(dep)
            is CjDependency.Path -> resolvePathDependency(dep)
            // ... 其他类型
        }
    }
}
```

### 2. 库文件作用域

```kotlin
// CjLibraryAnalysisContext.scope
override val scope: GlobalSearchScope by lazy {
    when (dependency) {
        is CjDependency.Library -> findLibraryFiles(dependency)
        is CjDependency.Stdlib -> findStdlibFiles()
        // ... 其他类型
    }
}
```

### 3. 传递依赖处理

从库的元数据文件中提取传递依赖信息。

### 4. 缓存机制

添加 `AnalysisContextProvider` 服务，缓存已创建的上下文实例。

## 测试

测试用例位于 `src/test/kotlin`（待添加）：
- `CjModuleAnalysisContextTest`：测试源码模块适配
- `CjLibraryAnalysisContextTest`：测试库依赖适配
- 集成测试：端到端的分析流程测试

## 相关文档

- [AnalysisContext 设计文档](../src/main/kotlin/org/cangnova/cangjie/descriptors/AnalysisContext.kt)
- [ModuleInfo 接口文档](../src/main/kotlin/org/cangnova/cangjie/descriptors/ModuleInfo.kt)
- [CjModule 项目模型](../../cangjie-project/src/main/kotlin/org/cangnova/cangjie/project/model/CjModule.kt)
