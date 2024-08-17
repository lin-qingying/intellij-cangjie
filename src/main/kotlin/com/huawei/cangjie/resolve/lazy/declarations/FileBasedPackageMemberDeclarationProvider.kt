package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.descriptors.AbstractPsiBasedDeclarationProvider
import com.huawei.cangjie.storage.StorageManager


class FileBasedPackageMemberDeclarationProvider(
    storageManager: StorageManager,
    private val fqName: FqName,
    private val factory: FileBasedDeclarationProviderFactory,
    private val packageFiles: Collection<CjFile>
) : AbstractPsiBasedDeclarationProvider(storageManager), PackageMemberDeclarationProvider {

    private val allDeclaredSubPackages = storageManager.createLazyValue<Collection<FqName>> {
        factory.getAllDeclaredSubPackagesOf(fqName)
    }

    override fun doCreateIndex(index: AbstractPsiBasedDeclarationProvider.Index) {
        for (file in packageFiles) {
            for (declaration in file.declarations) {
                assert(fqName == file.packageFqName) { "Files declaration utils contains file with invalid package" }
                index.putToIndex(declaration)
            }
        }
    }

    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName> = allDeclaredSubPackages()

    override fun getPackageFiles() = packageFiles

    override fun containsFile(file: CjFile) = file in packageFiles

    override fun toString() = "Declarations for package $fqName with files ${packageFiles.map { it.name }} " +
            "with declarations inside ${packageFiles.flatMap { it.declarations }.map { it.name ?: "???" }}"
}
