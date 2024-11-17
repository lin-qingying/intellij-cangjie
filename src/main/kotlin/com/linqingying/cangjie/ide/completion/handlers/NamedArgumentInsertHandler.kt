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

package com.linqingying.cangjie.ide.completion.handlers

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjValueArgument
import com.linqingying.cangjie.psi.CjValueArgumentList
import com.linqingying.cangjie.psi.psiUtil.firstIsInstanceOrNull
import com.linqingying.cangjie.psi.psiUtil.parentsWithSelf
import com.linqingying.cangjie.psi.psiUtil.siblings
import com.linqingying.cangjie.renderer.render
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace

class NamedArgumentInsertHandler(private val parameterName: Name) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor

        val (textAfterCompletionArea, doNeedTrailingSpace) = context.file.findElementAt(context.tailOffset).let { psi ->
            psi?.siblings()?.firstOrNull { it !is PsiWhiteSpace }?.text to (psi !is PsiWhiteSpace)
        }

        var text: String
        var caretOffset: Int
        if (textAfterCompletionArea == "=") {
            // User tries to manually rename existing named argument. We shouldn't add trailing `=` in such case
            text = parameterName.render()
            caretOffset = text.length
        } else {
            // For complicated cases let's try to normalize the document firstly in order to avoid parsing errors due to incomplete code
            editor.document.replaceString(context.startOffset, context.tailOffset, "")
            PsiDocumentManager.getInstance(context.project).commitDocument(editor.document)

            val nextArgument = context.file.findElementAt(context.startOffset)?.siblings()
                ?.firstOrNull { it !is PsiWhiteSpace }?.parentsWithSelf?.takeWhile { it !is CjValueArgumentList }
                ?.firstIsInstanceOrNull<CjValueArgument>()

            if (nextArgument?.isNamed() == true) {
                if (doNeedTrailingSpace) {
                    text = "${parameterName.render()} = , "
                    caretOffset = text.length - 2
                } else {
                    text = "${parameterName.render()} = ,"
                    caretOffset = text.length - 1
                }
            } else {
                text = "${parameterName.render()} = "
                caretOffset = text.length
            }
        }

        if (context.file.findElementAt(context.startOffset - 1)?.let { it !is PsiWhiteSpace && it.text != "(" } == true) {
            text = " $text"
            caretOffset++
        }

        editor.document.replaceString(context.startOffset, context.tailOffset, text)
        editor.caretModel.moveToOffset(context.startOffset + caretOffset)
    }
}
