package com.linqingying.lsp.api

import com.intellij.openapi.vfs.VirtualFile

import java.util.*



interface LspServerManagerListener : EventListener {
    fun serverInitializationFailed() {}
    fun fileOpened(file: VirtualFile) {}
    fun diagnosticsReceived(file: VirtualFile) {}
}
