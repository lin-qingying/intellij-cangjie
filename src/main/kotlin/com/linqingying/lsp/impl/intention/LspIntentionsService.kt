package com.linqingying.lsp.impl.intention

import com.google.common.collect.ImmutableList
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.linqingying.lsp.api.customization.LspCodeActionsSupport
import com.linqingying.lsp.api.customization.LspIntentionAction
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.impl.requests.LspCodeActionRequest
import com.linqingying.lsp.impl.requests.LspRequestExecutorImpl
import org.eclipse.lsp4j.CodeAction

@Service
internal class LspIntentionsService {
    companion object {
        fun getInstance(project: Project): LspIntentionsService {
            return project.getService(LspIntentionsService::class.java)
                ?: throw IllegalStateException("Cannot find application-level service ${LspIntentionsService::class.java.name}")
        }
    }

    private var lastFilePath: String = ""

    private var lastIntentionActions: List<LspIntentionAction> = emptyList()

    private var lastLength: Int = -1

    private var lastOffset: Int = -1

    private var lastPsiModificationCount: Long = -1L

    fun getIntentionActions(editor: Editor): List<LspIntentionAction> {

        if (editor is EditorWindow) {
            Logger.getInstance(
                LspIntentionsService::class.java
            ).error("EditorWindow not expected here")
            return emptyList()
        }

        val project: Project = editor.project ?: return emptyList()
        val virtualFile: VirtualFile = editor.virtualFile ?: return emptyList()
        val document: Document = editor.document
        val psiModificationCount: Long = PsiManager.getInstance(project).modificationTracker.modificationCount
        val filePath: String = virtualFile.path
        val caret: Caret = editor.caretModel.primaryCaret
        val offset: Int = caret.selectionStart
        val length: Int = caret.selectionEnd - offset

        if (lastPsiModificationCount == psiModificationCount &&
            lastFilePath == filePath &&
            lastOffset == offset &&
            lastLength == length
        ) {
            return lastIntentionActions
        }

        if (ApplicationManager.getApplication().isDispatchThread) {
            return emptyList()
        }

        val intentionActions: MutableList<LspIntentionAction> = mutableListOf()
        val servers: List<LspServerImpl> =
            LspServerManagerImpl.getInstanceImpl(project).getServersWithThisFileOpen(virtualFile)
        for (server in servers) {
            ProgressManager.checkCanceled()
            val codeActionsSupport: LspCodeActionsSupport? = server.descriptor.lspCodeActionsSupport
            if (codeActionsSupport != null) {
                if (codeActionsSupport.intentionActionsSupport) {
                    val requestExecutor: LspRequestExecutorImpl = server.requestExecutor
                    val codeActionRequest = LspCodeActionRequest(server, virtualFile, document, offset, length)
                    val codeActions: List<CodeAction>? = requestExecutor.sendRequestSync(codeActionRequest)
                    if (codeActions != null) {
                        codeActions.forEach { codeAction ->
                            val intentionAction: LspIntentionAction? =
                                codeActionsSupport.createIntentionAction(server, codeAction)
                            if (intentionAction != null) {
                                intentionActions.add(intentionAction)
                            }
                        }
                    }
                }
            }
        }

        lastIntentionActions = ImmutableList.builder<LspIntentionAction>().apply {
            addAll(intentionActions)
        }.build()
        lastPsiModificationCount = psiModificationCount
        lastFilePath = filePath
        lastOffset = offset
        lastLength = length

        return lastIntentionActions
    }
}




