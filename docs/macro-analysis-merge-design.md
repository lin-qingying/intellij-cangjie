# 宏展开文件与源文件的分析合并方案

## Context

源文件中通过宏调用生成的声明（如 `@Derive` 生成 getter 方法），在当前架构下无法被源文件引用。宏展开文件虽然已经正确生成，但分析结果没有正确地集成到主分析流程中。存在三个核心 Bug 导致此问题。

**展开文件特征**：包含源文件的完整内容（原始代码 + 宏展开代码），是源文件的"完整替换版本"。

## 根因分析

### Bug 1: 展开文件未注入主 Facade

`CangJieCacheServiceImpl.GlobalFacade.facadeForModules` 创建时 `macroExcludedSourceFiles` 始终为空。展开文件仅在 `createChildWithMacroExpandedFiles()` 创建的子 Facade 中可见。

**后果**：当文件 A 引用文件 B 中宏生成的声明时，文件 A 的分析看不到该声明。

**位置**：`CangJieCacheServiceImpl.kt` 第 243-250 行

### Bug 2: 源文件未从 Stub 索引中排除

`PluginDeclarationProviderFactory.createPackageMemberDeclarationProvider()` 使用未过滤的 `indexedFilesScope`。当展开文件通过 `macroFileBasedDeclarationProviderFactory` 加入时，源文件的声明（Stub 索引）和展开文件的声明同时可见。

**后果**：`class Foo` 同时存在两份 → REDECLARATION 错误。

**位置**：`PluginDeclarationProviderFactory.kt` 第 170-178 行

### Bug 3: MacroMergedBindingContext 仅处理宏展开区域内的元素

`MacroMergedBindingContext.get()` 仅当 `isInMacroExpansionRegion(key)` 为 true 时才查询展开文件的 BindingContext。但普通代码（如 `foo.getX()`）调用宏生成的声明时，调用点不在宏展开区域内。

**后果**：`foo.getX()` 始终显示为 unresolved reference。

**位置**：`MacroMergedBindingContext.kt`

## 方案设计：展开文件替换

### 核心思路

展开文件是源文件的"完整替换版本"。将展开文件在主 Facade 的 DeclarationProvider 层替换源文件，使整个项目的分析都能看到宏生成的声明。分析结果通过偏移映射回报到源文件。

```
当前架构（有 Bug）：
  主 Facade: 分析源文件 → 结果 A（看不到宏声明）
  子 Facade: 分析展开文件 → 结果 B
  合并 A + B → 仅宏区域内元素有效

新架构：
  主 Facade: 展开文件替换源文件 → 完整结果
  IDE 查询时: 源文件 PSI → 偏移映射 → 展开文件 PSI → 查 BindingContext → 返回结果
```

### 过期处理

当展开文件过期（源文件比展开文件更新）时，回退到纯源文件分析（不显示宏生成的声明），等重新展开后再更新。

---

## 实现步骤

### 步骤 1: 创建 MacroExpandedFileModificationTracker

**新建文件**：`macro/src/main/kotlin/.../macro/expanded/MacroExpandedFileModificationTracker.kt`

```kotlin
@Service(Service.Level.PROJECT)
class MacroExpandedFileModificationTracker(project: Project) : ModificationTracker {
    private val counter = AtomicLong(0)
    fun increment() { counter.incrementAndGet() }
    override fun getModificationCount(): Long = counter.get()

    companion object {
        fun getInstance(project: Project): MacroExpandedFileModificationTracker =
            project.service()
    }
}
```

**修改文件**：`macro/src/main/kotlin/.../macro/expanded/MacroExpandedFileManager.kt`
- 在 `processAndWrite()`、`clearAll()` 中调用 `MacroExpandedFileModificationTracker.getInstance(project).increment()`

### 步骤 2: 创建展开文件收集器

**修改文件**：`analysis/src/main/kotlin/.../macro/analysis/MacroExpandedAnalysisBridge.kt`

