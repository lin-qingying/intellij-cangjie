// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.linqingying.lsp.api.requests

import com.linqingying.lsp.api.LspServer


abstract class LspClientNotification(val lspServer: LspServer) {

  abstract fun sendNotification()
}
