package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.idea.project.tools.projectWizard.CangJieUiBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class CjpmRunConfigurationEditor(private val project: Project) : SettingsEditor<CjpmCommandConfiguration>() {


    val command = ComboBox<CjpmCommand>()


    val module = TextFieldWithBrowseButton()

    val args = JTextField()

    private val mainPanel: JPanel = panel {

        row(CangJieUiBundle.message("action.run.cjpm.configuration.modulejson.title")) {
            cell(module)
                .columns(COLUMNS_LARGE)
                .component
        }
        row(CangJieUiBundle.message("action.run.cjpm.configuration.command.title")) {
            cell(command)
                .columns(COLUMNS_LARGE)
                .component
        }
        row(CangJieUiBundle.message("action.run.cjpm.configuration.args.title")) {
            cell(args)
                .columns(COLUMNS_LARGE)
                .component
        }
    }

    init {
        initUI()
    }

    private fun initUI() {
        initModuleButton()
        initComboxItems()
    }

    private fun initComboxItems() {

        val model = DefaultComboBoxModel(CjpmCommand.toArray())
        command.model = model
        command.selectedIndex = 0
    }

    private fun initModuleButton() {

        module.addBrowseFolderListener(
            CangJieUiBundle.message("action.run.cjpm.configuration.select.modulejson.title"),
            CangJieUiBundle.message("action.run.cjpm.configuration.select.modulejson.desc"),
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor(),
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        )

        val moduleFile = getModuleJson()
        if (moduleFile != null) {
            module.text = moduleFile.path
        } else {
            module.text = ""
        }


        module.isEnabled = false
    }

    override fun resetEditorFrom(s: CjpmCommandConfiguration) {
        command.selectedIndex = s.command?.index ?: 0
        args.text = s.args
    }

    override fun applyEditorTo(s: CjpmCommandConfiguration) {
        s.command = CjpmCommand.fromInt(command.selectedIndex)
        s.args = args.text
    }

    private fun getModuleJson(): VirtualFile? {
        val basePath = project.basePath ?: return null
        val moduleJson = File(basePath, "module.json")
        return if (moduleJson.exists()) LocalFileSystem.getInstance().findFileByIoFile(moduleJson) else null
    }

    override fun createEditor(): JComponent = mainPanel


}
