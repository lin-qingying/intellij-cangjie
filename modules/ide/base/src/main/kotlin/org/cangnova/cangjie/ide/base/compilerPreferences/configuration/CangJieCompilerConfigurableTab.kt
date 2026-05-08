package org.cangnova.cangjie.ide.base.compilerPreferences.configuration

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.ide.base.compilerPreferences.CangJieBaseCompilerConfigurationUiBundle
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import javax.swing.JComponent

/**
 * 仓颉编译配置页。
 *
 * 对位 Kotlin `KotlinCompilerConfigurableTab` 的文件位置。
 * Kotlin 这里维护完整的编译参数、JPS 与 JS/JVM 选项；
 * 仓颉当前 IDE 层真实存在的是项目级工具链选择，因此这里显式围绕工具链配置展开。
 */
class CangJieCompilerConfigurableTab(
    private val project: Project,
) : SearchableConfigurable {
    private val ui = CangJieCompilerConfigurableUi()
    private val sdkConfig: CjProjectSdkConfig = CjProjectSdkConfig.getInstance(project)
    private val sdkRegistry: CjSdkRegistry = CjSdkRegistry.getInstance()

    init {
        ui.toolchainComboBox.addActionListener {
            updateToolchainInfo(getSelectedSdk())
        }
        fillVersions()
        reset()
    }

    override fun getId(): String = "org.cangnova.cangjie.compiler.configuration"

    override fun getDisplayName(): String =
        CangJieBaseCompilerConfigurationUiBundle.message("configuration.display.name")

    override fun createComponent(): JComponent = ui.panel

    override fun isModified(): Boolean {
        return getSelectedSdk()?.id != sdkConfig.getProjectSdkId()
    }

    override fun apply() {
        sdkConfig.setProjectSdkId(getSelectedSdk()?.id)
    }

    override fun reset() {
        val selectedSdkId = sdkConfig.getProjectSdkId()
        val selectedItem = ui.toolchainComboBoxModel.items.firstOrNull { item -> item.sdk?.id == selectedSdkId }
            ?: ui.toolchainComboBoxModel.items.firstOrNull { item -> item.sdk != null }

        ui.toolchainComboBox.selectedItem = selectedItem
        updateToolchainInfo((selectedItem as? CangJieJpsVersionItem)?.sdk)
    }

    private fun fillVersions() {
        val sdks = sdkRegistry.getAllSdks().sortedBy { sdk -> sdk.name }
        ui.toolchainComboBoxModel.removeAll()

        if (sdks.isEmpty()) {
            ui.toolchainComboBoxModel.add(CangJieJpsVersionItem.createLabel("No toolchain configured"))
            ui.warningLabel.text = CangJieBaseCompilerConfigurationUiBundle.message("configuration.no.toolchain")
            ui.warningLabel.isVisible = true
            return
        }

        ui.warningLabel.isVisible = false
        sdks.forEach { sdk -> ui.toolchainComboBoxModel.add(CangJieJpsVersionItem(sdk)) }
    }

    private fun getSelectedSdk(): CjSdk? {
        return (ui.toolchainComboBox.selectedItem as? CangJieJpsVersionItem)?.sdk
    }

    private fun updateToolchainInfo(sdk: CjSdk?) {
        if (sdk == null) {
            ui.toolchainHomeField.text = ""
            ui.toolchainVersionLabel.text = ""
            ui.compilerTypeLabel.text = ""
            ui.toolchainTargetLabel.text = ""
            return
        }

        ui.toolchainHomeField.text = sdk.homePath.toString()
        ui.toolchainVersionLabel.text = sdk.version?.semver?.parsedVersion ?: ""
        ui.compilerTypeLabel.text = sdk.version?.type ?: ""
        ui.toolchainTargetLabel.text = sdk.version?.targetPlatform ?: ""
    }
}
