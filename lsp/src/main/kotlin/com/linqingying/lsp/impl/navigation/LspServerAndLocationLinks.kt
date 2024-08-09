package com.linqingying.lsp.impl.navigation

import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.LocationLink

class LspServerAndLocationLinks(val lspServer: LspServer, val locationLinks: List<LocationLink>) {
    operator fun component1(): LspServer = lspServer

    operator fun component2(): List<LocationLink> = locationLinks

    override operator fun equals(other: Any?): Boolean {

        if (this === other) return true
        if (other !is LspServerAndLocationLinks) return false

        if (lspServer != other.lspServer) return false
        if (locationLinks != other.locationLinks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lspServer.hashCode()
        result = 31 * result + locationLinks.hashCode()
        return result
    }

    override fun toString(): String = "LspServerAndLocationLinks(lspServer=$lspServer, locationLinks=$locationLinks)"

}
