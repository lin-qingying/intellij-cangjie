package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.resolve.scopes.ChainedMemberScope
import com.huawei.cangjie.resolve.scopes.LazyScopeAdapter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.storage.getValue

open class LazyPackageViewDescriptorImpl(
    override val module: ModuleDescriptorImpl,
    override val fqName: FqName,
    storageManager: StorageManager
) : DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
        visitor.visitPackageViewDescriptor(this, data!!)


    protected val empty: Boolean by storageManager.createLazyValue {
        module.packageFragmentProvider.isEmpty(fqName)
    }

    override fun isEmpty(): Boolean = empty

    override val containingDeclaration: PackageViewDescriptor?
        get() = if (fqName.isRoot) null else module.getPackage(fqName.parent())
    override val memberScope: MemberScope = LazyScopeAdapter(storageManager) {
        if (isEmpty()) {
            MemberScope.Empty
        } else {
            // Packages from SubpackagesScope are got via getContributedDescriptors(DescriptorKindFilter.PACKAGES, MemberScope.ALL_NAME_FILTER)
            val scopes = fragments.map { it.getMemberScope() } + SubpackagesScope(module, fqName)
            ChainedMemberScope.create("package view scope for $fqName in ${module.name}", scopes)
        }
    }
    override val fragments: List<PackageFragmentDescriptor> by storageManager.createLazyValue {
        module.packageFragmentProvider.packageFragments(fqName)
    }

}




