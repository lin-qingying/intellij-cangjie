# 宏展开新架构：PSI 替换 + 源码位置保留

> 设计文档：取代基于磁盘展开文件的分析方案，改用内存中 PSI 替换 + SyntheticResolveExtension 注入

## 1. 问题背景

### 1.1 编译器对表达式级宏的约束

仓颉编译器对宏展开有严格的类型求值要求：

```cangjie
// 宏声明
public macro abc(a: Tokens): Tokens {
    return quote($a 1)
}

// ❌ 表达式级使用：编译器需要求出宏表达式的类型
let a = @abc(1)
// 报错：expected one expr, but found exprs or other node
// main.cj(21, 11): the error occurs after the macro is expanded

// ✅ 独立使用：无人需要返回值，不会报错
@abc(51152552)
```

**核心矛盾**：当宏表达式处于需要求值的位置（如赋值右侧、函数参数等），
编译器必须将展开结果作为一个完整表达式进行类型检查。
当前的"独立文件分析"方案无法解决这个问题，因为展开文件的分析上下文与源文件断裂。

### 1.2 当前架构的缺陷

当前方案采用 **磁盘展开文件 + 独立分析 + 结果合并** 的路线：

```
源文件 → cjc-frontend --debug-macro → .macrocall 磁盘文件
    → MacroExpandedFileProcessor 剥离标记 → macro-expanded/main.cj
    → 独立分析展开文件 → MacroMergedBindingContext 合并
```

**主要问题**：

| 问题 | 说明 |
|------|------|
| **分析上下文断裂** | 展开文件作为独立文件分析，丢失了宏调用处的类型期望上下文 |
| **LspMacroServer 被禁用** | 因为不生成 `.macrocall` 磁盘文件，与磁盘展开文件架构不兼容 |
| **管线复杂** | 编译→展开→写磁盘→分析→合并，时序协调困难 |
| **性能损耗** | 磁盘 I/O、进程启动、文件监听、缓存失效等 |
| **表达式级宏无法正确类型检查** | 展开结果脱离原始上下文，编译器无法推导类型 |

### 1.3 当前相关代码

| 模块 | 关键文件 | 将被替代/重构 |
|------|---------|-------------|
| macro | `MacroExpandedFileManager` | 移除（不再需要磁盘展开文件） |
| macro | `MacroExpandedFileProcessor` | 移除（不再需要剥离编译器标记） |
| macro | `MacroExpansionOffsetMapping` | 重构为内存中的 PSI 级别映射 |
| macro | `MacroPipelineCoordinator` | 简化（不再需要协调磁盘文件） |
| macro | `MacroExpansionBackgroundTask` | 重构为 PSI 替换任务 |
| analysis | `MacroExpandedAnalysisBridge` | 移除（不再需要独立分析展开文件） |
| analysis | `MacroMergedBindingContext` | 移除（不再需要合并两个 BindingContext） |
| analysis | `MacroExpandedModuleInfoProviderExtension` | 移除（展开不再是独立文件） |
| analysis | `MacroExpansionFileListener` | 移除（不再监听磁盘展开文件变更） |

---

## 2. 新架构设计

### 2.1 核心思路

将宏展开从"生成独立文件"转变为"内存中 PSI 节点替换"：

```
源文件 PSI 树
  → 找到所有 CjMacroExpression 节点
  → 调用 LspMacroServer 获取展开文本
  → 解析展开文本为 PSI 子树
  → 在内存中替换 CjMacroExpression 节点为展开后的 PSI 子树
  → 记录原始宏调用位置的映射关系
  → 语义分析在修改后的 PSI 树上正常进行
  → 错误通过映射指回原始宏调用位置
```

### 2.2 两条路径

新架构区分 **表达式级宏** 和 **声明级宏** 两种场景：

#### 路径 A：表达式级宏 → PSI 替换

适用于：`let a = @abc(1)`、`foo(@bar(x))`、`return @baz()` 等

```
  用户代码:        let a = @abc(1)
                          ├── CjMacroExpression(@abc)
                          └── input: CjMacroInput(1)

  宏展开输出:      "1"（源码文本）

  PSI 替换:        把 CjMacroExpression 替换为解析后的 CjExpression(1)
                   但保留原始宏调用的位置信息

  分析:            let a = 1  ← 类型推导正常进行
                   如果类型不匹配，报错指向 @abc(1) 的位置
```

#### 路径 B：声明级宏 → SyntheticResolveExtension

