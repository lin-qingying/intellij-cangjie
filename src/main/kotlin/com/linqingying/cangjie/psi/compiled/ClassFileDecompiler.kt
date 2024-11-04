package com.linqingying.cangjie.psi.compiled

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager


class ClassFileDecompiler : BinaryFileDecompiler {
    override fun decompile(file: VirtualFile): CharSequence {
        val decompiler: ClassFileDecompilers.Decompiler = ClassFileDecompilers.instance.find(
            file,
            ClassFileDecompilers.Decompiler::class.java
        )
//        if (decompiler is ClsDecompilerImpl) {
//            return ClsFileImpl.decompile(file)
//        }

        if (decompiler is ClassFileDecompilers.Full) {
            val manager = PsiManager.getInstance(DefaultProjectFactory.getInstance().defaultProject)
            return decompiler.createFileViewProvider(file, manager, true).contents
        }

//        if (decompiler is ClassFileDecompilers.Light) {
//            try {
//                return decompiler.getText(file)
//            } catch (e: ClassFileDecompilers.Light.CannotDecompileException) {
//                LOG.warn("decompiler: " + decompiler::class, e)
//                return ClsFileImpl.decompile(file)
//            }
//        }

        throw IllegalStateException(
            decompiler::class.java.name +
                    " should be on of " +
                    ClassFileDecompilers.Full::class.java.name +
                    " or " +
                    ClassFileDecompilers.Light::class.java.name
        )
    }

    companion object {
        private val LOG = Logger.getInstance(
            ClassFileDecompiler::class.java
        )
    }
}
