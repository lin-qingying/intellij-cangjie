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

package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.model.fb.parser.toFbPackage
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.stubs.CangJieStubVersions
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import org.jetbrains.annotations.TestOnly
import java.io.ByteArrayInputStream


private val stubVersionForStubBuilderAndDecompiler: Int
    get() = CangJieStubVersions.BUILTIN_STUB_VERSION + _root_ide_package_.org.cangnova.cangjie.decompiler.psi.CangJieBuiltInStubVersionOffsetProvider.getVersionOffset()

internal class CangJieBuiltInDecompiler :
    CangJieMetadataDecompiler<BuiltInsBinaryVersion>(
        CangJieBuiltInFileType,
        { BuiltInSerializerFlatbuffers },
        { BuiltInsBinaryVersion.INSTANCE },
        { BuiltInsBinaryVersion.INVALID_VERSION },
        stubVersionForStubBuilderAndDecompiler,
    ) {
    override val metadataStubBuilder: CangJieMetadataStubBuilder =
        CangJieBuiltInMetadataStubBuilder { project, file, bytes ->
            readFileSafely(project, file, bytes)
        }

    override fun readFile(
        project: Project,
        bytes: ByteArray,
        file: VirtualFile
    ): CangJieMetadataStubBuilder.FileWithMetadata? {
        return CangJieBuiltInDecompilationInterceptor.readFile(project, bytes, file)
            ?: BuiltInDefinitionFile.read(

                bytes,
                file
            )
    }


}

class BuiltInDefinitionFile(
    `package`: PackageWrapper,
    version: BuiltInsBinaryVersion,
    /**
     * Directory where the VirtualFile is situated. Can be null in the case when the builtin file is created in the air.
     */
    val packageDirectory: VirtualFile?,
    val isMetadata: Boolean,
    private val filterOutClassesExistingAsClassFiles: Boolean = true,
) : CangJieMetadataStubBuilder.FileWithMetadata.Compatible(`package`, version, BuiltInSerializerFlatbuffers) {
    override val classesToDecompile: List<ClassDeclWrapper>
        get() = super.classesToDecompile.let { classes ->
            if (packageDirectory == null) {
                // If a builtin file is created in the air,
                // that means we need all built-in files because there are no .class files to replace them with,
                return@let classes
            }
            if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES || !filterOutClassesExistingAsClassFiles) classes
            else classes.filter { decl ->
                shouldDecompileBuiltInClass(decl.classId, packageDirectory)
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


            val `package` = stream.toFbPackage().packageWrapper
            val version = `package`.cjoVersion
//            if (!version.isCompatibleWithCurrentCompilerVersion()) {
//                return Incompatible(version)
//            }
            val result = BuiltInDefinitionFile(
                `package`, version, file.parent,
                false,
                filterOutClassesExistingAsClassFiles
            )


            if (result.classesToDecompile.isEmpty() &&
                `package`.typeAliass.isEmpty() && `package`.functions.isEmpty() && `package`.variables.isEmpty()
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
    { BuiltInSerializerFlatbuffers },
    readFile
)