适用于：`@Derive[Eq] class Foo { }`、`@macro struct Bar { }` 等

```
  用户代码:        @Derive[Eq]
                   class Foo {
                       let x: Int64
                   }

  宏展开输出:      新增 operator func ==(...) { ... }

  注入方式:        通过 SyntheticResolveExtension 向 Foo 注入合成方法
                   Foo.== 在语义层可见，但不修改用户源码的 PSI 树
```

### 2.3 整体流程图

```
                    源码: let a = @abc(1)
                              │
                         ① Parse（正常解析）
                              │
                    PSI: CjProperty
                         └── CjMacroExpression(@abc(1))
                              │                     │
                              │            ② 保存 originalTextRange
                              │                     │
                         ③ LspMacroServer 展开    startOffset=8
                              │                   endOffset=16
                         expandedText = "1"
                              │
                    ④ CjPsiFactory.createExpression("1")
                              │
                    ⑤ PsiElement.replace()（内存中）
                              │
                    PSI: CjProperty
                         └── CjConstantExpression(1)
                              │            │
                              │    ⑥ MacroSourceInfo 附加到节点
                              │       (originalElement → @abc(1) 的位置)
                              │
                    ⑦ 语义分析在修改后的 PSI 树上正常进行
                              │
                    ⑧ 如果报错 → 通过 MacroSourceInfo 定位到 @abc(1) ✅
```

---

## 3. 详细设计

### 3.1 LspMacroServer 引擎重新启用

当前 `LspMacroServerProviderFactory` 在 `cangjie-macro.xml` 中被注释禁用，
原因是"不生成 .macrocall 磁盘文件，与新的基于磁盘展开文件的分析架构不兼容"。

新架构不再需要磁盘文件，因此可以重新启用 LspMacroServer：

```xml
<!-- 重新启用 -->
<macroExpansionProviderFactory
        implementation="org.cangnova.cangjie.macro.server.LspMacroServerProviderFactory"/>
```

LspMacroServer 的优势在新架构中得到充分发挥：
- **常驻进程**：避免每次展开都启动 cjc-frontend 新进程
- **宏调用级粒度**：可以单独展开指定宏调用，与 PSI 替换天然匹配
- **管道通信**：低延迟，适合实时 PSI 替换

### 3.2 宏源码位置映射

#### 3.2.1 MacroSourceInfo 数据结构

```kotlin
/**
 * 宏展开源码位置信息
 *
 * 附加到展开后的 PSI 节点上，记录原始宏调用的位置，
 * 使诊断信息和导航能正确指向用户编写的宏调用。
 */
data class MacroSourceInfo(
    /** 原始宏调用的文件路径 */
    val originalFilePath: String,
    /** 原始宏调用的文本范围 */
    val originalTextRange: TextRange,
    /** 宏名称 */
    val macroName: String,
    /** 展开深度（嵌套宏展开时递增） */
    val expansionDepth: Int = 0,
    /** 父级 MacroSourceInfo（嵌套展开时链接） */
    val parent: MacroSourceInfo? = null
) {
    companion object {
        val KEY = Key.create<MacroSourceInfo>("MACRO_SOURCE_INFO")
    }
}
```

#### 3.2.2 位置信息附加方式

IntelliJ PSI 支持通过 `UserData` 在节点上存储自定义信息：

```kotlin
// 展开后的 PSI 节点附加源码位置信息
expandedPsi.putUserData(MacroSourceInfo.KEY, MacroSourceInfo(
    originalFilePath = sourceFile.path,
    originalTextRange = macroCallExpression.textRange,
    macroName = macroCallExpression.shortName?.asString() ?: "unknown"
))

// 嵌套展开时，链接父级信息
fun attachSourceInfoRecursively(psi: PsiElement, info: MacroSourceInfo) {
    psi.putUserData(MacroSourceInfo.KEY, info)
    psi.children.forEach { child ->
        attachSourceInfoRecursively(child, info)
    }
}
```

#### 3.2.3 诊断位置映射

```kotlin
/**
 * 将展开后的诊断位置映射回原始宏调用位置
 */
fun mapDiagnosticToOriginalLocation(
    element: PsiElement,
    diagnostic: Diagnostic
): Diagnostic {
    val sourceInfo = element.getUserData(MacroSourceInfo.KEY) ?: return diagnostic

    return diagnostic.copy(
        textRange = sourceInfo.originalTextRange,
        // 附加宏展开注释信息
        additionalInfo = "expanded from macro @${sourceInfo.macroName}"
    )
}
```

