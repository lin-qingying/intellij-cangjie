package com.huawei.cangjie.resolve.lazy

import com.google.common.collect.HashMultimap
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.CjImportInfo
import com.huawei.cangjie.psi.CjPsiUtil
import com.huawei.cangjie.resolve.QualifiedExpressionResolver
import com.huawei.cangjie.resolve.scopes.ImportingScope
import com.huawei.cangjie.storage.NotNullLazyValue
import com.huawei.cangjie.storage.StorageManager

interface ImportForceResolver {
    fun forceResolveNonDefaultImports()
    fun forceResolveImport(importDirective: CjImportDirective)
}class ImportResolutionComponents(
    val storageManager: StorageManager,
    val qualifiedExpressionResolver: QualifiedExpressionResolver,
    val moduleDescriptor: ModuleDescriptor,
//    val platformToKotlinClassMapper: PlatformToCangJieClassMapper,
//    val languageVersionSettings: LanguageVersionSettings,
//    val deprecationResolver: DeprecationResolver,
//    val optimizingOptions: OptimizingOptions,
)

open class IndexedImports<I : CjImportInfo>(val imports: Array<I>) {
    open fun importsForName(name: Name): Iterable<I> = imports.asIterable()
}
open class LazyImportResolver<I : CjImportInfo>(
    internal val components: ImportResolutionComponents,
    val indexedImports: IndexedImports<I>,
    val excludedImportNames: Collection<FqName>,
    val traceForImportResolve: BindingTrace,
    val packageFragment: PackageFragmentDescriptor?
){
    private val importedScopesProvider = with(components) {
        storageManager.createMemoizedFunctionWithNullableValues { directive: CjImportInfo ->
            qualifiedExpressionResolver.processImportReference(
                directive, moduleDescriptor, traceForImportResolve, excludedImportNames, packageFragment
            )
        }
    }
    fun getImportScope(directive: CjImportInfo): ImportingScope {
        return importedScopesProvider(directive) ?: ImportingScope.Empty
    }
}



class LazyImportResolverForCjImportDirective(
    components: ImportResolutionComponents,
    indexedImports: IndexedImports<CjImportDirective>,
    excludedImportNames: Collection<FqName>,
    traceForImportResolve: BindingTrace,
    packageFragment: PackageFragmentDescriptor?
) : LazyImportResolver<CjImportDirective>(
    components, indexedImports, excludedImportNames, traceForImportResolve, packageFragment
), ImportForceResolver {
    override fun forceResolveNonDefaultImports() {
        forceResolveNonDefaultImportsTask()
    }

    private fun checkResolvedImportDirective(importDirective: CjImportInfo) {
        if (importDirective !is CjImportDirective) return
        val importedReference = CjPsiUtil.getLastReference(importDirective.importedReference ?: return) ?: return
//        val importedDescriptor = traceForImportResolve.bindingContext.get(BindingContext.REFERENCE_TARGET, importedReference) ?: return

//        val aliasName = importDirective.aliasName

//        if (importedDescriptor is FunctionDescriptor && importedDescriptor.isOperator &&
//            aliasName != null && OperatorConventions.isConventionName(Name.identifier(aliasName))) {
//            traceForImportResolve.report(Errors.OPERATOR_RENAMED_ON_IMPORT.on(importedReference))
//        }
    }

    private val forceResolveNonDefaultImportsTask: NotNullLazyValue<Unit> = components.storageManager.createLazyValue {
        val explicitClassImports = HashMultimap.create<String,CjImportDirective>()
        for (importInfo in indexedImports.imports) {
            forceResolveImport(importInfo)

            val scope = getImportScope(importInfo)

            val alias = importInfo.importedName
            if (alias != null) {
                val lookupLocation = CangJieLookupLocation(importInfo)
                if (scope.getContributedClassifier(alias, lookupLocation) != null) {
                    explicitClassImports.put(alias.asString(), importInfo)
                }
            }

            checkResolvedImportDirective(importInfo)
        }
        for ((alias, import) in explicitClassImports.entries()) {
            if (alias.all { it == '_' }) {
                TODO()
//                traceForImportResolve.report(Errors.UNDERSCORE_IS_RESERVED.on(import))
            }
        }
        for (alias in explicitClassImports.keySet()) {
            val imports = explicitClassImports.get(alias)
            if (imports.size > 1) {
                imports.forEach {
                    TODO()
//                    traceForImportResolve.report(Errors.CONFLICTING_IMPORT.on(it, alias))
                }
            }
        }
    }
    override fun forceResolveImport(importDirective: CjImportDirective) {
        TODO()
//        forceResolveImportDirective(importDirective)
    }
}