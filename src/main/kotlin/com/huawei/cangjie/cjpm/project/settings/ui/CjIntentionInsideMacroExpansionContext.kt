package com.huawei.cangjie.ide.project.settings.ui


import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiFile

class CjIntentionInsideMacroExpansionContext(
    val originalFile: PsiFile,
    val documentCopy: Document,
//    val rangeMap: RangeMap,
    val rootMacroBodyRange: RangeMarker,
    val changedRanges: MutableList<RangeMarker> = mutableListOf(),
    var finished: Boolean = false,
    var broken: Boolean = false,
    var applyChangesToOriginalDoc: Boolean = true,
) {
    val rootMacroCallBodyOffset: Int get() = rootMacroBodyRange.startOffset
}
