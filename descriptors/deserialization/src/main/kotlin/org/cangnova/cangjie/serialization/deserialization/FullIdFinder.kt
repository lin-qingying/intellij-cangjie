/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.withResolutionAnchor
import org.cangnova.cangjie.metadata.PackageIndex
import org.cangnova.cangjie.metadata.PackageIndex.CURRENT_PKG_INDEX
import org.cangnova.cangjie.metadata.PackageIndex.INVALID_PACKAGE_INDEX
import org.cangnova.cangjie.metadata.model.fb.FbFullId
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.metadata.model.wrapper.toClassDeclWrapper
import org.cangnova.cangjie.name.FqName

/**
 * FullIdFinder 接口用于通过完整ID查找声明描述符。
 * 完整ID由包ID、声明标识符和索引组成，用于在反序列化过程中定位特定的声明。
 */
interface FullIdFinder {

    fun findClassifierDescriptorByFullId(fullId: FbFullId): ClassifierDescriptor?
    fun findTypeAliasDescriptorByFullId(fullId: FbFullId): TypeAliasDescriptor?

}

class FullIdFinderImpl(
    val moduleDescriptor: ModuleDescriptor,
    val `package`: PackageWrapper,
    val context: DeserializationContext
) : FullIdFinder {


    override fun findClassifierDescriptorByFullId(fullId: FbFullId): ClassifierDescriptor? =
        when (PackageIndex.fromValue(fullId.pkgId)) {
            INVALID_PACKAGE_INDEX -> null
            CURRENT_PKG_INDEX -> {
                getClassifierByIndex(`package`.packageName, fullId.index) ?:
//                尝试在本包查找
                context.declDeserializer.loadClass(
                    context.declTable[fullId.index]
                        .let {
                            it.toClassDeclWrapper(`package`.declTable, `package`.typeTable) ?: return null

                        }

                )
            }

            null -> {


//                引用其他包的声明
                val packageFqName = `package`.importPackageFqNames[fullId.pkgId]

                if (fullId.index > 0) {
                    getClassifierByIndex(packageFqName, fullId.index)

                } else {
                    if (fullId.decl.isEmpty()) null else
                        getClassifierByExportId(packageFqName, fullId.decl)

                }
            }


            else -> null
        }


    override fun findTypeAliasDescriptorByFullId(fullId: FbFullId): TypeAliasDescriptor? {
        return findClassifierDescriptorByFullId(fullId) as? TypeAliasDescriptor
    }

    private fun getClassifierByExportId(packageName: FqName, exportId: String): ClassifierDescriptor? =
        moduleDescriptor.withResolutionAnchor {
            val packageViewDescriptor = moduleDescriptor.getPackage(packageName)
            val decl = packageViewDescriptor.memberScope.getContributedClassifierByExportId(
                exportId
            )

            return@withResolutionAnchor decl
        }

    private fun getClassifierByIndex(packageName: FqName, index: Int): ClassifierDescriptor? =
        moduleDescriptor.withResolutionAnchor {
            val packageViewDescriptor = moduleDescriptor.getPackage(packageName)
            return@withResolutionAnchor packageViewDescriptor.memberScope.getContributedClassifierByIndex(
                index
            )
        }

}