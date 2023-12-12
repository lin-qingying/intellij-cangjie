package com.debugger.runconfig.views

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout

class CjdbPanel(project: Project) : JBPanel<CjdbPanel>(BorderLayout()) {

    val console = LanguageConsoleImpl(project, ID, CangJieLanguage).apply {
        consoleEditor.isViewer = true

    }


    private val inputField = JBTextField().apply {
        toolTipText = "Plases start a debug session and input exppressions"
        emptyText.text = "Plases start a debug session and input exppressions"

        addActionListener {

            console.printByInput(text)
            // 清空输入框
            text = ""
        }
    }

    init {

        this.add(console.component, BorderLayout.CENTER)
        this.add(inputField, BorderLayout.SOUTH)
    }


    /**
     * 向控制台打印信息
     */
    fun print(text: String) {
        console.printByOutput(text)
    }

    /**
     * 移除输入框
     */
    fun removeInputField() {
        this.remove(inputField)
    }

    companion object {
        val ID = "CangJie CJDB"
    }
}


private fun LanguageConsoleView.printByInput(text: String) {
    this.print("$text\n", ConsoleViewContentType.USER_INPUT)
}


private fun LanguageConsoleView.printByOutput(text: String) {
    this.print("$text\n", ConsoleViewContentType.NORMAL_OUTPUT)
}
