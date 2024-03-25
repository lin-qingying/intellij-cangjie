package com.huawei.cangjie.resolve.source

import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.SourceFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface PsiSourceElement : SourceElement {
    val psi: PsiElement?

    override fun getContainingFile(): SourceFile = psi?.containingFile?.let(::PsiSourceFile) ?: SourceFile.NO_SOURCE_FILE
}

class PsiSourceFile(val psiFile: PsiFile) : SourceFile {
    override fun equals(other: Any?): Boolean = other is PsiSourceFile && psiFile == other.psiFile

    override fun hashCode(): Int = psiFile.hashCode()

    override fun toString(): String = psiFile.virtualFile.path

    override fun getName(): String? = psiFile.virtualFile?.name
}
fun SourceElement.getPsi(): PsiElement? = (this as? PsiSourceElement)?.psi
