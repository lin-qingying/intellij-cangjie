package com.huawei.cangjie.ide.quickfix

import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

abstract class CangJieCrossLanguageQuickFixAction<out T : PsiElement>(element: T) : QuickFixActionBase<T>(element) {
    override val isCrossLanguageFix: Boolean
        get() = true

    final override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        val element = element
        if (element != null &&
            (startInWriteAction() && getElementToMakeWritable(file) == file || IntentionPreviewUtils.prepareElementForWrite(element))) {
            invokeImpl(project, editor, file)
        }
    }

    protected abstract fun invokeImpl(project: Project, editor: Editor?, file: PsiFile)
}
