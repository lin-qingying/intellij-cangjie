package com.linqingying.lsp.impl.quickFix

import com.linqingying.lsp.api.customization.LspIntentionAction


class LspQuickFixWrapper(
    val quickFixSet: LspQuickFixSet,
    index: Int
) : LspIntentionActionWrapperBase(index) {
    override var lspIntentionAction: LspIntentionAction? = null


}

//
//class LspQuickFixWrapper(val quickFixSet: LspQuickFixSet, val index: Int) : IntentionAction,
//    Comparable<IntentionAction> {
//    override fun startInWriteAction(): Boolean = false
//
//
//    override fun getFamilyName(): String = ""
//
//    override fun getText(): String = lspIntentionAction?.text ?: ""
//
//    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
//        lspIntentionAction?.isAvailable(project, editor, file) ?: false
//
//    var lspIntentionAction: IntentionAction? = null
//    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
//        lspIntentionAction?.invoke(project, editor, file)
//    }
//
//    override fun compareTo(other: IntentionAction): Int {
//        return if (other is LspQuickFixWrapper) {
//            this.index - other.index
//        } else {
//            0
//        }
//    }
//}
//
