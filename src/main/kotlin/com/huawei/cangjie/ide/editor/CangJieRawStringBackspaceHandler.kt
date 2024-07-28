package com.huawei.cangjie.ide.editor

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.psi.CjStringTemplateExpression
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiFile

class CangJieRawStringBackspaceHandler : BackspaceHandlerDelegate() {


    private var rangeMarker: RangeMarker? = null

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        rangeMarker = null

        val offset = editor.caretModel.offset
        val psiElement = file.findElementAt(offset) ?: return


        if (c == '\'') {
            val prevText = file.findElementAt(offset - 1)?.text
            val currText = file.findElementAt(offset )?.text



            if (prevText == "\'" && currText == "\'") {
                rangeMarker = if (file.findElementAt(offset - 2)?.text == "r") {
                    editor.document.createRangeMarker(offset - 2, offset +1 )
                } else {
                    editor.document.createRangeMarker(offset - 1, offset +1 )

                }

            }

            return
        }

        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) {
            return
        }
        if (file.fileType != CangJieFileType) {
            return
        }


        psiElement.parent?.let {
            if (it is CjStringTemplateExpression && it.text == "\"\"\"\"\"\"") {
                if (editor.caretModel.offset == it.textOffset + 3) {
                    rangeMarker = editor.document.createRangeMarker(it.textRange)
                }
            } else if (it is CjStringTemplateExpression && it.text.startsWith("#") && it.text.endsWith("#")) {
//                #*n""#*n

//                #的数量
                val length = it.text.count { it == '#' } / 2 + 1
                if (editor.caretModel.offset == it.textOffset + length) {
                    rangeMarker = editor.document.createRangeMarker(it.textRange)
                }


            }
        }
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        rangeMarker?.let {
            editor.document.deleteString(it.startOffset, it.endOffset)
            rangeMarker = null
            return true
        }

        return false
    }
}
