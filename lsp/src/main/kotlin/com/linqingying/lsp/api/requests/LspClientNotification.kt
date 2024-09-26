
package com.linqingying.lsp.api.requests

import com.linqingying.lsp.api.LspServer


abstract class LspClientNotification(val lspServer: LspServer) {

  abstract fun sendNotification()
}
