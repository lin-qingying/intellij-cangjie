package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.QualifiedExpressionResolver.QualifierPart
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor

interface ResolutionScope {
    /**
     * Returns only non-deprecated classifiers.
     *
     * See [getContributedClassifierIncludeDeprecated] to get all classifiers.
     */
    fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?

    fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> = emptyList()


//    fun getFunctionClassDescriptor(parameterCount:Int):FunctionClassDescriptor?


    fun getExtendClass(name: Name): List<LazyExtendClassDescriptor>

    /**
     * Returns contributed classifier, but discriminates deprecated
     *
     * This method can return some classifier where [getContributedClassifier] haven't returned any,
     * but it should never return different one, even if it is deprecated.
     * Note that implementors are encouraged to provide non-deprecated classifier if it doesn't contradict
     * contract above.
     */
    fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? =
        getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createNonDeprecated(it) }

    fun getContributedClassifierIncludeDeprecateds(
        name: Name,
        location: LookupLocation
    ): List<DescriptorWithDeprecation<ClassifierDescriptor>>? =
        getContributedClassifiers(name, location).map { DescriptorWithDeprecation.createNonDeprecated(it) }

    fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor>
    fun getContributedPropertys(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor>

    fun getContributedFunctions(name: Name, location: LookupLocation): Collection<@JvmWildcard FunctionDescriptor>
    fun getContributedPackages(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PackageFragmentDescriptor> = emptyList()

    /**
     * All visible descriptors from current scope possibly filtered by the given name and kind filters
     * (that means that the implementation is not obliged to use the filters but may do so when it gives any performance advantage).
     */
    fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER
    ): Collection<DeclarationDescriptor>

    fun definitelyDoesNotContainName(name: Name): Boolean = false

    fun recordLookup(name: Name, location: LookupLocation) {
        getContributedFunctions(name, location)
    }


    /**
     * 通过   [com.linqingying.cangjie.name.Name] 查找文件中导入的完限定包名
     * return [com.linqingying.cangjie.name.FqName]
     */
    fun getContributedPackageFqName(name: Name/*, location: LookupLocation*/): List<FqName>? {
        return null
    }

    fun getContributedPackageQualifierPart(name: Name/*, location: LookupLocation*/): List<List<QualifierPart>>? {
        return null
    }
}
