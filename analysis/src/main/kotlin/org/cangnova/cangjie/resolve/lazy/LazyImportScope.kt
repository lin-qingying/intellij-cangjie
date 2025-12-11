/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.lazy

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import com.intellij.util.SmartList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorVisibilityUtils.isVisibleIgnoringReceiver
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.OPERATOR_RENAMED_ON_IMPORT
import org.cangnova.cangjie.diagnostics.infos.errors.UNDERSCORE_IS_RESERVED
import org.cangnova.cangjie.diagnostics.infos.warnings.CONFLICTING_IMPORT
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.LazyExplicitImportScope
import org.cangnova.cangjie.resolve.QualifiedExpressionResolver
import org.cangnova.cangjie.resolve.QualifiedExpressionResolver.QualifierPart
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.ImportingScope
import org.cangnova.cangjie.resolve.scopes.concat
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.expressions.isWithoutValueArguments
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.flatMapToNullable

interface ImportForceResolver {
    fun forceResolveNonDefaultImports()
    fun forceResolveImport(importDirective: CjImportDirectiveItem)
}

class ImportResolutionComponents(
    val storageManager: StorageManager,
    val qualifiedExpressionResolver: QualifiedExpressionResolver,
    val moduleDescriptor: ModuleDescriptor,
//    val platformToCangJieClassMapper: PlatformToCangJieClassMapper,
    val languageVersionSettings: LanguageVersionSettings,
    val deprecationResolver: DeprecationResolver,
//    val optimizingOptions: OptimizingOptions,
)

inline fun <reified I : CjImportInfo> makeAllUnderImportsIndexed(imports: Collection<I>): IndexedImports<I> =
    IndexedImports(imports.filter { it.isAllUnder }.toTypedArray())

class ExplicitImportsIndexed<I : CjImportInfo>(
    imports: Array<I>,
    storageManager: StorageManager
) : IndexedImports<I>(imports) {

    private val nameToDirectives: NotNullLazyValue<ListMultimap<Name, I>> = storageManager.createLazyValue {
        val builder = ImmutableListMultimap.builder<Name, I>()

        for (directive in imports) {
            val importedName = directive.importedName ?: continue // parse error
            builder.put(importedName, directive)
        }

        builder.build()
    }

    override fun importsForName(name: Name) = nameToDirectives().get(name)
}

open class IndexedImports<I : CjImportInfo>(val imports: Array<I>) {
    open fun importsForName(name: Name): Iterable<I> = imports.asIterable()
}

inline fun <reified I : CjImportInfo> makeExplicitImportsIndexed(
    imports: Collection<I>,
    storageManager: StorageManager
): IndexedImports<I> =
    ExplicitImportsIndexed(imports.filter { !it.isAllUnder }.toTypedArray(), storageManager)

