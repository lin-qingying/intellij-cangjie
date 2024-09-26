
package com.linqingying.lsp.api.requests

import com.linqingying.lsp.api.LspServer
import java.util.concurrent.CompletableFuture


abstract class LspRequest<Lsp4jResponse, Result>(val lspServer: LspServer) {
  override fun toString(): String = javaClass.simpleName // for logging


  abstract fun sendRequest(): CompletableFuture<Lsp4jResponse>


  abstract fun preprocessResponse(lsp4jResponse: Lsp4jResponse): Result
}
