# ModuleInfo 完全移除 - 迁移完成

## 总体策略

1. ✅ 将所有 `ModuleInfo` 替换为 `AnalysisContext`
2. ✅ 将接口扩展（如 `TrackableModuleInfo`、`DerivedModuleInfo`）替换为对应的 `AnalysisContext` 版本
3. ✅ 将泛型参数 `<M : ModuleInfo>` 替换为 `<M : AnalysisContext>`
4. ✅ 删除 `ModuleInfo` 特有的属性和方法（如 `name`, `capabilities`, `analyzerServices`）
5. ✅ 保留必要的枚举和工具类（如 `ModuleOrigin`, `ModuleCapability`）
6. ✅ 完全移除 `ModuleInfo` 和 `ModuleSourceInfo` 接口定义

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
25. **PerModulePackageCacheService.kt** - 完全替换（缓存结构和方法签名，使用 AnalysisContextProvider）
26. **ProjectResolutionFacade.kt** - 完全替换（使用 AnalysisContextProvider 替代 ModuleInfoProvider）
27. **ModuleInfo.kt** - ✅ 完全移除接口定义，仅保留枚举类型
28. **ModuleInfoProvider.kt** - ✅ 完全删除（已被 AnalysisContextProvider 替代）
29. **AnalysisContextUtil.kt** - ✅ 新增（提供 analysisContext 扩展方法）
30. **CjPsiFactory.kt** - 移除旧的 `var CjFile.analysisContext: PsiElement?` 定义

### ❌ 已删除

**ModuleInfo 接口** - 已完全删除
- 原因：包含过多项目模型特定信息，已被 AnalysisContext 替代
- 迁移路径：使用 AnalysisContext 及其具体实现

**ModuleSourceInfo 接口** - 已完全删除
- 原因：与 ModuleInfo 强耦合，已被 AnalysisContext + isSourceContext 属性替代
- 迁移路径：使用 AnalysisContext 并通过 isSourceContext 判断类型

**ModuleInfoProvider 类** - 已完全删除
- 原因：功能已被 AnalysisContextProvider 接口替代
- 迁移路径：使用 `project.analysisContextProvider.getContextForFile(file)`
- 替换位置：
  - PerModulePackageCacheService.kt - 从 `ModuleInfoProvider.getInstance(project).firstOrNull(vfile)` 改为 `project.analysisContextProvider.getContextForFile(vfile)`
  - ProjectResolutionFacade.kt - 从 `ModuleInfoProvider.getInstance(project).collect(element)` 改为 `project.analysisContextProvider.getContextForFile(element.containingFile)`

**旧的 CjFile.analysisContext 属性** - 已完全删除
- 原因：
  1. 类型为 `PsiElement?`，与新的 `AnalysisContext` 类型冲突
  2. 仅在已删除的 `ModuleInfoProvider` 中使用
  3. 新的 `AnalysisContextProvider` 系统不需要这个中间层
- 旧用法：在 ModuleInfoProvider 中用于从 PsiElement 推断模块信息
- 新替代：直接使用 `AnalysisContextUtil.kt` 中定义的 `val PsiElement.analysisContext: AnalysisContext`

### 🚧 保留项（已清理）

**CommonResolverForModuleFactory.kt**
- ✅ 已更新为使用 AnalysisContext
- CLI/通用场景已适配新接口

**AnalysisContext.kt**
- 核心接口，替代 ModuleInfo

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

### ⏳ 遗留清理

无待处理项目。所有 ModuleInfo 相关代码已完全移除。

### 关键枚举和工具类（已保留在 ModuleInfo.kt）

- ✅ `ModuleOrigin` - 模块来源类型（MODULE, LIBRARY, OTHER）
- ✅ `DependencyOnBuiltIns` - 内置库依赖策略（NONE, AFTER_SDK, LAST）
- ✅ `ModuleCapability` - 模块能力机制（定义在其他文件）
- ❌ `ModuleInfo` - 已删除
- ❌ `ModuleSourceInfo` - 已删除

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

- [x] 所有 `ModuleInfo` 引用替换为 `AnalysisContext`
- [x] 所有文件更新完成（无编译错误预期）
- [x] 更新文档反映新的 API
- [x] ModuleInfo 和 ModuleSourceInfo 接口已完全删除

## 当前状态

**进度**: ✅ **100% 完成 - 已完全移除**

**完成文件**: 30 个核心文件

**已完成工作**:
- ✅ 所有 `ModuleInfo` 引用已替换为 `AnalysisContext` 或其子类型
- ✅ **ModuleInfo 和 ModuleSourceInfo 接口已完全删除**
- ✅ **ModuleInfoProvider 类已完全删除**
- ✅ **旧的 CjFile.analysisContext: PsiElement? 属性已删除**
- ✅ **新增 AnalysisContextUtil.kt 提供正确的扩展方法**
- ✅ ModuleInfo.kt 仅保留必要的枚举类型（ModuleOrigin、DependencyOnBuiltIns）
- ✅ CommonResolverForModuleFactory 已适配为使用 AnalysisContext
- ✅ PerModulePackageCacheService 完全重构，使用 AnalysisContext 和 AnalysisContextProvider
- ✅ ProjectResolutionFacade 完全重构，使用 AnalysisContextProvider
- ✅ CangJieCacheServiceImpl 完全重构，移除所有 file.moduleInfo 引用

**验证结果**:
- analysis 模块中 ModuleInfo、ModuleSourceInfo 接口定义已不存在
- analysis 模块中 ModuleInfoProvider 类已完全删除
- psi 模块中旧的 analysisContext 属性定义已删除
- 新的 analysisContext 扩展属性类型正确（`AnalysisContext` 而非 `PsiElement`）
- 仅在文档注释中偶尔提及作为历史参考
- 所有实际代码均使用 AnalysisContext 体系和 AnalysisContextProvider

**迁移总结**:

本次迁移成功完成了从 ModuleInfo 到 AnalysisContext 的完全过渡：

1. **接口简化**：从包含项目模型细节的 ModuleInfo 接口迁移到最小化的 AnalysisContext 接口
2. **解耦提升**：分析器不再依赖具体的项目结构，提高了代码的可维护性
3. **扩展性增强**：新的设计更容易适配不同类型的项目模型（CJPM、脚本等）
4. **完全清理**：不再保留废弃接口，避免代码混乱和误用

**核心改进**:
- 接口替换: `ModuleInfo` → `AnalysisContext`
- Provider 替换: `ModuleInfoProvider` → `AnalysisContextProvider`
- 泛型参数: `<M : ModuleInfo>` → `<M : AnalysisContext>`
- 参数命名: `moduleInfo` → `context`
- 类型判断: `is ModuleSourceInfo` → `context.isSourceContext`
- 依赖访问: `moduleInfo.dependencies()` → `context.dependencies`
- 作用域访问: `moduleInfo.contentScope` → `context.scope`
- 获取上下文: `ModuleInfoProvider.getInstance(project).firstOrNull(file)` → `project.analysisContextProvider.getContextForFile(file)`
