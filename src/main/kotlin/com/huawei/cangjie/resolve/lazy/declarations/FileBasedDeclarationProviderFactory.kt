package com.huawei.cangjie.resolve.lazy.declarations

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.storage.NotNullLazyValue
import com.huawei.cangjie.storage.StorageManager


class FileBasedDeclarationProviderFactory(
    val storageManager: StorageManager,
    files: Collection<CjFile>
) :
    AbstractDeclarationProviderFactory(storageManager) {
    private class Index {
        val filesByPackage: Multimap<FqName, CjFile> =
            LinkedHashMultimap.create<FqName, CjFile>()
        val declaredPackages: MutableSet<FqName> =
            HashSet<FqName>()
    }

    companion object {
        private fun addMeAndParentPackages(
            index: Index,
            name:  FqName
        ) {
            index.declaredPackages.add(name)
            if (!name.isRoot()) {
                addMeAndParentPackages(index, name.parent())
            }
        }
        private fun computeFilesByPackage(files: Collection<CjFile>): Index {
            val index: Index =
                Index()
            for (file in files) {
                val packageFqName: FqName = file.packageFqName
                addMeAndParentPackages(
                    index,
                    packageFqName
                )
                index.filesByPackage.put(packageFqName, file)
            }
            return index
        }

    }

    private val index: NotNullLazyValue<Index> = storageManager.createLazyValue {
        computeFilesByPackage(
            files
        )
    }

    override fun packageExists(packageFqName: FqName): Boolean {

        return index.invoke().declaredPackages.contains(packageFqName)

    }

    override fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider? {
        TODO("Not yet implemented")
    }
}