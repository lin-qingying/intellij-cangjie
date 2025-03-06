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

import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lexer.CjTokens
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.PsiFile

class CangJieStringTemplateBackspaceHandler : BackspaceHandlerDelegate()  {

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        if (c != '{' || file.fileType != CangJieFileType.INSTANCE || !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return

        val offset = editor.caretModel.offset

        val highlighter = (editor as EditorEx).highlighter
        val iterator = highlighter.createIterator(offset)
        if (iterator.tokenType != CjTokens.LONG_TEMPLATE_ENTRY_END) return
        iterator.retreat()
        if (iterator.tokenType != CjTokens.LONG_TEMPLATE_ENTRY_START) return
        editor.document.deleteString(offset, offset + 1)
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        return false
    }
}
