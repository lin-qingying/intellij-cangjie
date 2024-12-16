package com.linqingying.cangjie.dag

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class GenerateDependencyGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor

        if (editor != null) {
            val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

            // 获取当前文件路径和内容，进行包依赖关系分析
            val filePath = virtualFile?.path ?: return
            val packageGraph = generatePackageDependencyGraph(filePath)

            // 在插件界面上显示依赖关系图
            displayDependencyGraph(packageGraph)
        } else {
            Messages.showMessageDialog(project, "No editor found", "Error", Messages.getErrorIcon())
        }
    }

    // 模拟生成依赖关系图的逻辑
    private fun generatePackageDependencyGraph(filePath: String): String {
        // 假设这里会分析文件并返回图形字符串
        return "Package Dependency Graph for $filePath"
    }

    private fun displayDependencyGraph(graph: String) {
        // 显示依赖关系图，可以在弹窗中或新的工具窗口中显示
        Messages.showMessageDialog(graph, "Dependency Graph", Messages.getInformationIcon())
    }
}
