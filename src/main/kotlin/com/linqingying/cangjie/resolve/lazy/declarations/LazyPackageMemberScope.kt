package com.linqingying.cangjie.resolve.lazy.declarations

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.incremental.record
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.resolve.lazy.ResolveSession
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.utils.Printer


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

    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>) {
//        c.syntheticResolveExtension.generateSyntheticClasses(thisDescriptor, name, c, declarationProvider, result)

    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.lookupTracker.record(location, thisDescriptor, name)
    }
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return computeDescriptorsFromDeclaredElements(kindFilter, nameFilter, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS)

    }

    override fun getNonDeclaredMacros(name: Name, result: MutableSet<MacroDescriptor>) {

    }
    override fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope=
        getScopeForMemberDeclarationResolution(declaration)


    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        // No extra properties
    }

    override fun getNonDeclaredVariables(name: Name, result: MutableSet<VariableDescriptor>) {

    }
//    override fun getContributedDescriptors(
//        kindFilter: DescriptorKindFilter,
//        nameFilter: (Name) -> Boolean
//    ): Collection<DeclarationDescriptor> {
//        return computeDescriptorsFromDeclaredElements(kindFilter, nameFilter, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS)
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
