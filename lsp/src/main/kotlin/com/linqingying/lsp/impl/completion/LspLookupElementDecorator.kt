package com.linqingying.lsp.impl.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.linqingying.lsp.impl.LspServerImpl

import org.eclipse.lsp4j.CompletionItem

class LspLookupElementDecorator(
    lspServer: LspServerImpl,
    lookupElement: LookupElement,
    completionItem: CompletionItem
) : LookupElementDecorator<LookupElement>(lookupElement) {
    private val lspCompletionObject: LspCompletionObject =
        LspCompletionObject(lspServer, completionItem)

    override fun getDecoratorInsertHandler(): com.intellij.codeInsight.completion.InsertHandler<LookupElement> {
        return LspCompletionItemInsertHandler

    }

    override fun getExpensiveRenderer(): LookupElementRenderer<LspLookupElementDecorator> {
        return LspExpensiveRenderer

    }

    override fun renderElement(presentation: LookupElementPresentation) {
        LspInstantRenderer.renderLookupElement(
            this.lspCompletionObject, presentation
        )

    }

    override fun getObject(): LspCompletionObject {
        return this.lspCompletionObject

    }

    private object LspInstantRenderer {
        fun renderLookupElement(
            completionObject: LspCompletionObject,
            presentation: LookupElementPresentation
        ) {
            completionObject.lspServer.descriptor.lspCompletionSupport?.let { completionSupport ->
                val completionItem = completionObject.completionItem
                completionSupport.renderLookupElement(completionItem, presentation)
            }
        }
    }

    private object LspExpensiveRenderer :
        LookupElementRenderer<LspLookupElementDecorator>() {
        @RequiresBackgroundThread
        override fun renderElement(
            element: LspLookupElementDecorator,
            presentation: LookupElementPresentation
        ) {
            val completionObject = element.lspCompletionObject
            completionObject.resolveCompletionItem()
            LspInstantRenderer.renderLookupElement(completionObject, presentation)

        }
    }

}