新增方法：
```kotlin
fun collectAllExpandedCjFiles(project: Project): List<CjFile> {
    val manager = MacroExpandedFileManager.getInstance(project)
    val expandedVirtualFiles = manager.getAllExpandedFiles()
    val psiManager = PsiManager.getInstance(project)
    return expandedVirtualFiles.mapNotNull { vf ->
        // 过期检查：源文件比展开文件新则跳过
        val sourceVf = manager.findOriginalFile(vf) ?: return@mapNotNull null
        if (sourceVf.modificationStamp > vf.modificationStamp) return@mapNotNull null
        psiManager.findFile(vf) as? CjFile
    }
}
```

**修改文件**：`macro/src/main/kotlin/.../macro/expanded/MacroExpandedFileManager.kt`

新增方法：
```kotlin
fun getAllExpandedFiles(): List<VirtualFile>
fun findOriginalFile(expandedFile: VirtualFile): VirtualFile?
```

### 步骤 3: 注入展开文件到主 Facade

**修改文件**：`analysis/src/main/kotlin/.../resolve/caches/CangJieCacheServiceImpl.kt`

在 `GlobalFacade` 内部类中修改 `facadeForModules` 的创建：

```kotlin
val facadeForModules = run {
    val expandedFiles = MacroExpandedAnalysisBridge.collectAllExpandedCjFiles(project)
    ProjectResolutionFacade(
        "facadeForModules", modulesResolverName,
        project, modulesContext,
        reuseDataFrom = facadeForLibraries,
        moduleFilter = { true },
        dependencies = listOf(
            ProjectRootModificationTracker.getInstance(project),
            MacroExpandedFileModificationTracker.getInstance(project)  // 新增依赖
        ),
        invalidateOnOOCB = true,
        macroExcludedSourceFiles = expandedFiles  // 新增：展开文件注入
    )
}
```

### 步骤 4: 在 PluginDeclarationProviderFactory 中排除源文件

**修改文件**：`analysis/src/main/kotlin/.../stubindex/resolve/PluginDeclarationProviderFactory.kt`

```kotlin
class PluginDeclarationProviderFactory(
    private val project: Project,
    private val indexedFilesScope: GlobalSearchScope,
    private val storageManager: StorageManager,
    private val nonIndexedFiles: Collection<CjFile>,
    private val context: ModuleInfo,
    private val macroExcludedFiles: Collection<CjFile> = emptyList()
) : AbstractDeclarationProviderFactory(storageManager) {

    // 新增：计算需要从 Stub 索引中排除的源文件
    private val macroExcludedSourceVirtualFiles: Set<VirtualFile> by lazy {
        macroExcludedFiles.mapNotNull { expandedCjFile ->
            val expandedVf = expandedCjFile.virtualFile ?: return@mapNotNull null
            MacroExpandedFileManager.getInstance(project).findOriginalFile(expandedVf)
        }.toSet()
    }

    // 新增：过滤后的 Stub 索引作用域
    private val filteredIndexedFilesScope: GlobalSearchScope by lazy {
        if (macroExcludedSourceVirtualFiles.isEmpty()) indexedFilesScope
        else MacroSourceFileFilteringScope(indexedFilesScope, macroExcludedSourceVirtualFiles)
    }

    // 修改：使用过滤后的作用域
    override fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider? {
        val stubBasedProvider = StubBasedPackageMemberDeclarationProvider(
            name, project, filteredIndexedFilesScope
        )
        val fileBasedProvider = fileBasedDeclarationProviderFactory.getPackageMemberDeclarationProvider(name)
        val macroFileBasedProvider = macroFileBasedDeclarationProviderFactory?.getPackageMemberDeclarationProvider(name)
        val providers = listOfNotNull(stubBasedProvider, fileBasedProvider, macroFileBasedProvider)
        if (providers.isEmpty()) return null
        return CombinedPackageMemberDeclarationProvider(providers)
    }
}

// 新增内部类：过滤特定 VirtualFile 的作用域
private class MacroSourceFileFilteringScope(
    private val delegate: GlobalSearchScope,
    private val excludedFiles: Set<VirtualFile>
) : DelegatingGlobalSearchScope(delegate) {
    override fun contains(file: VirtualFile): Boolean =
        file !in excludedFiles && super.contains(file)
}
```

### 步骤 5: 重写 MacroTranslatingBindingContext

**修改文件**：`analysis/src/main/kotlin/.../macro/analysis/MacroMergedBindingContext.kt`

