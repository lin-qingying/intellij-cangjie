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

package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.psi.compiled.ClsStubBuilder
import org.cangnova.cangjie.psi.compiled.impl.ClassFileStubBuilder
import org.cangnova.cangjie.serialization.deserialization.BLACK_LIST

/**
 * 从 CangJie 的"元数据文件"中构建 PSI Stub，用于 IDE 在没有源码时进行代码结构索引、导航与反编译查看。
 */
open class CangJieMetadataStubBuilder(
    private val version: Int,
    private val fileType: FileType,
    private val serializerFlatbuffers: () -> SerializerExtensionFlatbuffers,
    private val readFile: (Project, VirtualFile, ByteArray) -> FileWithMetadata?
) : ClsStubBuilder() {

    override val stubVersion: Int = ClassFileStubBuilder.STUB_VERSION + version
    override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {

        return null;
    }
    protected open fun createCallableSource(file:  FileWithMetadata.Compatible, filename: String): SourceElement? = null


    sealed class FileWithMetadata {
        class Incompatible(val version: BinaryVersion) : FileWithMetadata()
        open class Compatible(
            val `package`: PackageWrapper,
            val version: BinaryVersion,
            serializerProtocol: SerializerExtensionFlatbuffers
        ) : FileWithMetadata() {
            val packageFqName = `package`.packageName

            open val classesToDecompile: List<ClassDeclWrapper> =
                `package`.allClassDecls.filter { decl ->

                    !decl.classId.isNestedClass && decl.classId !in BLACK_LIST
                }
        }
    }

}