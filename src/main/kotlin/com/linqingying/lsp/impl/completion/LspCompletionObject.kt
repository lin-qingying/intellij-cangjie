package com.linqingying.lsp.impl.completion

import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.linqingying.lsp.api.customization.requests.util.convertMarkupContentToHtml
import com.linqingying.lsp.impl.LspServerImpl
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.jsonrpc.messages.Either


class LspCompletionObject(
    val lspServer: LspServerImpl,
    val completionItem: CompletionItem
) : Pointer<LspCompletionObject>,
    Symbol, DocumentationTarget {


    private var resolvedCompletionItem: CompletionItem? = null

    override fun computeDocumentation(): DocumentationResult? {
        val lsp4jDocs = completionItem.documentation
        if (lsp4jDocs != null) {
            return V(lsp4jDocs)
        } else {
            val lsp4jDocs2 = resolvedCompletionItem?.documentation
            if (lsp4jDocs2 != null) {
                return V(lsp4jDocs2)
            }

            if (resolvedCompletionItem != null) {
                return null
            } else {
                val completionProvider = lspServer.getServerCapabilities()?.completionProvider
                val resolveProvider = completionProvider?.resolveProvider
                return if (resolveProvider == true) {
                    DocumentationResult.asyncDocumentation {
                        this@LspCompletionObject.resolvedCompletionItem =
                            lspServer.requestExecutor
                                .getResolvedCompletionItem(this@LspCompletionObject.completionItem)

                        val item = this@LspCompletionObject.resolvedCompletionItem

                        if (item != null) {
                            val documentation = item.documentation
                            if (documentation != null) {


                                return@asyncDocumentation V(documentation)

                            }
                        }
                        return@asyncDocumentation null


                    }
                } else {
                    null
                }
            }
        }
    }

    override fun computePresentation(): TargetPresentation {
        val label = completionItem.label
        return TargetPresentation.builder(label).presentation()
    }

    override fun createPointer(): Pointer<LspCompletionObject> = Pointer.hardPointer(this)

    override fun dereference(): LspCompletionObject = this

    private fun V(lsp4jDocs: Either<String, MarkupContent>): DocumentationResult.Documentation? { /* compiled code */

        if (lsp4jDocs.isLeft) {
            val left = lsp4jDocs.left as String
            if (left.isNotEmpty()) {
                val html = HtmlBuilder().append(left).toString()
                return DocumentationResult.documentation(html)
            }
        }

        if (lsp4jDocs.isRight) {
            val right = (lsp4jDocs.right as MarkupContent).value
            if (right.isNotEmpty()) {
                val html = convertMarkupContentToHtml(lsp4jDocs.right as MarkupContent)
                return DocumentationResult.documentation(html)
            }
        }

        return null
    }
}

