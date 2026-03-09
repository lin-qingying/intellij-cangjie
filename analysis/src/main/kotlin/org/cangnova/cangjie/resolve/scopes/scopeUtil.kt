/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package org.cangnova.cangjie.resolve.scopes


import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.SourceForBinaryModuleInfo
import org.cangnova.cangjie.moduleinfo.provider.moduleInfo
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.projectStructure.CangJieSourceFilterScope
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.resolve.CangJieResolveScopeEnlarger
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.getResolutionFacade
import org.cangnova.cangjie.resolve.frontendService
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.lazy.FileScopeProvider
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.qualified.QualifierPart
import org.cangnova.cangjie.resolve.scopes.util.parentsWithSelf
import org.cangnova.cangjie.types.error.ErrorClassDescriptor
import org.cangnova.cangjie.types.error.ErrorEntity
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.getImplicitReceiversWithInstance
/**
 * 将 MemberScope 包装为 ImportingScope，使其可以参与导入作用域链。
 * @param parentScope 父级 ImportingScope，默认为 null（即作为链的末端）
 */
@JvmOverloads
fun MemberScope.memberScopeAsImportingScope(parentScope: ImportingScope? = null): ImportingScope =
    MemberScopeToImportingScopeAdapter(parentScope, this)

/**
 * 从当前作用域及所有父作用域中收集符合条件的描述符。
 * 结果已按 kindFilter 和 nameFilter 过滤。
 *
 * @param kindFilter 描述符类型过滤器，默认收集所有类型
 * @param nameFilter 名称过滤器，默认接受所有名称
 * @param changeNamesForAliased 是否对别名符号使用别名替换原名
 * @return 所有匹配的声明描述符集合
 */
fun HierarchicalScope.collectDescriptorsFiltered(
    kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
    nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER,
    changeNamesForAliased: Boolean = false
): Collection<DeclarationDescriptor> {
    if (kindFilter.kindMask == 0) return listOf()
    val result = collectAllFromMeAndParent {
        // ImportingScope 有额外的 changeNamesForAliased 参数
        if (it is ImportingScope)
            it.getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)
        else
            it.getContributedDescriptors(kindFilter, nameFilter)
    }.filter { kindFilter.accepts(it) && nameFilter(it.name) }
    return result
}

/**
 * 从当前作用域及父作用域的所有 ImportingScope 中，找到第一个非空结果。
 * 遇到非 ImportingScope 类型的作用域时跳过。
 */
inline fun <T : Any> HierarchicalScope.findFirstFromImportingScopes(fetch: (ImportingScope) -> T?): T? {
    return findFirstFromMeAndParent { if (it is ImportingScope) fetch(it) else null }
}

/**
 * 从当前作用域及所有父作用域中收集指定名称的所有函数描述符。
 */
fun HierarchicalScope.collectFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
    collectAllFromMeAndParent { it.getContributedFunctions(name, location) }

/**
 * 从当前作用域及所有父作用域中收集指定名称的所有变量描述符。
 */
fun HierarchicalScope.collectVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> =
    collectAllFromMeAndParent { it.getContributedVariables(name, location) }

/**
 * 从当前作用域及所有父作用域中收集指定名称的所有宏描述符。
 */
fun HierarchicalScope.collectMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
    collectAllFromMeAndParent { it.getContributedMacros(name, location) }

/**
 * 获取当前 PSI 元素所处的词法作用域（LexicalScope）。
 * 通过完整模式（FULL）分析该元素，然后从 BindingContext 中提取作用域。
 */
fun CjElement.getResolutionScope(): LexicalScope {
    val resolutionFacade = getResolutionFacade()
    val context = resolutionFacade.analyze(this, BodyResolveMode.FULL)
    return getResolutionScope(context, resolutionFacade)
}

/**
 * 在当前作用域及所有父作用域中查找满足条件的第一个函数描述符。
 *
 * @param name 函数名称
 * @param location 查找位置（用于统计和调试）
 * @param predicate 额外的过滤条件，默认接受所有函数
 */
