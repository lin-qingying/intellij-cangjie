package com.linqingying.lsp.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

internal class LspFileEditorManagerListener :
    FileEditorManagerListener {
    override fun fileClosed(
        fileEditorManager: FileEditorManager,
        file: VirtualFile
    ) {
        val project = fileEditorManager.project
        if (!project.isDefault && file.isInLocalFileSystem) {
            if (!fileEditorManager.isFileOpen(file)) {
                val document = FileDocumentManager.getInstance().getCachedDocument(file)
                if (document != null && !FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
                    val servers = LspServerManagerImpl.getInstanceImpl(project).getServersWithThisFileOpen(file)
                    if (servers.isNotEmpty()) {
                        runReadAction {
                            for (server in servers) {
                                server.sendDidCloseRequest(file)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun fileOpened(
        fileEditorManager: FileEditorManager,
        file: VirtualFile
    ) {
        val project = fileEditorManager.project
        if (!project.isDefault && file.isInLocalFileSystem) {
            LspOpenedFilesService.getInstance(project).processOpenedFiles(listOf(file))
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val project = event.manager.project

        if (!project.isDefault) {
            event.newFile?.let { newFile ->
                if (newFile.isInLocalFileSystem) {
                    LspOpenedFilesService.getInstance(project).processOpenedFiles(listOf(newFile))
                }
            }
        }
    }
}

