package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.UserDataProperty
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly

abstract class CangJieImportQuickFixAction<out T : CjElement>(element: T): CangJieQuickFixAction<T>(element) {
    /**
     * @return import action if quick fix element is still valid, null, otherwise.
     * Note that provided action might require addition actions by the user (e.g. when there are multiple available imports to choose from).
     */
    abstract fun createImportAction(
        editor: Editor,
        file: CjFile,
    ): QuestionAction?

    /**
     * @return import action if quick fix can be applied without any additional actions by the user, null, otherwise.
     */
    abstract fun createAutoImportAction(
        editor: Editor,
        file: CjFile,
        filterSuggestions: (Collection<FqName>) -> Collection<FqName> = { suggestions -> suggestions },
    ): QuestionAction?
}
object CangJieAddImportActionInfo {
    interface ExecuteListener {
        fun onExecute(variants: List<AutoImportVariant>)
    }

    var PsiFile.executeListener: ExecuteListener? by UserDataProperty(Key("CANGJIE_IMPORT_EXECUTE_LISTENER"))

    @TestOnly
    fun setExecuteListener(file: PsiFile, disposable: Disposable, listener: ExecuteListener) {
        assert(file.executeListener == null)
        file.executeListener = listener
        Disposer.register(disposable) { file.executeListener = null }
    }
}
