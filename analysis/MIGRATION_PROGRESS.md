# ModuleInfo 完全移除 - 迁移进度跟踪

## 总体策略

1. 将所有 `ModuleInfo` 替换为 `AnalysisContext`
2. 将接口扩展（如 `TrackableModuleInfo`、`DerivedModuleInfo`）替换为对应的 `AnalysisContext` 版本
3. 将泛型参数 `<M : ModuleInfo>` 替换为 `<M : AnalysisContext>`
4. 删除 `ModuleInfo` 特有的属性和方法（如 `name`, `capabilities`, `analyzerServices`）
5. 保留必要的枚举和工具类（如 `ModuleOrigin`, `ModuleCapability`）

## 文件修改清单

### ✅ 已完成

1. **AnalysisContext.kt** - 新增核心接口
2. **AnalysisContextProvider.kt** - 新增 Provider 接口
3. **CjModuleAnalysisContext.kt** - 实现源码模块适配
4. **CjLibraryAnalysisContext.kt** - 实现库依赖适配
5. **CjAnalysisContextProvider.kt** - 实现 Provider 服务
6. **AnalyzerFacade.kt** - 完全替换（所有接口和类）
7. **PackageOracle.kt** - 完全替换
8. **PlatformDependentAnalyzerServices.kt** - 完全替换，提取 DependencyOnBuiltIns 为顶层枚举
9. **AbstractResolverForProject.kt** - 完全替换
10. **IdeaResolverForProject.kt** - 完全替换，更新 ModuleContent 数据类
11. **BuiltInsCacheKey.kt** - 完全替换
12. **ResolverForSingleModuleProject.kt** - 完全替换
13. **ResolutionFacade.kt** - 完全替换
14. **ResolutionFacadeImpl.kt** - 完全替换
15. **ResolveElementCache.kt** - 完全替换
16. **CangJieCacheService.kt** - 完全替换
17. **CangJieCacheServiceImpl.kt** - 完全替换
18. **ModuleResolutionFacadeImpl.kt** - 完全替换
19. **ProjectResolutionFacade.kt** - 完全替换
20. **ResolutionFacadeWithDebugInfo.kt** - 完全替换
21. **ResolveOptimizingOptionsProvider.kt** - 完全替换
22. **DeclarationProviderFactoryService.kt** - 完全替换
23. **ScopeUtils.kt** - 完全替换
24. **PluginDeclarationProviderFactory.kt** - 完全替换
25. **ModuleInfo.kt** - 提取 DependencyOnBuiltIns 枚举为顶层

### 🚧 保留项

**CommonResolverForModuleFactory.kt**
- 保留内部类 SourceModuleInfo 实现 ModuleInfo（用于 CLI/通用场景）
- 这是合理的，因为 ModuleInfo 仍然存在（标记为废弃）

**AnalysisContext.kt**
- 文档注释中提到 ModuleInfo（合理的向后兼容说明）

### 📊 替换统计

- **完全替换的核心概念：**
  - `ModuleInfo` → `AnalysisContext`
  - `TrackableModuleInfo` → `TrackableAnalysisContext`
  - `DerivedModuleInfo` → `DerivedAnalysisContext`
  - `moduleInfo` 参数名 → `context`
  - `ModuleInfo.DependencyOnBuiltIns` → `DependencyOnBuiltIns`（顶层枚举）
  - `ModuleContent<M : ModuleInfo>` → `ModuleContent<M : AnalysisContext>`
  - `moduleContentScope` → `scope`
  - `moduleInfoByDescriptor` → `contextByDescriptor`
  - `diagnoseUnknownModuleInfo` → `diagnoseUnknownContext`

- **文件范围：**
  - analysis/src/main/kotlin/org/cangnova/cangjie/resolve/ 目录及所有子目录
  - analysis/src/main/kotlin/org/cangnova/cangjie/descriptors/ 部分文件
  - analysis/src/main/kotlin/org/cangnova/cangjie/stubindex/resolve/ 目录

### ⏳ 待处理（可选）

26. **ModuleInfo.kt** - 决定最终处理方式
    - 选项A：继续保留为 @Deprecated 接口（推荐，保持向后兼容）
    - 选项B：完全删除接口定义（需要删除 SourceModuleInfo 等遗留使用）
    - **当前状态**：已废弃但保留，SourceModuleInfo 仍在使用

### 关键枚举和工具类（已提取为顶层）

- ✅ `ModuleOrigin` - 模块来源类型（MODULE, LIBRARY, OTHER）
- ✅ `DependencyOnBuiltIns` - 内置库依赖策略（NONE, AFTER_SDK, LAST）
- ✅ `ModuleCapability` - 模块能力机制
- ⚠️ `SourceModuleInfo` - CommonResolverForModuleFactory 中保留（CLI场景）

## 关键替换规则

### 接口继承

```kotlin
// 之前
interface TrackableModuleInfo : ModuleInfo

// 之后
interface TrackableAnalysisContext : AnalysisContext
```

### 泛型参数

```kotlin
// 之前
class ResolverForProject<M : ModuleInfo>

// 之后
class ResolverForProject<M : AnalysisContext>
```

### 函数参数

```kotlin
// 之前
fun analyze(moduleInfo: ModuleInfo)

// 之后
fun analyze(context: AnalysisContext)
```

### 属性访问

```kotlin
// 之前
moduleInfo.name
moduleInfo.contentScope
moduleInfo.dependencies()
moduleInfo.analyzerServices

// 之后
context.contextId
context.scope
context.dependencies
// analyzerServices 移到全局服务
```

### 能力键

```kotlin
// 之前
ModuleInfo.Capability

// 之后
AnalysisContextCapability
```

## 编译检查点

每完成5个文件，运行一次编译检查：

```bash
./gradlew :analysis:compileKotlin
```

## 迁移步骤

1. ✅ 先修改 AnalyzerFacade.kt（核心文件）
2. ⏳ 修改依赖 AnalyzerFacade 的文件
3. ⏳ 修改缓存和解析相关文件
4. ⏳ 最后处理 ModuleInfo.kt

## 完成标准

- [ ] 所有 `ModuleInfo` 引用替换为 `AnalysisContext`
- [ ] 所有文件编译通过
- [ ] 更新文档反映新的 API
- [ ] 运行测试确保功能正常

## 当前状态

**进度**: 约 95% 完成
**完成文件**: 25+ 个核心文件
**剩余工作**:
- ModuleInfo 接口可以选择性删除或继续保留为废弃状态
- CommonResolverForModuleFactory 中的 SourceModuleInfo 需要保留（CLI/通用场景使用）

**下一步建议**:
1. 运行 `./gradlew :analysis:compileKotlin` 检查编译错误
2. 修复任何编译错误（主要是泛型类型不匹配）
3. 运行测试确保功能正常
4. 决定是否完全删除 ModuleInfo 接口定义