### 3.3 PSI 替换引擎

#### 3.3.1 MacroPsiReplacer

```kotlin
/**
 * 宏 PSI 替换引擎
 *
 * 在内存中将 CjMacroExpression 替换为展开后的 PSI 子树。
 * 替换后的节点携带 MacroSourceInfo，保留原始位置信息。
 */
class MacroPsiReplacer(private val project: Project) {

    /**
     * 替换文件中所有宏调用表达式
     *
     * @param file 源文件的 PSI
     * @param expansions 每个宏调用的展开结果
     * @return 替换后的文件副本（不修改原始 PSI）
     */
    fun replaceAllMacros(
        file: CjFile,
        expansions: Map<CjMacroExpression, MacroExpansionResult>
    ): CjFile {
        // 创建文件副本，避免修改原始 PSI
        val fileCopy = file.copy() as CjFile

        // 按偏移量降序排列，从后向前替换避免偏移量偏移
        val sortedEntries = expansions.entries
            .sortedByDescending { it.key.textOffset }

        for ((originalMacro, result) in sortedEntries) {
            val macroCopy = findCorrespondingElement(fileCopy, originalMacro) ?: continue
            replaceSingleMacro(macroCopy, result, originalMacro)
        }

        return fileCopy
    }

    private fun replaceSingleMacro(
        macroExpr: CjMacroExpression,
        result: MacroExpansionResult,
        originalMacro: CjMacroExpression
    ) {
        val factory = CjPsiFactory(project)

        // 根据宏调用的上下文判断应该解析为什么类型
        val expandedPsi = when {
            isExpressionContext(macroExpr) ->
                factory.createExpression(result.expandedText)
            isStatementContext(macroExpr) ->
                factory.createStatement(result.expandedText)
            isDeclarationContext(macroExpr) ->
                factory.createDeclaration(result.expandedText)
            else ->
                factory.createExpression(result.expandedText)
        }

        // 记录原始位置信息
        val sourceInfo = MacroSourceInfo(
            originalFilePath = originalMacro.containingFile.virtualFile?.path ?: "",
            originalTextRange = originalMacro.textRange,
            macroName = originalMacro.shortName?.asString() ?: "unknown"
        )
        attachSourceInfoRecursively(expandedPsi, sourceInfo)

        // 在内存中替换
        macroExpr.replace(expandedPsi)
    }
}
```

#### 3.3.2 上下文感知的展开

宏调用可能出现在不同的语法位置，PSI 替换需要感知上下文：

```kotlin
/**
 * 判断宏调用所在的语法上下文
 */
private fun isExpressionContext(macro: CjMacroExpression): Boolean {
    val parent = macro.parent
    return parent is CjProperty          // let a = @macro(...)
        || parent is CjReturnExpression  // return @macro(...)
        || parent is CjValueArgument     // foo(@macro(...))
        || parent is CjBinaryExpression  // a + @macro(...)
        // ... 其他表达式上下文
}

private fun isStatementContext(macro: CjMacroExpression): Boolean {
    val parent = macro.parent
    return parent is CjBlockExpression   // { @macro(...) }
}

private fun isDeclarationContext(macro: CjMacroExpression): Boolean {
    // 声明级宏通常通过 SyntheticResolveExtension 处理
    // 这里仅处理宏展开为声明的特殊情况
    return false
}
```

### 3.4 SyntheticResolveExtension 用于声明级宏

#### 3.4.1 MacroSyntheticResolveExtension

项目已有完整的 `SyntheticResolveExtension` 接口（当前被注释废弃），
新架构将重新启用此扩展点：

