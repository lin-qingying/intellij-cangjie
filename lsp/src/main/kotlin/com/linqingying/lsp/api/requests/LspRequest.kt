package com.linqingying.lsp.api.customization.requests



import com.linqingying.lsp.api.LspServer
import java.util.concurrent.CompletableFuture


/**
 * A specific [request](https://microsoft.github.io/language-server-protocol/specification/#requestMessage) that can be sent to
 * the LSP server. This class can also (optionally) preprocess the received response, i.e., translate the object received from the `lsp4j`
 * library to some other object, which is more convenient to work with.
 *
 * @param <ServerResponse> object created by the `lsp4j` library, which encapsulates the response from the LSP server
 * @param <Result> any object based on the received [ServerResponse], or simply the [ServerResponse] itself
 */
abstract class LspRequest<ServerResponse, Result>(open val lspServer: com.linqingying.lsp.api.LspServer) {
    override fun toString(): String = javaClass.simpleName // for logging

    /**
     * Typical implementation:
     *
     *     lspServer.lsp4jServer.foo(...)
     */
    abstract fun sendRequest(): CompletableFuture<ServerResponse>

    /**
     * Objects received from the `lsp4j` library are not always convenient to work with. So, this function can
     * optionally translate the received [ServerResponse] into any other object, which is more convenient for further usage.
     */
    abstract fun preprocessResponse(serverResponse: ServerResponse): Result
}
