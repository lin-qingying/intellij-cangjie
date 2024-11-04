package com.linqingying.cangjie.metadata

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.lang.declarations.CjDecompiledFile
import com.linqingying.cangjie.metadata.decompiler.CangJieDecompiledFileViewProvider
import com.linqingying.cangjie.metadata.decompiler.CangJieMetadataDeserializerForDecompiler
import com.linqingying.cangjie.metadata.decompiler.DecompiledText
import com.linqingying.cangjie.metadata.decompiler.createIncompatibleAbiVersionDecompiledText
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.psi.compiled.ClassFileDecompilers
import com.linqingying.cangjie.psi.compiled.ClsStubBuilder
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol
import com.linqingying.cangjie.serialization.deserialization.FlexibleTypeDeserializer
import com.linqingying.cangjie.serialization.deserialization.getClassId
import com.linqingying.cangjie.utils.addIfNotNull
import org.jetbrains.annotations.TestOnly
import java.io.IOException

abstract class CangJieMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    private val expectedBinaryVersion: () -> V,
    private val invalidBinaryVersion: () -> V,
    stubVersion: Int
) : ClassFileDecompilers.Full() {
    protected open val metadataStubBuilder: CangJieMetadataStubBuilder =
        CangJieMetadataStubBuilder(stubVersion, fileType, serializerProtocol, ::readFileSafely)

    private val renderer: DescriptorRenderer by lazy {
        DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
    }

    abstract fun readFile(project: Project, bytes: ByteArray, file: VirtualFile): CangJieMetadataStubBuilder.FileWithMetadata?

    override fun accepts(file: VirtualFile) = file.extension == fileType.defaultExtension || file.fileType == fileType


    override val stubBuilder: ClsStubBuilder
        get() = metadataStubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return CangJieDecompiledFileViewProvider(manager, file, physical) {project, provider ->
            val virtualFile = provider.virtualFile
            readFileSafely(project,virtualFile)?.let { fileWithMetadata ->
                CjDecompiledFile(provider) {
                    check(it == virtualFile) {
                        "Unexpected file $it, expected ${virtualFile.fileType}"
                    }
                    buildDecompiledText( manager.project,  fileWithMetadata)
                }
            }
        }
    }

    @TestOnly
    fun readFile(  project:Project,file: VirtualFile) = readFileSafely(project,file)

    protected fun readFileSafely(
        project:Project,
        file: VirtualFile,
        content: ByteArray? = null
    ): CangJieMetadataStubBuilder.FileWithMetadata? {
        if (!file.isValid) return null

        return try {
            readFile(project,content ?: file.contentsToByteArray(false), file)
        } catch (e: IOException) {
            // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
            // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
            // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
            // it's not always allowed and also is likely to degrade performance
            null
        }
    }

    fun buildDecompiledText(project:Project,file: CangJieMetadataStubBuilder.FileWithMetadata): DecompiledText {
        return when (file) {
            is CangJieMetadataStubBuilder.FileWithMetadata.Incompatible -> {
                createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion(), file.version)
            }

            is CangJieMetadataStubBuilder.FileWithMetadata.Compatible -> {
                val packageFqName = file.packageFqName
                val resolver = CangJieMetadataDeserializerForDecompiler(project,
                    packageFqName, file.proto, file.nameResolver, file.version,
                    serializerProtocol(), flexibleTypeDeserializer
                )
                val declarations = arrayListOf<DeclarationDescriptor>()
                declarations.addAll(resolver.resolveDeclarationsInFacade(packageFqName))
                for (classProto in file.classesToDecompile) {
                    val classId = file.nameResolver.getClassId(classProto.fqName)
                    declarations.addIfNotNull(resolver.resolveTopLevelClass(classId))
                }
                buildDecompiledText(packageFqName, declarations, renderer)
            }
        }
    }
}

