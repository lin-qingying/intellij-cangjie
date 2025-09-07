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
import org.cangnova.cangjie.serialization.deserialization.ClassDeserializer

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

                    !decl.classId.isNestedClass && decl.classId !in ClassDeserializer.BLACK_LIST
                }
        }
    }

}