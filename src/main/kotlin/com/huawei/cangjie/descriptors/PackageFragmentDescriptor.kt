package com.huawei.cangjie.descriptors

import com.huawei.cangjie.resolve.scopes.MemberScope


interface ClassOrPackageFragmentDescriptor : DeclarationDescriptorNonRoot


interface PackageFragmentDescriptor : PackageData, ClassOrPackageFragmentDescriptor {

    fun getMemberScope(): MemberScope


    override val containingDeclaration: ModuleDescriptor


}
