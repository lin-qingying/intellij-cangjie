package com.huawei.cangjie.ide.completion.smart

import com.huawei.cangjie.ide.completion.shortenReferences
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement

class ArtificialElementInsertHandler(
    private val textBeforeCaret: String,
    private val textAfterCaret: String,
    private val shortenRefs: Boolean
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val offset = context.editor.caretModel.offset
        val startOffset = offset - item.lookupString.length
        context.document.deleteString(startOffset, offset) // delete inserted lookup string
        context.document.insertString(startOffset, textBeforeCaret + textAfterCaret)
        context.editor.caretModel.moveToOffset(startOffset + textBeforeCaret.length)

        if (shortenRefs) {
            shortenReferences(context, startOffset, startOffset + textBeforeCaret.length + textAfterCaret.length)
        }
    }
}




