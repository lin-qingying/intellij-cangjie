package com.linqingying.cangjie.ide.completion.handlers

import com.linqingying.cangjie.ide.completion.DeclarationLookupObject
import com.linqingying.cangjie.renderer.render
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement

open class BaseDeclarationInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val name = (item.`object` as? DeclarationLookupObject)?.name
        if (name != null && !name.isSpecial) {
            val startOffset = context.startOffset
            if (startOffset > 0 && context.document.isTextAt(startOffset - 1, "`")) {
                context.document.deleteString(startOffset - 1, startOffset)
            }
            context.document.replaceString(context.startOffset, context.tailOffset, name.render())
        }
    }
}
