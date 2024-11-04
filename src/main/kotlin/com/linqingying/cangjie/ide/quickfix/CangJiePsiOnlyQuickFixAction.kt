package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile


abstract class CangJiePsiOnlyQuickFixAction<out T : PsiElement>(element: T) : QuickFixActionBase<T>(element) {
    protected open fun isAvailable(project: Project, editor: Editor?, file: CjFile) = true

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val cjFile = file as? CjFile ?: return false
        return isAvailable(project, editor, cjFile)
    }

    final override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        if (file is CjFile) {
            invoke(project, editor, file)
        }
    }

    protected abstract operator fun invoke(project: Project, editor: Editor?, file: CjFile)

    override fun startInWriteAction() = true
}
