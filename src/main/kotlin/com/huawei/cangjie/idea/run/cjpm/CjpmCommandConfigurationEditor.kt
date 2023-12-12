package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.idea.project.tools.projectWizard.CangJieUiBundle
import com.huawei.cangjie.idea.run.CjCommandConfiguration
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.impl.SingleConfigurationConfigurable
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EditorTextField
import com.intellij.ui.ExpandableEditorSupport
import com.intellij.ui.TextAccessor
import com.intellij.ui.components.CheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.Function
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel


class CjpmCommandConfigurationEditor(project: Project) :
    CjCommandConfigurationEditor<CjpmCommandConfiguration>(project) {

    private var panel: JComponent? = null


    protected val workingDirectory: LabeledComponent<TextFieldWithBrowseButton> =
        WorkingDirectoryComponent()
    override val command  = CjCommandLineEditor(project, CjpmCommandCompletionProvider())

    override fun createEditor(): JComponent = panel {
        row(CangJieUiBundle.message("action.run.cjpm.configuration.command.title")) {
            fullWidthCell(command)
                .component


        }
//
//        row(workingDirectory.label) {
//            fullWidthCell(workingDirectory)
//                .resizableColumn()
//
//        }
    }.also { panel = it }
    private val buildOnRemoteTarget = CheckBox(CangJieUiBundle.message("checkbox.build.on.remote.target"), true)
    private val isRemoteTarget: Boolean
        get() = DataManager.getInstance().getDataContext(panel).getData(SingleConfigurationConfigurable.RUN_ON_TARGET_NAME_KEY) != null

    private fun hideUnsupportedFieldsIfNeeded() {
        if (!ApplicationManager.getApplication().isDispatchThread) return
        buildOnRemoteTarget.isVisible = isRemoteTarget
    }
    override fun resetEditorFrom(configuration: CjpmCommandConfiguration) {
        super.resetEditorFrom(configuration)

        hideUnsupportedFieldsIfNeeded()

    }

    override fun applyEditorTo(configuration: CjpmCommandConfiguration) {
        super.applyEditorTo(configuration)


        hideUnsupportedFieldsIfNeeded()
    }
}

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .align(Align.FILL)
}


abstract class CjCommandConfigurationEditor<T : CjCommandConfiguration>(
    protected val project: Project
) : SettingsEditor<T>() {
    abstract val command: CjCommandLineEditor


    override fun resetEditorFrom(configuration: T) {
        command.text = configuration.command?.executeCommand ?: ""
    }

    override fun applyEditorTo(configuration: T) {
        configuration.command = CjpmCommand.fromCommand(command.text)
    }

}

class CjCommandLineEditor(
    private val project: Project,
    private val completionProvider: TextFieldCompletionProvider
) : JPanel(BorderLayout()), TextAccessor {
    private val textField = createTextField("")
    private fun createTextField(value: String): TextFieldWithCompletion =
        TextFieldWithCompletion(
            project,
            completionProvider,
            value,
            true,
            false,
            false
        )

    init {
        ExpandableEditorSupportWithCustomPopup(textField, this::createTextField)
        add(textField, BorderLayout.CENTER)
    }

    override fun setText(text: String) {

        textField.text = text

    }

    override fun getText(): String = textField.text

}


class CjpmCommandCompletionProvider : CjCommandCompletionProvider()

abstract class CjCommandCompletionProvider : TextFieldCompletionProvider() {
    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        TODO("Not yet implemented")
    }

}

private class ExpandableEditorSupportWithCustomPopup(
    field: EditorTextField,
    private val createPopup: (text: String) -> EditorTextField
) : ExpandableEditorSupport(field) {
    @Suppress("UnstableApiUsage")
    override fun prepare(field: EditorTextField, onShow: Function<in String, String>): Content {
        val popup = createPopup(onShow.`fun`(field.text))
        val background = field.background

        popup.background = background
        popup.setOneLineMode(false)
        popup.preferredSize = Dimension(field.width, 5 * field.height)
        popup.addSettingsProvider { editor ->
            initPopupEditor(editor, background)
            copyCaretPosition(editor, field.editor)
        }

        return object : Content {
            override fun getContentComponent(): JComponent = popup
            override fun getFocusableComponent(): JComponent = popup
            override fun cancel(onHide: Function<in String, String>) {
                field.text = onHide.`fun`(popup.text)
                val editor = field.editor
                if (editor != null) copyCaretPosition(editor, popup.editor)
                if (editor is EditorEx) updateFieldFolding((editor as EditorEx?)!!)
            }
        }
    }

    companion object {
        private fun copyCaretPosition(destination: Editor, source: Editor?) {
            if (source == null) return  // unexpected
            try {
                destination.caretModel.caretsAndSelections = source.caretModel.caretsAndSelections
            } catch (ignored: IllegalArgumentException) {
            }
        }
    }
}

private class WorkingDirectoryComponent : LabeledComponent<TextFieldWithBrowseButton>() {
    init {
        component = TextFieldWithBrowseButton().apply {
            val fileChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                title = ExecutionBundle.message("select.working.directory.message")
            }
            addBrowseFolderListener(null, null, null, fileChooser)
        }
        text = ExecutionBundle.message("run.configuration.working.directory.label")
    }
}
