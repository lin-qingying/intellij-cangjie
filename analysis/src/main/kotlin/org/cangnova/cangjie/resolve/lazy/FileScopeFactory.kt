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

/**
 * 文件作用域工厂
 *
 * 本文件提供了仓颉文件的作用域创建和管理功能。
 * 作用域是符号解析的基础，决定了在代码的不同位置可以访问哪些声明。
 *
 * ## 核心概念
 *
 * ### 作用域层次结构
 *
 * 仓颉文件的作用域按以下层次组织（从内到外）：
 *
 * ```
 * 1. 词法作用域 (LexicalScope)          - 最内层，包含当前文件的顶层声明
 *    ↓
 * 2. 显式导入作用域 (Explicit Imports)  - 用户明确写出的 import 语句
 *    ↓
 * 3. 当前包作用域 (Current Package)     - 当前包的所有可见成员
 *    ↓
 * 4. 全通配导入作用域 (All-Under)        - import foo.* 类型的导入
 *    ↓
 * 5. 默认导入作用域 (Default Imports)   - 语言自动导入的标准库
 *    ↓
 * 6. 低优先级导入作用域                  - 低优先级的默认导入
 * ```
 *
 * ### 导入类型
 *
 * - **显式导入** (Explicit Import): `import foo.bar.Baz` 或 `import foo.bar.Baz as Alias`
 * - **全通配导入** (All-Under Import): `import foo.bar.*`
 * - **默认导入** (Default Import): 编译器自动添加的导入（如标准库）
 * - **低优先级导入** (Low Priority Import): 优先级较低的默认导入
 * - **额外导入** (Extra Import): 通过扩展点提供的额外导入
 *
 * ### 可见性过滤
 *
 * 作用域按可见性分为两种过滤模式：
 * - **VISIBLE_CLASSES**: 只包含可见的类和声明
 * - **INVISIBLE_CLASSES**: 只包含不可见的类（用于诊断和错误提示）
 *
 * ## 主要组件
 *
 * - [FileScopeFactory]: 作用域工厂，负责创建和配置作用域
 * - [FileScopes]: 文件作用域容器，包含词法作用域、导入作用域和导入解析器
 * - [LazyImportResolver]: 懒加载导入解析器，按需解析导入的符号
 * - [ImportingScope]: 导入作用域接口，提供符号查找功能
 * - [CurrentPackageScope]: 当前包作用域实现
 *
 * @see ImportingScope
 * @see LexicalScope
 * @see LazyImportResolver
 */

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjImportDirectiveItem
import org.cangnova.cangjie.psi.CjFile

import org.cangnova.cangjie.psi.CjImportInfo
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices
import org.cangnova.cangjie.resolve.extensions.ExtraImportsProviderExtension
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.source.CangJieSourceElement
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.utils.Printer
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.cangnova.cangjie.psi.ImportPath
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace

/**
 * 文件作用域容器
 *
 * 封装了一个仓颉文件所需的所有作用域信息。
 *
 * ## 组成部分
 *
 * - **词法作用域** ([lexicalScope]): 提供对当前文件顶层声明的访问
 * - **导入作用域** ([importingScope]): 提供对导入符号的访问
 * - **导入强制解析器** ([importForceResolver]): 用于强制解析特定的导入语句
 *
 * ## 使用场景
 *
 * - 符号解析: 在给定位置查找符号定义
 * - 代码补全: 提供可用符号列表
 * - 错误检查: 验证符号是否可访问
 *
 * @property lexicalScope 词法作用域，包含文件顶层声明
 * @property importingScope 导入作用域，包含所有导入的符号
 * @property importForceResolver 导入强制解析器，用于按需解析导入
 *
 * @see FileScopeFactory.createScopesForFile
 */
data class FileScopes(
    val lexicalScope: LexicalScope,
    val importingScope: ImportingScope,
    val importForceResolver: ImportForceResolver
)

