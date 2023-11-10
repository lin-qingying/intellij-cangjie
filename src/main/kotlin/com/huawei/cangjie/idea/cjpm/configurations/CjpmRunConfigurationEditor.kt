//package com.huawei.cangjie.idea.cjpm.configurations
//
//import com.intellij.application.options.ModuleDescriptionsComboBox
//import com.intellij.openapi.options.SettingsEditor
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.LabeledComponent
//import com.intellij.ui.PanelWithAnchor
//import kotlinx.serialization.json.Json.Default.configuration
//import javax.swing.JComponent
//import javax.swing.JPanel
//
//class CjpmRunConfigurationEditor(project:Project): SettingsEditor<CjpmRunConfiguration>(), PanelWithAnchor {
//
//
//    private val mainPanel: JPanel = JPanel()
//
//
//
//    private val moduleChooser: LabeledComponent<ModuleDescriptionsComboBox>
//
//
//
//
//    private val anchor: JComponent
//
//    init {
//
//    }
//
//    override fun resetEditorFrom(s: CjpmRunConfiguration) {
//        commonProgramParameters.reset(configuration)
//
//        val runClass: String = configuration.getRunClass()
//        mainClass.getComponent().setText(runClass?.replace("\\$".toRegex(), "\\.") ?: "")
//        jrePathEditor.setPathOrName(configuration.getAlternativeJrePath(), configuration.isAlternativeJrePathEnabled())
//        shortenClasspathModeCombo.getComponent().setSelectedItem(configuration.getShortenCommandLine())
//
//    }
//
//    override fun applyEditorTo(s: CjpmRunConfiguration) {
//        TODO("Not yet implemented")
//    }
//
//    override fun createEditor(): JComponent {
//        return mainPanel
//    }
//
//    override fun getAnchor(): JComponent {
//        TODO("Not yet implemented")
//    }
//
//    override fun setAnchor(anchor: JComponent?) {
//        TODO("Not yet implemented")
//    }
//}
