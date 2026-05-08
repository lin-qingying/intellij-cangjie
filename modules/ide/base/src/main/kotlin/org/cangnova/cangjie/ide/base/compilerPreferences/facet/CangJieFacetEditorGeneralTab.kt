package org.cangnova.cangjie.ide.base.compilerPreferences.facet

import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import org.cangnova.cangjie.facet.CangJieFacetConfiguration
import org.cangnova.cangjie.facet.getLibraryVersion
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 仓颉 facet 通用编辑页。
 *
 * 对位 Kotlin `KotlinFacetEditorGeneralTab` 的文件与类型位置。
 * 仓颉没有多平台、JS/JVM 这些 Kotlin UI 面，因此这里只承载：
 * 1. facet 自身的 `useProjectSettings`
 * 2. 当前模块可观察到的编译器版本
 * 3. 当前模块可观察到的运行库版本
 */
class CangJieFacetEditorGeneralTab(
    private val configuration: CangJieFacetConfiguration,
    private val editorContext: FacetEditorContext,
    private val validatorsManager: FacetValidatorsManager,
) : FacetEditorTab() {

    class EditorComponent(
        val module: Module,
        private val configuration: CangJieFacetConfiguration?,
    ) : JPanel(BorderLayout()), Disposable {
        val useProjectSettingsCheckBox = JBCheckBox("Use project settings")
        val compilerVersionLabel = JBLabel()
        val runtimeLibraryVersionLabel = JBLabel()

        private val isMultiEditor: Boolean
            get() = configuration == null

        fun initialize() {
            useProjectSettingsCheckBox.isEnabled = !isMultiEditor
            updateFromConfiguration()

            val panel = FormBuilder.createFormBuilder()
                .addComponent(useProjectSettingsCheckBox)
                .addLabeledComponent("Compiler version", compilerVersionLabel)
                .addLabeledComponent("Runtime library version", runtimeLibraryVersionLabel)
                .panel
                .apply {
                    border = JBUI.Borders.empty(10)
                }

            add(panel, BorderLayout.NORTH)

            useProjectSettingsCheckBox.addActionListener {
                configuration?.settings?.useProjectSettings = useProjectSettingsCheckBox.isSelected
                validatorsManagerSafeValidate()
            }
        }

        fun updateFromConfiguration() {
            useProjectSettingsCheckBox.isSelected = configuration?.settings?.useProjectSettings ?: true
            compilerVersionLabel.text = CangJieVersionInfoProviderByModuleDependencies().getCompilerVersion(module) ?: ""
            runtimeLibraryVersionLabel.text = getLibraryVersion(module, null) ?: ""
        }

        private fun validatorsManagerSafeValidate() {
            // editor 组件本身不持有 validatorsManager；保持局部状态更新即可。
        }

        override fun dispose() {
        }
    }

    val editor: EditorComponent = EditorComponent(editorContext.module, configuration).apply {
        initialize()
    }

    init {
        for (creator in CangJieFacetValidatorCreator.EP_NAME.extensionList) {
            validatorsManager.registerValidator(creator.create(editor, validatorsManager, editorContext))
        }
    }

    override fun getDisplayName(): String = "General"

    override fun createComponent(): JComponent = editor

    override fun isModified(): Boolean {
        return configuration.settings.useProjectSettings != editor.useProjectSettingsCheckBox.isSelected
    }

    override fun apply() {
        configuration.settings.useProjectSettings = editor.useProjectSettingsCheckBox.isSelected
    }

    override fun reset() {
        editor.updateFromConfiguration()
    }

    override fun disposeUIResources() {
        Disposer.dispose(editor)
    }
}
