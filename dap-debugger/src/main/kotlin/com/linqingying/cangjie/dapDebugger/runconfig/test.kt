package com.linqingying.cangjie.dapDebugger.runconfig

import com.intellij.execution.ExecutionResult
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentManager
import java.awt.BorderLayout
import javax.swing.JPanel

import com.intellij.execution.ui.*

import com.intellij.ui.content.*

class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 创建你的自定义面板
        val myPanel = JPanel(BorderLayout())
        // TODO: 在这里添加你的自定义面板内容

//        更改名称
        toolWindow.title = "CangJie"

        // 创建内容管理器
        val contentManager: ContentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(myPanel, "My Panel", false)
        contentManager.addContent(content)
    }
}



