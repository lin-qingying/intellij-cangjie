package org.cangnova.cangjie.ide.base.compilerPreferences.configuration

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.cangnova.cangjie.ide.base.compilerPreferences.CangJieBaseCompilerConfigurationUiBundle
import javax.swing.JLabel
import javax.swing.JTextField

internal class CangJieCompilerConfigurableUi {
    val toolchainComboBoxModel = ComboBoxModelWithPossiblyDisabledItems()

    lateinit var warningLabel: JLabel
    lateinit var toolchainComboBox: ComboBox<CangJieJpsVersionItem>
    lateinit var toolchainHomeField: JTextField
    lateinit var toolchainVersionLabel: JLabel
    lateinit var compilerTypeLabel: JLabel
    lateinit var toolchainTargetLabel: JLabel

    @JvmField
    val panel = panel {
        row {
            warningLabel = label("")
                .visible(false)
                .component
        }

        row(CangJieBaseCompilerConfigurationUiBundle.message("configuration.toolchain")) {
            toolchainComboBox = comboBox(toolchainComboBoxModel)
                .align(AlignX.FILL)
                .component
        }

        row(CangJieBaseCompilerConfigurationUiBundle.message("configuration.toolchain.home")) {
            toolchainHomeField = textField()
                .align(AlignX.FILL)
                .enabled(false)
                .component
        }

        row(CangJieBaseCompilerConfigurationUiBundle.message("configuration.toolchain.version")) {
            toolchainVersionLabel = label("").component
        }

        row(CangJieBaseCompilerConfigurationUiBundle.message("configuration.compiler.type")) {
            compilerTypeLabel = label("").component
        }

        row(CangJieBaseCompilerConfigurationUiBundle.message("configuration.toolchain.target")) {
            toolchainTargetLabel = label("").component
        }
    }
}

internal class ComboBoxModelWithPossiblyDisabledItems : MutableCollectionComboBoxModel<CangJieJpsVersionItem>() {
    override fun setSelectedItem(item: Any?) {
        if (item == null) return
        check(item is CangJieJpsVersionItem) { "$item is supposed to be CangJieJpsVersionItem" }
        if (!item.enabled) return
        super.setSelectedItem(item)
    }
}
