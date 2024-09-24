package com.linqingying.lsp.api.requests

import com.linqingying.lsp.api.LspServer


abstract class LspClientNotification(open val  lspServer: LspServer) {
    /**
     * Typical implementation:
     *
     *     lspServer.lsp4jServer.foo(...)
     */
    abstract fun sendNotification()
}
