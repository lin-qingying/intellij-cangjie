package com.linqingying.lsp.impl.requests

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.requests.LspClientNotification
import com.linqingying.lsp.impl.LspServerManagerImpl.Companion.getInstanceImpl
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SemanticTokensFullNotification(override val lspServer: LspServer, val file: VirtualFile) :
    LspClientNotification(lspServer) {

    companion object {
        //节流时间
        private const val throttleTime = 500L
        private val executor = Executors.newSingleThreadScheduledExecutor()
        private var future: ScheduledFuture<*>? = null


        fun sendNotification(file: VirtualFile) {
            sendThrottledNotification(file)
        }

        private fun sendThrottledNotification(file: VirtualFile) {
            future?.cancel(false)

            val task = Runnable {
                val projects = ProjectManager.getInstance().openProjects
                for (project in projects) {
                    val servers = getInstanceImpl(project).servers
                    for (server in servers) {
                        server.requestExecutor.sendNotification(SemanticTokensFullNotification(server, file))
                    }
                }
            }

            future = executor.schedule(task, throttleTime, TimeUnit.MILLISECONDS)
        }


//
//
//        fun sendNotification(file: VirtualFile) {
//            val projects = ProjectManager.getInstance().openProjects
//            for (project in projects) {
//                val servers = getInstanceImpl(project).servers
//                for (server in servers) {
//
//                    server.requestExecutor.sendNotification(SemanticTokensFullNotification(server, file))
//                }
//            }
//
//        }


    }


    override fun sendNotification() {

        val textDocumentItem = TextDocumentIdentifier()
        textDocumentItem.uri = lspServer.descriptor.getFileUri(file)
        lspServer.lsp4jServer.textDocumentService.semanticTokensFull(SemanticTokensParams(textDocumentItem))
    }
}
