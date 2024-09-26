
package com.linqingying.lsp.api

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import java.util.*


interface LspServerManagerListener : EventListener {
  fun serverStateChanged(lspServer: LspServer) {}
  fun fileOpened(lspServer: LspServer, file: VirtualFile) {}
  fun diagnosticsReceived(lspServer: LspServer, file: VirtualFile) {}
}