fun HierarchicalScope.findFunction(
    name: Name,
    location: LookupLocation,
    predicate: (FunctionDescriptor) -> Boolean = { true }
): FunctionDescriptor? {
    processForMeAndParent {
        it.getContributedFunctions(name, location).firstOrNull(predicate)?.let { return it }
    }
    return null
}

/**
 * 从当前作用域及所有父作用域中收集单个结果，返回列表。
 * 内部使用 SmartList 以减少小列表的内存开销。
 */
private inline fun <T : Any> HierarchicalScope.collectFromMeAndParent(
    collect: (HierarchicalScope) -> T?
): List<T> {
    var result: MutableList<T>? = null
    processForMeAndParent {
        val element = collect(it)
        if (element != null) {
            if (result == null) {
                result = SmartList()
            }
            result.add(element)
        }
    }
    return result ?: emptyList()
}

/**
 * 获取当前词法作用域的隐式接收者层级列表。
 * 列表按局部性排序：最近（最内层）的接收者排在最前面。
 * 例如：lambda 内部的 this 优先于外部类的 this。
 */
fun LexicalScope.getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = collectFromMeAndParent {
    if (it is LexicalScope) listOfNotNull(it.implicitReceiver) else null
}.flatten()

/**
 * 在当前作用域及所有父作用域中查找满足条件的第一个变量描述符。
 *
 * @param name 变量名称
 * @param location 查找位置
 * @param predicate 额外的过滤条件，默认接受所有变量
 */
fun HierarchicalScope.findVariable(
    name: Name,
    location: LookupLocation,
    predicate: (VariableDescriptor) -> Boolean = { true }
): VariableDescriptor? {
    processForMeAndParent {
        it.getContributedVariables(name, location).firstOrNull(predicate)?.let { return it }
    }
    return null
}

/**
 * 将 MemberScope 适配为 ImportingScope 的私有实现类。
 * 所有查询操作均委托给内部持有的 memberScope。
 *
 * @param parent 父级 ImportingScope（构成作用域链）
 * @param memberScope 被包装的成员作用域
 */
private class MemberScopeToImportingScopeAdapter(override val parent: ImportingScope?, val memberScope: MemberScope) :
    ImportingScope {

    // 不提供包视图，始终返回 null
    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
        // changeNamesForAliased 在 MemberScope 中不适用，直接忽略
    ) = memberScope.getContributedDescriptors(kindFilter, nameFilter)

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        memberScope.getContributedClassifier(name, location)

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        memberScope.getContributedVariables(name, location)

    override fun getContributedPropertys(name: Name, location: LookupLocation) =
        memberScope.getContributedPropertys(name, location)

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        memberScope.getContributedFunctions(name, location)

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return memberScope.getContributedMacros(name, location)
    }

    // 两个适配器包装同一个 memberScope，视为相等
    override fun equals(other: Any?) = other is MemberScopeToImportingScopeAdapter && other.memberScope == memberScope

    override fun hashCode() = memberScope.hashCode()

    override fun toString() = "${this::class.java.simpleName} for $memberScope"

    // 可导入的名称集合 = memberScope 的所有名称
    override fun computeImportedNames() = memberScope.computeAllNames()

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName)
        p.pushIndent()
        memberScope.printScopeStructure(p.withholdIndentOnce())
        p.popIndent()
        p.println("}")
    }
}

/**
 * 对作用域做快照。
 * 若是可写作用域（LexicalWritableScope），则创建不可变快照；否则直接返回自身。
 * 用于在解析过程中固化某一时刻的作用域状态，避免后续写入影响已有引用。
 */
fun HierarchicalScope.takeSnapshot(): HierarchicalScope = if (this is LexicalWritableScope) takeSnapshot() else this

/**
 * 从多个作用域中收集所有结果并合并，避免不必要的内存分配。
 * - 0 个作用域：返回空列表
 * - 1 个作用域：直接返回该作用域的结果，不创建新集合
 * - 多个作用域：使用 concat 逐步合并
 */
