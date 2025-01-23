package com.linqingying.lsp.api.customization


import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Diagnostic



/**
 * Handles [CodeAction](https://microsoft.github.io/language-server-protocol/specification#codeAction) objects received from the LSP server.
 */

open class LspCodeActionsSupport {

    open val intentionActionsSupport: Boolean = true

    open val quickFixesSupport: Boolean = true

    open fun createIntentionAction(
        lspServer: LspServer,
        codeAction: CodeAction
    ): LspIntentionAction? {
        return LspIntentionAction(lspServer, codeAction)

    }


    /**
     * Creates [LspIntentionAction] for the specific [CodeAction]. Implementations may return `null` if they don't want to provide a quick fix
     * for this [codeAction].
     *
     * @param codeAction result of the
     * [textDocument/codeAction](https://microsoft.github.io/language-server-protocol/specification/#textDocument_codeAction) request to the
     * LSP server, which asked for quick fixes for a specific [Diagnostic] (see [CodeAction.diagnostics]).
     */
    open fun createQuickFix(lspServer: LspServer, codeAction: CodeAction): LspIntentionAction? {
        return LspIntentionAction(lspServer, codeAction)
    }
}
