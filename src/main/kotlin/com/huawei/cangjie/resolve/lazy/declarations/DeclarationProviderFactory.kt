package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile

interface DeclarationProviderFactory{

    fun diagnoseMissingPackageFragment(fqName: FqName, file: CjFile?)
    fun getPackageMemberDeclarationProvider(packageFqName:  FqName):  PackageMemberDeclarationProvider?

}