/**
 * 文件作用域工厂
 *
 * 负责为仓颉文件创建和配置各种作用域。
 * 这是符号解析系统的核心组件，管理着导入、包作用域等复杂的符号查找逻辑。
 *
 * ## 主要功能
 *
 * 1. **创建文件作用域**: 为每个文件创建完整的作用域层次结构
 * 2. **管理默认导入**: 处理语言级别的默认导入（如标准库）
 * 3. **处理显式导入**: 解析用户编写的 import 语句
 * 4. **支持别名**: 处理 `import foo.Bar as Baz` 形式的别名导入
 * 5. **可见性过滤**: 根据可见性规则过滤可访问的符号
 * 6. **扩展点集成**: 通过 [ExtraImportsProviderExtension] 支持额外导入
 *
 * ## 工作原理
 *
 * ### 作用域构建流程
 *
 * ```
 * createScopesForFile()
 *   ↓
 * FilesScopesBuilder
 *   ↓
 * 创建导入解析器 (explicitImportResolver, allUnderImportResolver)
 *   ↓
 * 创建默认导入解析器 (defaultImportResolvers)
 *   ↓
 * 构建作用域链 (createImportingScope)
 *   ↓
 * 返回 FileScopes (lexicalScope + importingScope + importForceResolver)
 * ```
 *
 * ### 默认导入机制
 *
 * 默认导入分为两类：
 * - **标准优先级**: 从 [PlatformDependentAnalyzerServices.getDefaultImports] 获取
 * - **低优先级**: 从 [PlatformDependentAnalyzerServices.defaultLowPriorityImports] 获取
 *
 * 这些导入会被缓存（通过 `defaultImportResolvers`）以提高性能。
 *
 * ### 别名处理
 *
 * 别名导入会被记录在 `aliasImportNames` 中，
 * 用于防止别名与原始名称冲突（如果导入了 `import Foo as Bar`，则 `Foo` 会被排除）。
 *
 * ## 依赖组件
 *
 * - [TopLevelDescriptorProvider]: 提供顶层声明的描述符
 * - [BindingTrace]: 记录符号绑定信息
 * - [PlatformDependentAnalyzerServices]: 提供平台相关的分析服务
 * - [ImportResolutionComponents]: 导入解析所需的组件集合
 *
 * @param topLevelDescriptorProvider 顶层描述符提供者，用于获取包片段和顶层声明
 * @param bindingTrace 绑定追踪器，记录符号解析结果
 * @param analyzerServices 平台相关的分析器服务，提供默认导入等配置
 * @param components 导入解析组件，包含模块描述符、存储管理器等
 *
 * @see FileScopes
 * @see ImportResolutionComponents
 * @see LazyImportResolver
 */
