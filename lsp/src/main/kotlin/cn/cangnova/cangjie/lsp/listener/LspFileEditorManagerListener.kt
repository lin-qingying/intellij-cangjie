package cn.cangnova.cangjie.lsp.listener

import cn.cangnova.cangjie.lsp.core.server.LspServerManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class LspFileEditorManagerListener : FileEditorManagerListener {
    private val openedFilesToHandle: MutableSet<VirtualFile> = HashSet()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.isInLocalFileSystem) {

            openedFilesToHandle.add(file)
            handleFiles(source.project)

        }
    }

    fun handleFiles(project: Project) {

        val serverManager = LspServerManager.getInstance(project)
        val server = serverManager.server

        if (server.isRunning()) {
TODO()
        } else {
            serverManager.startServer()
        }

//        val handledFiles: MutableSet<VirtualFile> = HashSet()
//        ReadAction.nonBlocking {
//            synchronized(openedFilesToHandle) {
//                handledFiles.addAll(openedFilesToHandle)
//            }
//
//        }


    }
}