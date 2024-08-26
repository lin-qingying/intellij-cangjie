package com.huawei.cangjie.ide.quickfix.overrideImplement

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

abstract class AbstractGenerateMembersHandler<T : ClassMember> : LanguageCodeInsightActionHandler {
    abstract val toImplement: Boolean

    fun collectMembersToGenerateUnderProgress(classOrObject: CjTypeStatement): Collection<T> {
        return ProgressManager.getInstance().runProcessWithProgressSynchronously<Collection<T>, RuntimeException>(
            { runReadAction { collectMembersToGenerate(classOrObject) } },
            CangJieBundle.message("dialog.progress.collect.members.to.generate"), true, classOrObject.project
        )
    }

    @RequiresBackgroundThread(generateAssertion = false)
    abstract fun collectMembersToGenerate(classOrObject: CjTypeStatement): Collection<T>

    abstract fun generateMembers(editor: Editor, classOrObject: CjTypeStatement, selectedElements: Collection<T>, copyDoc: Boolean)

    @NlsContexts.DialogTitle
    protected abstract fun getChooserTitle(): String

    @NlsContexts.HintText
    protected abstract fun getNoMembersFoundHint(): String

    protected open fun isValidForClass(classOrObject: CjTypeStatement) = true

    private fun showOverrideImplementChooser(project: Project, members: Collection<T>): MemberChooser<T>? {
        @Suppress("UNCHECKED_CAST")
        val memberArray = members.toTypedArray<ClassMember>() as Array<T>
        val chooser = MemberChooser(memberArray, false, true, project)
        chooser.title = getChooserTitle()
        if (toImplement) {
            chooser.selectElements(memberArray)
        }

        chooser.show()
        if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) return null
        return chooser
    }

    override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
        if (file !is CjFile) return false
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<CjTypeStatement>()
        return classOrObject != null && isValidForClass(classOrObject)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        invoke(project, editor, file, implementAll = isUnitTestMode())
    }

    fun invoke(project: Project, editor: Editor, file: PsiFile, implementAll: Boolean) {
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<CjTypeStatement>() ?: return

        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val members = collectMembersToGenerateUnderProgress(classOrObject)
        if (members.isEmpty() && !implementAll) {
            HintManager.getInstance().showErrorHint(editor, getNoMembersFoundHint())
            return
        }

        val copyDoc: Boolean
        val selectedElements: Collection<T>

        if (implementAll) {
            selectedElements = members
            copyDoc = false
        } else {
            val chooser = showOverrideImplementChooser(project, members) ?: return
            selectedElements = chooser.selectedElements ?: return
            copyDoc = chooser.isCopyJavadoc
        }

        if (selectedElements.isEmpty()) return

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        generateMembers(editor, classOrObject, selectedElements, copyDoc)
    }

    override fun startInWriteAction(): Boolean = false
}
