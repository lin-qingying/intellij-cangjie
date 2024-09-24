package com.linqingying.lsp.impl.quickFix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.util.application
import com.linqingying.lsp.api.customization.LspIntentionAction
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.intention.asCodeAction
import org.eclipse.lsp4j.*


class LspQuickFixSet(
    val lspServer: LspServerImpl,
    val file: VirtualFile,
    val diagnostic: Diagnostic
) {
    private val MAX_QUICK_FIXES: Int = 8


    private var psiModCountWhenRequestSent: Long = 0

    val quickFixes: List<IntentionAction>

    private var vfsModCountWhenRequestSent: Long = 0


    init {

        val list = mutableListOf<LspQuickFixWrapper>()

        for (i in 0 until MAX_QUICK_FIXES) {
            list.add(LspQuickFixWrapper(this, i))
        }
        quickFixes = list
    }

    companion object {
        val LOG = Logger.getInstance(LspQuickFixSet::class.java)
    }

    private fun processIntentionActions(codeActions: List<CodeAction>) {

        lspServer.descriptor.lspCodeActionsSupport?.let { support ->
            val minSize = minOf(codeActions.size, quickFixes.size)
            for (i in 0 until minSize) {
                val action = support.createQuickFix(lspServer, codeActions[i])
                (quickFixes[i] as? LspQuickFixWrapper)?.lspIntentionAction = action
            }
            if (codeActions.size > quickFixes.size) {
                LOG.info(
                    "Received ${codeActions.size} quick fixes from server, only ${quickFixes.size} will be handled"
                )
            }
        }

    }

    internal fun ensureInitialized() {
        if (!application.isDispatchThread) {
            synchronized(this) {
                try {
                    ProgressManager.checkCanceled()
                    val currentPsiModCount =
                        PsiManager.getInstance(lspServer.project).modificationTracker.modificationCount
                    val currentVfsModCount = VirtualFileManager.getInstance().modificationCount

                    if (psiModCountWhenRequestSent != currentPsiModCount || vfsModCountWhenRequestSent != currentVfsModCount) {
                        quickFixes.forEach { quickFix ->
                            requireNotNull(quickFix as? LspQuickFixWrapper) { "null cannot be cast to non-null type com.intellij.platform.lsp.impl.quickFix.LspQuickFixWrapper" }
                            (quickFix as LspQuickFixWrapper).lspIntentionAction = null
                        }

                        val documentIdentifier = lspServer.getDocumentIdentifier(file)
                        val range = diagnostic.range
                        val codeActionContext = CodeActionContext().apply {
                            only = listOf("quickfix")
                            diagnostics = listOf(diagnostic)
                            triggerKind = CodeActionTriggerKind.Automatic
                        }

                        val codeActionParams = CodeActionParams(documentIdentifier, range, codeActionContext)

                        lspServer.requestExecutor.sendRequestAsyncButWaitForResponseWithCheckCanceled({ languageServer ->
                            languageServer.textDocumentService.codeAction(codeActionParams)
                        }) { lsp4jResults ->
                            if (currentPsiModCount == PsiManager.getInstance(lspServer.project).modificationTracker.modificationCount &&
                                currentVfsModCount == VirtualFileManager.getInstance().modificationCount
                            ) {
                                psiModCountWhenRequestSent = currentPsiModCount
                                vfsModCountWhenRequestSent = currentVfsModCount

                                lsp4jResults?.let { results ->
                                    val intentionActions = results.map { either ->
                                        requireNotNull(either) // Ensure non-null
                                        either.asCodeAction()
                                    }
                                    processIntentionActions(intentionActions)
                                }
                            }
                        }
                    }
                } finally {
                    // Any necessary cleanup can be done here
                }
            }
        }
    }
}

private class LspQuickFixWrapper(
    val quickFixSet: LspQuickFixSet,
    index: Int
) : LspIntentionActionWrapperBase(index) {
    override var lspIntentionAction: LspIntentionAction? = null


}

