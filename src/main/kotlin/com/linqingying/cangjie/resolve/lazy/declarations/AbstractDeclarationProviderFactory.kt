package com.linqingying.cangjie.resolve.lazy.declarations

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.storage.MemoizedFunctionToNullable
import com.linqingying.cangjie.storage.StorageManager

abstract class AbstractDeclarationProviderFactory(

    storageManager: StorageManager
) :
    DeclarationProviderFactory {

    private val packageDeclarationProviders: MemoizedFunctionToNullable<FqName, PackageMemberDeclarationProvider>

    init {
        this.packageDeclarationProviders =
            storageManager.createMemoizedFunctionWithNullableValues { name: FqName ->
                this.createPackageMemberDeclarationProvider(
                    name
                )
            }
    }

    abstract fun packageExists(fqName:  FqName): Boolean

    override fun getPackageMemberDeclarationProvider(packageFqName:  FqName):  PackageMemberDeclarationProvider? {
        if (!packageExists(packageFqName)) return null
        return packageDeclarationProviders.invoke(packageFqName)
    }
    protected abstract fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider?
    override fun diagnoseMissingPackageFragment(fqName:  FqName, file: CjFile?) {
        var message = "Cannot find package fragment $fqName"
        if (file != null) {
            message += """
            
            vFile = ${file.virtualFilePath}, file package = '${file.packageFqName}'
            """.trimIndent()
        }
        throw IllegalStateException(message)
    }
}
