package com.linqingying.lsp.impl.navigation

import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.LocationLink

  data class LspServerAndLocationLinks(
    val lspServer: LspServer,
    val locationLinks: List<LocationLink>
)