```kotlin
/**
 * 宏展开合成解析扩展
 *
 * 将声明级宏（如 @Derive、@Codable）的展开结果注入到解析器中，
 * 使宏生成的方法、属性、接口实现等在语义层可见。
 */
class MacroSyntheticResolveExtension(private val project: Project) : SyntheticResolveExtension {

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        // 从缓存的宏展开结果中提取函数名
        val macroResults = getMacroExpansionsForClass(thisDescriptor)
        return macroResults.flatMap { extractFunctionNames(it.expandedText) }
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val macroResults = getMacroExpansionsForClass(thisDescriptor)
        for (macroResult in macroResults) {
            val syntheticMethods = buildSyntheticMethodDescriptors(
                thisDescriptor, name, macroResult
            )
            result.addAll(syntheticMethods)
        }
    }

    override fun addSyntheticSupertypes(
        thisDescriptor: ClassDescriptor,
        supertypes: MutableList<CangJieType>
    ) {
        // 宏可能添加接口实现，如 @Derive[Eq] → implements Eq<T>
        val macroResults = getMacroExpansionsForClass(thisDescriptor)
        for (macroResult in macroResults) {
            val syntheticSupertypes = extractSupertypes(macroResult)
            supertypes.addAll(syntheticSupertypes)
        }
    }

    // 包级别：宏展开生成的顶层声明
    override fun generateSyntheticFunctions(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<SimpleFunctionDescriptor>
    ) {
        // 处理顶层宏展开生成的函数
    }

    override fun generateSyntheticTopLevelClasses(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        // 处理顶层宏展开生成的类
    }

    /**
     * 获取类上的宏展开结果（带缓存）
     */
    private fun getMacroExpansionsForClass(
        descriptor: ClassDescriptor
    ): List<MacroExpansionResult> {
        val psi = descriptor.source.getPsi() as? CjClassOrObject ?: return emptyList()
        val macroExprs = findMacroAnnotations(psi)
        if (macroExprs.isEmpty()) return emptyList()

        val service = MacroExpansionService.getInstance(project)
        return macroExprs.mapNotNull { macroExpr ->
            service.expandMacroSync(macroExpr)
        }
    }
}
```

#### 3.4.2 声明级 vs 表达式级的判断

```kotlin
/**
 * 判断 CjMacroExpression 是声明级宏还是表达式级宏
 *
 * 声明级宏：修饰类/结构体/枚举/函数等声明的宏
 * 表达式级宏：出现在表达式位置的宏
 */
fun CjMacroExpression.isDeclarationLevelMacro(): Boolean {
    val parent = this.parent
    // 如果宏的输入是一个声明（class, struct, func 等），则为声明级
    val input = this.input ?: return false
    return input.declarations != null
}

fun CjMacroExpression.isExpressionLevelMacro(): Boolean {
    return !isDeclarationLevelMacro()
}
```

### 3.5 展开时机与缓存

#### 3.5.1 展开触发时机

| 场景 | 触发方式 | 说明 |
|------|---------|------|
| **文件打开** | `FileEditorManagerListener` | 首次打开时展开文件中所有宏 |
| **文件修改** | `PsiTreeChangeListener` | 监听 PSI 变更，增量更新 |
| **分析请求** | `ResolutionFacade` 钩子 | 分析前确保宏已展开 |
| **手动触发** | 用户操作（行标点击等） | 按需展开 |

#### 3.5.2 缓存策略

```kotlin
/**
 * 宏展开 PSI 缓存
 *
 * 缓存已展开的 PSI 文件副本，避免重复展开和替换。
 * 通过 CachedValuesManager 与文件修改计数器关联，自动失效。
 */
class MacroPsiExpansionCache(private val project: Project) {

    fun getOrCreateExpandedFile(file: CjFile): CjFile {
        return CachedValuesManager.getCachedValue(file) {
            val expanded = performExpansion(file)
            CachedValueProvider.Result.create(
                expanded,
                file,  // 源文件修改时失效
                MacroCompilationModificationTracker.getInstance(project)  // 宏库重编译时失效
            )
        }
    }
}
```

---

## 4. 与 Kotlin Compiler 的类比

用户提到的 Kotlin 编译器 `KtSourceElement` 体系，在仓颉插件中的对应关系：

| Kotlin 概念 | 仓颉插件对应 | 说明 |
|------------|-------------|------|
| `KtRealPsiSourceElement` | 正常的 PSI 节点 | source 就是自己 |
| `KtFakePsiSourceElement` | 带 `MacroSourceInfo` 的 PSI 节点 | source 指向原始宏调用 |
| `KtFakeSourceElementKind` | `MacroSourceInfo` 标记 | 标识为宏展开产物 |
| `RawFirBuilder` | 仓颉的 `DescriptorResolver` | 构建语义模型时使用 source 信息 |
| `source = fakeElement(MacroExpansion)` | `putUserData(MacroSourceInfo.KEY, ...)` | 附加位置映射 |

**关键区别**：
- Kotlin 在编译器（FIR）层面处理 source 映射
- 仓颉插件在 IDE（PSI + IntelliJ 平台）层面处理
- IntelliJ 的 `UserData` 机制提供了灵活的节点级元数据存储

---

## 5. 嵌套宏展开

### 5.1 递归展开策略

