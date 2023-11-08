package com.linqingying.lsp.impl.requests

import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.LspRequest
import org.eclipse.lsp4j.CompletionItem
import java.util.concurrent.CompletableFuture

class LspResolveCompletionItemRequest(override val lspServer: com.linqingying.lsp.api.LspServer, val completionItem: CompletionItem) :
    LspRequest<CompletionItem, CompletionItem>(lspServer) {


    override fun sendRequest(): CompletableFuture<CompletionItem> =
        lspServer.lsp4jServer.textDocumentService.resolveCompletionItem(completionItem)


    override fun preprocessResponse(serverResponse: CompletionItem): CompletionItem = serverResponse

}

