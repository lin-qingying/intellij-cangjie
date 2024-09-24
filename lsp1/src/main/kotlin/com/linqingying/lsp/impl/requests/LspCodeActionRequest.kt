package com.linqingying.lsp.impl.requests

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.LspRequest
import com.linqingying.lsp.api.customization.requests.util.getLsp4jRange
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

internal class LspCodeActionRequest private constructor(
    lspServer: LspServer,
    val codeActionParams: CodeActionParams
) : LspRequest<List<Either<Command, CodeAction>>, List<CodeAction>>(lspServer) {
    constructor(
        lspServer: LspServer,
        file: VirtualFile,
        diagnostic: Diagnostic
    ) : this(
        lspServer,
        CodeActionParams(
            lspServer.requestExecutor.getDocumentIdentifier(file),
            diagnostic.range,
            CodeActionContext().apply {
                only = listOf("quickfix")
                diagnostics = listOf(diagnostic)
                triggerKind = CodeActionTriggerKind.Automatic
            })
    )

    constructor(
        lspServer: LspServer,
        file: VirtualFile,
        document: Document,
        startOffset: Int,
        length: Int
    ) : this(
        lspServer,
        CodeActionParams(
            lspServer.requestExecutor.getDocumentIdentifier(file),
            getLsp4jRange(document, startOffset, length),
            CodeActionContext().apply {
                diagnostics = emptyList<Diagnostic>()
                triggerKind = CodeActionTriggerKind.Automatic
            }
        )
    )


    override fun preprocessResponse(serverResponse: List<Either<Command, CodeAction>>): List<CodeAction> { /* compiled code */

        if (serverResponse.isEmpty()) {
            return emptyList()
        } else {
            val processedResponse = ArrayList<CodeAction>(serverResponse.size)
            for (item in serverResponse) {
                val codeAction = if (item.isLeft) {
                    val command = item.left
                    CodeAction().apply {
                        title = command.title
                        this.command = command
                        diagnostics = codeActionParams.context.diagnostics
                    }
                } else {
                    item.right as CodeAction
                }
                processedResponse.add(codeAction)
            }
            return processedResponse
        }
    }


    override fun sendRequest(): CompletableFuture<List<Either<Command, CodeAction>>> =
        lspServer.lsp4jServer.textDocumentService.codeAction(codeActionParams)

    override fun toString(): String = "textDocument/codeAction"
}

//
//class LspCodeActionRequest(
//    override val lspServer: LspServer,
//    file: VirtualFile,
//    diagnostic: Diagnostic
//) :
//    LspRequest<List<Either<Command, CodeAction>>, List<CodeAction>>(lspServer) {
//
//
//    private val codeActionParams: CodeActionParams
//
//
//    init {
//        val codeActionContext = CodeActionContext()
//        codeActionContext.diagnostics = listOf(diagnostic)
//        codeActionContext.only = listOf(CodeActionKind.QuickFix)
//        codeActionContext.triggerKind = CodeActionTriggerKind.Automatic
//
//
//        codeActionParams =
//            CodeActionParams(lspServer.requestExecutor.getDocumentIdentifier(file), diagnostic.range, codeActionContext)
//    }
//
//    override fun sendRequest(): CompletableFuture<List<Either<Command, CodeAction>>> =
//        lspServer.lsp4jServer.textDocumentService.codeAction(codeActionParams)
//
//
//    override fun preprocessResponse(serverResponse: List<Either<Command, CodeAction>>): List<CodeAction> {
//        if (serverResponse.isEmpty()) {
//            return emptyList()
//        } else {
//            val processedResponse = ArrayList<CodeAction>(serverResponse.size)
//            for (item in serverResponse) {
//                val codeAction = if (item.isLeft) {
//                    val command = item.left
//                    CodeAction().apply {
//                        title = command.title
//                        this.command = command
//                        diagnostics = codeActionParams.context.diagnostics
//                    }
//                } else {
//                    item.right as CodeAction
//                }
//                processedResponse.add(codeAction)
//            }
//            return processedResponse
//        }
//    }
//
//
//}
