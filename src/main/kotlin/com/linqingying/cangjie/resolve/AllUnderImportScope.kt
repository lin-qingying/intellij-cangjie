package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.utils.Printer
import com.intellij.util.SmartList

class AllUnderImportScope private constructor(
    descriptor: DeclarationDescriptor,
    excludedImportNames: Collection<FqName>,
    private val scope1: MemberScope,
    private val scope2: MemberScope?
) : BaseImportingScope(null) {
    private val excludedNames = if (excludedImportNames.isEmpty()) { // optimization
        emptySet()
    } else {
        val fqName = DescriptorUtils.getFqNameSafe(descriptor)
        // toSet() is used here instead mapNotNullTo(hashSetOf()) because it results in not keeping empty sets as separate instances
        excludedImportNames.mapNotNull { if (it.parent() == fqName) it.shortName() else null }.toSet()
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        val nameFilterToUse = if (excludedNames.isEmpty()) { // optimization
            nameFilter
        } else {
            { it !in excludedNames && nameFilter(it) }
        }

        val noPackagesKindFilter = kindFilter.withoutKinds(DescriptorKindFilter.PACKAGES_MASK)
        val result = SmartList<DeclarationDescriptor>()
        forEachScope(scope1, scope2) { scope ->
            scope.getContributedDescriptors(noPackagesKindFilter, nameFilterToUse)
                .filterTo(result) { it !is PackageViewDescriptor }
        }
        return result
    }

    override fun computeImportedNames(): Set<Name>? {
        val names1 = scope1.computeAllNames()
        return when {
            scope2 == null -> names1
            names1 == null -> null
            else -> {
                val names2 = scope2.computeAllNames()
                when {
                    names2 == null -> null
                    names1.isEmpty() -> names2
                    else -> names1.toMutableSet().also {
                        it.addAll(names2)
                    }
                }
            }
        }
    }

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        if (name in excludedNames) return emptyList()
        val classifier1 = scope1.getContributedClassifiers(name, location)
        val classifier2 = scope2?.getContributedClassifiers(name, location)
        return classifier1 + (classifier2 ?: emptyList())
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        if (name in excludedNames) return null
        val classifier1 = scope1.getContributedClassifier(name, location)
        val classifier2 = scope2?.getContributedClassifier(name, location)
        return if (classifier1 != null && classifier2 != null) null else (classifier1 ?: classifier2)
    }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        if (name in excludedNames) return emptyList()
        val classifier1 = scope1.getExtendClass(name)
        val classifier2 = scope2?.getExtendClass(name)
        return classifier1 + (classifier2 ?: emptyList())
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        if (name in excludedNames) return emptyList()
        return flatMapScopes(scope1, scope2) { it.getContributedVariables(name, location) }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (name in excludedNames) return emptyList()
        return flatMapScopes(scope1, scope2) { it.getContributedFunctions(name, location) }
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        scope1.recordLookup(name, location)
        scope2?.recordLookup(name, location)
    }

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName)
    }


    companion object {
        fun create(descriptor: DeclarationDescriptor, excludedImportNames: Collection<FqName>): ImportingScope {
            val scope1 =
                if (descriptor is ClassDescriptor) {
                    descriptor.staticScope
                } else {
                    assert(descriptor is PackageViewDescriptor) {
                        "Must be class or package view descriptor: $descriptor"
                    }
                    (descriptor as PackageViewDescriptor).memberScope
                }

            val scope2 =
                if (descriptor is ClassDescriptor) {
                    descriptor.unsubstitutedInnerClassesScope.takeIf { it !== MemberScope.Empty }
                } else null

            return if (scope1 === MemberScope.Empty) {
                if (scope2 == null || scope2 === MemberScope.Empty) ImportingScope.Empty
                else AllUnderImportScope(descriptor, excludedImportNames, scope2, null)
            } else AllUnderImportScope(descriptor, excludedImportNames, scope1, scope2)
        }
    }
}
