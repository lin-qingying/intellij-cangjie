package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentDescriptor
import com.huawei.cangjie.descriptors.PropertyDescriptor
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.incremental.record
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.resolve.lazy.ResolveSession
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.utils.Printer


class LazyPackageMemberScope(
    private val resolveSession: ResolveSession,
    declarationProvider: PackageMemberDeclarationProvider,
    thisPackage: PackageFragmentDescriptor
) : AbstractLazyMemberScope<PackageFragmentDescriptor, PackageMemberDeclarationProvider>(
    resolveSession,
    declarationProvider,
    thisPackage,
    resolveSession.trace
) {
    override fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration) =
        resolveSession.fileScopeProvider!!.getFileResolutionScope(declaration.getContainingCjFile())

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {

    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.lookupTracker?.record(location, thisDescriptor, name)
    }
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return computeDescriptorsFromDeclaredElements(kindFilter, nameFilter, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)

    }


    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        // No extra properties
    }

//    override fun getContributedDescriptors(
//        kindFilter: DescriptorKindFilter,
//        nameFilter: (Name) -> Boolean
//    ): Collection<DeclarationDescriptor> {
//        return computeDescriptorsFromDeclaredElements(kindFilter, nameFilter, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
//    }
//
//    override fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration) =
//        resolveSession.fileScopeProvider.getFileResolutionScope(declaration.getContainingCjFile())
//
//    override fun getScopeForInitializerResolution(declaration: CjDeclaration) =
//        getScopeForMemberDeclarationResolution(declaration)
//
//    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>) {
//        c.syntheticResolveExtension.generateSyntheticClasses(thisDescriptor, name, c, declarationProvider, result)
//    }
//
//    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {
//        // No extra functions
//    }
//
//    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
//        // No extra properties
//    }
//
//    override fun recordLookup(name: Name, location: LookupLocation) {
//        c.lookupTracker.record(location, thisDescriptor, name)
//    }
//
//    override fun getClassifierNames(): Set<Name>? = declarationProvider.getDeclarationNames()
//    override fun getFunctionNames() = declarationProvider.getDeclarationNames()
//    override fun getVariableNames() = declarationProvider.getDeclarationNames()

    // Do not add details here, they may compromise the laziness during debugging
    override fun toString() = "lazy scope for package " + thisDescriptor.name
}
