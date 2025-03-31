package cn.cangnova.cangjie.lsp

import cn.cangnova.cangjie.lsp.core.server.LspServerManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class LspProjectActivity: ProjectActivity {
    override suspend fun execute(project: Project) {


    }
}