open class LazyImportResolver<I : CjImportInfo>(
    internal val components: ImportResolutionComponents,
    val indexedImports: IndexedImports<I>,
    private val excludedImportNames: Collection<FqName>,
    val traceForImportResolve: BindingTrace,
    val packageFragment: PackageFragmentDescriptor?
) {
    val allNames: Set<Name>? by components.storageManager.createNullableLazyValue {
        indexedImports.imports.asIterable()
            .flatMapToNullable(ObjectOpenHashSet()) { getImportScope(it).computeImportedNames() }
    }

    private val importedScopesProvider = with(components) {
        storageManager.createMemoizedFunctionWithNullableValues { directive: CjImportInfo ->

//            (traceForImportResolve.bindingContext.diagnostics as MutableDiagnosticsWithSuppression).clear()
            qualifiedExpressionResolver.processImportReference(
                directive, moduleDescriptor, traceForImportResolve, excludedImportNames, packageFragment
            )
        }
    }

    fun recordLookup(name: Name, location: LookupLocation) {
        if (allNames == null) return
        for (it in indexedImports.importsForName(name)) {
            val scope = getImportScope(it)
            if (scope !== ImportingScope.Empty) {
                scope.recordLookup(name, location)
            }
        }
    }

    fun definitelyDoesNotContainName(name: Name): Boolean {
        // Calculation of all names is undesirable for cases when the scope doesn't live long and is big enough.
        // In such cases we often do the same work twice - first time for computing definitelyDoesNotContainName
        // and second time for resolution itself. Results seem to be not reused.
        // This optimization is used in CangJie Notebooks
//        return if (components.optimizingOptions.shouldCalculateAllNamesForLazyImportScopeOptimizing(packageFragment?.containingDeclaration)) {
//            allNames?.let { name !in it } == true
//        } else {
//            false
//        }
        return false
    }

    fun <D : DeclarationDescriptor> collectFromImports(
        name: Name,
        descriptorsSelector: (ImportingScope) -> Collection<D>
    ): Collection<D> =
        components.storageManager.compute {
            var descriptors: Collection<D>? = null
            for (directive in indexedImports.importsForName(name)) {
                val descriptorsForImport = descriptorsSelector(getImportScope(directive))
                descriptors = descriptors.concat(descriptorsForImport)
            }

            descriptors.orEmpty()
        }

    fun getImportScope(directive: CjImportInfo): ImportingScope {
        return importedScopesProvider(directive) ?: ImportingScope.Empty
    }
}


/**
 * LazyImportResolverForCjImportDirective 是用于解析 CjImportDirectiveItem 的 LazyImportResolver 实现。
 * 它负责在需要时解析导入语句，处理特定的导入逻辑。
 *
 * @param components 提供解析导入所需的各种组件。
 * @param indexedImports 包含已索引的导入指令。
 * @param excludedImportNames 要排除解析的导入名称集合。
 * @param traceForImportResolve 用于记录绑定信息的跟踪对象。
 * @param packageFragment 可能相关的包片段描述符。
 */
class LazyImportResolverForCjImportDirective(
    components: ImportResolutionComponents,
    indexedImports: IndexedImports<CjImportDirectiveItem>,
    excludedImportNames: Collection<FqName>,
    traceForImportResolve: BindingTrace,
    packageFragment: PackageFragmentDescriptor?
) : LazyImportResolver<CjImportDirectiveItem>(
    components, indexedImports, excludedImportNames, traceForImportResolve, packageFragment
), ImportForceResolver {
    /**
     * 强制解析非默认导入。
     * 这是 ImportForceResolver 接口要求实现的方法。
     */
    override fun forceResolveNonDefaultImports() {
        forceResolveNonDefaultImportsTask()
    }

    /**
     * 检查解析后的导入指令是否存在特定问题，例如重命名的运算符导入。
     *
     * @param importDirective 要检查的导入指令信息。
     */
    private fun checkResolvedImportDirective(importDirective: CjImportInfo) {
        if (importDirective !is CjImportDirectiveItem) return
        val importedReference = CjPsiUtil.getLastReference(importDirective.importedReference ?: return) ?: return
        val importedDescriptor =
            traceForImportResolve.bindingContext[BindingContext.REFERENCE_TARGET, importedReference] ?: return

        val aliasName = importDirective.aliasName

        if (importedDescriptor is FunctionDescriptor && importedDescriptor.isOperator &&
            aliasName != null && OperatorConventions.isConventionName(Name.identifier(aliasName))
        ) {
            traceForImportResolve.report(OPERATOR_RENAMED_ON_IMPORT.on(importedReference))
        }
    }

    /**
     * 强制解析非默认导入的任务。
     * 这个值是懒加载的，以提高性能。
     */
    private val forceResolveNonDefaultImportsTask: NotNullLazyValue<Unit> = components.storageManager.createLazyValue {
        val explicitClassImports = HashMultimap.create<String, CjImportDirectiveItem>()
        for (importInfo in indexedImports.imports) {
            forceResolveImport(importInfo)

            val scope = getImportScope(importInfo)

            val alias = importInfo.importedName
            if (alias != null) {
                if (scope.getContributedDescriptors {
                        it == alias
                    }.isNotEmpty()) {
                    explicitClassImports.put(alias.asString(), importInfo)

                }
//                val lookupLocation = CangJieLookupLocation(importInfo)
//                if (scope.getContributedDescriptors(alias, lookupLocation) != null) {
//                    explicitClassImports.put(alias.asString(), importInfo)
//                }
            }

            checkResolvedImportDirective(importInfo)
        }
        for ((alias, import) in explicitClassImports.entries()) {
            if (alias.all { it == '_' }) {

                traceForImportResolve.report(UNDERSCORE_IS_RESERVED.on(import))
            }
        }
        for (alias in explicitClassImports.keySet()) {
            val imports = explicitClassImports.get(alias)
            if (imports.size > 1) {
                imports.forEach {

                    traceForImportResolve.report(CONFLICTING_IMPORT.on(it, alias))
                }
            }
        }
    }

    /**
     * 用于强制解析导入指令的备忘录函数。
     * 它确保相同地导入指令不会被多次解析。
     */
    private val forceResolveImportDirective =
        components.storageManager.createMemoizedFunction { directive: CjImportDirectiveItem ->
            val scope = getImportScope(directive)
            if (scope is LazyExplicitImportScope) {
                val allDescriptors = scope.storeReferencesToDescriptors()
//            PlatformClassesMappedToCangJieChecker.checkPlatformClassesMappedToCangJie(
//                components.platformToCangJieClassMapper, traceForImportResolve, directive, allDescriptors
//            )
            }

            Unit
        }

    /**
     * 强制解析给定的导入指令。
     *
     * @param importDirective 要解析的导入指令。
     */
    override fun forceResolveImport(importDirective: CjImportDirectiveItem) {

        forceResolveImportDirective(importDirective)
    }
}