inline fun <Scope, T> getFromAllScopes(scopes: Array<Scope>, callback: (Scope) -> Collection<T>): Collection<T> =
    when (scopes.size) {
        0 -> emptyList()
        1 -> callback(scopes[0])
        else -> {
            var result: Collection<T>? = null
            for (scope in scopes) {
                result = result.concat(callback(scope))
            }
            result ?: emptySet()
        }
    }

/**
 * 从多个作用域中依次查找，返回第一个非空结果。
 * - 0 个作用域：返回 null
 * - 1 个作用域：直接返回结果
 * - 多个作用域：短路查找，找到即停止
 */
inline fun <Scope, T> getFirstFromAllScopes(scopes: Array<Scope>, callback: (Scope) -> T?): T? =
    when (scopes.size) {
        0 -> null
        1 -> callback(scopes[0])
        else -> {
            for (scope in scopes) {
                callback(scope)?.let { return it }
            }
            null
        }
    }

/**
 * 从可迭代的 MemberScope? 列表中过滤掉 null 和空作用域，返回 SmartList。
 */
fun listOfNonEmptyScopes(scopes: Iterable<MemberScope?>): SmartList<MemberScope> =
    scopes.filterNotNull().filterTo(SmartList()) { it !== MemberScope.Empty }

/**
 * 从可变参数的 MemberScope? 中过滤掉 null 和空作用域，返回 SmartList。
 */
fun listOfNonEmptyScopes(vararg scopes: MemberScope?): SmartList<MemberScope> =
    scopes.filterNotNull().filterTo(SmartList()) { it !== MemberScope.Empty }

/**
 * 从多个作用域中收集所有分类器（ClassifierDescriptor）列表，合并去重。
 * 与 getFirstClassifierDiscriminateHeaders 不同，此方法收集所有作用域的结果而非只取第一个。
 */
inline fun <Scope, T : ClassifierDescriptor> getListClassifierDiscriminateHeaders(
    scopes: Array<Scope>,
    callback: (Scope) -> List<T>
): List<T> {
    val result = mutableListOf<T>()
    for (scope in scopes) {
        val newResult = callback(scope)
        if (newResult.isNotEmpty())
            result.addAll(newResult)
    }
    return result
}

/**
 * 从多个作用域中查找第一个匹配的分类器描述符，区分"期望类"（Expect）与普通类。
 *
 * 优先级规则：
 * - 若找到非期望类（普通实现类）→ 立即返回，优先级最高
 * - 若只找到期望类（ClassifierDescriptorWithTypeParameters）→ 记录为候选，继续查找
 * - 所有作用域遍历完毕后返回候选（或 null）
 *
 * 注意：此方法为性能敏感路径，请勿替换为 map().firstOrNull()。
 */
inline fun <Scope, T : ClassifierDescriptor> getFirstClassifierDiscriminateHeaders(
    scopes: Array<Scope>,
    callback: (Scope) -> T?
): T? {
    var result: T? = null
    for (scope in scopes) {
        val newResult = callback(scope)
        if (newResult != null) {
            if (newResult is ClassifierDescriptorWithTypeParameters) {
                // 期望类：记为候选，但继续查找是否有更优先的普通类
                if (result == null) result = newResult
            } else {
                // 普通实现类：直接返回，最高优先级
                return newResult
            }
        }
    }
    return result
}

/**
 * 将两个集合拼接，尽量复用已有集合以减少内存分配。
 *
 * 策略：
 * - 若 collection 为空 → 直接返回 this（不创建新集合）
 * - 若 this 为 null → 直接返回 collection
 * - 若 this 是 LinkedHashSet → 原地 addAll，返回 this
 * - 其他情况 → 创建新的 LinkedHashSet 合并两者
 *
 * 注意：若 this 是可变集合可能被修改。
 */
fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (collection.isEmpty()) return this
    if (this == null) return collection
    if (this is LinkedHashSet) {
        addAll(collection)
        return this
    }
    val result = LinkedHashSet(this)
    result.addAll(collection)
    return result
}

/**
 * 获取 PSI 元素所在的词法作用域。
 * 优先从 BindingContext 中查找；若找不到（如文件顶层），则回退到文件级别的解析作用域。
 *
 * @param bindingContext 当前的绑定上下文
 * @param resolutionFacade 解析门面，用于获取文件级作用域（后续可考虑移除此参数）
 */
fun PsiElement.getResolutionScope(
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade
): LexicalScope = getResolutionScope(bindingContext) ?: when (containingFile) {
    is CjFile -> resolutionFacade.getFileResolutionScope(containingFile as CjFile)
    else -> error("Not in CjFile")
}

/**
 * 获取 CjFile 对应的词法作用域（线程安全的读操作）。
 */
val CjFile.scope: LexicalScope
    get() = runReadAction { this.getResolutionScope() }

/**
 * 通过 FileScopeProvider 服务获取文件级别的词法作用域。
 * 使用 @OptIn(FrontendInternals::class) 因为 FileScopeProvider 是内部 API。
 */
@OptIn(FrontendInternals::class)
fun ResolutionFacade.getFileResolutionScope(file: CjFile): LexicalScope {
    return frontendService<FileScopeProvider>().getFileResolutionScope(file)
}

/**
 * 从 BindingContext 中查找 PSI 元素对应的词法作用域。
 *
 * 查找策略（从当前节点向上遍历父节点）：
 * 1. 若是 CjElement → 直接从 BindingContext[LEXICAL_SCOPE] 获取
 * 2. 若是 CjAbstractClassBody → 获取外部类描述符的成员声明作用域
 * 3. 到达 CjFile 则停止查找，返回 null
 */
fun PsiElement.getResolutionScope(bindingContext: BindingContext): LexicalScope? {
    for (parent in parentsWithSelf) {
        if (parent is CjElement) {
            val scope = bindingContext[BindingContext.LEXICAL_SCOPE, parent]
            if (scope != null) return scope
        }

        if (parent is CjAbstractClassBody) {
            val classDescriptor =
                bindingContext[BindingContext.CLASS, parent.getParent()] as? ClassDescriptorWithResolutionScopes
            if (classDescriptor != null) {
                return classDescriptor.scopeForMemberDeclarationResolution
            }
        }
        if (parent is CjFile) break
    }
    return null
}

/**
 * 判断某个描述符在给定作用域中是否可以在不触发弃用警告的情况下被解析。
 *
 * 逐层向上遍历作用域链，对不同类型的描述符采用不同的检查策略：
 * - ClassifierDescriptor：使用专用的 getContributedClassifierIncludeDeprecated 精确判断
 * - VariableDescriptor / FunctionDescriptor：在 ImportingScope 中做启发式检查（显式导入场景）
 *
 * @return true 表示该描述符在当前作用域中可见且未被标记为弃用
 */
fun DeclarationDescriptor.canBeResolvedWithoutDeprecation(
    scopeForResolution: HierarchicalScope,
    location: LookupLocation
): Boolean {
    for (scope in scopeForResolution.parentsWithSelf) {
        val hasNonDeprecatedSuitableCandidate = when (this) {
            is ClassifierDescriptor -> scope.getContributedClassifierIncludeDeprecated(name, location)
                ?.let { it.descriptor == this && !it.isDeprecated }

            is VariableDescriptor -> (scope as? ImportingScope)?.getContributedVariables(name, location)
                ?.any { it == this }

            is FunctionDescriptor -> (scope as? ImportingScope)?.getContributedFunctions(name, location)
                ?.any { it == this }

            else -> null
        }

        if (hasNonDeprecatedSuitableCandidate == true) return true
    }
    return false
}

/**
 * 从当前作用域及所有父作用域中查找指定名称的所有分类器描述符列表。
 * 找到第一个非空结果后立即返回（不继续向上查找）。
 */
