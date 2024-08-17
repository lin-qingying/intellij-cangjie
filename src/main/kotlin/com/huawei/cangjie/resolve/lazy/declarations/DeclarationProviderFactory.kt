package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.huawei.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider

interface DeclarationProviderFactory{

    fun diagnoseMissingPackageFragment(fqName: FqName, file: CjFile?)
    fun getPackageMemberDeclarationProvider(packageFqName:  FqName):  PackageMemberDeclarationProvider?
      fun getClassMemberDeclarationProvider(classLikeInfo: CjClassLikeInfo): ClassMemberDeclarationProvider

}
