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

package org.cangnova.cangjie.completion.handlers

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.resolve.calls.util.CallType


class CangJiePropertyInsertHandler(callType: CallType<*>) : CangJieCallableInsertHandler(callType) {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val surroundedWithBraces = surroundWithBracesIfInStringTemplate(context)

        super.handleInsert(context, item)

        if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) {
            deleteEmptyParenthesis(context)
        }

        if (surroundedWithBraces) {
            removeRedundantBracesInStringTemplate(context)
        }
    }

    private fun deleteEmptyParenthesis(context: InsertionContext) {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitDocument(context.document)
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

        val offset = context.tailOffset
        val document = context.document
        val chars = document.charsSequence

        val lParenOffset = chars.indexOfSkippingSpace('(', offset) ?: return
        val rParenOffset = chars.indexOfSkippingSpace(')', lParenOffset + 1) ?: return

        document.deleteString(offset, rParenOffset + 1)
    }
}
