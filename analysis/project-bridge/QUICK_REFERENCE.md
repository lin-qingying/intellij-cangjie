# AnalysisContext 快速参考

## 一句话总结

**用 `AnalysisContext` 替代 `ModuleInfo`，通过 `AnalysisContextProvider` 获取实例。**

---

## 核心 API

### 获取上下文

```kotlin
// 从文件获取
val context = project.analysisContextProvider.getContextForFile(file)

// 获取所有上下文
val allContexts = project.analysisContextProvider.getAllContexts()
```

### 使用上下文

```kotlin
context.contextId          // 标识符
context.scope              // 文件范围
context.dependencies       // 依赖列表
context.isSourceContext    // 是否为源码
```

### 扩展方法

```kotlin
context.scopeWithDependencies     // 包含依赖的范围
context.allDependencies           // 所有传递依赖
context.isLibraryContext          // 是否为库
```

---

## 常见模式

### 符号解析

```kotlin
fun resolveSymbol(name: String, context: AnalysisContext): Symbol? {
    // 在当前模块查找
    findInScope(name, context.scope)?.let { return it }

    // 在依赖中查找
    context.dependencies.forEach { dep ->
        findInScope(name, dep.scope)?.let { return it }
    }

    return null
}
```

### 全局分析

```kotlin
fun analyzeAll(project: Project) {
    val allContexts = project.analysisContextProvider.getAllContexts()

    allContexts.forEach { context ->
        if (context.isSourceContext) {
            analyzeSourceCode(context)
        }
    }
}
```

### 依赖遍历

```kotlin
fun printDependencies(context: AnalysisContext) {
    println("模块: ${context.contextId}")

    // 直接依赖
    context.dependencies.forEach { dep ->
        println("  ├─ ${dep.contextId}")
    }

    // 所有传递依赖
    val allDeps = context.allDependencies
    println("传递依赖共 ${allDeps.size} 个")
}
```

---

## 迁移速查表

| 旧 API (ModuleInfo) | 新 API (AnalysisContext) |
|-------------------|----------------------|
| `moduleInfo.contentScope` | `context.scope` |
| `moduleInfo.dependencies()` | `context.dependencies` |
| `moduleInfo.moduleOrigin == MODULE` | `context.isSourceContext` |
| `moduleInfo.project` | `context.project` |
| `moduleInfo.name.asString()` | `context.contextId` |
| 手动创建 ModuleInfo | `project.analysisContextProvider.getContextForFile(file)` |

---

## 配置检查清单

- [ ] 在 plugin.xml 中注册 `CjAnalysisContextProvider` 服务
- [ ] 在使用模块的 build.gradle.kts 中添加 `:analysis:project-bridge` 依赖
- [ ] 将现有 `ModuleInfo` 使用替换为 `AnalysisContext`
- [ ] 项目同步后清除缓存：`provider.clearCache()`

---

## 常见问题

**Q: 如何从 CjModule 获取 AnalysisContext？**
```kotlin
val context = cjModule.asAnalysisContext()
```

**Q: 如何判断文件属于哪个模块？**
```kotlin
val context = project.analysisContextProvider.getContextForFile(file)
println("文件属于模块: ${context?.contextId}")
```

**Q: ModuleInfo 还能用吗？**
```kotlin
// 可以，但已标记为 @Deprecated
// 建议尽快迁移到 AnalysisContext
```

**Q: 如何清除缓存？**
```kotlin
project.analysisContextProvider.clearCache()
```

---

## 文件位置

- **接口定义**: `analysis/src/main/kotlin/org/cangnova/cangjie/descriptors/`
  - `AnalysisContext.kt`
  - `AnalysisContextProvider.kt`

- **实现代码**: `analysis/project-bridge/src/main/kotlin/org/cangnova/cangjie/analysis/bridge/`
  - `CjAnalysisContextProvider.kt`
  - `CjModuleAnalysisContext.kt`
  - `CjLibraryAnalysisContext.kt`

- **详细文档**: `analysis/project-bridge/REFACTOR_SUMMARY.md`
