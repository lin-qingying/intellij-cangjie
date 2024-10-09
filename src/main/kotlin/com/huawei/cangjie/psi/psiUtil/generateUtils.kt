package com.huawei.cangjie.psi.psiUtil

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiDocumentManager


fun Editor.moveCaret(offset: Int, scrollType: ScrollType = ScrollType.RELATIVE) {
    caretModel.moveToOffset(offset)
    scrollingModel.scrollToCaret(scrollType)
}
fun Editor.unblocCDocument() {
    project?.let {
        PsiDocumentManager.getInstance(it).doPostponedOperationsAndUnblockDocument(document)
    }
}