/**
 * LazyImportScope 是一个懒加载的导入作用域，用于管理导入的类、变量、函数等。
 *
 * @param parent 父导入作用域
 * @param importResolver 主要的懒加载导入解析器
 * @param secondaryImportResolver 次要的懒加载导入解析器
 * @param filteringKind 过滤类型
 * @param debugName 调试名称
 */
class LazyImportScope(
    override val parent: ImportingScope?,
    private val importResolver: LazyImportResolver<*>,
    private val secondaryImportResolver: LazyImportResolver<*>?,
    private val filteringKind: FilteringKind,
    private val debugName: String
) : ImportingScope {

    /**
     * FilteringKind枚举类用于定义过滤类型的常量
     * 它帮助确定在特定操作或方法中应考虑的对象范围
     */
    enum class FilteringKind {
        /**
         * 表示所有类，无论可见性如何
         */
        ALL,

        /**
         * 仅表示可见的类
         */
        VISIBLE_CLASSES,

        /**
         * 仅表示不可见的类
         */
        INVISIBLE_CLASSES
    }

    /**
     * 判断分类器描述符在导入作用域中是否可见。
     *
     * 此函数重写了分类器描述符在导入作用域中的可见性检查逻辑，考虑了过滤模式和可见性规则。
     * 它特别处理了在不同语言版本设置下的可见性检查。
     *
     * @param descriptor 要检查可见性的分类器描述符。
     * @return 如果分类器描述符在导入作用域中可见，则返回 true；否则返回 false。
     */
    private fun LazyImportResolver<*>.isClassifierVisible(descriptor: ClassifierDescriptor): Boolean {
        // 如果过滤模式为 ALL，则所有描述符都被认为是可见的。
        if (filteringKind == FilteringKind.ALL) return true

        // TODO: 不在此处执行此检查，因为正确工作需要相应的 PSI 元素
        // 如果描述符已弃用并在解析中隐藏，则认为不可见。
        //    if (components.deprecationResolver.isHiddenInResolution(descriptor, fromImportingScope = true)) return false

        // 获取描述符的可见性。
        val visibility = (descriptor as DeclarationDescriptorWithVisibility).visibility
        // 根据过滤模式确定是否只包括可见的类。
        val includeVisible = filteringKind == FilteringKind.VISIBLE_CLASSES
        // 如果可见性不需要在导入中检查，则根据过滤模式直接返回结果。
        if (!visibility.mustCheckInImports()) return includeVisible
        // 根据不同的语言版本设置确定从哪个描述符检查可见性。
        val fromDescriptor = packageFragment ?: components.moduleDescriptor
        // 检查忽略接收者的可见性是否与预期的可见性匹配。
        return isVisibleIgnoringReceiver(
            descriptor, fromDescriptor, components.languageVersionSettings
        ) == includeVisible
    }


    /**
     * 根据名称和位置获取贡献的分类器描述符
     *
     * 此函数旨在解析并返回一个与给定名称和位置相关的分类器描述符它首先尝试使用主导入解析器
     * 解析分类器，如果未能找到，则尝试使用次级导入解析器此方法支持在类型解析过程中处理
     * 不同的命名空间和导入路径
     *
     * @param name 分类器的名称
     * @param location 查找位置，提供了关于在哪里进行查找的上下文信息
     * @return 返回找到的分类器描述符，如果没有找到则返回null
     */
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        // 首先尝试使用主导入解析器获取分类器
        return importResolver.getClassifier(name, location) ?: secondaryImportResolver?.getClassifier(name, location)
    }

    /**
     * 获取贡献的枚举条目
     *
     * @param name 枚举名称
     * @param location 查找位置
     * @return 枚举描述符列表
     */
    override fun getContributedEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return importResolver.getEnumEntrys(name, location)
    }

    /**
     * 获取贡献的分类器
     *
     * @param name 分类器名称
     * @param location 查找位置
     * @return 分类器描述符列表
     */
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return importResolver.getClassifiers(name, location)
    }

    /**
     * 获取枚举条目
     *
     * @param name 枚举名称
     * @param location 查找位置
     * @return 枚举描述符列表
     */
    private fun LazyImportResolver<*>.getEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        components.storageManager.compute {
            val names = packageFragment?.declarationProvider?.getEnumEntryDeclarations(name)?.map {
                it.enumTypeName
            } ?: emptyList()
            val imports = if (names.isNotEmpty()) {
                names.flatMap {
                    indexedImports.importsForName(it)
                }

            } else {
                indexedImports.importsForName(name)
            }


            val target: MutableList<ClassifierDescriptor> = mutableListOf()
            for (directive in imports) {

                val importScope = getImportScope(directive)
                val descriptors = importScope.getContributedEnumEntrys(name, location)
                target.addAll(descriptors)
            }

            target
        }

    /**
     * 获取分类器
     *
     * @param name 分类器名称
     * @param location 查找位置
     * @return 分类器描述符列表
     */
    private fun LazyImportResolver<*>.getClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        components.storageManager.compute {
            val imports = indexedImports.importsForName(name)

            val target: MutableList<ClassifierDescriptor> = mutableListOf()
            for (directive in imports) {
                val descriptors = getImportScope(directive).getContributedClassifiers(name, location)
                target.addAll(descriptors)
            }

            target
        }





    /**
     * 根据名称获取分类描述符
     *
     * @param name 名称
     * @param location 查找位置
     * @return 分类描述符，如果找不到则返回null
     */
    private fun LazyImportResolver<*>.getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        components.storageManager.compute {
            // 获取指定名称的导入信息
            val imports = indexedImports.importsForName(name)

            var target: ClassifierDescriptor? = null
            for (directive in imports) {
                // 从导入作用域中获取分类描述符
                val descriptor = getImportScope(directive).getContributedClassifier(name, location)
                // 如果描述符不是类描述符或类型别名描述符，或者不可见，则继续下一次循环
                if (descriptor !is ClassDescriptor && descriptor !is TypeAliasDescriptor || !isClassifierVisible(
                        descriptor
                    )
                )
                    continue /* type parameters can't be imported */
                if (target != null && target != descriptor) {
                    // 如果存在多个不同的描述符，则表示存在歧义，返回null
//                if (isCangJieOrJvmThrowsAmbiguity(
//                        descriptor,
//                        target
//                    ) || isCangJieOrNativeThrowsAmbiguity(descriptor, target)
//                ) {
//                    if (descriptor.isCangJieThrows()) {
//                        target = descriptor
//                    }
//                } else {
                    return@compute null // ambiguity
//                }
                } else {
                    // 如果没有歧义，将描述符设置为目标
                    target = descriptor
                }
            }

            // 返回最终的目标描述符
            target
        }

    /**
     * 获取贡献的包视图描述符
     *
     * @param name 名称
     * @return 包视图描述符，如果找不到则返回null
     */
    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    /**
     * 获取贡献的变量描述符集合
     *
     * @param name 名称
     * @param location 查找位置
     * @return 变量描述符集合
     */
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        // 如果过滤类型是不可见的类，则返回空集合
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        // 从导入解析器中收集变量描述符，如果为空，则尝试从次要导入解析器中收集
        return importResolver.collectFromImports(name) { scope -> scope.getContributedVariables(name, location) }
            .ifEmpty {
                secondaryImportResolver?.collectFromImports(name) { scope ->
                    scope.getContributedVariables(
                        name,
                        location
                    )
                }.orEmpty()
            }
    }

    /**
     * 获取当前作用域中具有给定名称和位置的贡献属性。
     *
     * @param name 属性名称
     * @param location 查找位置
     * @return 包含贡献属性的集合
     */
    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope -> scope.getContributedPropertys(name, location) }
            .ifEmpty {
                secondaryImportResolver?.collectFromImports(name) { scope ->
                    scope.getContributedPropertys(
                        name,
                        location
                    )
                }.orEmpty()
            }
    }

    /**
     * 获取当前作用域中具有给定名称和位置的贡献函数。
     *
     * @param name 函数名称
     * @param location 查找位置
     * @return 包含贡献函数的集合
     */
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope -> scope.getContributedFunctions(name, location) }
            .ifEmpty {
                secondaryImportResolver?.collectFromImports(name) { scope ->
                    scope.getContributedFunctions(
                        name,
                        location
                    )
                }.orEmpty()
            }
    }

    /**
     * 获取当前作用域中具有给定名称和位置的贡献宏。
     *
     * @param name 宏名称
     * @param location 查找位置
     * @return 包含贡献宏的集合
     */
    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope -> scope.getContributedMacros(name, location) }
            .ifEmpty {
                secondaryImportResolver?.collectFromImports(name) { scope ->
                    scope.getContributedMacros(
                        name,
                        location
                    )
                }.orEmpty()
            }
    }

    /**
     * 获取当前作用域中符合指定条件的贡献描述符。
     *
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param changeNamesForAliased 是否更改别名名称
     * @return 包含贡献描述符的集合
     */
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        // 不在此处进行可见性过滤，因为所有描述符（无论是可见还是不可见）都将被添加
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        val storageManager = importResolver.components.storageManager
        if (secondaryImportResolver != null) {
            assert(storageManager === secondaryImportResolver.components.storageManager) { "不支持多个存储管理器" }
        }

        return storageManager.compute {
            val result = linkedSetOf<DeclarationDescriptor>()
            val importedNames = if (secondaryImportResolver == null) null else hashSetOf<Name>()

            for (directive in importResolver.indexedImports.imports) {
                val importedName = directive.importedName
                if (importedName == null || nameFilter(importedName)) {
                    val newDescriptors =
                        importResolver.getImportScope(directive)
                            .getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)
                    result.addAll(newDescriptors)

                    if (importedNames != null) {
                        for (descriptor in newDescriptors) {
                            importedNames.add(descriptor.name)
                        }
                    }
                }
            }

            secondaryImportResolver?.let { resolver ->
                for (directive in resolver.indexedImports.imports) {
                    val newDescriptors =
                        resolver.getImportScope(directive)
                            .getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)

                    for (descriptor in newDescriptors) {
                        if (descriptor.name !in (importedNames ?: return@let)) {
                            result.add(descriptor)
                        }
                    }
                }
            }

            result
        }
    }

    /**
     * 返回 LazyImportScope 的字符串表示形式。
     *
     * @return 字符串表示形式
     */
    override fun toString() = "LazyImportScope: $debugName"

    /**
     * 打印结构信息
     *
     * @param p 打印器对象，用于输出结构信息
     */
    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", debugName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }

    /**
     * 判断是否肯定不包含指定名称
     *
     * @param name 要检查的名称
     * @return 如果肯定不包含指定名称返回true，否则返回false
     */
    override fun definitelyDoesNotContainName(name: Name): Boolean =
        importResolver.definitelyDoesNotContainName(name) && secondaryImportResolver?.definitelyDoesNotContainName(name) != false

    /**
     * 记录名称查找
     *
     * @param name 要记录的名称
     * @param location 名称出现的位置
     */
    override fun recordLookup(name: Name, location: LookupLocation) {
        importResolver.recordLookup(name, location)
        secondaryImportResolver?.recordLookup(name, location)
    }

    /**
     * 获取贡献的包限定部分
     *
     * @param name 名称对象
     * @return 包含限定部分的列表
     */
    override fun getContributedPackageQualifierPart(name: Name): List<List<QualifierPart>> {
        val list = importResolver.indexedImports.importsForName(name).mapNotNull {
//    it as CjImportDirectiveItem
            it.importContent?.asQualifierPartList()
        }

        return list
    }

    /**
     * 获取贡献的包完全限定名
     *
     * @param name 名称对象
     * @return 包含完全限定名的列表，如果为空则返回null
     */
    override fun getContributedPackageFqName(name: Name/*, location: LookupLocation*/): List<FqName>? {
        val list = importResolver.indexedImports.importsForName(name).mapNotNull {
            it.importedFqName
        }
//        过滤 非模块名的导入
            .filter {
                !it.isModuleName
            }
        if (list.isEmpty()) return null
        return list
    }

    /**
     * 计算导入的名称集合
     *
     * @return 导入的名称集合，如果为空则返回null
     */
    override fun computeImportedNames(): Set<Name>? =
        importResolver.allNames?.union(secondaryImportResolver?.allNames.orEmpty())
}


fun CjImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
    when (this) {
        is CjImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()
        is CjImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
    }


fun CjExpression.asQualifierPartList(doubleColonLHS: Boolean = false): List<QualifiedExpressionResolver.ExpressionQualifierPart> {
    val result = SmartList<QualifiedExpressionResolver.ExpressionQualifierPart>()

    fun addQualifierPart(expression: CjExpression?): Boolean {
        if (expression is CjSimpleNameExpression) {
            result.add(QualifiedExpressionResolver.ExpressionQualifierPart(expression))
            return true
        }
        if (doubleColonLHS && expression is CjCallExpression && expression.isWithoutValueArguments) {
            val simpleName = expression.calleeExpression
            if (simpleName is CjSimpleNameExpression) {
                result.add(
                    QualifiedExpressionResolver.ExpressionQualifierPart(
                        simpleName.referencedNameAsName,
                        simpleName,
                        expression.typeArgumentList
                    )
                )
                return true
            }
        }
        return false
    }

    var expression: CjExpression? = this
    while (true) {
        if (addQualifierPart(expression)) break
        if (expression !is CjQualifiedExpression) break

        addQualifierPart(expression.selectorExpression)

        expression = expression.receiverExpression
    }

    return result.asReversed()
}