将 `MacroMergedBindingContext` 重写为 `MacroTranslatingBindingContext`：

**核心变化**：不再是"合并两个 BindingContext"，而是"翻译查询：源文件 PSI → 展开文件 PSI → 委托 BindingContext"。

```kotlin
class MacroTranslatingBindingContext(
    private val delegate: BindingContext,
    private val expandedFile: CjFile,
    private val sourceFile: CjFile,
    private val mapping: MacroExpansionOffsetMapping
) : BindingContext {

    override fun <K : Any, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        // 1. 直接查找（非 PSI 键，或键本身属于展开文件）
        delegate[slice, key]?.let { return it }

        // 2. 如果键是源文件的 PSI 元素，映射到展开文件元素后查找
        if (key is PsiElement && key.containingFile?.virtualFile == sourceFile.virtualFile) {
            val expandedElement = mapSourceToExpandedElement(key)
            if (expandedElement != null) {
                @Suppress("UNCHECKED_CAST")
                delegate[slice, expandedElement as K]?.let { return it }
            }
        }
        return null
    }

    override val diagnostics: Diagnostics
        get() = MacroTranslatedDiagnostics(delegate.diagnostics, expandedFile, sourceFile, mapping)

    override fun getType(expression: CjExpression): CangJieType? {
        delegate.getType(expression)?.let { return it }
        if (expression.containingFile?.virtualFile == sourceFile.virtualFile) {
            val expandedExpr = mapSourceToExpandedElement(expression) as? CjExpression
            if (expandedExpr != null) {
                return delegate.getType(expandedExpr)
            }
        }
        return null
    }

    // 通过偏移映射找到展开文件中对应位置的 PSI 元素
    private fun mapSourceToExpandedElement(sourceElement: PsiElement): PsiElement? {
        val sourceOffset = sourceElement.textOffset
        val expandedOffset = mapping.mapOriginalOffsetToExpanded(sourceOffset) ?: return null
        return expandedFile.findElementAt(expandedOffset)
            ?.let { PsiTreeUtil.getParentOfType(it, sourceElement::class.java, false) }
    }
}
```

### 步骤 6: 添加反向偏移映射

**修改文件**：`macro/src/main/kotlin/.../macro/expanded/MacroExpansionOffsetMapping.kt`

新增方法（目前只有展开→源的映射，缺少源→展开的映射）：

```kotlin
fun mapOriginalOffsetToExpanded(originalOffset: Int): Int? {
    // 根据行映射计算：源文件行号 → 展开文件行号
    // 非宏区域内列号保持不变
}

fun mapOriginalLineToExpanded(originalLine: Int): Int? {
    // 遍历行映射，找到源文件行号对应的展开文件行号
    for (lineMapping in lineMappings) {
        if (lineMapping.originalLine == originalLine && !lineMapping.isMacroExpansion) {
            return lineMapping.expandedLine
        }
    }
    return null
}
```

### 步骤 7: 简化 ResolutionFacadeImpl

**修改文件**：`analysis/src/main/kotlin/.../resolve/ResolutionFacadeImpl.kt`（或 `ModuleResolutionFacadeImpl`）

将 `mergeWithExpandedAnalysis()` 简化为结果包装：

```kotlin
// 旧逻辑（删除）：创建子 Facade → 分析展开文件 → 合并 BindingContext
// 新逻辑：主 Facade 已包含展开文件，只需包装 BindingContext 做偏移映射

private fun wrapWithMacroTranslation(element: CjElement, result: AnalysisResult): AnalysisResult {
    if (result.isError()) return result
    val bridge = MacroExpandedAnalysisBridge.getInstance(project)
    if (!bridge.isEnabled()) return result

    val sourceFile = element.containingFile as? CjFile ?: return result
    val expandedFile = bridge.getExpandedPsiFile(sourceFile) ?: return result
    val mapping = bridge.getOffsetMapping(sourceFile) ?: return result

    // 过期检查
    val sourceVf = sourceFile.virtualFile ?: return result
    val expandedVf = expandedFile.virtualFile ?: return result
    if (sourceVf.modificationStamp > expandedVf.modificationStamp) return result

    val translatingContext = MacroTranslatingBindingContext(
        delegate = result.bindingContext,
        expandedFile = expandedFile,
        sourceFile = sourceFile,
        mapping = mapping
    )
    return AnalysisResult.success(translatingContext, result.moduleDescriptor)
}
```

