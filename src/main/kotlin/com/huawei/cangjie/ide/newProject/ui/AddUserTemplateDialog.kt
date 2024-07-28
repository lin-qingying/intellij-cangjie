package com.huawei.cangjie.ide.newProject.ui


import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.newProject.state.CjUserTemplate
import com.huawei.cangjie.ide.newProject.state.CjUserTemplatesState
import com.huawei.cangjie.ide.project.settings.ui.addTextChangeListener
import com.huawei.cangjie.ide.project.settings.ui.fullWidthCell
import com.huawei.cangjie.ide.project.settings.ui.trimmedText

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent


class AddUserTemplateDialog : DialogWrapper(null) {
    private val repoUrlField: JBTextField = JBTextField().apply {
        addTextChangeListener(::suggestName)
    }

    private val nameField: JBTextField = JBTextField()

    init {
        title = CangJieBundle.message("dialog.create.project.custom.add.template.title")
        setOKButtonText(CangJieBundle.message("dialog.create.project.custom.add.template.action.add"))
        init()
    }

    override fun getPreferredFocusedComponent(): JComponent = repoUrlField

    override fun createCenterPanel(): JComponent = panel {
        row(CangJieBundle.message("dialog.create.project.custom.add.template.url")) {
            fullWidthCell(repoUrlField)
                .comment(CangJieBundle.message("dialog.create.project.custom.add.template.url.description"))
        }
        row(CangJieBundle.message("dialog.create.project.custom.add.template.name")) {
            fullWidthCell(nameField)
        }
    }

    override fun doOKAction() {
        // TODO: Find a better way to handle dialog form validation
        val name = nameField.trimmedText
        val repoUrl = repoUrlField.trimmedText

        if (name.isBlank()) return
        if (CjUserTemplatesState.getInstance().templates.any { it.name == name }) return

        CjUserTemplatesState.getInstance().templates.add(
            CjUserTemplate(name, repoUrl)
        )

        super.doOKAction()
    }

    private fun suggestName(event: DocumentEvent) {
        // Suggest name only if the whole URL was inserted
        if (event.type == DocumentEvent.EventType.INSERT && event.length == event.document.length) {
            if (nameField.text.isNotBlank()) return

            val repoUrl = repoUrlField.trimmedText
            if (KNOWN_URL_PREFIXES.none { repoUrl.startsWith(it) }) return

            nameField.text = repoUrl
                .removeSuffix("/")
                .removeSuffix(".git")
                .substringAfterLast("/")
        }
    }

    companion object {
        private val KNOWN_URL_PREFIXES = listOf("http://", "https://")
    }
}
