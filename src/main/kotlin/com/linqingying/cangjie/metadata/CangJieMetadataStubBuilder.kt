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

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.metadata.decompiler.*
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.deserialization.NameResolverImpl
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.compiled.ClsStubBuilder
import com.linqingying.cangjie.psi.compiled.impl.ClassFileStubBuilder
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol
import com.linqingying.cangjie.serialization.deserialization.ClassDeserializer
import com.linqingying.cangjie.serialization.deserialization.ProtoBasedClassDataFinder
import com.linqingying.cangjie.serialization.deserialization.ProtoContainer
import com.linqingying.cangjie.serialization.deserialization.getClassId


open class CangJieMetadataStubBuilder(
    private val version: Int,
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val readFile: (Project, VirtualFile, ByteArray) -> FileWithMetadata?
) : ClsStubBuilder() {

    override val stubVersion: Int = ClassFileStubBuilder.STUB_VERSION + version
    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        assert(virtualFile.extension == fileType.defaultExtension || virtualFile.fileType == fileType) { "Unexpected file type ${virtualFile.fileType.name}" }
        val file = readFile(content.project,virtualFile, content.content) ?: return null

        when (file) {
            is FileWithMetadata.Incompatible -> {
                return createIncompatibleAbiVersionFileStub()
            }
            is FileWithMetadata.Compatible -> {
                val packageProto = file.proto.`package`
                val packageFqName = file.packageFqName
                val nameResolver = file.nameResolver
                val protocol = serializerProtocol()
                val components = ClsStubBuilderComponents(
                    ProtoBasedClassDataFinder(file.proto, nameResolver, file.version),
                    AnnotationLoaderForStubBuilderImpl(protocol),
                    virtualFile,
                    protocol
                )
                val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))

                val fileStub = createFileStub(packageFqName )
                createPackageDeclarationsStubs(
                    fileStub, context,
                    ProtoContainer.Package(
                        packageFqName,
                        context.nameResolver,
                        context.typeTable,
                        source = createCallableSource(file, content.fileName)
                    ),
                    packageProto
                )
                for (classProto in file.classesToDecompile) {
                    createClassStub(
                        fileStub, classProto, nameResolver, nameResolver.getClassId(classProto.fqName), source = null, context = context
                    )
                }
                return fileStub
            }
        }
    }

    protected open fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement? = null

    sealed class FileWithMetadata {
        class Incompatible(val version: BinaryVersion) : FileWithMetadata()

        open class Compatible(
            val proto: ProtoBuf.PackageFragment,
            val version: BinaryVersion,
            serializerProtocol: SerializerExtensionProtocol
        ) : FileWithMetadata() {
            val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)
            val packageFqName = FqName(nameResolver.getPackageFqName(proto.`package`.getExtension(serializerProtocol.packageFqName)))

            open val classesToDecompile: List<ProtoBuf.Class> =
                proto.class_List.filter { proto ->
                    val classId = nameResolver.getClassId(proto.fqName)
                    !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
                }
        }
    }
}

