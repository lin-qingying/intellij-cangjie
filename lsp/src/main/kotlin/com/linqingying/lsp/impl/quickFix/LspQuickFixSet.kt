/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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
                            requireNotNull(quickFix as? LspQuickFixWrapper) { "null cannot be cast to non-null type com.linqingying.lsp.impl.quickFix.LspQuickFixWrapper" }
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

