package com.linqingying.lsp.api

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import java.util.*


@ApiStatus.Internal
interface LspServerManagerListener : EventListener {
    fun serverInitializationFailed() {}
    fun fileOpened(file: VirtualFile) {}
    fun diagnosticsReceived(file: VirtualFile) {}
}
