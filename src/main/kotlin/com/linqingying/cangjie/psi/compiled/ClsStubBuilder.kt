package com.linqingying.cangjie.psi.compiled

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent

abstract class ClsStubBuilder {
    /**
     * Non-zero positive number expected.
     */
    abstract val stubVersion: Int

    /**
     * May return `null` for inner or synthetic classes - i.e. those indexed as a part of their parent .class file.
     */
    @Throws(ClsFormatException::class)
    abstract fun buildFileStub(fileContent: FileContent): PsiFileStub<*>?
}
