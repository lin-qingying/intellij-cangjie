package com.huawei.cangjie.descriptors

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.resolve.scopes.MemberScope


interface ClassOrPackageFragmentDescriptor : DeclarationDescriptorNonRoot


interface PackageFragmentDescriptor : ClassOrPackageFragmentDescriptor {

    fun getMemberScope(): MemberScope


    override val containingDeclaration: DeclarationDescriptor

    val fqName: FqName


}
