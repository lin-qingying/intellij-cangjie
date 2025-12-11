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

package org.cangnova.cangjie.resolve.lazy.declarations

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.lazy.data.CjClassLikeInfo
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.PsiBasedClassMemberDeclarationProvider
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager


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

    /*package*/
    fun getAllDeclaredSubPackagesOf(parent: FqName): Collection<FqName> {
        return index.invoke().declaredPackages.filter<FqName> { fqName: FqName -> !fqName.isRoot && fqName.parent() == parent }
    }

    companion object {
        private fun addMeAndParentPackages(
            index: Index,
            name: FqName
        ) {
            index.declaredPackages.add(name)
            if (!name.isRoot) {
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
        if (packageExists(name)) {
            return FileBasedPackageMemberDeclarationProvider(
                storageManager, name, this, index.invoke().filesByPackage[name]
            )
        }

        return null
    }

    override fun getClassMemberDeclarationProvider(classLikeInfo: CjClassLikeInfo): ClassMemberDeclarationProvider {
        check(index.invoke().filesByPackage.containsKey(classLikeInfo.containingPackageFqName)) { "This factory doesn't know about this class: $classLikeInfo" }

        return PsiBasedClassMemberDeclarationProvider(
            storageManager,
            classLikeInfo
        )
    }
}
