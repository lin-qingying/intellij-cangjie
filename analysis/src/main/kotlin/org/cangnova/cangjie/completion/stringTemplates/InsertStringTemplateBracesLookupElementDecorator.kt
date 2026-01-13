/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion.stringTemplates

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.psiUtil.startOffset
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile


fun wrapLookupElementForStringTemplateAfterDotCompletion(originLookupElement: LookupElement): LookupElement {
    return LookupElementDecorator.withDelegateInsertHandler(originLookupElement) { insertionContext: InsertionContext, lookupElement: LookupElement ->
        insertStringTemplateBraces(insertionContext, lookupElement)
    }
}

private fun insertStringTemplateBraces(insertionContext: InsertionContext, lookupElement: LookupElement) {
    val document = insertionContext.document
    val startOffset = insertionContext.startOffset

    val psiDocumentManager = PsiDocumentManager.getInstance(insertionContext.project)
    psiDocumentManager.commitAllDocuments()

    val token = getToken(insertionContext.file, document.charsSequence, startOffset)
    val nameRef = token.parent as CjNameReferenceExpression

    document.insertString(nameRef.startOffset, "{")

    val tailOffset = insertionContext.tailOffset
    document.insertString(tailOffset, "}")
    insertionContext.tailOffset = tailOffset

    lookupElement.handleInsert(insertionContext)
}
private fun getToken(file: PsiFile, charsSequence: CharSequence, startOffset: Int): PsiElement {
    assert(startOffset > 1 && charsSequence[startOffset - 1] == '.')
    val token = file.findElementAt(startOffset - 2)!!
    return if (token.node.elementType == CjTokens.IDENTIFIER || token.node.elementType == CjTokens.THIS_KEYWORD)
        token
    else
        getToken(file, charsSequence, token.startOffset + 1)
}
