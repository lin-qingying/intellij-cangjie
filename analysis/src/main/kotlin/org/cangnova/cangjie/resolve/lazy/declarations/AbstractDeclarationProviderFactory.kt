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

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.storage.MemoizedFunctionToNullable
import org.cangnova.cangjie.storage.StorageManager

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

    abstract fun packageExists(fqName: FqName): Boolean

    override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
        if (!packageExists(packageFqName)) return null
        return packageDeclarationProviders.invoke(packageFqName)
    }

    protected abstract fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider?
    override fun diagnoseMissingPackageFragment(fqName: FqName, file: CjFile?) {
        var message = "Cannot find package fragment $fqName"
        if (file != null) {
            message += """
            
            vFile = ${file.virtualFilePath}, file package = '${file.packageFqName}'
            """.trimIndent()
        }
        throw IllegalStateException(message)
    }
}
