package com.linqingying.lsp.impl.requests

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.lsp.api.customization.requests.LspRequest
import com.linqingying.lsp.api.customization.requests.util.getLsp4jPosition
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

class LspDefinitionRequest(override val lspServer: com.linqingying.lsp.api.LspServer, file: VirtualFile, document: Document, offset: Int) :
    LspRequest<Either<List<Location>, List<LocationLink>>, List<LocationLink>>(lspServer) {

    private val definitionParams: DefinitionParams = DefinitionParams(
        lspServer.requestExecutor.getDocumentIdentifier(file),
        getLsp4jPosition(document, offset)
    )

    override fun sendRequest(): CompletableFuture<Either<List<Location>, List<LocationLink>>> =
        lspServer.lsp4jServer.textDocumentService.definition(definitionParams)
    override fun toString(): String ="textDocument/definition"

    override fun preprocessResponse(serverResponse: Either<List<Location>, List<LocationLink>>): List<LocationLink> {
        return if (serverResponse.isRight) {
            serverResponse.right?.distinct() ?: emptyList()
        } else {
            val left = serverResponse.left as? Iterable<Location> ?: emptyList()
            val processedResponse = left.map { LocationLink(it.uri, it.range, it.range) }
            processedResponse.distinct()
        }
    }

}
