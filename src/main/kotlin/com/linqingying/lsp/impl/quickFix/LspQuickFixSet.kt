package com.linqingying.lsp.impl.quickFix

import com.linqingying.lsp.api.LspServer
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.requests.LspCodeActionRequest
import com.linqingying.lsp.impl.requests.LspRequestExecutorImpl
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Diagnostic

class LspQuickFixSet(val lspServer: LspServerImpl, val file: VirtualFile, val diagnostic: Diagnostic) {
    val quickFixes: List<IntentionAction> = emptyList()

    val MAX_QUICK_FIXES: Int = 8

    companion object {
        val LOG = Logger.getInstance(LspQuickFixSet::class.java)
    }


    var psiModCountWhenRequestSent: Long? = null


    var vfsModCountWhenRequestSent: Long? = null

    private fun processCodeActions(codeActions: List<CodeAction>) {

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
        if (!ApplicationManager.getApplication().isDispatchThread) {
            ProgressManager.checkCanceled()
            val psiModCount = PsiManager.getInstance(lspServer.project).modificationTracker.modificationCount
            val vfsModCount = VirtualFileManager.getInstance().modificationCount
            if (psiModCountWhenRequestSent != psiModCount || vfsModCountWhenRequestSent != vfsModCount) {
                quickFixes.forEach { quickFix ->
                    (quickFix as? LspQuickFixWrapper)?.lspIntentionAction = null
                }
                val request = LspCodeActionRequest(lspServer, file, diagnostic)
                (lspServer.requestExecutor as LspRequestExecutorImpl).sendRequestAsyncButWaitForResponseWithCheckCanceled(
                    request
                ) { codeActions ->
                    if (psiModCount == PsiManager.getInstance(lspServer.project).modificationTracker.modificationCount &&
                        vfsModCount == VirtualFileManager.getInstance().modificationCount
                    ) {
                        psiModCountWhenRequestSent = psiModCount
                        vfsModCountWhenRequestSent = vfsModCount
                        codeActions?.let { processCodeActions(it) }
                    }
                }
            }
        }
    }
}
