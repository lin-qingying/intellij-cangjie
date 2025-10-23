package org.cangnova.cangjie.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.analysis.decompiler.psi.BuiltinsVirtualFileProvider
interface CangJieBuiltInStubVersionOffsetProvider {
    fun getVersionOffset(): Int

    companion object {
        fun getVersionOffset(): Int =
            ApplicationManager.getApplication().getService(CangJieBuiltInStubVersionOffsetProvider::class.java)
                ?.getVersionOffset() ?: 0
    }
}

/**
 * Applies no changes to the K1 IDE stub version and adds a big constant offset to the K2 IDE stub version for .kotlin_builtins files.
 * It should be practically impossible to get a big enough stub version with K1 for it to clash with the K2 version range.
 * See the comment in [CangJieBuiltInStubVersionOffsetProvider] for the reasons why the offset is needed.
 */
internal class IdeCangJieBuiltInStubVersionOffsetProvider :
    CangJieBuiltInStubVersionOffsetProvider {
    override fun getVersionOffset(): Int {
        return 0
    }
}

interface CangJieBuiltInDecompilationInterceptor {
    fun readFile(bytes: ByteArray, file: VirtualFile): CangJieMetadataStubBuilder.FileWithMetadata?

    companion object {
        fun readFile(
            project: Project,
            bytes: ByteArray,
            file: VirtualFile
        ): CangJieMetadataStubBuilder.FileWithMetadata? =
            project.service<CangJieBuiltInDecompilationInterceptor>().readFile(bytes, file)

    }
}
internal class IdeCangJieBuiltInDecompilationInterceptor(private val project: Project) : CangJieBuiltInDecompilationInterceptor {
    override fun readFile(bytes: ByteArray, file: VirtualFile): CangJieMetadataStubBuilder.FileWithMetadata? {
        if (file in BuiltinsVirtualFileProvider.getInstance(project).getBuiltinVirtualFiles(project))
            return BuiltInDefinitionFile.read(bytes, file, filterOutClassesExistingAsClassFiles = false)
        else return null
    }
}
