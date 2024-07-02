package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile

interface PackageMemberDeclarationProvider : DeclarationProvider {
    fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName>

    fun getPackageFiles(): Collection<CjFile>

    fun containsFile(file: CjFile): Boolean
}
