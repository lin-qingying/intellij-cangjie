package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.analyzer.CangJieModuleInfo
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.FrontendInternals
import com.huawei.cangjie.ide.base.projectStructure.CangJieSourceFilterScope
import com.huawei.cangjie.ide.projectStructure.CangJieResolveScopeEnlarger
import com.huawei.cangjie.ide.projectStructure.moduleInfo
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjClassBody
import com.huawei.cangjie.psi.CjCodeFragment
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.QualifiedExpressionResolver.QualifierPart
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.frontendService
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.lazy.FileScopeProvider
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.util.parentsWithSelf
import com.huawei.cangjie.types.error.ErrorClassDescriptor
import com.huawei.cangjie.types.error.ErrorEntity
import com.huawei.cangjie.utils.Printer
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList

@JvmOverloads
fun MemberScope.memberScopeAsImportingScope(parentScope: ImportingScope? = null): ImportingScope =
    MemberScopeToImportingScopeAdapter(parentScope, this)

fun HierarchicalScope.findPackage(name: Name): PackageViewDescriptor? =
    findFirstFromImportingScopes { it.getContributedPackage(name) }

inline fun <T : Any> HierarchicalScope.findFirstFromImportingScopes(fetch: (ImportingScope) -> T?): T? {
    return findFirstFromMeAndParent { if (it is ImportingScope) fetch(it) else null }
}

fun CjElement.getResolutionScope(): LexicalScope {
    val resolutionFacade = getResolutionFacade()
    val context = resolutionFacade.analyze(this, BodyResolveMode.FULL)
    return getResolutionScope(context, resolutionFacade)
}

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
            result!!.add(element)
        }
    }
    return result ?: emptyList()
}

/**
 * Adds receivers to the list in order of locality, so that the closest (the most local) receiver goes first
 */
fun LexicalScope.getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = collectFromMeAndParent {
    if (it is LexicalScope) listOfNotNull(it.implicitReceiver) + it.contextReceiversGroup else null
}.flatten()

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

private class MemberScopeToImportingScopeAdapter(override val parent: ImportingScope?, val memberScope: MemberScope) :
    ImportingScope {
    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ) = memberScope.getContributedDescriptors(kindFilter, nameFilter)

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        memberScope.getContributedClassifier(name, location)

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> =
        memberScope.getExtendClass(name)

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        memberScope.getContributedVariables(name, location)

    override fun getContributedPropertys(name: Name, location: LookupLocation) =
        memberScope.getContributedPropertys(name, location)

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        memberScope.getContributedFunctions(name, location)

    override fun equals(other: Any?) = other is MemberScopeToImportingScopeAdapter && other.memberScope == memberScope

    override fun hashCode() = memberScope.hashCode()

    override fun toString() = "${this::class.java.simpleName} for $memberScope"

    override fun computeImportedNames() = memberScope.computeAllNames()

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName)
        p.pushIndent()

        memberScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}

fun HierarchicalScope.takeSnapshot(): HierarchicalScope = if (this is LexicalWritableScope) takeSnapshot() else this
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

fun listOfNonEmptyScopes(scopes: Iterable<MemberScope?>): SmartList<MemberScope> =
    scopes.filterTo(SmartList<MemberScope>()) { it != null && it !== MemberScope.Empty }

fun listOfNonEmptyScopes(vararg scopes: MemberScope?): SmartList<MemberScope> =
    scopes.filterTo(SmartList<MemberScope>()) { it != null && it !== MemberScope.Empty }

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

inline fun <Scope, T : ClassifierDescriptor> getFirstClassifierDiscriminateHeaders(
    scopes: Array<Scope>,
    callback: (Scope) -> T?
): T? {
    // NOTE: This is performance-sensitive; please don't replace with map().firstOrNull()
    var result: T? = null
    for (scope in scopes) {
        val newResult = callback(scope)
        if (newResult != null) {
            if (newResult is ClassifierDescriptorWithTypeParameters /*&& newResult.isExpect*/) {
                if (result == null) result = newResult
            }
            // this class is Impl or usual class
            else {
                return newResult
            }
        }
    }
    return result
}

/**
 * Concatenates the contents of this collection with the given collection, avoiding allocations if possible.
 * Can modify `this` if it is a mutable collection.
 */
fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (collection.isEmpty()) {
        return this
    }
    if (this == null) {
        return collection
    }
    if (this is LinkedHashSet) {
        addAll(collection)
        return this
    }

    val result = LinkedHashSet(this)
    result.addAll(collection)
    return result
}

fun PsiElement.getResolutionScope(
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade/*TODO: get rid of this parameter*/
): LexicalScope = getResolutionScope(bindingContext) ?: when (containingFile) {
    is CjFile -> resolutionFacade.getFileResolutionScope(containingFile as CjFile)
    else -> error("Not in CjFile")
}

val CjFile.scope: LexicalScope
    get() {
        return runReadAction {
            this.getResolutionScope()
        }
    }


@OptIn(FrontendInternals::class)
fun ResolutionFacade.getFileResolutionScope(file: CjFile): LexicalScope {
    return frontendService<FileScopeProvider>().getFileResolutionScope(file)
}

