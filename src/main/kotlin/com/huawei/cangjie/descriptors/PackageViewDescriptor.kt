package com.huawei.cangjie.descriptors

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.resolve.scopes.MemberScope

interface PackageViewDescriptor : DeclarationDescriptor {

    val fqName: FqName

    override val containingDeclaration: PackageViewDescriptor?

    val memberScope: MemberScope

    val module: ModuleDescriptor

    val fragments: List<PackageFragmentDescriptor>

    fun isEmpty(): Boolean = fragments.isEmpty()
}
