package com.linqingying.cangjie.metadata

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.metadata.decompiler.BuiltInsVirtualFileProvider

/**
 * Application service that adds a constant offset to the stub version of .kotlin_builtins files.
 * The purpose of this offset is to rebuild the decompiled text and the stubs for .kotlin_builtins files after K1 <-> K2 IDE switches.
 * K1 and K2 provide different sets of declarations from .kotlin_builtins files under certain conditions,
 * see [com.linqingying.cangjie.analysis.decompiler.psi.BuiltInDefinitionFile].
 * Not forcing a rebuild for affected decompiled files and corresponding stubs leads to a stub error.
 */
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
internal class IdeCangJieBuiltInStubVersionOffsetProvider : CangJieBuiltInStubVersionOffsetProvider {
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
//            ApplicationManager.getApplication().getService(CangJieBuiltInDecompilationInterceptor::class.java)
//                ?.readFile(bytes, file)
    }
}

/**
 * Decompiles .kotlin_builtins files that belong to the kotlin-stdlib from the plugin classpath without class filtering.
 * The decompiled classes from these files are used in the symbol provider for built-ins in all modules, including non-JVM.
 * For common modules in particular, the lack of these classes leads to unresolved code, as the declarations are not published
 * in .kotlin_medata files of kotlin-stdlib-common.
 * See [com.linqingying.cangjie.analysis.decompiler.psi.BuiltInDefinitionFile].
 */
internal class IdeCangJieBuiltInDecompilationInterceptor(val project: Project) :
    CangJieBuiltInDecompilationInterceptor {
    override fun readFile(bytes: ByteArray, file: VirtualFile): CangJieMetadataStubBuilder.FileWithMetadata? {
        if (file in BuiltInsVirtualFileProvider.getInstance(project).getBuiltinVirtualFiles())

            return BuiltInDefinitionFile.read(  bytes, file, filterOutClassesExistingAsClassFiles = false)
        else return null
    }
}
