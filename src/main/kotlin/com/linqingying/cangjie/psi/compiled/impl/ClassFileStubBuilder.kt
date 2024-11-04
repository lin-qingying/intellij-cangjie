package com.linqingying.cangjie.psi.compiled.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.stubs.BinaryFileStubBuilder
import com.intellij.psi.stubs.Stub
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent
import com.linqingying.cangjie.psi.compiled.ClassFileDecompilers
import com.linqingying.cangjie.psi.compiled.ClassFileDecompilers.Full
import java.util.function.Supplier
import java.util.stream.Stream

class ClassFileStubBuilder : BinaryFileStubBuilder.CompositeBinaryFileStubBuilder<Full> {
    override fun getFileFilter(): VirtualFileFilter {
        return VirtualFileFilter.ALL // any file of file type that this builder is registered for
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return true
    }

    override fun getAllSubBuilders(): Stream<Full?> {
        return ClassFileDecompilers.instance.EP_NAME.extensionList.stream().filter { d -> d is Full }
            .map { d -> d as Full }
    }

    override fun getSubBuilder(fileContent: FileContent): Full? {
        return fileContent.file
            .computeWithPreloadedContentHint(fileContent.content,
                Supplier { ClassFileDecompilers.instance.find(fileContent.file, Full::class.java) })
    }

    override fun getSubBuilderVersion(decompiler: Full?): String {
        if (decompiler == null) return "default"
        val version: Int = decompiler.stubBuilder.stubVersion
        return decompiler::class.java.name + ":" + version
    }

    override fun buildStubTree(fileContent: FileContent, decompiler: Full?): Stub? {
        if (decompiler == null) return null
        return fileContent.file.computeWithPreloadedContentHint(fileContent.content,
            Supplier {
                val file = fileContent.file
                try {
                    return@Supplier decompiler.stubBuilder.buildFileStub(fileContent)
                } catch (e: ClsFormatException) {
                    if (LOG.isDebugEnabled) {
                        LOG.debug(file.path, e)
                    } else {
                        LOG.info(file.path + ": " + e.message)
                    }
                }
                null
            })
    }

    override fun getStubVersion(): Int {
        return STUB_VERSION
    }

    companion object {
        private val LOG = Logger.getInstance(
            ClassFileStubBuilder::class.java
        )

        const val STUB_VERSION: Int = 27
    }
}