```kotlin
/**
 * 递归展开嵌套宏
 *
 * @abc(@def(1)) 的展开流程：
 * 1. 先展开内层 @def(1) → "2"
 * 2. 替换后得到 @abc(2)
 * 3. 再展开 @abc(2) → "3"
 * 4. 最终结果: 3
 *
 * MacroSourceInfo 通过 parent 字段形成链表：
 * 最终的 "3" → MacroSourceInfo(@abc, parent → MacroSourceInfo(@def))
 */
fun expandRecursively(
    file: CjFile,
    maxDepth: Int = 10
): CjFile {
    var current = file
    var depth = 0

    while (depth < maxDepth) {
        val macros = findAllMacroExpressions(current)
        if (macros.isEmpty()) break

        val expansions = expandAll(macros)
        current = replacer.replaceAllMacros(current, expansions)
        depth++
    }

    return current
}
```

### 5.2 位置映射链

嵌套展开时，MacroSourceInfo 形成链表，IDE 可以展示完整的展开路径：

```
错误: Type mismatch at line 5
  → expanded from @abc(...)  at line 5:9
    → expanded from @def(...) at line 5:14
```

---

## 6. 诊断去重

### 6.1 问题

宏展开后的 PSI 节点如果触发诊断，可能导致：
- 同一个语义错误在展开后的多个节点上报告
- 宏展开的中间产物产生无意义的诊断

### 6.2 解决方案

```kotlin
/**
 * 诊断过滤器
 *
 * 过滤宏展开产物的重复诊断，只保留映射到不同原始位置的诊断。
 */
class MacroDiagnosticFilter {

    fun filterDiagnostics(diagnostics: List<Diagnostic>): List<Diagnostic> {
        val seen = mutableSetOf<Pair<String, TextRange>>()

        return diagnostics.filter { diagnostic ->
            val element = diagnostic.psiElement
            val sourceInfo = element.getUserData(MacroSourceInfo.KEY)

            if (sourceInfo != null) {
                // 宏展开产物：按原始位置去重
                val key = Pair(diagnostic.factory.name, sourceInfo.originalTextRange)
                seen.add(key)
            } else {
                // 非宏代码：直接保留
                true
            }
        }
    }
}
```

---

## 7. IDE 功能集成

### 7.1 宏展开预览

用户悬停 `@abc(1)` 时，展示展开后的代码。这不需要 PSI 替换，
可以复用现有的 `MacroExpansionPanel`：

```kotlin
// 现有代码可以继续使用
MacroExpansionPanel.showExpansion(macroExpr, expandedText)
```

### 7.2 导航

从展开后的符号导航到宏声明：

```kotlin
/**
 * 宏展开导航提供者
 *
 * 当用户在展开后的代码上 Ctrl+Click 时，
 * 提供导航到原始宏声明的选项。
 */
class MacroExpansionNavigationProvider : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        val sourceInfo = element?.getUserData(MacroSourceInfo.KEY) ?: return null
        // 提供导航到原始宏调用位置
        // ...
    }
}
```

### 7.3 代码补全

在宏展开后的上下文中提供补全。由于 PSI 替换后类型信息完整，
现有的补全基础设施无需修改即可工作。

---

## 8. 实现计划

### Phase 1：基础设施（可先行）

1. 定义 `MacroSourceInfo` 数据结构
2. 实现 `MacroPsiReplacer` 核心引擎
3. 实现 `CjPsiFactory` 的 `createExpression`/`createStatement` 方法（如果尚未存在）
4. 重新启用 `LspMacroServerProviderFactory`

### Phase 2：表达式级宏支持

1. 在 `ResolutionFacade` 中集成宏展开钩子
2. 实现展开结果的缓存（`CachedValuesManager`）
3. 实现诊断位置映射
4. 实现诊断去重过滤

### Phase 3：声明级宏支持

1. 重新启用 `SyntheticResolveExtension` 扩展点注册
2. 实现 `MacroSyntheticResolveExtension`
3. 处理类成员注入（方法、属性、嵌套类）
4. 处理包级别注入（顶层函数、类、变量）

### Phase 4：清理

1. 移除磁盘展开文件相关代码
   - `MacroExpandedFileManager`
   - `MacroExpandedFileProcessor`
   - `MacroExpandedAnalysisBridge`
   - `MacroMergedBindingContext`
   - `MacroExpandedModuleInfoProviderExtension`
   - `MacroExpansionFileListener`
2. 简化 `MacroPipelineCoordinator`（只需协调编译，不需要协调展开文件写入）
3. 更新 `cangjie-macro.xml` 和 `cangjie-analysis.xml` 配置

