package com.huawei.cangjie.resolve.lazy


import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.ide.stubindex.CangJieImportFqNameForPackageNameIndex
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.CjImportInfo
import com.huawei.cangjie.resolve.ImportPath
import com.huawei.cangjie.resolve.PlatformDependentAnalyzerServices
import com.huawei.cangjie.resolve.TemporaryBindingTrace
import com.huawei.cangjie.resolve.extensions.ExtraImportsProviderExtension
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.resolve.source.CangJieSourceElement
import com.huawei.cangjie.storage.getValue
import com.huawei.cangjie.utils.Printer
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

data class FileScopes(
    val lexicalScope: LexicalScope,
    val importingScope: ImportingScope,
    val importForceResolver: ImportForceResolver
)

class FileScopeFactory(
    private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
    private val bindingTrace: BindingTrace,
    private val analyzerServices: PlatformDependentAnalyzerServices,
    private val components: ImportResolutionComponents
) {
    private val defaultImports =
        analyzerServices.getDefaultImports(components.languageVersionSettings, includeLowPriorityImports = false)
            .map(::DefaultImportImpl)

    private val defaultLowPriorityImports = analyzerServices.defaultLowPriorityImports.map(::DefaultImportImpl)

    private class DefaultImportImpl(private val importPath: ImportPath) : CjImportInfo {
        override val isAllUnder: Boolean get() = importPath.isAllUnder

        override val importContent = CjImportInfo.ImportContent.FqNameBased(importPath.fqName)

        override val aliasName: String? get() = importPath.alias?.asString()

        override val importedFqName: FqName get() = importPath.fqName
//        override val importedFqNames: MutableList<FqName> = mutableListOf(importPath.fqName)
    }

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
        aliasImportNames: Collection<FqName>
    ): DefaultImportResolvers {
        val tempTrace =
            TemporaryBindingTrace.create(bindingTrace, "Transient trace for default imports lazy resolve", false)
        val allImplicitImports = defaultImports concat extraImports

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
        indexedImports: IndexedImports<CjImportDirective>,
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

        val imports = file.importDirectives

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
                createImportingScope()
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

            override fun forceResolveImport(importDirective: CjImportDirective) {
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
                file.project,
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
                file.project,

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
        project: Project,
        packageView: PackageViewDescriptor,
        packageFragment: PackageFragmentDescriptor,
        aliasImportNames: Collection<FqName>,
        fromDescriptor: DummyContainerDescriptor,
        filteringKind: FilteringKind,
        parentScope: ImportingScope
    ): ImportingScope {
//        val scope = packageView.memberScope
//        val names by lazy(LazyThreadSafetyMode.PUBLICATION) { scope.computeAllNames()?.let(::ObjectOpenHashSet) }
//        val packageName = packageView.fqName
//        val excludedNames = aliasImportNames.mapNotNull { if (it.parent() == packageName) it.shortName() else null }
//
//

        return CurrentPackageScope(
            project = project,
            packageView,
            packageFragment,
            aliasImportNames,
            fromDescriptor,
            filteringKind,
            parentScope,
            components.languageVersionSettings
        )
//        return object : ImportingScope {
//            override val parent: ImportingScope = parentScope
//
//            override fun getContributedPackage(name: Name): Nothing? = null
//
//            override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
//                if (name in excludedNames) return null
//                val classifier = scope.getContributedClassifier(name, location) ?: return null
//                val visible = DescriptorVisibilityUtils.isVisibleIgnoringReceiver(
//                    classifier as DeclarationDescriptorWithVisibility,
//                    fromDescriptor,
//                    components.languageVersionSettings
//                )
//                return classifier.takeIf { filteringKind == if (visible) FilteringKind.VISIBLE_CLASSES else FilteringKind.INVISIBLE_CLASSES }
//            }
//
//            override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
//                if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
//                if (name in excludedNames) return emptyList()
//                return scope.getContributedVariables(name, location)
//            }
//
//            override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
//                if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
//                if (name in excludedNames) return emptyList()
//                return scope.getContributedPropertys(name, location)
//            }
//
//            override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
//                if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
//                if (name in excludedNames) return emptyList()
//                return scope.getContributedFunctions(name, location)
//            }
//
//            override fun getContributedDescriptors(
//                kindFilter: DescriptorKindFilter,
//                nameFilter: (Name) -> Boolean,
//                changeNamesForAliased: Boolean
//            ): Collection<DeclarationDescriptor> {
//                // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
//                if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
//                return scope.getContributedDescriptors(
//                    kindFilter.withoutKinds(DescriptorKindFilter.PACKAGES_MASK)
//                ) { name -> name !in excludedNames && nameFilter(name) }
//                    .filter { it !is PackageViewDescriptor } // subpackages of the current package not accessible by the short name
//            }
//
//            override fun computeImportedNames() = packageView.memberScope.computeAllNames()
//
//            override fun definitelyDoesNotContainName(name: Name) = names?.let { name !in it } == true
//
//            override fun toString() = "Scope for current package (${filteringKind.name})"
//
//            override fun printStructure(p: Printer) {
//                p.println(this.toString())
//            }
//        }
    }

    inner class CurrentPackageScope(
        val project: Project,
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


        //        TODO 没有什么实现思路，导入树结构应该重构
        private val imports: Collection<CjImportDirective> = runReadAction {
            CangJieImportFqNameForPackageNameIndex.getReexport(
                packageFragment.fqName.asString(),
                project,
                GlobalSearchScope.allScope(project)
            )
        }

        private val explicitImportResolver =
            createImportResolver(
                makeExplicitImportsIndexed(imports, components.storageManager),
                bindingTrace, aliasImportNames, packageFragment
            )
        private val allUnderImportResolver = createImportResolver(
            makeAllUnderImportsIndexed(imports),
            bindingTrace,
            aliasImportNames,
            packageFragment
        )

        override val parent: ImportingScope = parentScope

        override fun getContributedPackage(name: Name): Nothing? = null


        private fun getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {

            return explicitImportResolver.getClassifier(name, location) ?: allUnderImportResolver.getClassifier(
                name,
                location
            )
        }

        private fun LazyImportResolver<*>.getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            components.storageManager.compute {
                val imports = indexedImports.importsForName(name)

                var target: ClassifierDescriptor? = null
                for (directive in imports) {
                    val descriptor = getImportScope(directive).getContributedClassifier(name, location)
                    if (descriptor !is ClassDescriptor && descriptor !is TypeAliasDescriptor /*|| !isClassifierVisible(
                            descriptor
                        )*/
                    )
                        continue /* type parameters can't be imported */
                    if (target != null && target != descriptor) {
//                    if (isCangJieOrJvmThrowsAmbiguity(
//                            descriptor,
//                            target
//                        ) || isCangJieOrNativeThrowsAmbiguity(descriptor, target)
//                    ) {
////                        if (descriptor.isCangJieThrows()) {
////                            target = descriptor
////                        }
//                    } else {
                        return@compute null // ambiguity
//                    }
                    } else {
                        target = descriptor
                    }
                }

                target
            }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            if (name in excludedNames) return null
            val classifier = scope.getContributedClassifier(name, location) ?:
            /*   如在当前包查找不到，在重导出语句中查找 (重导出语句不管什么访问修饰，都可以在本包访问)*/
            getClassifier(
                name,
                location
            ) ?: return null


            val visible = DescriptorVisibilityUtils.isVisibleIgnoringReceiver(
                classifier as DeclarationDescriptorWithVisibility,
                fromDescriptor,
                languageVersionSettings
            )
            return classifier.takeIf { filteringKind == if (visible) FilteringKind.VISIBLE_CLASSES else FilteringKind.INVISIBLE_CLASSES }
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
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
        override fun getSource() = sourceElement

        override val original = this
        override val annotations: Annotations get() = Annotations.EMPTY

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
            throw UnsupportedOperationException()
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
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
