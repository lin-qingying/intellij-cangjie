package org.cangnova.cangjie.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.decompiler.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.decompiler.psi.text.DecompiledText
import org.cangnova.cangjie.analysis.decompiler.psi.text.buildDecompiledText
import org.cangnova.cangjie.analysis.decompiler.psi.text.createIncompatibleMetadataVersionDecompiledText
import org.cangnova.cangjie.analysis.decompiler.psi.text.defaultDecompilerRendererOptions
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.EnumWrapper
import org.cangnova.cangjie.psi.compiled.ClassFileDecompilers
import org.cangnova.cangjie.psi.compiled.ClsStubBuilder
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.utils.addIfNotNull
import org.jetbrains.annotations.TestOnly
import java.io.IOException

abstract class CangJieMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerFlatbuffers: () -> SerializerExtensionFlatbuffers,
//    private val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    private val expectedBinaryVersion: () -> V,
    private val invalidBinaryVersion: () -> V,
    stubVersion: Int
) : ClassFileDecompilers.Full(){
    private val renderer: DescriptorRenderer by lazy {
        DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
    }
    protected open val metadataStubBuilder: CangJieMetadataStubBuilder =
        CangJieMetadataStubBuilder(stubVersion, fileType, serializerFlatbuffers, ::readFileSafely)
    override fun accepts(file: VirtualFile) = file.extension == fileType.defaultExtension || file.fileType == fileType

    override val stubBuilder: ClsStubBuilder
        get() = metadataStubBuilder
    protected fun readFileSafely(
        project: Project,
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
//            为方便调试，这里先抛出异常
            throw e
        }
    }
    @TestOnly
    fun readFile(project: Project, file: VirtualFile) = readFileSafely(project,file)
    abstract fun readFile(project: Project, bytes: ByteArray, file: VirtualFile): CangJieMetadataStubBuilder.FileWithMetadata?
    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return CangJieDecompiledFileViewProvider(manager, file, physical) {project, provider ->
            val virtualFile = provider.virtualFile
            readFileSafely(project,virtualFile)?.let { fileWithMetadata ->
                CjDecompiledFile(provider) {
                    check(it == virtualFile) {
                        "Unexpected file $it, expected ${virtualFile.fileType}"
                    }
                    buildDecompiledText(manager.project, fileWithMetadata)
                }
            }
        }
    }
    fun buildDecompiledText(project:Project,file: CangJieMetadataStubBuilder.FileWithMetadata): DecompiledText {
        return when (file) {
            is CangJieMetadataStubBuilder.FileWithMetadata.Incompatible -> {
                createIncompatibleMetadataVersionDecompiledText(expectedBinaryVersion(), file.version)
            }

            is CangJieMetadataStubBuilder.FileWithMetadata.Compatible -> {
                val packageFqName = file.packageFqName
                val resolver = CangJieMetadataDeserializerForDecompiler(project,
                    packageFqName, file.`package`,  file.version,
                    serializerFlatbuffers()
                )
                val declarations = arrayListOf<DeclarationDescriptor>()
                declarations.addAll(resolver.resolveAllDeclarationsInPackage(packageFqName))
//                for (`class` in file.classesToDecompile) {
//                    val classId = `class`.classId
//                    if(`class` is EnumWrapper)
//                        declarations.addIfNotNull(resolver.resolveTopLevelEnum(classId))
//                        else
//                    declarations.addIfNotNull(resolver.resolveTopLevelClass(classId))
//                }
                buildDecompiledText(packageFqName, declarations, renderer)
            }
        }
    }

}