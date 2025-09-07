package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.withResolutionAnchor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.metadata.PackageIndex
import org.cangnova.cangjie.metadata.PackageIndex.*
import org.cangnova.cangjie.metadata.model.fb.FbFullId
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.FqName

/**
 * FullIdFinder 接口用于通过完整ID查找声明描述符。
 * 完整ID由包ID、声明标识符和索引组成，用于在反序列化过程中定位特定的声明。
 */
interface FullIdFinder {

    fun findClassifierDescriptorByFullId(fullId: FbFullId): ClassifierDescriptor?
    fun findTypeAliasDescriptorByFullId(fullId: FbFullId): TypeAliasDescriptor?

}

class FullIdFinderImpl(val moduleDescriptor: ModuleDescriptor, val `package`: PackageWrapper) : FullIdFinder {


    override fun findClassifierDescriptorByFullId(fullId: FbFullId): ClassifierDescriptor? =
        when (PackageIndex.fromValue(fullId.pkgId)) {
            INVALID_PACKAGE_INDEX -> TODO()
            CURRENT_PKG_INDEX -> {
                getClassifierByIndex(`package`.packageName, fullId.index)
            }

            null -> if (fullId.pkgId > 0) {

//                引用其他包的声明
                val packageFqName = `package`.importPackageFqNames[fullId.pkgId]

                if(fullId.index > 0){
                    getClassifierByIndex(packageFqName, fullId.index)

                }else{
                    getClassifierByExportId(packageFqName, fullId.decl)

                }
            } else null

            else -> null
        }


    override fun findTypeAliasDescriptorByFullId(fullId: FbFullId): TypeAliasDescriptor? {
        return findClassifierDescriptorByFullId(fullId) as? TypeAliasDescriptor
    }
    private fun getClassifierByExportId(packageName: FqName, exportId: String): ClassifierDescriptor? =
        moduleDescriptor.withResolutionAnchor {
            val packageViewDescriptor = moduleDescriptor.getPackage(packageName)
            return@withResolutionAnchor packageViewDescriptor.memberScope.getContributedClassifierByExportId(
                exportId

            )
        }
    private fun getClassifierByIndex(packageName: FqName, index: Int): ClassifierDescriptor? =
        moduleDescriptor.withResolutionAnchor {
            val packageViewDescriptor = moduleDescriptor.getPackage(packageName)
            return@withResolutionAnchor packageViewDescriptor.memberScope.getContributedClassifierByIndex(
                index

            )
        }

}