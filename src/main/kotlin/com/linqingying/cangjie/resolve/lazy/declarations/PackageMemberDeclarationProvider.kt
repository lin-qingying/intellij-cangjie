package com.linqingying.cangjie.resolve.lazy.declarations

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjFile

interface PackageMemberDeclarationProvider : DeclarationProvider {
    fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName>

    fun getPackageFiles(): Collection<CjFile>

    fun containsFile(file: CjFile): Boolean
}