fun HierarchicalScope.findClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
    findFirstFromMeAndParent {
        val list = it.getContributedClassifiers(name, location)
        list.ifEmpty { null }
    } ?: emptyList()

/**
 * 从当前作用域及所有父作用域中查找指定名称的第一个分类器描述符。
 */
fun HierarchicalScope.findClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
    findFirstFromMeAndParent { it.getContributedClassifier(name, location) }

/**
 * 从当前作用域及所有父作用域中查找第一个分类器描述符，同时携带弃用状态信息。
 * 用于在报告弃用警告时需要区分"找到了但已弃用"与"未找到"的场景。
 */
fun HierarchicalScope.findFirstClassifierWithDeprecationStatus(
    name: Name,
    location: LookupLocation
): DescriptorWithDeprecation<ClassifierDescriptor>? {
    return findFirstFromMeAndParent { it.getContributedClassifierIncludeDeprecated(name, location) }
}

/**
 * 在词法作用域链中查找局部变量描述符。
 *
 * 查找规则：
 * - 跳过 ImportingScope（导入作用域不包含局部变量）
 * - 跳过 LexicalChainedScope（链式作用域）
 * - 仅在普通词法作用域中查找
 *
 * @deprecated 请使用 getContributedProperties 替代
 */
@Deprecated("Use getContributedProperties instead")
fun LexicalScope.findLocalVariable(name: Name): VariableDescriptor? {
    return findFirstFromMeAndParent { originalScope ->
        // 解包 LexicalScopeWrapper 以便准确判断是否为 ImportingScope
        val possiblyUnpackedScope = when (originalScope) {
            is LexicalScopeWrapper -> originalScope.delegate
            else -> originalScope
        }

        when {
            // 只在非导入、非链式的词法作用域中查找局部变量
            possiblyUnpackedScope !is ImportingScope && possiblyUnpackedScope !is LexicalChainedScope ->
                possiblyUnpackedScope.getContributedVariables(
                    name,
                    NoLookupLocation.MATCH_GET_LOCAL_VARIABLE
                ).singleOrNull()

            else -> null
        }
    }
}

/**
 * 从作用域链中查找指定名称对应的所有包全限定名列表。
 * 找到第一个非空结果后停止。
 */
fun HierarchicalScope.findPackageFqNames(name: Name): List<FqName>? {
    return findFirstFromMeAndParent { it.getContributedPackageFqName(name) }
}

/**
 * 从作用域链中查找指定名称对应的包限定符部分列表。
 * 当前为 TODO 状态，尚未实现。
 */
fun HierarchicalScope.findPackageQualifierParts(name: Name): List<List<QualifierPart>>? {
    return findFirstFromMeAndParent {
        TODO()
    }
}

/**
 * 在当前词法作用域的导入链头部插入单个导入作用域。
 */
fun LexicalScope.addImportingScope(importScope: ImportingScope): LexicalScope = addImportingScopes(listOf(importScope))

/**
 * 在当前词法作用域的导入链头部插入多个导入作用域。
 *
 * 实现思路：
 * 1. 找到词法作用域链的最后一个 LexicalScope
 * 2. 取其 parent（即原来的第一个 ImportingScope）
 * 3. 将新的导入作用域列表链接到原链头部
 * 4. 用新链替换原有的导入作用域链
 */
fun LexicalScope.addImportingScopes(importScopes: List<ImportingScope>): LexicalScope {
    val lastLexicalScope = parentsWithSelf.last { it is LexicalScope }
    val firstImporting = lastLexicalScope.parent as ImportingScope
    val newFirstImporting = chainImportingScopes(importScopes, firstImporting)
    return replaceImportingScopes(newFirstImporting)
}

/**
 * 将当前词法作用域中的导入作用域链替换为指定的新链。
 * 若传入 null，则使用 ImportingScope.Empty 作为占位。
 *
 * 若自身已经是 LexicalScopeWrapper，则直接替换其导入链，避免重复包装。
 */
