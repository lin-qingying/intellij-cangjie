package cn.cangnova.cangjie.lsp.core.server

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

interface LspServerManager {
    val server: LspServer

    fun startServer()
    companion object {
        fun isLspRunning(): Boolean {
            val projectList = ProjectManager.getInstance().openProjects
//            var length = projectList.size

            for (project in projectList) {
//                val iterator = getInstanceImpl(project).servers.iterator()
//                while (iterator.hasNext()) {
//                    val next = iterator.next()
//                    if (next.isRunning()) {
//                        return true
//                    }
//                }

                val server = getInstance(project).server
                if (server.isRunning()) {
                    return true
                }
            }
            return false
        }

        fun getInstance(project: Project): LspServerManager {
            return project.service()
        }
    }


}

//用户打开文件 → 启动 LSP 服务器 → 初始化 → didOpen → 持续监听变更（didChange）
//↑          |          |          |
//└──────────┴──若服务器已运行，跳过启动步骤

internal class LspServerManagerImpl(val project: Project) : LspServerManager {
    override val server: LspServer = LspServerImpl(project)


    override fun startServer() {
        if (server.isRunning()) {
            return
        }
        server.start()
    }

}