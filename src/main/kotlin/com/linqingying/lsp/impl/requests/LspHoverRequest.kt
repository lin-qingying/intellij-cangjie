package com.linqingying.lsp.impl.requests

import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.LspRequest
import com.linqingying.lsp.api.customization.requests.util.getLsp4jPosition
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.MarkupContent
import java.util.concurrent.CompletableFuture

class LspHoverRequest(override val lspServer: com.linqingying.lsp.api.LspServer, file: VirtualFile, document: Document, offset: Int) :
    LspRequest<Hover, MarkupContent>(lspServer) {


    private val hoverParams: HoverParams = HoverParams(
        lspServer.requestExecutor.getDocumentIdentifier(file),
        getLsp4jPosition(document, offset)
    )


    override fun sendRequest(): CompletableFuture<Hover> =
        lspServer.lsp4jServer.textDocumentService.hover(hoverParams)


    override fun preprocessResponse(serverResponse: Hover): MarkupContent {
        val contents = serverResponse.contents
        return if (contents.isRight) {
            contents.right as MarkupContent
        } else {
            val left = contents.left as? Iterable<*> ?: emptyList<MarkupContent>()
            val joinedString = left.joinToString(separator = "\n\n")
            MarkupContent("markdown", joinedString)
        }
    }


}
