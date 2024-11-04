package com.linqingying.cangjie.resolve.lazy.declarations

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.linqingying.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider

interface DeclarationProviderFactory {

    fun diagnoseMissingPackageFragment(fqName: FqName, file: CjFile?)
    fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider?
    fun getClassMemberDeclarationProvider(classLikeInfo: CjClassLikeInfo): ClassMemberDeclarationProvider

}
