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

package cn.cangnova.cangjie.ide.editor

import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement


class CangJieRawStringTypedHandler : TypedHandlerDelegate() {
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        // A quote is typed after 2 other quotes
        val offset = editor.caretModel.offset


//        if(c == '\''){
//
//
//
//            val prevText = file.findElementAt(offset - 1)?.text
//
//            if(prevText!= "\'"){
//
//                if(prevText == "r"){
//                    editor.document.insertString(offset, "''")
//                    editor.caretModel.currentCaret.moveToOffset(offset + 1)
//
//                }else{
//                    editor.document.insertString(offset, "r''")
//                    editor.caretModel.currentCaret.moveToOffset(offset +2 )
//
//
//                }
//
//            }
//
//
//
//            return Result.STOP
//        }
        if (c != '"' && c != '\'') {
            return Result.CONTINUE
        }
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) {
            return Result.CONTINUE
        }
        if (file !is CjFile) {
            return Result.CONTINUE
        }



        if (offset < 2) {
            return Result.CONTINUE
        }
        val openQue = if (c == '"') {
            "\""
        } else {
            "\'"
        }

        if (file.findElementAt(offset - 1)?.text == "#") {
            var length = 1

            while (file.findElementAt(offset - (length + 1))?.text == "#") {
                length++
            }

            val str = StringBuilder().append(openQue).append(openQue).apply {
                for (i in 0 until length)
                    append("#")
            }
            editor.document.insertString(offset, str.toString())
            editor.caretModel.currentCaret.moveToOffset(offset + 1)

            return Result.STOP
        }


        val openQuote = file.findElementAt(offset - 2)
        if (openQuote == null || openQuote !is LeafPsiElement || openQuote.elementType != CjTokens.OPEN_QUOTE) {
            return Result.CONTINUE
        }

        val closeQuote = file.findElementAt(offset - 1)
        if (closeQuote == null || closeQuote !is LeafPsiElement || closeQuote.elementType != CjTokens.CLOSING_QUOTE) {
            return Result.CONTINUE
        }

        if (closeQuote.text != "\"" && closeQuote.text != "'")  {
            // Check it is not a multi-line quote
            return Result.CONTINUE
        }



        editor.document.insertString(offset, "$openQue\n$openQue$openQue$openQue")
        editor.caretModel.currentCaret.moveToOffset(offset + 1)

        return Result.STOP
    }
}