### 步骤 8: 清理不再需要的代码

**修改文件**：
- `MacroExpandedAnalysisBridge.kt` — 删除 `expandedFacades` 缓存、`getOrCreateExpandedFacade()`、`mergeAnalysisResults()` 等子 Facade 相关方法
- `ProjectResolutionFacade.kt` — 可保留 `createChildWithMacroExpandedFiles()` 但标记 @Deprecated
- `MacroPipelineCoordinatorImpl.kt` — 在展开完成后调用 `MacroExpandedFileModificationTracker.getInstance(project).increment()` 触发主 Facade 缓存失效

---

## 数据流总结

```
项目同步 / 文件保存
    ↓
MacroPipelineCoordinator.schedule*Pipeline()
    ├─ 编译宏包
    └─ 展开所有文件 → MacroExpandedFileManager 写入展开文件
        └─ MacroExpandedFileModificationTracker.increment()
            └─ 触发 facadeForModules 缓存失效
                ↓
facadeForModules 重建
    ├─ 收集非过期展开文件 → macroExcludedSourceFiles
    └─ 传入 IdeaResolverForProject → ModuleContent → AnalyzerFacade
        ↓
PluginDeclarationProviderFactory
    ├─ filteredIndexedFilesScope (排除有展开版本的源文件)
    ├─ StubBasedProvider (查过滤后的 Stub 索引)
    ├─ FileBasedProvider (非索引文件)
    └─ MacroFileBasedProvider (展开文件声明)
        ↓
分析运行 → BindingContext 以展开文件 PSI 为键
        ↓
IDE 查询源文件元素时
    ↓
MacroTranslatingBindingContext
    ├─ 源文件 PSI → 偏移映射 → 展开文件 PSI → 查 BindingContext
    └─ 诊断：展开文件诊断 → 偏移映射 → 源文件位置
```

## 需要修改的文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `macro/.../expanded/MacroExpandedFileModificationTracker.kt` | 新建 | 追踪展开文件变更的 ModificationTracker |
| `macro/.../expanded/MacroExpandedFileManager.kt` | 修改 | 添加 `getAllExpandedFiles()`、`findOriginalFile()`、increment tracker |
| `macro/.../expanded/MacroExpansionOffsetMapping.kt` | 修改 | 添加 `mapOriginalOffsetToExpanded()` 反向映射 |
| `analysis/.../macro/analysis/MacroExpandedAnalysisBridge.kt` | 修改 | 添加 `collectAllExpandedCjFiles()`，删除子 Facade 相关代码 |
| `analysis/.../macro/analysis/MacroMergedBindingContext.kt` | 重写 | 改为 `MacroTranslatingBindingContext`，处理所有元素 |
| `analysis/.../resolve/caches/CangJieCacheServiceImpl.kt` | 修改 | `GlobalFacade.facadeForModules` 注入展开文件和 tracker |
| `analysis/.../stubindex/resolve/PluginDeclarationProviderFactory.kt` | 修改 | 添加 Stub 索引源文件过滤 + `MacroSourceFileFilteringScope` |
| `analysis/.../resolve/ResolutionFacadeImpl.kt` | 修改 | 简化 merge 逻辑为 translation wrapper |
| `macro/.../pipeline/MacroPipelineCoordinatorImpl.kt` | 修改 | 展开完成后 increment tracker |

## 验证方案

1. **单元测试**：创建包含宏调用的源文件和对应的展开文件，验证源文件中对宏生成声明的引用可以正确解析
2. **跨文件测试**：文件 A 引用文件 B 中宏生成的声明，验证解析成功
3. **类成员测试**：宏向已有类添加方法，验证方法可以被调用
4. **过期测试**：源文件比展开文件新时，验证回退到源文件分析
5. **IDE 集成测试**：`./gradlew :plugin:runIde`，在编辑器中打开含宏调用的文件，验证无红色错误标记、代码补全包含宏生成的声明、Go to Definition 可导航到宏生成的代码