package com.linqingying.lsp.impl

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.linqingying.lsp.api.LspServerState
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.services.LanguageServer
import java.util.*

internal class LspDocumentListener : DocumentListener {
    private val documentsToHandle = Collections.synchronizedSet(HashSet<Document>())

    override fun beforeDocumentChange(event: DocumentEvent) {
        val targetFile = LspDidChangeUtil.getFileToHandle(event)

        if (targetFile != null) {
            val openProjects = ProjectManager.getInstance().openProjects

            for (project in openProjects) {
                val lspServerManager = LspServerManagerImpl.getInstanceImpl(project)
                val lspServers = lspServerManager.lspServers

                for (lspServer in lspServers) {
                    if (lspServer.state == LspServerState.Running) {
                        lspServer.fileEdited(targetFile, event)

                        if (lspServer.textDocumentSyncKind == TextDocumentSyncKind.Incremental &&
                            lspServer.isFileOpened(targetFile)
                        ) {

                            val changeParams =
                                LspDidChangeUtil.createIncrementalDidChangeParamsBeforeDocumentChange(
                                    lspServer,
                                    event,
                                    targetFile
                                )

                            lspServer.sendNotification { it: LanguageServer ->

                                it.textDocumentService.didChange(changeParams)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun documentChanged(event: DocumentEvent) { 

        if (LspDidChangeUtil.getFileToHandle(event) != null) {
            if (FileDocumentManager.getInstance().isDocumentUnsaved(event.document)) {
                documentsToHandle.add(event.document)
                performAction() // 假设 P() 是你想调用的方法
            }
        }
    }

    private fun collectChangedFilesData(listener: LspDocumentListener): ChangedFilesData {

        val changedFilesData = ChangedFilesData()
        val documentsToHandle = listener.documentsToHandle

        synchronized(documentsToHandle) {
            changedFilesData.handledDocuments.addAll(documentsToHandle)
        }

        val fileDocumentManager = FileDocumentManager.getInstance()

        val projects = ProjectManager.getInstance().openProjects

        for (project in projects) {
            val lspServers = LspServerManagerImpl.getInstanceImpl(project).lspServers
            for (lspServer in lspServers) {
                if (lspServer.state == LspServerState.Running) {
                    for (document in changedFilesData.handledDocuments) {
                        ProgressManager.checkCanceled()

                        if (fileDocumentManager.isDocumentUnsaved(document)) {
                            val virtualFile = fileDocumentManager.getFile(document)
                            virtualFile?.let { file ->
                                if (file.isInLocalFileSystem) {
                                    if (lspServer.isFileOpened(file)) {
                                        if (lspServer.textDocumentSyncKind == TextDocumentSyncKind.Full) {
                                            changedFilesData.serversToSendDidChange.add(
                                                lspServer to LspDidChangeUtil.createFullDidChangeParams(
                                                    lspServer,
                                                    document,
                                                    file
                                                )
                                            )
                                        }
                                    } else if (lspServer.isSupportedFile(file)) {
                                        changedFilesData.serversToSendDidOpen.add(lspServer to file)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return changedFilesData
    }

    private fun performAction() {
        ReadAction.nonBlocking<Unit> { collectChangedFilesData(this) }
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.nonModal()) { collectChangedFilesData(this) }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private class ChangedFilesData {
        val handledDocuments: MutableSet<Document> = HashSet()

        val serversToSendDidChange: MutableCollection<Pair<LspServerImpl, DidChangeTextDocumentParams>> =
            mutableListOf()

        val serversToSendDidOpen = mutableListOf<Pair<LspServerImpl, VirtualFile>>()
    }
}

