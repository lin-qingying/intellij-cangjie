package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.huawei.cangjie.psi.CjCodeFragment
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.psiUtil.CREATE_BY_PATTERN_MAY_NOT_REFORMAT
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.createSmartPointer
import com.intellij.util.ReflectionUtil

abstract class QuickFixActionBase<out T : PsiElement>(element: T) : IntentionAction, Cloneable {
    @SafeFieldForPreview // not actually safe but will be properly patched in getFileModifierForPreview
    private val elementPointer = element.createSmartPointer()

    val element: T?
        get() = elementPointer.element

    open val isCrossLanguageFix: Boolean = false

    protected open fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean = true

    final override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (isUnitTestMode()) {
            CREATE_BY_PATTERN_MAY_NOT_REFORMAT = true
        }
        try {
            val element = element ?: return false
            return element.isValid &&
                    !element.project.isDisposed &&
                    (file.manager.isInProject(file) || file is CjCodeFragment  ) &&
                    (file is CjFile || isCrossLanguageFix) &&
                    isAvailableImpl(project, editor, file)
        } finally {
            CREATE_BY_PATTERN_MAY_NOT_REFORMAT = false
        }
    }

    override fun startInWriteAction(): Boolean = true

    /**
     * This implementation clones current intention replacing [elementPointer]
     * field value with the pointer to the corresponding element
     * in the target file. It returns null if subclass has potentially unsafe fields not
     * marked with [@SafeFieldForPreview][SafeFieldForPreview].
     */
    override fun getFileModifierForPreview(target: PsiFile): FileModifier? {
        // Check field safety in subclass
        if (super.getFileModifierForPreview(target) !== this) return null
        val oldElement: T = element ?: return null
        if (IntentionPreviewUtils.getOriginalFile(target) != oldElement.containingFile) {
            throw IllegalStateException("Intention action ${this::class} ($familyName) refers to the element from another source file. " +
                    "It's likely that it's going to modify a file not opened in the editor, " +
                    "so default preview strategy won't work. Also, if another file is modified, " +
                    "getElementToMakeWritable() must be properly implemented to denote the actual file " +
                    "to be modified.")
        }
        val newElement = PsiTreeUtil.findSameElementInCopy(oldElement, target)
        val clone = try {
            super.clone() as QuickFixActionBase<*>
        } catch (e: CloneNotSupportedException) {
            throw InternalError(e) // should not happen as we implement Cloneable
        }
        if (!ReflectionUtil.setField(
                QuickFixActionBase::class.java, clone, SmartPsiElementPointer::class.java, "elementPointer",
                newElement.createSmartPointer()
            )) {
            return null
        }
        return clone
    }

    /**
     * Do not call this method, it's non-functional
     *
     * @throws CloneNotSupportedException always
     */
    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any = throw CloneNotSupportedException()
}
