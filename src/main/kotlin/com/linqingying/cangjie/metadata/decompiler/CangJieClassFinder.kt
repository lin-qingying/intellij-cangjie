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

import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.serialization.CangJieMetadataVersion
import java.io.InputStream


interface CangJieClassFinder : CangJieMetadataFinder {
    fun findCangJieClassOrContent(classId: ClassId, metadataVersion: CangJieMetadataVersion): Result?

//    fun findCangJieClassOrContent(cangjieClass:  CangJieClass, jvmMetadataVersion: CangJieMetadataVersion): Result?

    sealed class Result {
//        fun toCangJieBinaryClass(): CangJieBinaryClass? = (this as? CangJieClass)?.kotlinJvmBinaryClass

//        class CangJieClass(val kotlinJvmBinaryClass: CangJieBinaryClass, val byteContent: ByteArray? = null) : Result() {
//            operator fun component1(): CangJieBinaryClass = kotlinJvmBinaryClass
//            operator fun component2(): ByteArray? = byteContent
//        }

        class ClassFileContent(val content: ByteArray) : Result()
    }
}
class DirectoryBasedClassFinder(
    val packageDirectory: VirtualFile,
    val directoryPackageFqName: FqName
) : CangJieClassFinder {
    override fun findCangJieClassOrContent(
        classId: ClassId,
         metadataVersion: CangJieMetadataVersion
    ): CangJieClassFinder.Result? {
//        if (classId.packageFqName != directoryPackageFqName) {
//            return null
//        }
//        val targetName = classId.relativeClassName.pathSegments().joinToString("$", postfix = ".class")
//        val virtualFile = packageDirectory.findChild(targetName)
//        if (virtualFile != null && isCangJieWithCompatibleAbiVersion(virtualFile, jvmMetadataVersion)) {
//            return ClsCangJieBinaryClassCache.getInstance().getKotlinBinaryClass(virtualFile)?.let(::KotlinClass)
//        }
        return null
    }


    // TODO
    override fun findMetadata(classId: ClassId): InputStream? = null

    // TODO
    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? = null

    // TODO
    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    // TODO: load built-ins from packageDirectory?
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null
}
