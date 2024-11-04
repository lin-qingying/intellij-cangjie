package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.resolve.scopes.MemberScope


interface ClassOrPackageFragmentDescriptor : DeclarationDescriptorNonRoot


interface PackageFragmentDescriptor : PackageData, ClassOrPackageFragmentDescriptor {

    fun getMemberScope(): MemberScope


    override val containingDeclaration: ModuleDescriptor


}
