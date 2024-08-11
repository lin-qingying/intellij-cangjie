package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjClassBody
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.FrontendInternals
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.frontendService
import com.huawei.cangjie.resolve.lazy.FileScopeProvider
import com.huawei.cangjie.resolve.scopes.util.parentsWithSelf
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.SmartList
import com.huawei.cangjie.utils.parentsWithSelf
import com.intellij.psi.PsiElement

@JvmOverloads
fun MemberScope.memberScopeAsImportingScope(parentScope: ImportingScope? = null): ImportingScope =
    MemberScopeToImportingScopeAdapter(parentScope, this)

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

    override fun getContributedVariables(name: Name, location: LookupLocation) =
        memberScope.getContributedVariables(name, location)

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
            is VariableDescriptor -> (scope as? ImportingScope)?.getContributedVariables(name, location)?.any { it == this }

            is FunctionDescriptor -> (scope as? ImportingScope)?.getContributedFunctions(name, location)?.any { it == this }

            else -> null
        }

        if (hasNonDeprecatedSuitableCandidate == true) return true
    }

    return false
}
