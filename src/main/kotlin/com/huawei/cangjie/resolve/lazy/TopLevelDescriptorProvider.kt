package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.declarations.LazyPackageDescriptor

interface TopLevelDescriptorProvider {
    fun getPackageFragmentOrDiagnoseFailure(fqName: FqName, from: CjFile?): LazyPackageDescriptor
    fun getPackageFragment(fqName: FqName): LazyPackageDescriptor?

    fun assertValid()

}