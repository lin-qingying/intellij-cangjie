package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.descriptors.PackageViewDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.resolve.PlatformDependentAnalyzerServices
import com.huawei.cangjie.resolve.scopes.ImportingScope
import com.huawei.cangjie.resolve.scopes.LexicalScope

class FileScopeFactory(
    private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
    private val bindingTrace: BindingTrace,
    private val analyzerServices: PlatformDependentAnalyzerServices,
    private val components: ImportResolutionComponents
) {
    fun createScopesForFile(file: CjFile, existingImports: ImportingScope? = null, createDefaultImportingScopes: Boolean = true): FileScopes {
        val packageView = components.moduleDescriptor.getPackage(file.packageFqName)
        val packageFragment = topLevelDescriptorProvider.getPackageFragmentOrDiagnoseFailure(file.packageFqName, file)

        return FilesScopesBuilder(file, existingImports, packageFragment, packageView, createDefaultImportingScopes).result
    }

    private inner class FilesScopesBuilder(
        private val file: CjFile,
        private val existingImports: ImportingScope?,
        private val packageFragment: PackageFragmentDescriptor,
        private val packageView: PackageViewDescriptor,
        private val createDefaultImportingScopes: Boolean,
    ){
        private fun createImportResolver(
            indexedImports: IndexedImports<CjImportDirective>,
            trace: BindingTrace,
            aliasImportNames: Collection<FqName>,
            packageFragment: PackageFragmentDescriptor?,
            excludedImports: List<FqName>? = null
        ) = LazyImportResolverForCjImportDirective(
            components, indexedImports, aliasImportNames concat excludedImports, trace, packageFragment
        )
        val lazyImportingScope = object : ImportingScope by ImportingScope.Empty {
            // avoid constructing the scope before we query it
//            override val parent: ImportingScope by components.storageManager.createLazyValue {
//                createImportingScope()
//
//            }
        }
//        val explicitImportResolver =
//            createImportResolver(
//                makeExplicitImportsIndexed(imports, components.storageManager),
//                bindingTrace, aliasImportNames, packageFragment
//            )
        val importResolver = object : ImportForceResolver {
            override fun forceResolveNonDefaultImports() {
//                explicitImportResolver.forceResolveNonDefaultImports()
//                allUnderImportResolver.forceResolveNonDefaultImports()
            }

            override fun forceResolveImport(importDirective: CjImportDirective) {
                if (importDirective.isAllUnder) {
                    TODO()
//                    allUnderImportResolver.forceResolveImport(importDirective)
                } else {
//                    explicitImportResolver.forceResolveImport(importDirective)
                }
            }
        }
//        fun createImportingScope(): LazyImportScope {
//            val (defaultExplicitImportResolver, defaultAllUnderImportResolver, defaultLowPriorityImportResolver) =
//                createDefaultImportResolversForFile()
//
//            val dummyContainerDescriptor = DummyContainerDescriptor(file, packageFragment)
//
//            var scope: ImportingScope? = existingImports
//
//            val debugName = "LazyFileScope for file " + file.name
//
//            if (createDefaultImportingScopes) {
//                scope = LazyImportScope(
//                    scope, defaultAllUnderImportResolver, defaultLowPriorityImportResolver,
//                    LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
//                    "Default all under imports in $debugName (invisible classes only)"
//                )
//            }
//
//            scope = LazyImportScope(
//                scope, allUnderImportResolver, null, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
//                "All under imports in $debugName (invisible classes only)"
//            )
//
//            scope = currentPackageScope(packageView, aliasImportNames, dummyContainerDescriptor, FilteringKind.INVISIBLE_CLASSES, scope)
//
//            if (createDefaultImportingScopes) {
//                scope = LazyImportScope(
//                    scope, defaultAllUnderImportResolver, defaultLowPriorityImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
//                    "Default all under imports in $debugName (visible classes)"
//                )
//            }
//
//            scope = LazyImportScope(
//                scope, allUnderImportResolver, null, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
//                "All under imports in $debugName (visible classes)"
//            )
//
//            if (createDefaultImportingScopes) {
//                scope = LazyImportScope(
//                    scope, defaultExplicitImportResolver, null, LazyImportScope.FilteringKind.ALL,
//                    "Default explicit imports in $debugName"
//                )
//            }
//
//            scope = SubpackagesImportingScope(scope, components.moduleDescriptor, FqName.ROOT)
//
//            scope = currentPackageScope(packageView, aliasImportNames, dummyContainerDescriptor, FilteringKind.VISIBLE_CLASSES, scope)
//
//            return LazyImportScope(scope, explicitImportResolver, null, LazyImportScope.FilteringKind.ALL, "Explicit imports in $debugName")
//        }

        val lexicalScope =
            LexicalScope.Base(lazyImportingScope, topLevelDescriptorProvider.getPackageFragmentOrDiagnoseFailure(file.packageFqName, file))

        val result = FileScopes(lexicalScope, lazyImportingScope, importResolver)

    }
}

private infix fun <T> Collection<T>.concat(other: Collection<T>?) =
    if (other == null || other.isEmpty()) this else this + other
