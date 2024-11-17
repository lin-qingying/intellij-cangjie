/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.ide.editor

import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.psi.CjStringTemplateExpression
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


//        if (c == '\'') {
//            val prevText = file.findElementAt(offset - 1)?.text
//            val currText = file.findElementAt(offset )?.text
//
//
//
//            if (prevText == "\'" && currText == "\'") {
//                rangeMarker = if (file.findElementAt(offset - 2)?.text == "r") {
//                    editor.document.createRangeMarker(offset - 2, offset +1 )
//                } else {
//                    editor.document.createRangeMarker(offset - 1, offset +1 )
//
//                }
//
//            }
//
//            return
//        }

        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) {
            return
        }
        if (file.fileType != CangJieFileType.INSTANCE) {
            return
        }

//
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
