/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.newProject.ui


import cn.cangnova.cangjie.ide.newProject.state.CjUserTemplate
import cn.cangnova.cangjie.ide.newProject.state.CjUserTemplatesState
import cn.cangnova.cangjie.ide.project.settings.ui.addTextChangeListener
import cn.cangnova.cangjie.ide.project.settings.ui.fullWidthCell
import cn.cangnova.cangjie.ide.project.settings.ui.trimmedText
import cn.cangnova.cangjie.messages.CangJieBundle

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
