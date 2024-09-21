package com.huawei.cangjie.psi.psiUtil

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType


fun Editor.moveCaret(offset: Int, scrollType: ScrollType = ScrollType.RELATIVE) {
    caretModel.moveToOffset(offset)
    scrollingModel.scrollToCaret(scrollType)
}
