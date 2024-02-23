package com.linqingying.lsp.api.customization

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
open class LspFormattingSupport() {
    @RequiresEdt
    open fun shouldFormatThisFileExclusivelyByServer(
        file: VirtualFile,
        ideCanFormatThisFileItself: Boolean,
        serverExplicitlyWantsToFormatThisFile: Boolean
    ): Boolean {
        return !ideCanFormatThisFileItself && serverExplicitlyWantsToFormatThisFile

    }
}

