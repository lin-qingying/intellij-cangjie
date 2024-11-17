/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.metadata.decompiler

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import java.io.InputStream
@DefaultImplementation(CangJieMetadataFinder.Companion.Default::class)
interface CangJieMetadataFinder {
    /**
     * @return an [InputStream] which should be used to load the .kotlin_metadata file for class with the given [classId].
     * [classId] identifies either a real top level class, or a package part (e.g. it can be "foo/bar/_1Kt")
     */
    fun findMetadata(classId: ClassId): InputStream?

    fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>?

    /**
     * @return `true` iff this finder is able to locate the package with the given [fqName], containing .kotlin_metadata files.
     * Note that returning `true` makes [MetadataPackageFragmentProvider] construct the package fragment for the package,
     * and that fact can alter the qualified name expression resolution in the compiler front-end
     */
    fun hasMetadataPackage(fqName: FqName): Boolean

    /**
     * @return an [InputStream] which should be used to load the .kotlin_builtins file for package with the given [packageFqName].
     */
    fun findBuiltInsData(packageFqName: FqName): InputStream?


    companion object{
        object Default: CangJieMetadataFinder{
            override fun findMetadata(classId: ClassId): InputStream? {
             return null
            }

            override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? {
                return null
            }

            override fun hasMetadataPackage(fqName: FqName): Boolean {
                return false
            }

            override fun findBuiltInsData(packageFqName: FqName): InputStream? {
                return null
            }

        }
    }
}
