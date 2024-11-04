package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorImpl
import com.linqingying.cangjie.resolve.scopes.MemberScope

class MutablePackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName
) :
      PackageFragmentDescriptorImpl(module, fqName) {

    override fun getMemberScope(): MemberScope {
        return MemberScope.Empty
    }
}
