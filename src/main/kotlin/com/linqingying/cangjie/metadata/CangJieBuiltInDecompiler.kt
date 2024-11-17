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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lang.declarations.CangJieBuiltInFileType
import com.linqingying.cangjie.metadata.builtins.BuiltInsBinaryVersion
import com.linqingying.cangjie.name.CangJieClassName
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.stubs.CangJieStubVersions
import com.linqingying.cangjie.serialization.METADATA_FILE_EXTENSION
import com.linqingying.cangjie.serialization.deserialization.BuiltInSerializerProtocol
import com.linqingying.cangjie.serialization.deserialization.FlexibleTypeDeserializer
import com.linqingying.cangjie.serialization.deserialization.descriptors.CangJiePackagePartSource
import com.linqingying.cangjie.serialization.deserialization.getClassId
import org.jetbrains.annotations.TestOnly
import java.io.ByteArrayInputStream


/**
 * This version is used for .kotlin_builtins and is not used for .kotlin_metadata files:
 * K1 IDE and K2 IDE produce different decompiled files and stubs for .kotlin_builtins, but not for .kotlin_metadata
 */
private val stubVersionForStubBuilderAndDecompiler: Int
    get() = CangJieStubVersions.BUILTIN_STUB_VERSION + CangJieBuiltInStubVersionOffsetProvider.getVersionOffset()

class CangJieBuiltInDecompiler : CangJieMetadataDecompiler<BuiltInsBinaryVersion>(
    CangJieBuiltInFileType,
    { BuiltInSerializerProtocol },
    FlexibleTypeDeserializer.ThrowException,
    { BuiltInsBinaryVersion.INSTANCE },
    { BuiltInsBinaryVersion.INVALID_VERSION },
    stubVersionForStubBuilderAndDecompiler,
) {
    override val metadataStubBuilder: CangJieMetadataStubBuilder =
        CangJieBuiltInMetadataStubBuilder(::readFileSafely)

    override fun readFile(
        project: Project,
        bytes: ByteArray,
        file: VirtualFile
    ): CangJieMetadataStubBuilder.FileWithMetadata? {
        return CangJieBuiltInDecompilationInterceptor.readFile(project, bytes, file) ?: BuiltInDefinitionFile.read(

            bytes,
            file
        )
    }
}

class BuiltInDefinitionFile(
    proto: ProtoBuf.PackageFragment,
    version: BuiltInsBinaryVersion,
    /**
     * Directory where the VirtualFile is situated. Can be null in the case when the builtin file is created in the air.
     */
    val packageDirectory: VirtualFile?,
    val isMetadata: Boolean,
    private val filterOutClassesExistingAsClassFiles: Boolean = true,
) : CangJieMetadataStubBuilder.FileWithMetadata.Compatible(proto, version, BuiltInSerializerProtocol) {
    override val classesToDecompile: List<ProtoBuf.Class>
        get() = super.classesToDecompile.let { classes ->
            if (packageDirectory == null) {
                // If a builtin file is created in the air,
                // that means we need all built-in files because there are no .class files to replace them with,
                return@let classes
            }
            if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES || !filterOutClassesExistingAsClassFiles) classes
            else classes.filter { classProto ->
                shouldDecompileBuiltInClass(nameResolver.getClassId(classProto.fqName), packageDirectory)
            }
        }

    private fun shouldDecompileBuiltInClass(classId: ClassId, packageDirectory: VirtualFile): Boolean {
        val realClassFileName = classId.shortClassName.asString() + "." + CangJieFileType.INSTANCE.defaultExtension
        return packageDirectory.findChild(realClassFileName) == null
    }

    companion object {
        var FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = true
            @TestOnly set

        @JvmOverloads
        fun read(

            contents: ByteArray, file: VirtualFile,
            filterOutClassesExistingAsClassFiles: Boolean = true
        ): CangJieMetadataStubBuilder.FileWithMetadata? {
            val stream = ByteArrayInputStream(contents)

            val version = BuiltInsBinaryVersion.readFrom(stream)
            if (!version.isCompatibleWithCurrentCompilerVersion()) {
                return Incompatible(version)
            }

            val proto = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            val result = BuiltInDefinitionFile(
                proto, version, file.parent,
                file.extension == METADATA_FILE_EXTENSION,
                filterOutClassesExistingAsClassFiles
            )

            val packageProto = result.proto.`package`
            if (result.classesToDecompile.isEmpty() &&
                packageProto.typeAliasCount == 0 && packageProto.functionCount == 0 && packageProto.variableCount == 0
            ) {
                // No declarations to decompile: should skip this file
                return null
            }

            return result
        }
    }
}

private class CangJieBuiltInMetadataStubBuilder(
    readFile: (Project, VirtualFile, ByteArray) -> FileWithMetadata?,
) : CangJieMetadataStubBuilder(
    stubVersionForStubBuilderAndDecompiler,
    CangJieBuiltInFileType,
    { BuiltInSerializerProtocol },
    readFile
) {
    override fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement {
        val fileNameForFacade =
            when (val withoutExtension = filename.removeSuffix(BuiltInSerializerProtocol.DOT_DEFAULT_EXTENSION)) {
                // this is the filename used in stdlib, others should match
                "cangjie" -> "library"
                else -> withoutExtension
            }

        val facadeFqName = PackagePartClassUtils.getPackagePartFqName(file.packageFqName, fileNameForFacade)
        return CangJiePackagePartSource(
            CangJieClassName.byClassId(ClassId.topLevel(facadeFqName)),
            null,
            file.proto.`package`,
            file.nameResolver
        )
    }
}
