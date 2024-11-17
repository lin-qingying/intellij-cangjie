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

import com.linqingying.cangjie.ide.editor.LtGtTypingUtils.isAfterFunckeyword
import com.linqingying.cangjie.ide.editor.LtGtTypingUtils.isAfterToken
import com.linqingying.cangjie.lexer.CjTokens.IDENTIFIER
import com.linqingying.cangjie.psi.CjFile
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class CangJieBackspaceHandlerDelegate : BackspaceHandlerDelegate() {
    private var deleteGt = false
    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {

        val offset = editor.caretModel.offset - 1


        deleteGt = c == '<' && file is CjFile && (isAfterFunckeyword(
            offset,
            editor
        ) || isAfterToken(offset, editor, IDENTIFIER))

    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        val offset = editor.caretModel.offset
        val chars = editor.document.charsSequence
        if (editor.document.textLength <= offset) return false //文件结束后的虚拟空间


        val c1 = chars[offset]
        if (c == '<' && deleteGt) {
            if (c1 == '>') {
                LtGtTypingUtils.handleCangJieLTDeletion(editor, offset)
            }
            return true
        }

        return false
    }
}
