package org.cangnova.cangjie.analysis.decompiler.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.metadata.CangJieMetadataStubBuilder
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.model.Decl
import org.cangnova.cangjie.metadata.model.Package
import org.cangnova.cangjie.metadata.model.parser.toPackage
import org.cangnova.cangjie.name.CangJieClassName
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.compiled.ClsStubBuilder
import org.cangnova.cangjie.psi.stubs.CangJieStubVersions
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import org.jetbrains.annotations.TestOnly
import java.io.ByteArrayInputStream


private val stubVersionForStubBuilderAndDecompiler: Int
    get() = CangJieStubVersions.BUILTIN_STUB_VERSION + CangJieBuiltInStubVersionOffsetProvider.getVersionOffset()

class CangJieBuiltInDecompiler : CangJieMetadataDecompiler<BuiltInsBinaryVersion>(
    CangJieBuiltInFileType,
    { BuiltInSerializerFlatbuffers },
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
    `package`: Package,
    version: BuiltInsBinaryVersion,
    /**
     * Directory where the VirtualFile is situated. Can be null in the case when the builtin file is created in the air.
     */
    val packageDirectory: VirtualFile?,
    val isMetadata: Boolean,
    private val filterOutClassesExistingAsClassFiles: Boolean = true,
) : CangJieMetadataStubBuilder.FileWithMetadata.Compatible(`package`, version, BuiltInSerializerFlatbuffers) {
    //    override val classesToDecompile: List<ProtoBuf.Class>
//        get() = super.classesToDecompile.let { classes ->
//            if (packageDirectory == null) {
//                // If a builtin file is created in the air,
//                // that means we need all built-in files because there are no .class files to replace them with,
//                return@let classes
//            }
//            if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES || !filterOutClassesExistingAsClassFiles) classes
//            else classes.filter { classProto ->
//                shouldDecompileBuiltInClass(nameResolver.getClassId(classProto.fqName), packageDirectory)
//            }
//        }
    override val declToDecompile: List<Decl>
        get() = super.declToDecompile

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


            val `package` = stream.toPackage()
            val version = `package`.cjoVersion
//            if (!version.isCompatibleWithCurrentCompilerVersion()) {
//                return Incompatible(version)
//            }
            val result = BuiltInDefinitionFile(
                `package`, version, file.parent,
                false,
                filterOutClassesExistingAsClassFiles
            )


            if (result.declToDecompile.isEmpty() &&
                `package`.types.size == 0 && `package`.values.size == 0

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
) {
//    override fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement {
//        val fileNameForFacade =
//            when (val withoutExtension = filename.removeSuffix(BuiltInSerializerFlatbuffers.DOT_DEFAULT_EXTENSION)) {
//                // this is the filename used in stdlib, others should match
//                "cangjie" -> "library"
//                else -> withoutExtension
//            }
//
//        val facadeFqName = PackagePartClassUtils.getPackagePartFqName(file.packageFqName, fileNameForFacade)
//        return CangJiePackagePartSource(
//            CangJieClassName.byClassId(ClassId.topLevel(facadeFqName)),
//            null,
//            file. `package`,
//
//        )
//    }
}