fun LexicalScope.replaceImportingScopes(importingScopeChain: ImportingScope?): LexicalScope {
    val newImportingScopeChain = importingScopeChain ?: ImportingScope.Empty
    if (this is LexicalScopeWrapper) {
        return LexicalScopeWrapper(this.delegate, newImportingScopeChain)
    }
    return LexicalScopeWrapper(this, newImportingScopeChain)
}

/**
 * 词法作用域包装器，用于替换底层词法作用域的导入链，同时保持其他行为不变。
 *
 * @param delegate 被包装的原始词法作用域（不能是另一个 LexicalScopeWrapper，避免嵌套性能问题）
 * @param newImportingScopeChain 替换后的新导入作用域链
 */
private class LexicalScopeWrapper(
    val delegate: LexicalScope,
    private val newImportingScopeChain: ImportingScope
) : LexicalScope by delegate {
    init {
        assert(delegate !is LexicalScopeWrapper) {
            "Do not wrap again to avoid performance issues"
        }
    }

    /**
     * 重写 parent：
     * - 若原 parent 是词法作用域 → 递归替换其导入链
     * - 若原 parent 是导入作用域 → 直接使用新的导入链作为 parent
     *
     * 使用 SYNCHRONIZED 懒加载，保证多线程安全。
     */
    override val parent: HierarchicalScope by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        assert(delegate !is ImportingScope)

        val parent = delegate.parent
        if (parent is LexicalScope) {
            parent.replaceImportingScopes(newImportingScopeChain)
        } else {
            newImportingScopeChain
        }
    }

    override fun toString() = kind.toString()
}

/**
 * 将多个 ImportingScope 链接成一条链，尾部连接到 tail。
 * 链接顺序：scopes 列表倒序折叠，使得列表中第一个元素成为链的头部（优先级最高）。
 *
 * @param scopes 要链接的作用域列表（每个元素的 parent 必须为 null）
 * @param tail 链的末尾作用域，默认为 null
 * @return 链的头部 ImportingScope，若 scopes 为空则返回 tail
 */
fun chainImportingScopes(scopes: List<ImportingScope>, tail: ImportingScope? = null): ImportingScope? {
    return scopes.asReversed()
        .fold(tail) { current, scope ->
            assert(scope.parent == null)
            scope.withParent(current)
        }
}

/**
 * 创建一个新的 ImportingScope，其 parent 被替换为指定的 newParent。
 * 使用匿名对象委托原有实现，仅覆盖 parent 属性。
 */
fun ImportingScope.withParent(newParent: ImportingScope?): ImportingScope {
    return object : ImportingScope by this {
        override val parent: ImportingScope?
            get() = newParent
    }
}

/** 获取当前项目的全局搜索范围。 */
fun Project.projectScope(): GlobalSearchScope = GlobalSearchScope.projectScope(this)

/**
 * 通过标签名获取当前词法作用域链中所有匹配的声明描述符。
 * 只有当某个 LexicalScope 的 ownerDescriptor 名称与 labelName 匹配，
 * 且 isOwnerDescriptorAccessibleByLabel 为 true 时，才将其 ownerDescriptor 加入结果。
 *
 * 用于解析带标签的 return/break/continue 跳转目标。
 */
fun LexicalScope.getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> =
    collectAllFromMeAndParent {
        if (it is LexicalScope && it.isOwnerDescriptorAccessibleByLabel && it.ownerDescriptor.name == labelName) {
            listOf(it.ownerDescriptor)
        } else {
            listOf()
        }
    }

/**
 * 从当前作用域及所有父作用域中收集所有结果并合并。
 * 使用 concat 延迟分配，在结果较少时避免创建大集合。
 *
 * @param collect 对每个作用域调用的收集函数
 * @return 所有作用域结果的合并集合，若全为空则返回 emptySet()
 */
inline fun <T : Any> HierarchicalScope.collectAllFromMeAndParent(
    collect: (HierarchicalScope) -> Collection<T>
): Collection<T> {
    var result: Collection<T>? = null
    processForMeAndParent { result = result.concat(collect(it)) }
    return result ?: emptySet()
}

