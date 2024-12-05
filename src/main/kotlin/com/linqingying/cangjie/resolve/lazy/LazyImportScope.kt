/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.resolve.lazy

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.DescriptorVisibilityUtils.isVisibleIgnoringReceiver
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.incremental.CangJieLookupLocation
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjImportDirective
import com.linqingying.cangjie.psi.CjImportInfo
import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.psi.asQualifierPartList
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.LazyExplicitImportScope
import com.linqingying.cangjie.resolve.QualifiedExpressionResolver
import com.linqingying.cangjie.resolve.QualifiedExpressionResolver.QualifierPart
import com.linqingying.cangjie.resolve.deprecation.DeprecationResolver
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.ImportingScope
import com.linqingying.cangjie.resolve.scopes.concat
import com.linqingying.cangjie.storage.NotNullLazyValue
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.storage.getValue
import com.linqingying.cangjie.utils.Printer
import com.linqingying.cangjie.utils.flatMapToNullable
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

interface ImportForceResolver {
    fun forceResolveNonDefaultImports()
    fun forceResolveImport(importDirective: CjImportDirective)
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

    override fun importsForName(name: Name)  = nameToDirectives().get(name)
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
    val excludedImportNames: Collection<FqName>,
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
        val importedDescriptor =
            traceForImportResolve.bindingContext.get(BindingContext.REFERENCE_TARGET, importedReference) ?: return

        val aliasName = importDirective.aliasName

//        if (importedDescriptor is FunctionDescriptor && importedDescriptor.isOperator &&
//            aliasName != null && OperatorConventions.isConventionName(Name.identifier(aliasName))) {
//            traceForImportResolve.report(Errors.OPERATOR_RENAMED_ON_IMPORT.on(importedReference))
//        }
    }

    private val forceResolveNonDefaultImportsTask: NotNullLazyValue<Unit> = components.storageManager.createLazyValue {
        val explicitClassImports = HashMultimap.create<String, CjImportDirective>()
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
//        for ((alias, import) in explicitClassImports.entries()) {
//            if (alias.all { it == '_' }) {
//                TODO()
//                traceForImportResolve.report(Errors.UNDERSCORE_IS_RESERVED.on(import))
//            }
//        }
//        for (alias in explicitClassImports.keySet()) {
//            val imports = explicitClassImports.get(alias)
//            if (imports.size > 1) {
//                imports.forEach {
//                    TODO()
////                    traceForImportResolve.report(Errors.CONFLICTING_IMPORT.on(it, alias))
//                }
//            }
//        }
    }
    private val forceResolveImportDirective =
        components.storageManager.createMemoizedFunction { directive: CjImportDirective ->
            val scope = getImportScope(directive)
            if (scope is LazyExplicitImportScope) {
                val allDescriptors = scope.storeReferencesToDescriptors()
//            PlatformClassesMappedToCangJieChecker.checkPlatformClassesMappedToCangJie(
//                components.platformToCangJieClassMapper, traceForImportResolve, directive, allDescriptors
//            )
            }

            Unit
        }

    override fun forceResolveImport(importDirective: CjImportDirective) {
//        TODO()
        forceResolveImportDirective(importDirective)
    }
}


class LazyImportScope(
    override val parent: ImportingScope?,
    private val importResolver: LazyImportResolver<*>,
    private val secondaryImportResolver: LazyImportResolver<*>?,
    private val filteringKind: FilteringKind,
    private val debugName: String
) : ImportingScope {

    enum class FilteringKind {
        ALL,
        VISIBLE_CLASSES,
        INVISIBLE_CLASSES
    }

    private fun LazyImportResolver<*>.isClassifierVisible(descriptor: ClassifierDescriptor): Boolean {
        if (filteringKind == FilteringKind.ALL) return true

        // TODO: do not perform this check here because for correct work it requires corresponding PSI element
//        if (components.deprecationResolver.isHiddenInResolution(descriptor, fromImportingScope = true)) return false

        val visibility = (descriptor as DeclarationDescriptorWithVisibility).visibility
        val includeVisible = filteringKind == FilteringKind.VISIBLE_CLASSES
        if (!visibility.mustCheckInImports()) return includeVisible
        val fromDescriptor =
            if (components.languageVersionSettings.supportsFeature(LanguageFeature.ProperInternalVisibilityCheckInImportingScope)) {
                packageFragment ?: components.moduleDescriptor
            } else {
                components.moduleDescriptor
            }
        return isVisibleIgnoringReceiver(
            descriptor, fromDescriptor, components.languageVersionSettings
        ) == includeVisible
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return importResolver.getClassifier(name, location) ?: secondaryImportResolver?.getClassifier(name, location)
    }

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return importResolver.getClassifiers(name, location)

    }

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

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return importResolver.getExtendClassifier(name) + (secondaryImportResolver?.getExtendClassifier(name)
            ?: emptySet())

    }

    private fun LazyImportResolver<*>.getExtendClassifier(name: Name): List<LazyExtendClassDescriptor> {
        val imports = indexedImports.importsForName(name)

        val target = mutableListOf<LazyExtendClassDescriptor>()
        for (directive in imports) {
            val descriptors = getImportScope(directive).getExtendClass(name)
            if (descriptors.isNotEmpty()) {
                target.addAll(descriptors)
            }

        }

        return target
    }


    private fun LazyImportResolver<*>.getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        components.storageManager.compute {
            val imports = indexedImports.importsForName(name)

            var target: ClassifierDescriptor? = null
            for (directive in imports) {
                val descriptor = getImportScope(directive).getContributedClassifier(name, location)
                if (descriptor !is ClassDescriptor && descriptor !is TypeAliasDescriptor || !isClassifierVisible(
                        descriptor
                    )
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


    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
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
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        val storageManager = importResolver.components.storageManager
        if (secondaryImportResolver != null) {
            assert(storageManager === secondaryImportResolver.components.storageManager) { "Multiple storage managers are not supported" }
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
                        if (descriptor.name !in importedNames!!) {
                            result.add(descriptor)
                        }
                    }
                }
            }

            result
        }
    }

    override fun toString() = "LazyImportScope: $debugName"

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", debugName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }

    override fun definitelyDoesNotContainName(name: Name): Boolean =
        importResolver.definitelyDoesNotContainName(name) && secondaryImportResolver?.definitelyDoesNotContainName(name) != false

    override fun recordLookup(name: Name, location: LookupLocation) {
        importResolver.recordLookup(name, location)
        secondaryImportResolver?.recordLookup(name, location)
    }

    override fun getContributedPackageQualifierPart(name: Name): List<List<QualifierPart>> {
        val list = importResolver.indexedImports.importsForName(name).mapNotNull {
//    it as CjImportDirective
            it.importContent?.asQualifierPartList()
        }


        return list
    }

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

    override fun computeImportedNames(): Set<Name>? =
        importResolver.allNames?.union(secondaryImportResolver?.allNames.orEmpty())
}
