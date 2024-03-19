package com.huawei.cangjie.descriptors

import com.huawei.cangjie.name.FqName


interface ClassOrPackageFragmentDescriptor : DeclarationDescriptorNonRoot


interface PackageFragmentDescriptor : ClassOrPackageFragmentDescriptor {



    override val containingDeclaration: DeclarationDescriptor

    val fqName: FqName

//    fun getMemberScope(): MemberScope
}