class FileScopeFactory(
    private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
    private val bindingTrace: BindingTrace,
    private val analyzerServices: PlatformDependentAnalyzerServices,
    private val components: ImportResolutionComponents
) {
    /**
     * 默认导入列表
     *
     * 从平台服务获取的默认导入，不包含低优先级导入。
     * 这些导入会自动添加到每个文件中，无需显式 import 语句。
     *
     * 示例：标准库中的常用类型、函数等
     */
    private val defaultImports =
        analyzerServices.getDefaultImports(components.languageVersionSettings, includeLowPriorityImports = false)
            .map(::DefaultImportImpl)


    /**
     * 低优先级默认导入列表
     *
     * 这些导入的优先级低于用户的显式导入和当前包的符号，
     * 用于避免与用户代码冲突。
     */
    private val defaultLowPriorityImports = analyzerServices.defaultLowPriorityImports.map(::DefaultImportImpl)


    /**
     * 默认导入的实现类
     *
     * 将 [ImportPath] 适配为 [CjImportInfo] 接口。
     *
     * @param importPath 导入路径，包含完全限定名和别名信息
     */
    private class DefaultImportImpl(private val importPath: ImportPath) : CjImportInfo {
        override val isAllUnder: Boolean get() = importPath.isAllUnder

        override val importContent = CjImportInfo.ImportContent.FqNameBased(importPath.fqName)

        override val aliasName: String? get() = importPath.alias?.asString()

        override val importedFqName: FqName get() = importPath.fqName
//        override val importedFqNames: MutableList<FqName> = mutableListOf(importPath.fqName)
    }

    /**
     * 为文件创建作用域
     *
     * 这是主要的公共 API，为给定的仓颉文件创建完整的作用域结构。
     *
     * ## 工作流程
     *
     * 1. 获取文件所属的包视图和包片段
     * 2. 创建 [FilesScopesBuilder] 来构建作用域
     * 3. 返回包含词法作用域、导入作用域和导入解析器的 [FileScopes]
     *
     * ## 参数说明
     *
     * @param file 要创建作用域的文件
     * @param existingImports 已存在的导入作用域（可选），用于作用域链接
     * @param createDefaultImportingScopes 是否创建默认导入作用域，默认为 true
     *
     * @return 包含所有必要作用域信息的 [FileScopes] 对象
     *
     * ## 使用示例
     *
     * ```kotlin
     * val fileScopes = factory.createScopesForFile(cjFile)
     * val lexicalScope = fileScopes.lexicalScope
     * val symbol = lexicalScope.findClassifier(name, location)
     * ```
     *
     * @see FileScopes
     * @see FilesScopesBuilder
     */
    fun createScopesForFile(
        file: CjFile,
        existingImports: ImportingScope? = null,
        createDefaultImportingScopes: Boolean = true
    ): FileScopes {
        val packageView = components.moduleDescriptor.getPackage(file.packageFqName)
        val packageFragment = topLevelDescriptorProvider.getPackageFragmentOrDiagnoseFailure(file.packageFqName, file)

        return FilesScopesBuilder(
            file,
            existingImports,
            packageFragment,
            packageView,
            createDefaultImportingScopes
        ).result
    }

    private data class DefaultImportResolvers(
        val explicit: LazyImportResolver<CjImportInfo>,
        val allUnder: LazyImportResolver<CjImportInfo>,
        val lowPriority: LazyImportResolver<CjImportInfo>
    )

    private fun createDefaultImportResolvers(
        extraImports: Collection<CjImportInfo>,
        aliasImportNames: Collection<FqName>,
    ): DefaultImportResolvers {
        val tempTrace =
            TemporaryBindingTrace.create(bindingTrace, "Transient trace for default imports lazy resolve", false)
        val allImplicitImports = defaultImports concat extraImports/* concat enumDefualtImports*/

        val defaultImportsFiltered = if (aliasImportNames.isEmpty()) { // optimization
            allImplicitImports
        } else {
            allImplicitImports.filter { it.isAllUnder || it.importedFqName !in aliasImportNames }
        }

        val explicit = createDefaultImportResolver(
            makeExplicitImportsIndexed(defaultImportsFiltered, components.storageManager),
            tempTrace,
            packageFragment = null,
            aliasImportNames = aliasImportNames
        )
        val allUnder = createDefaultImportResolver(
            makeAllUnderImportsIndexed(defaultImportsFiltered),
            tempTrace,
            packageFragment = null,
            aliasImportNames = aliasImportNames,
            excludedImports = analyzerServices.excludedImports
        )
        val lowPriority = createDefaultImportResolver(
            makeAllUnderImportsIndexed(defaultLowPriorityImports.also { imports ->
                assert(imports.all { it.isAllUnder }) { "All low priority imports must be all-under: $imports" }
            }),
            tempTrace,
            packageFragment = null,
            aliasImportNames = aliasImportNames
        )

        return DefaultImportResolvers(explicit, allUnder, lowPriority)
    }

    private val defaultImportResolvers by components.storageManager.createLazyValue {
        createDefaultImportResolvers(emptyList(), emptyList())
    }

    private fun createDefaultImportResolver(
        indexedImports: IndexedImports<CjImportInfo>,
        trace: BindingTrace,
        aliasImportNames: Collection<FqName>,
        packageFragment: PackageFragmentDescriptor?,
        excludedImports: List<FqName>? = null
    ) = LazyImportResolver(
        components, indexedImports, aliasImportNames concat excludedImports, trace, packageFragment
    )

    private fun createImportResolver(
        indexedImports: IndexedImports<CjImportDirectiveItem>,
        trace: BindingTrace,
        aliasImportNames: Collection<FqName>,
        packageFragment: PackageFragmentDescriptor?,
        excludedImports: List<FqName>? = null
    ) = LazyImportResolverForCjImportDirective(
        components, indexedImports, aliasImportNames concat excludedImports, trace, packageFragment
    )

    private inner class FilesScopesBuilder(
        private val file: CjFile,
        private val existingImports: ImportingScope?,
        private val packageFragment: PackageFragmentDescriptor,
        private val packageView: PackageViewDescriptor,
        private val createDefaultImportingScopes: Boolean,
    ) {

        val imports = file.importDirectivesItem

        //        val aliasImportNames = file.importListsField.mapNotNull {
//
//            if (it.aliasName != null) {
//                it.fqName
//            } else {
//                null
//            }
//        }
        val aliasImportNames = imports.mapNotNull {

            if (it.aliasName != null) {
                it.importedFqName
            } else {
                null
            }
        }
        val explicitImportResolver =
            createImportResolver(
                makeExplicitImportsIndexed(imports, components.storageManager),
                bindingTrace, aliasImportNames, packageFragment
            )
        val allUnderImportResolver = createImportResolver(
            makeAllUnderImportsIndexed(imports),
            bindingTrace,
            aliasImportNames,
            packageFragment
        ) // TODO: should we count excludedImports here also?

        val lazyImportingScope = object : ImportingScope by ImportingScope.Empty {
            // avoid constructing the scope before we query it
            override val parent: ImportingScope by components.storageManager.createLazyValue {

                createCurrentFileScope()
            }
        }

        val lexicalScope =
            LexicalScope.Base(
                lazyImportingScope,
                topLevelDescriptorProvider.getPackageFragmentOrDiagnoseFailure(file.packageFqName, file)
            )

        val importResolver = object : ImportForceResolver {
            override fun forceResolveNonDefaultImports() {
                explicitImportResolver.forceResolveNonDefaultImports()
                allUnderImportResolver.forceResolveNonDefaultImports()
            }

            override fun forceResolveImport(importDirective: CjImportDirectiveItem) {
                if (importDirective.isAllUnder) {
                    allUnderImportResolver.forceResolveImport(importDirective)
                } else {
                    explicitImportResolver.forceResolveImport(importDirective)
                }

            }
        }

        val result = FileScopes(lexicalScope, lazyImportingScope, importResolver)

        private fun createDefaultImportResolversForFile(): DefaultImportResolvers {
            val extraImports = ExtraImportsProviderExtension.getInstance(file.project).getExtraImports(file)



            if (extraImports.isEmpty() && aliasImportNames.isEmpty()) {
                return defaultImportResolvers
            }

            return createDefaultImportResolvers(extraImports, aliasImportNames)
        }


        fun createCurrentFileScope(): ImportingScope {
            return CurrentFileScope(createImportingScope())
        }

        private inner class CurrentFileScope(override val parent: ImportingScope?) : ImportingScope {

            override fun getContributedPackage(name: Name): PackageViewDescriptor? {
                val importDirectives = explicitImportResolver.indexedImports.importsForName(name)

                val packageViews = importDirectives.map {
//                     explicitImportResolver.getImportScope(it)实际上附带了name名称
                    val importScope = explicitImportResolver.getImportScope(it)

                    importScope.getContributedPackage(name)
                }
                return packageViews.firstOrNull()?.takeIf { descriptor -> !descriptor.isEmpty() }
            }

            override fun getContributedDescriptors(
                kindFilter: DescriptorKindFilter,
                nameFilter: (Name) -> Boolean,
                changeNamesForAliased: Boolean
            ): Collection<DeclarationDescriptor> {
                return emptyList()
            }

            override fun computeImportedNames(): Set<Name>? {
                return null
            }

            override fun toString() = "Scope for current file ${file.name} "

            override fun printStructure(p: Printer) {
                p.println(this.toString())

            }


            override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {

//                val elements = file.declarations.flatMap {
//                    when(it){
//                        is CjEnum ->{
//                            it.entry + listOf(it)
//                        }
//                        else -> listOf(it)
//                    }
//                }.filter {
//
//                    it.name === name.asString()
//                }
//                if (elements.isEmpty()) return emptyList()
                var i = 0
                var _parent = parent
                while (_parent !is CurrentPackageScope && i < 15) {
                    _parent = _parent?.parent
                    i++
                }
//                if (_parent !is CurrentPackageScope) {
//                    return null
//                }
                return _parent?.getContributedClassifiers(name, location) ?: emptyList()

            }

            override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {


                var i = 0
                var _parent = parent
                while (_parent !is CurrentPackageScope && i < 15) {
                    _parent = _parent?.parent
                    i++
                }

                return _parent?.getContributedClassifier(name, location)


            }



            override fun getContributedVariables(
                name: Name,
                location: LookupLocation
            ): Collection<@JvmWildcard VariableDescriptor> {
                return emptyList()

            }

            override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
                return emptyList()

            }

            override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
                return emptyList()
            }

            override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {

                return emptyList()
            }

        }

        fun createImportingScope(): LazyImportScope {
            val (defaultExplicitImportResolver, defaultAllUnderImportResolver, defaultLowPriorityImportResolver) =
                createDefaultImportResolversForFile()

            val dummyContainerDescriptor = DummyContainerDescriptor(file, packageFragment)

            var scope: ImportingScope? = existingImports

            val debugName = "LazyFileScope for file " + file.name

            if (createDefaultImportingScopes) {
                scope = LazyImportScope(
                    scope, defaultAllUnderImportResolver, defaultLowPriorityImportResolver,
                    LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                    "Default all under imports in $debugName (invisible classes only)"
                )
            }

            scope = LazyImportScope(
                scope, allUnderImportResolver, null, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                "All under imports in $debugName (invisible classes only)"
            )

            scope = currentPackageScope(
                packageView,
                packageFragment,
                aliasImportNames,
                dummyContainerDescriptor,
                FilteringKind.INVISIBLE_CLASSES,
                scope
            )

            if (createDefaultImportingScopes) {
                scope = LazyImportScope(
                    scope,
                    defaultAllUnderImportResolver,
                    defaultLowPriorityImportResolver,
                    LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                    "Default all under imports in $debugName (visible classes)"
                )
            }

            scope = LazyImportScope(
                scope, allUnderImportResolver, null, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                "All under imports in $debugName (visible classes)"
            )

            if (createDefaultImportingScopes) {
                scope = LazyImportScope(
                    scope, defaultExplicitImportResolver, null, LazyImportScope.FilteringKind.ALL,
                    "Default explicit imports in $debugName"
                )
            }

            scope = SubpackagesImportingScope(scope, components.moduleDescriptor, FqName.ROOT)

            scope = currentPackageScope(
                packageView,
                packageFragment,
                aliasImportNames,
                dummyContainerDescriptor,
                FilteringKind.VISIBLE_CLASSES,
                scope
            )

            return LazyImportScope(
                scope,
                explicitImportResolver,
                null,
                LazyImportScope.FilteringKind.ALL,
                "Explicit imports in $debugName"
            )
        }

    }

    enum class FilteringKind {
        VISIBLE_CLASSES, INVISIBLE_CLASSES
    }


    private fun currentPackageScope(
        packageView: PackageViewDescriptor,
        packageFragment: PackageFragmentDescriptor,
        aliasImportNames: Collection<FqName>,
        fromDescriptor: DummyContainerDescriptor,
        filteringKind: FilteringKind,
        parentScope: ImportingScope
    ): ImportingScope {
        return CurrentPackageScope(
            packageView,
            packageFragment,
            aliasImportNames,
            fromDescriptor,
            filteringKind,
            parentScope,
            components.languageVersionSettings
        )
    }

    /**
     * 当前包作用域
     *
     * 只负责当前包自身成员的访问，不处理重导出逻辑。
     * 重导出逻辑由 [PackageReexportScope] 在单独的作用域层处理。
     *
     * ## 职责
     *
     * - 提供当前包中声明的类型、函数、变量等的访问
     * - 根据可见性规则过滤成员
     * - 处理别名导入导致的名称排除
     *
     * @see PackageReexportScope 处理重导出声明的作用域
     */
    inner class CurrentPackageScope(
        val packageView: PackageViewDescriptor,
        val packageFragment: PackageFragmentDescriptor,
        val aliasImportNames: Collection<FqName>,
        val fromDescriptor: DummyContainerDescriptor,
        val filteringKind: FilteringKind,
        val parentScope: ImportingScope,
        val languageVersionSettings: LanguageVersionSettings
    ) : ImportingScope {
        val scope = packageView.memberScope
        val names by lazy(LazyThreadSafetyMode.PUBLICATION) { scope.computeAllNames()?.let(::ObjectOpenHashSet) }
        val packageName = packageView.fqName
        val excludedNames = aliasImportNames.mapNotNull { if (it.parent() == packageName) it.shortName() else null }

        override val parent: ImportingScope = parentScope

        override fun getContributedPackage(name: Name): Nothing? = null

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            if (name in excludedNames) return null
            val classifier = scope.getContributedClassifier(name, location) ?: return null

            val visible = DescriptorVisibilityUtils.isVisibleIgnoringReceiver(
                classifier as DeclarationDescriptorWithVisibility,
                fromDescriptor,
                languageVersionSettings
            )
            return classifier.takeIf { filteringKind == if (visible) FilteringKind.VISIBLE_CLASSES else FilteringKind.INVISIBLE_CLASSES }
        }

        override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
            if (name in excludedNames) return emptyList()
            return scope.getContributedClassifiers(name, location)
        }



        override fun getContributedVariables(
            name: Name,
            location: LookupLocation
        ): Collection<@JvmWildcard VariableDescriptor> {
            if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
            if (name in excludedNames) return emptyList()
            return scope.getContributedVariables(name, location)
        }

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
            if (name in excludedNames) return emptyList()
            return scope.getContributedPropertys(name, location)
        }

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
            if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
            if (name in excludedNames) return emptyList()
            return scope.getContributedFunctions(name, location)
        }

        override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
            if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
            if (name in excludedNames) return emptyList()
            return scope.getContributedMacros(name, location)
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            changeNamesForAliased: Boolean
        ): Collection<DeclarationDescriptor> {
            // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
            if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
            return scope.getContributedDescriptors(
                kindFilter.withoutKinds(DescriptorKindFilter.PACKAGES_MASK)
            ) { name -> name !in excludedNames && nameFilter(name) }
                .filter { it !is PackageViewDescriptor } // subpackages of the current package not accessible by the short name
        }

        override fun computeImportedNames() = packageView.memberScope.computeAllNames()

        override fun definitelyDoesNotContainName(name: Name) = names?.let { name !in it } == true

        override fun toString() = "Scope for current package (${filteringKind.name})"

        override fun printStructure(p: Printer) {
            p.println(this.toString())
        }


    }


    // we use this dummy implementation of DeclarationDescriptor to check accessibility of symbols from the current package
    class DummyContainerDescriptor(file: CjFile, private val packageFragment: PackageFragmentDescriptor) :
        DeclarationDescriptorNonRoot {
        private val sourceElement = CangJieSourceElement(file)


        override val containingDeclaration: DeclarationDescriptor = packageFragment

        override val source: SourceElement = sourceElement

        override val original = this
        override val annotations: Annotations get() = Annotations.EMPTY

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
            throw UnsupportedOperationException()
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
            throw UnsupportedOperationException()
        }

        override val name: Name
            get() = throw UnsupportedOperationException()


    }
}

private infix fun <T> Collection<T>.concat(other: Collection<T>?) =
    if (other == null || other.isEmpty()) this else this + other


//@Service(Service.Level.APP)
//class CurrentPackageScopeService( ) {
//
//
//    companion object {
//        fun getInstance( ): CurrentPackageScopeService {
//          return  ServiceManager.getService(CurrentPackageScopeService::class.java)
//        }
//    }
//}
