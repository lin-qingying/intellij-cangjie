package com.linqingying.lsp.impl.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.template.TemplateManager
import com.linqingying.lsp.util.applyTextEdits
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.InsertTextFormat


const val END_VARIABLE: String = "\$END$"

val TEMPLATE_VARIABLE_REGEX: Regex = Regex("\\$\\{(\\d+):?([^{^}]*)}|\\$(\\d+)")

private const val VARIABLE_DEFAULT_VALUE_GROUP: Int = 2

const val VARIABLE_NAME_PREFIX: String = "VAR_"

val VARIABLE_NUMBER_GROUPS: IntArray = intArrayOf(1, 3)

private val LookupElement.lsp4jCompletionItem: CompletionItem?
    get() {
        val lspCompletionObject = this.`object` as? LspCompletionObject
        if (lspCompletionObject != null) {
            val completionItem = lspCompletionObject.completionItem
            return completionItem
        }

        val completionItem = this.`object` as? CompletionItem
        return completionItem
    }


object LspCompletionItemInsertHandler :
    InsertHandler<LookupElement> {

    private fun applyAdditionalTextEdits(
        context: InsertionContext,
        lookupElement: LookupElement
    ) {
        val completionItem = lookupElement.lsp4jCompletionItem
        val additionalTextEdits = completionItem?.additionalTextEdits
        if (additionalTextEdits != null) {
            val textEdit = completionItem.textEdit?.left?.range ?: completionItem.textEdit?.right?.insert
            if (textEdit != null) {
                val filteredTextEdits = additionalTextEdits.filter { edit ->
                    textEdit.start.line - edit.range.start.line > 0 || textEdit.start.character - edit.range.start.character > 0
                }
                val document = context.document
                applyTextEdits(document, filteredTextEdits)
            }
        }
    }

    override fun handleInsert(
        context: InsertionContext,
        lookupElement: LookupElement
    ) {
        applyAdditionalTextEdits(context, lookupElement)

        if (lookupElement is LookupElementDecorator<*>) {
            val delegate = lookupElement.delegate
            delegate?.handleInsert(context)
        }

        handleCodeInsertion(context, lookupElement)
    }

    private fun handleCodeInsertion(
        context: InsertionContext,
        lookupElement: LookupElement
    ) {
        val lspCompletionItem = lookupElement.lsp4jCompletionItem
        if (lspCompletionItem?.insertTextFormat == InsertTextFormat.Snippet) {
            context.document.replaceString(context.startOffset, context.tailOffset, "")
            val templateManager = TemplateManager.getInstance(context.project)
            val editor = context.editor
            val project = context.project

            val lookupString = lookupElement.lookupString

            val snippetToTemplateConverter = SnippetToTemplateConverter(project, lookupString)
            templateManager.runTemplate(editor, snippetToTemplateConverter.computeTemplate())
        }
    }
}

