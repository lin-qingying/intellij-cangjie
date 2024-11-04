package com.linqingying.cangjie.ide.completion.handlers

import com.linqingying.cangjie.utils.CallType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager



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
