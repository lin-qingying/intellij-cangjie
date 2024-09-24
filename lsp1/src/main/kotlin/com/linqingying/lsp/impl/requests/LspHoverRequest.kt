package com.linqingying.lsp.impl.requests

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile

import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.LspRequest
import com.linqingying.lsp.api.customization.requests.util.getLsp4jPosition
import com.linqingying.lsp.api.customization.requests.util.getRangeInDocument
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.MarkupContent
import java.util.concurrent.CompletableFuture

internal class LspHoverRequest(
    lspServer: LspServer,
    val file: VirtualFile,
    val document: Document,
    val offset: Int
) : LspRequest<Hover, TextRangeAndMarkupContent>(lspServer) {
    private val hoverParams: HoverParams = HoverParams(
        lspServer.requestExecutor.getDocumentIdentifier(file),
        getLsp4jPosition(document, offset)
    )

    override fun preprocessResponse(serverResponse: Hover): TextRangeAndMarkupContent {
        val range = serverResponse.range?.let { getRangeInDocument(document, it) } ?: TextRange(offset, offset)
        val content = serverResponse.contents
        return if (content.isRight) {
            val markupContent = content.right
            TextRangeAndMarkupContent(range, markupContent)
        } else {
            val leftContent = content.left

            val text = leftContent.joinToString(separator = "\n\n") {
                if (it.isRight) {
                    it.right.value
                } else {
                    it.left
                }
            }
//            val text = leftContent.joinToString(separator = "\n\n")
            val markupContent = MarkupContent("markdown", text)
            TextRangeAndMarkupContent(range, markupContent)
        }
    }

    override fun sendRequest(): CompletableFuture<Hover> =
        lspServer.lsp4jServer.textDocumentService.hover(hoverParams)


    override fun toString(): String {
        return "textDocument/hover"
    }
}


//class LspHoverRequest(
//    override val lspServer: LspServer,
//    file: VirtualFile,
//    document: Document,
//    offset: Int
//) :
//    LspRequest<Hover, MarkupContent>(lspServer) {
//
//
//    private val hoverParams: HoverParams = HoverParams(
//        lspServer.requestExecutor.getDocumentIdentifier(file),
//        getLsp4jPosition(document, offset)
//    )
//
//
//    override fun sendRequest(): CompletableFuture<Hover> =
//        lspServer.lsp4jServer.textDocumentService.hover(hoverParams)
//
//
//    override fun preprocessResponse(serverResponse: Hover): MarkupContent {
//        val contents = serverResponse.contents
//        return if (contents.isRight) {
//            contents.right as MarkupContent
//        } else {
//            val left = contents.left as? Iterable<*> ?: emptyList<MarkupContent>()
//            val joinedString = left.joinToString(separator = "\n\n")
//            MarkupContent("markdown", joinedString)
//        }
//    }
//
//
//}