fun PsiElement.getResolutionScope(bindingContext: BindingContext): LexicalScope? {
    for (parent in parentsWithSelf) {
        if (parent is CjElement) {
            val scope = bindingContext[BindingContext.LEXICAL_SCOPE, parent]
            if (scope != null) return scope
        }

        if (parent is CjClassBody) {
            val classDescriptor =
                bindingContext[BindingContext.CLASS, parent.getParent()] as? ClassDescriptorWithResolutionScopes
            if (classDescriptor != null) {
                return classDescriptor.scopeForMemberDeclarationResolution
            }
        }
        if (parent is CjFile) {
            break
        }
    }

    return null
}

fun DeclarationDescriptor.canBeResolvedWithoutDeprecation(
    scopeForResolution: HierarchicalScope,
    location: LookupLocation
): Boolean {
    for (scope in scopeForResolution.parentsWithSelf) {
        val hasNonDeprecatedSuitableCandidate = when (this) {
            // Looking for classifier: fair check via special method in ResolutionScope
            is ClassifierDescriptor -> scope.getContributedClassifierIncludeDeprecated(name, location)
                ?.let { it.descriptor == this && !it.isDeprecated }

            // Looking for member: heuristically check only one case, when another descriptor visible through explicit import
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

fun HierarchicalScope.findClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
    findFirstFromMeAndParent { it.getContributedClassifier(name, location) }

fun HierarchicalScope.findFirstClassifierWithDeprecationStatus(
    name: Name,
    location: LookupLocation
): DescriptorWithDeprecation<ClassifierDescriptor>? {
    return findFirstFromMeAndParent { it.getContributedClassifierIncludeDeprecated(name, location) }
}

@Deprecated("Use getContributedProperties instead")
fun LexicalScope.findLocalVariable(name: Name): VariableDescriptor? {
    return findFirstFromMeAndParent { originalScope ->
        // Unpacking LexicalScopeWrapper may be important to check that it is not ImportingScope
        val possiblyUnpackedScope = when (originalScope) {
            is LexicalScopeWrapper -> originalScope.delegate
            else -> originalScope
        }

        when {
            possiblyUnpackedScope !is ImportingScope && possiblyUnpackedScope !is LexicalChainedScope ->
                possiblyUnpackedScope.getContributedVariables(
                    name,
                    NoLookupLocation.MATCH_GET_LOCAL_VARIABLE
                ).singleOrNull() /* todo check this*/

            else -> null
        }
    }
}

fun HierarchicalScope?.getExtendClasss(name: Name, location: LookupLocation): List<LazyExtendClassDescriptor> {

    return this?.getListFromMeAndParent { it.getExtendClass(name) } ?:  emptyList()
}

fun HierarchicalScope.findPackageFqNames(
    name: Name,
//    location: LookupLocation
): List<FqName>? {
    return findFirstFromMeAndParent { it.getContributedPackageFqName(name/*, location*/) }
}

fun HierarchicalScope.findPackageQualifierParts(
    name: Name,
//    location: LookupLocation
): List<List<QualifierPart>>? {
    return findFirstFromMeAndParent { it.getContributedPackageQualifierPart(name/*, location*/) }
}

fun LexicalScope.addImportingScope(importScope: ImportingScope): LexicalScope = addImportingScopes(listOf(importScope))
fun LexicalScope.addImportingScopes(importScopes: List<ImportingScope>): LexicalScope {
    val lastLexicalScope = parentsWithSelf.last { it is LexicalScope }
    val firstImporting = lastLexicalScope.parent as ImportingScope
    val newFirstImporting = chainImportingScopes(importScopes, firstImporting)
    return replaceImportingScopes(newFirstImporting)
}

fun LexicalScope.replaceImportingScopes(importingScopeChain: ImportingScope?): LexicalScope {
    val newImportingScopeChain = importingScopeChain ?: ImportingScope.Empty
    if (this is LexicalScopeWrapper) {
        return LexicalScopeWrapper(this.delegate, newImportingScopeChain)
    }
    return LexicalScopeWrapper(this, newImportingScopeChain)
}


private class LexicalScopeWrapper(
    val delegate: LexicalScope,
    private val newImportingScopeChain: ImportingScope
) : LexicalScope by delegate {
    init {
        assert(delegate !is LexicalScopeWrapper) {
            "Do not wrap again to avoid performance issues"
        }
    }

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

fun chainImportingScopes(scopes: List<ImportingScope>, tail: ImportingScope? = null): ImportingScope? {
    return scopes.asReversed()
        .fold(tail) { current, scope ->
            assert(scope.parent == null)
            scope.withParent(current)
        }
}

fun ImportingScope.withParent(newParent: ImportingScope?): ImportingScope {
    return object : ImportingScope by this {
        override val parent: ImportingScope?
            get() = newParent
    }
}

fun Project.projectScope(): GlobalSearchScope = GlobalSearchScope.projectScope(this)

object ScopeUtils {
    @JvmStatic
    fun makeScopeForPropertyInitializer(
        propertyHeader: LexicalScope,
        propertyDescriptor: PropertyDescriptor
    ): LexicalScope {
        return makeScopeForVariableBaseInitializer(propertyHeader, propertyDescriptor)

    }

    private fun makeScopeForVariableBaseInitializer(
        variableHeader: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope {
        return LexicalScopeImpl(
            variableHeader,
            variableDescriptor,
            false,
            null,
            emptyList(),
            LexicalScopeKind.VARIABLE_INITIALIZER_OR_DELEGATE
        )
    }

    @JvmStatic
    fun makeScopeForVariableInitializer(
        variableHeader: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope {
        return makeScopeForVariableBaseInitializer(variableHeader, variableDescriptor)
    }

//    @JvmStatic
//    fun makeScopeForVariableInitializer(
//        variableHeader: LexicalScope,
//        variableDescriptor: VariableCallableDescriptor
//    ): LexicalScope {
//        return makeScopeForVariableBaseInitializer(variableHeader, variableDescriptor)
//    }

    private fun makeScopeForVariableBaseHeader(
        parent: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope {
        return LexicalScopeImpl(
            parent,
            variableDescriptor,
            false,
            null,
            emptyList(),
            LexicalScopeKind.PROPERTY_HEADER,  // redeclaration on type parameters should be reported early, see: DescriptorResolver.resolvePropertyDescriptor()
            LocalRedeclarationChecker.DO_NOTHING
        ) {
            for (typeParameterDescriptor in variableDescriptor.typeParameters) {
                addClassifierDescriptor(typeParameterDescriptor)
            }

        }
    }

    @JvmStatic
    fun makeScopeForVariableHeader(
        parent: LexicalScope,
        variableDescriptor: VariableDescriptor
    ): LexicalScope {
        return makeScopeForVariableBaseHeader(parent, variableDescriptor)
    }

    @JvmStatic
    fun makeScopeForPropertyHeader(
        parent: LexicalScope,
        propertyDescriptor: PropertyDescriptor
    ): LexicalScope {
        return makeScopeForVariableBaseHeader(parent, propertyDescriptor)
    }

}

class ErrorLexicalScope : LexicalScope {
    override val parent: HierarchicalScope = object : HierarchicalScope {
        override val parent: HierarchicalScope? = null

        override fun printStructure(p: Printer) {
            p.print(ErrorEntity.PARENT_OF_ERROR_SCOPE.debugText)
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
        override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> = emptyList()

        override fun getContributedVariables(
            name: Name,
            location: LookupLocation
        ): Collection<@JvmWildcard VariableDescriptor> =
            emptySet()

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
            emptySet()

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
            emptySet()

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> = emptySet()

//        override fun getContributedPackageFqName(name: Name, location: LookupLocation): List<FqName> = emptyList()
    }

    override fun printStructure(p: Printer) {
        p.print(ErrorEntity.ERROR_SCOPE.debugText)
    }

    override val ownerDescriptor: DeclarationDescriptor =
        ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format("unknown")))
    override val isOwnerDescriptorAccessibleByLabel: Boolean = false
    override val implicitReceiver: ReceiverParameterDescriptor? = null
    override val contextReceiversGroup: List<ReceiverParameterDescriptor> = emptyList()
    override val kind: LexicalScopeKind = LexicalScopeKind.THROWING
//    override fun getContributedPackageFqName(name: Name, location: LookupLocation): List<FqName> {
//        return mutableListOf()
//    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null
    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> = emptyList()

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        emptySet()

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        emptySet()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
        emptySet()

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = emptySet()
}

inline fun <Scope> forEachScope(scope1: Scope?, scope2: Scope?, action: (Scope) -> Unit) {
    if (scope1 != null) action(scope1)
    if (scope2 != null) action(scope2)
}

inline fun <Scope, R> flatMapScopes(
    scope1: Scope?,
    scope2: Scope?,
    transform: (Scope) -> Collection<R>
): Collection<R> {
    val results1 = if (scope1 != null) transform(scope1) else emptyList()
    if (scope2 == null) return results1
    else {
        val results2 = transform(scope2)
        if (results1.isEmpty()) return results2
        else return results1.toMutableList().also {
            it.addAll(results2)
        }
    }
}


fun getResolveScope(file: CjFile): GlobalSearchScope {
    if (file is CjCodeFragment) {

        val contextScope = file.getContextContainingFile()?.resolveScope
        if (contextScope != null) {
            return when (file.moduleInfo) {
//                is SourceForBinaryModuleInfo -> CangJieSourceFilterScope.libraryClasses(contextScope, file.project)
                else -> CangJieSourceFilterScope.projectSourcesAndLibraryClasses(contextScope, file.project)
            }
        }
    }

    return when (file.moduleInfo) {
        is CangJieModuleInfo -> {
            val projectScope = CangJieSourceFilterScope.projectFiles(file.resolveScope, file.project)
            CangJieResolveScopeEnlarger.enlargeScope(projectScope, file)
        }

        else -> GlobalSearchScope.EMPTY_SCOPE
    }
}
