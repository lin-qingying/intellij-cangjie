package com.linqingying.lsp.impl.intention

import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.util.ui.EDT
import com.linqingying.lsp.api.customization.LspIntentionAction
import com.linqingying.lsp.util.getLsp4jRange
import com.linqingying.lsp.impl.LspServerManagerImpl
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either

internal fun Either<Command, CodeAction>.asCodeAction(): CodeAction {
    return if (isLeft) {
        val command = left as Command
        CodeAction().apply {
            title = command.title
            this.command = command
            edit = WorkspaceEdit()
        }
    } else {
        requireNotNull(right) { "Right value cannot be null" }
    }
}


@Service(Service.Level.PROJECT)
internal class LspIntentionActionService {
    companion object {
        fun getInstance(project: Project): LspIntentionActionService {
            return project.getService(LspIntentionActionService::class.java)
                ?: throw IllegalStateException("Cannot find application-level service ${LspIntentionActionService::class.java.name}")
        }
    }

    private var lastFilePath: String = ""

    private var lastIntentionActions: List<LspIntentionAction> = emptyList()

    private var lastLength: Int = -1

    private var lastOffset: Int = -1

    private var lastPsiModificationCount: Long = -1L

    fun getIntentionActions(editor: Editor): List<LspIntentionAction> {

        if (editor is EditorWindow) {
            Logger.getInstance(LspIntentionActionService::class.java).error("EditorWindow not expected here")
            return emptyList()
        }

        val project = editor.project ?: return emptyList()
        val virtualFile = editor.virtualFile ?: return emptyList()
        val document = editor.document
        val modificationCount = PsiManager.getInstance(project).modificationTracker.modificationCount
        val filePath = virtualFile.path
        val primaryCaret = editor.caretModel.primaryCaret
        val selectionStart = primaryCaret.selectionStart
        val selectionLength = primaryCaret.selectionEnd - selectionStart

        if (lastPsiModificationCount == modificationCount && lastFilePath == filePath && lastOffset == selectionStart && lastLength == selectionLength) {
            return lastIntentionActions
        } else if (EDT.isCurrentThreadEdt()) {
            return emptyList()
        }

        val intentionActions = mutableListOf<LspIntentionAction>()
        val servers = LspServerManagerImpl.getInstanceImpl(project).getServersWithThisFileOpen(virtualFile)

        for (server in servers) {
            ProgressManager.checkCanceled()
            val codeActionsSupport = server.descriptor.lspCodeActionsSupport

            if (codeActionsSupport?.intentionActionsSupport == true) {
                val documentIdentifier = server.getDocumentIdentifier(virtualFile)
                val range = getLsp4jRange(document, selectionStart, selectionLength)
                val codeActionContext = CodeActionContext().apply {
                    diagnostics = emptyList()
                    triggerKind = CodeActionTriggerKind.Automatic
                }

                val codeActionParams = CodeActionParams(documentIdentifier, range, codeActionContext)
                val codeActions =
                    server.sendRequestSync { it.textDocumentService.codeAction(codeActionParams) }

                codeActions?.forEach { either ->
                    val codeAction = either.asCodeAction()
                    if (codeAction.kind?.startsWith("quickfix") != true) {
                        val intentionAction = codeActionsSupport.createIntentionAction(server, codeAction)
                        intentionAction?.let { intentionActions.add(it) }
                    }
                }
            }
        }

        // Cache the results
        lastIntentionActions = intentionActions.toList()
        lastPsiModificationCount = modificationCount
        lastFilePath = filePath
        lastOffset = selectionStart
        lastLength = selectionLength

        return lastIntentionActions
    }
}




