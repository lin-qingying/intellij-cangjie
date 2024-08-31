package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.declarations.LazyPackageDescriptor

interface TopLevelDescriptorProvider {
    fun getPackageFragmentOrDiagnoseFailure(fqName: FqName, from: CjFile?): LazyPackageDescriptor
    fun getPackageFragment(fqName: FqName): LazyPackageDescriptor?
    fun getTopLevelClassifierDescriptors(fqName: FqName, location: LookupLocation): Collection<ClassifierDescriptor>

    fun assertValid()

}
