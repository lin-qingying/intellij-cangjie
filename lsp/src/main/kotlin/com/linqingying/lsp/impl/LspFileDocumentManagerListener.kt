package com.linqingying.lsp.impl

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager

internal class LspFileDocumentManagerListener :
    FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        if (virtualFile != null && virtualFile.isInLocalFileSystem) {
            val projects = ProjectManager.getInstance().openProjects


            for (project in projects) {
                if (!FileEditorManager.getInstance(project).isFileOpen(virtualFile)) {
                    val lspManager = LspServerManagerImpl.getInstanceImpl(project)
                    lspManager.scheduleClosingFilesThatAreNotOfInterest()
                }
            }
        }
    }

    override fun unsavedDocumentDropped(document: Document) {
        this.beforeDocumentSaving(document)

    }
}

