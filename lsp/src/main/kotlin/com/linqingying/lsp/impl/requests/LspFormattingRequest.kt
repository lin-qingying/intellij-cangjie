package com.linqingying.lsp.impl.requests

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.LspRequest
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.TextEdit
import java.util.concurrent.CompletableFuture

internal class LspFormattingRequest(
    lspServer: LspServer,
    file: VirtualFile,
    codeStyleSettings: CodeStyleSettings
) : LspRequest<List<TextEdit>, List<TextEdit>>(lspServer) {
    private val params: DocumentFormattingParams = DocumentFormattingParams(
        lspServer.requestExecutor.getDocumentIdentifier(
            file
        ), FormattingOptions().apply {
            tabSize = codeStyleSettings.getIndentSize(file.fileType)
            isInsertSpaces = !codeStyleSettings.useTabCharacter(file.fileType)
        }

    )


    override fun preprocessResponse(serverResponse: List<TextEdit>): List<TextEdit> = serverResponse
    override fun sendRequest(): CompletableFuture<List<TextEdit>> =
        lspServer.lsp4jServer.textDocumentService.formatting(this.params)

    override fun toString(): String = "textDocument/formatting"
}
