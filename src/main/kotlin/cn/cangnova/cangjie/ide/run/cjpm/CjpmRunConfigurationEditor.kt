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

//package cn.cangnova.cangjie.ide.run.cjpm
//
//import cn.cangnova.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
//import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
//import com.intellij.openapi.options.SettingsEditor
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.ComboBox
//import com.intellij.openapi.ui.TextComponentAccessor
//import com.intellij.openapi.ui.TextFieldWithBrowseButton
//import com.intellij.openapi.vfs.LocalFileSystem
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.ui.dsl.builder.COLUMNS_LARGE
//import com.intellij.ui.dsl.builder.columns
//import com.intellij.ui.dsl.builder.panel
//import java.io.File
//import javax.swing.DefaultComboBoxModel
//import javax.swing.JComponent
//import javax.swing.JPanel
//import javax.swing.JTextField
//
//class CjpmRunConfigurationEditor(private val project: Project) : SettingsEditor<CjpmCommandConfiguration>() {
//
//
//    val command = ComboBox<CjpmCommand>()
//
//
//    val module = TextFieldWithBrowseButton()
//
//    val args = JTextField()
//
//    private val mainPanel: JPanel = panel {
//
//        row(CangJieUiBundle.message("action.run.cjpm.configuration.modulejson.title")) {
//            cell(module)
//                .columns(COLUMNS_LARGE)
//                .component
//        }
//        row(CangJieUiBundle.message("action.run.cjpm.configuration.command.title")) {
//            cell(command)
//                .columns(COLUMNS_LARGE)
//                .component
//        }
//        row(CangJieUiBundle.message("action.run.cjpm.configuration.args.title")) {
//            cell(args)
//                .columns(COLUMNS_LARGE)
//                .component
//        }
//    }
//
//    init {
//        initUI()
//    }
//
//    private fun initUI() {
//        initModuleButton()
//        initComboxItems()
//    }
//
//    private fun initComboxItems() {
//
//        val model = DefaultComboBoxModel(CjpmCommand.toArray())
//        command.model = model
//        command.selectedIndex = 0
//    }
//
//    private fun initModuleButton() {
//
//        module.addBrowseFolderListener(
//            CangJieUiBundle.message("action.run.cjpm.configuration.select.modulejson.title"),
//            CangJieUiBundle.message("action.run.cjpm.configuration.select.modulejson.desc"),
//            project,
//            FileChooserDescriptorFactory.createSingleFileDescriptor(),
//            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
//        )
//
//        val moduleFile = getModuleJson()
//        if (moduleFile != null) {
//            module.text = moduleFile.path
//        } else {
//            module.text = ""
//        }
//
//
//        module.isEnabled = false
//    }
//
//    override fun resetEditorFrom(s: CjpmCommandConfiguration) {
//        command.selectedIndex = s.command?.index ?: 0
//        args.text = s.args
//    }
//
//    override fun applyEditorTo(s: CjpmCommandConfiguration) {
//        s.command = CjpmCommand.fromInt(command.selectedIndex)
//        s.args = args.text
//    }
//
//    private fun getModuleJson(): VirtualFile? {
//        val basePath = project.basePath ?: return null
//        val moduleJson = File(basePath, "module.json")
//        return if (moduleJson.exists()) LocalFileSystem.getInstance().findFileByIoFile(moduleJson) else null
//    }
//
//    override fun createEditor(): JComponent = mainPanel
//
//
//}
