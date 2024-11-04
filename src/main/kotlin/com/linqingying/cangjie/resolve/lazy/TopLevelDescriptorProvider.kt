package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.lazy.declarations.LazyPackageDescriptor

interface TopLevelDescriptorProvider {
    fun getPackageFragmentOrDiagnoseFailure(fqName: FqName, from: CjFile?): LazyPackageDescriptor
    fun getPackageFragment(fqName: FqName): LazyPackageDescriptor?
    fun getTopLevelClassifierDescriptors(fqName: FqName, location: LookupLocation): Collection<ClassifierDescriptor>

    fun assertValid()

}