/**
 * 从所有隐式接收者的成员作用域中查找指定名称的变量。
 * 按接收者层级顺序查找，返回第一个匹配的变量描述符。
 *
 * 使用场景：在 lambda 或扩展函数体内，通过 this 隐式访问接收者的属性。
 */
fun LexicalScope.getVariableFromImplicitReceivers(name: Name): VariableDescriptor? {
    getImplicitReceiversWithInstance().forEach {
        it.type.memberScope.getContributedVariables(name, NoLookupLocation.FROM_IDE).singleOrNull()?.let { return it }
    }
    return null
}

/**
 * 作用域相关的工具方法集合。
 * 封装了为属性、变量创建特定用途词法作用域的工厂方法。
 */
object ScopeUtils {

    /**
     * 为属性初始化器创建词法作用域。
     * 属性初始化器（val x = <这里>）的作用域不包含属性自身，避免自引用。
     */
    fun makeScopeForPropertyInitializer(
        propertyHeader: LexicalScope,
        propertyDescriptor: PropertyDescriptor
    ): LexicalScope = makeScopeForVariableBaseInitializer(propertyHeader, propertyDescriptor)

    /**
     * 为变量初始化器创建词法作用域的内部实现。
     * 创建一个 VARIABLE_INITIALIZER_OR_DELEGATE 类型的词法作用域。
     */
    private fun makeScopeForVariableBaseInitializer(
        variableHeader: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope = LexicalScopeImpl(
        variableHeader,
        variableDescriptor,
        false,
        null,
        LexicalScopeKind.VARIABLE_INITIALIZER_OR_DELEGATE
    )

    /**
     * 为局部变量初始化器创建词法作用域。
     */
    fun makeScopeForVariableInitializer(
        variableHeader: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope = makeScopeForVariableBaseInitializer(variableHeader, variableDescriptor)

    /**
     * 为变量/属性头部（类型注解、默认值之前）创建词法作用域的内部实现。
     * 将变量的类型参数注册到作用域中（用于泛型属性），
     * 使用 DO_NOTHING 重复声明检查器（类型参数的重复声明在更早阶段报告）。
     */
    private fun makeScopeForVariableBaseHeader(
        parent: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope = LexicalScopeImpl(
        parent,
        variableDescriptor,
        false,
        null,
        LexicalScopeKind.PROPERTY_HEADER,
        LocalRedeclarationChecker.DO_NOTHING
    ) {
        for (typeParameterDescriptor in variableDescriptor.typeParameters) {
            addClassifierDescriptor(typeParameterDescriptor)
        }
    }

    /**
     * 为变量头部创建词法作用域（含类型参数）。
     */
    fun makeScopeForVariableHeader(
        parent: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope = makeScopeForVariableBaseHeader(parent, variableDescriptor)

    /**
     * 为属性头部创建词法作用域（含类型参数）。
     */
    fun makeScopeForPropertyHeader(
        parent: LexicalScope,
        propertyDescriptor: PropertyDescriptor
    ): LexicalScope = makeScopeForVariableBaseHeader(parent, propertyDescriptor)
}

/**
 * 错误恢复用的词法作用域实现。
 * 在解析失败或遇到错误节点时使用，所有查询均返回空结果，避免级联错误。
 *
 * - ownerDescriptor：使用特殊的 ErrorClassDescriptor 占位
 * - implicitReceiver：无隐式接收者
 * - kind：THROWING（表示此作用域不应正常使用）
 */
class ErrorLexicalScope : LexicalScope {

    /** 错误作用域的父级也是一个占位对象，所有查询返回空。 */
    override val parent: HierarchicalScope = object : HierarchicalScope {
        override val parent: HierarchicalScope? = null

        override fun printStructure(p: Printer) {
            p.print(ErrorEntity.PARENT_OF_ERROR_SCOPE.debugText)
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> = emptySet()
        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = emptySet()
        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = emptySet()
        override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> = emptyList()
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = emptySet()
    }

    override fun printStructure(p: Printer) {
        p.print(ErrorEntity.ERROR_SCOPE.debugText)
    }

    override val ownerDescriptor: DeclarationDescriptor =
        ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format("unknown")))
    override val isOwnerDescriptorAccessibleByLabel: Boolean = false
    override val implicitReceiver: ReceiverParameterDescriptor? = null
    override val kind: LexicalScopeKind = LexicalScopeKind.THROWING

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> = emptySet()
    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = emptySet()
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = emptySet()
    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> = emptyList()
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = emptySet()
}

/**
 * 对两个可空作用域依次执行操作，跳过 null。
 * 用于减少空检查样板代码。
 */
inline fun <Scope> forEachScope(scope1: Scope?, scope2: Scope?, action: (Scope) -> Unit) {
    if (scope1 != null) action(scope1)
    if (scope2 != null) action(scope2)
}

/**
 * 对两个可空作用域分别执行 transform，将结果合并返回。
 *
 * 优化策略：
 * - scope2 为 null → 直接返回 scope1 的结果
 * - scope1 结果为空 → 直接返回 scope2 的结果
 * - 否则 → 合并两个结果列表
 */
inline fun <Scope, R> flatMapScopes(
    scope1: Scope?,
    scope2: Scope?,
    transform: (Scope) -> Collection<R>
): Collection<R> {
    val results1 = if (scope1 != null) transform(scope1) else emptyList()
    if (scope2 == null) return results1
    val results2 = transform(scope2)
    if (results1.isEmpty()) return results2
    return results1.toMutableList().also { it.addAll(results2) }
}

/**
 * 获取指定 CjFile 的解析搜索范围。
 *
 * 规则：
 * - CjCodeFragment（代码片段）：使用其宿主文件的搜索范围
 *   - SourceForBinaryModuleInfo → 只包含库类（libraryClasses）
 *   - 其他 → 项目源码 + 库类（projectSourcesAndLibraryClasses）
 * - ModuleSourceInfo（普通源文件）：项目文件范围，并通过 CangJieResolveScopeEnlarger 扩展
 * - 其他模块类型 → 返回空范围
 */
fun getResolveScope(file: CjFile): GlobalSearchScope {
    if (file is CjCodeFragment) {
        val contextScope = file.getContextContainingFile()?.resolveScope
        if (contextScope != null) {
            return when (file.moduleInfo) {
                is SourceForBinaryModuleInfo -> CangJieSourceFilterScope.libraryClasses(contextScope, file.project)
                else -> CangJieSourceFilterScope.projectSourcesAndLibraryClasses(contextScope, file.project)
            }
        }
    }
    return when (file.moduleInfo) {
        is ModuleSourceInfo -> {
            val projectScope = CangJieSourceFilterScope.projectFiles(file.resolveScope, file.project)
            CangJieResolveScopeEnlarger.Companion.enlargeScope(projectScope, file)
        }
        else -> GlobalSearchScope.EMPTY_SCOPE
    }
}

/**
 * 在词法作用域中查找指定名称的包视图描述符。
 *
 * 查找策略（优先级从高到低）：
 * 1. 从模块根包（FqName.ROOT）直接子包中查找 → 优先使用，避免导入歧义
 * 2. 若根包中没有 → 从导入作用域中查找
 *
 * @param name 包的简单名称
 * @param location 查找位置（用于统计）
 */
fun LexicalScope.getPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? {
    return ownerDescriptor.module.getPackage(FqName.ROOT.child(name)).takeUnless { it.isEmpty() }
        ?: findPackage(name)
}

/**
 * 从导入作用域链中查找指定名称的包视图描述符。
 * 只在 ImportingScope 中查找，词法作用域（局部变量等）不包含包信息。
 */
fun HierarchicalScope.findPackage(name: Name): PackageViewDescriptor? =
    findFirstFromImportingScopes { it.getContributedPackage(name) }