---

## 9. 风险与挑战

### 9.1 PSI 替换的线程安全

IntelliJ PSI 修改必须在写锁（Write Action）中执行。
宏展开可能在后台线程触发，需要切换到 EDT：

```kotlin
ApplicationManager.getApplication().invokeLater {
    WriteAction.run<Throwable> {
        macroExpr.replace(expandedPsi)
    }
}
```

**缓解方案**：使用 PSI 文件副本（`file.copy()`）进行替换，避免修改用户正在编辑的文件。

### 9.2 展开结果解析失败

宏展开可能返回无效的源码文本，`CjPsiFactory` 解析失败：

```kotlin
// 安全的解析包装
fun safeCreateExpression(text: String): CjExpression? {
    return try {
        CjPsiFactory(project).createExpression(text)
    } catch (e: Exception) {
        LOG.warn("宏展开结果解析失败: $text", e)
        null  // 保留原始的 CjMacroExpression 不替换
    }
}
```

### 9.3 与 Stub 索引的兼容

PSI 替换后的节点不应被 Stub 索引收录（因为它们是临时的内存替换）。
需要确保替换操作只在文件副本上进行，而非原始的 PSI 文件。

### 9.4 编辑时的实时更新

用户编辑源文件时，宏调用的参数可能变化。需要：
- 监听 PSI 变更事件
- 智能判断是否需要重新展开（防抖）
- 增量更新（只重新展开变更的宏调用）

---

## 10. 对比总结

| 维度 | 当前方案（磁盘展开文件） | 新方案（PSI 替换） |
|------|---------------------|------------------|
| 展开方式 | 生成独立 .cj 文件 | 内存中 PSI 节点替换 |
| 分析方式 | 独立分析 + BindingContext 合并 | 在修改后的 PSI 上直接分析 |
| 类型检查 | ❌ 上下文断裂 | ✅ 保留完整上下文 |
| 错误定位 | 通过 OffsetMapping 映射 | 通过 MacroSourceInfo 映射 |
| LspMacroServer | ❌ 被禁用 | ✅ 重新启用，宏调用级粒度 |
| 声明注入 | 通过 Stub 索引自动发现 | 通过 SyntheticResolveExtension 精确注入 |
| 磁盘 I/O | 需要写入/读取磁盘文件 | 纯内存操作 |
| 复杂度 | 高（管线协调、文件管理、缓存失效） | 中（PSI 替换、缓存、位置映射） |
| 性能 | 受磁盘 I/O 和进程启动影响 | 内存操作，常驻进程，更快 |

---

## 附录 A：当前 PSI 宏节点结构

```
CjMacroExpression (MACRO_EXPRESSION)
  ├─ PsiElement ('@')
  ├─ CjNameReferenceExpression (REFERENCE_EXPRESSION)
  │   └─ PsiElement (IDENTIFIER) "macroName"
  ├─ CjMacroAttr (MACRO_ATTR) [可选]
  │   └─ CjQuoteTokens (QUOTE_TOKENS)
  └─ CjMacroInput (MACRO_INPUT)
      └─ [输入内容]
```

## 附录 B：SyntheticResolveExtension 接口概览

```
SyntheticResolveExtension
├── 类成员级别
│   ├── getSyntheticNestedClassNames()
│   ├── getSyntheticFunctionNames()
│   ├── getSyntheticPropertiesNames()
│   ├── getSyntheticCompanionObjectNameIfNeeded()
│   ├── addSyntheticSupertypes()
│   ├── generateSyntheticNestedClasses()
│   ├── generateSyntheticMethods()
│   ├── generateSyntheticProperties()
│   └── generateSyntheticSecondaryConstructors()
└── 包级别
    ├── getSyntheticPackageNames()
    ├── generateSyntheticTopLevelClasses()
    ├── generateSyntheticEnums()
    ├── generateSyntheticFunctions()
    └── generateSyntheticVariables()
```

## 附录 C：LspMacroServer 协议要点

- **管道帧格式**：8 字节 size_t 小端长度前缀 + payload
- **FlatBuffers 缓冲**：`sizedByteArray()` 不包含 size prefix
- **消息类型**：defLib=1, multiCalls=2, macroResult=3, exitTask=4
- **一问一答**：每条 MultiMacroCalls 只处理并返回一条 MacroResult
- **展开粒度**：MACRO_CALL 级别，可单独展开指定宏调用