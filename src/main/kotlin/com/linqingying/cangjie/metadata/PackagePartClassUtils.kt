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

package com.linqingying.cangjie.metadata

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.NameUtils
import com.linqingying.cangjie.psi.CjFile
import org.jetbrains.annotations.TestOnly


object PackagePartClassUtils {
    @JvmStatic
    fun getPathHashCode(file: VirtualFile): Int = file.path.lowercase().hashCode()

    private const val PART_CLASS_NAME_SUFFIX = "Cj"

    @JvmStatic
    private fun decapitalizeAsJavaClassName(str: String): String =
    // NB use Locale.ENGLISH so that build is locale-independent.
        // See Javadoc on java.lang.String.toUpperCase() for more details.
        when {
            Character.isJavaIdentifierStart(str[0]) -> str.substring(0, 1).lowercase() + str.substring(1)
            str[0] == '_' -> str.substring(1)
            else -> str
        }

    @TestOnly
    @JvmStatic
    fun getDefaultPartFqName(facadeClassFqName: FqName, file: VirtualFile): FqName =
        getPackagePartFqName(facadeClassFqName.parent(), file.name)

    @JvmStatic
    fun getPackagePartFqName(packageFqName: FqName, fileName: String): FqName {
        val partClassName = getFilePartShortName(fileName)
        return packageFqName.child(Name.identifier(partClassName))
    }

    @JvmStatic
    fun getFilesWithCallables(files: Collection<CjFile>): List<CjFile> =
        files.filter { it.hasTopLevelCallables() }

    @JvmStatic
    fun getFilePartShortName(fileName: String): String =
        NameUtils.getPackagePartClassNamePrefix(FileUtil.getNameWithoutExtension(fileName)) + PART_CLASS_NAME_SUFFIX

    @JvmStatic
    fun getFileNameByFacadeName(facadeClassName: String): String? {
        if (!facadeClassName.endsWith(PART_CLASS_NAME_SUFFIX)) return null
        val baseName = facadeClassName.substring(0, facadeClassName.length - PART_CLASS_NAME_SUFFIX.length)
        if (baseName == "_") return null
        return "${decapitalizeAsJavaClassName(baseName)}.${CangJieFileType.EXTENSION}"
    }
}
