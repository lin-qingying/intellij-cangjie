package com.linqingying.lsp.impl.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator

import com.linqingying.lsp.impl.LspServerImpl
import org.eclipse.lsp4j.CompletionItem

class LspLookupElementDecorator(
    lspServer: LspServerImpl,
    lookupElement: LookupElement,
    completionItem: CompletionItem
) : LookupElementDecorator<LookupElement>(lookupElement) {
    private val lspCompletionObject: LspCompletionObject =
        LspCompletionObject(lspServer, completionItem)

    override fun getDecoratorInsertHandler(): com.intellij.codeInsight.completion.InsertHandler<LookupElement> { /* compiled code */
        return LspCompletionItemInsertHandler

    }

    override fun getObject(): LspCompletionObject {
        return this.lspCompletionObject

    }
}

