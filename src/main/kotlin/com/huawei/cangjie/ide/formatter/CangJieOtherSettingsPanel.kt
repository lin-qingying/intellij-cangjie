

package com.huawei.cangjie.ide.formatter

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.codeStyle.CodeStyleSettings


import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.*

class CangJieOtherSettingsPanel(settings: CodeStyleSettings) : CodeStyleAbstractPanel(CangJieLanguage, null, settings) {
    private val cbTrailingComma = JCheckBox(CangJieBundle.message("formatter.checkbox.text.use.trailing.comma"))

    override fun getRightMargin() = throw UnsupportedOperationException()

    override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()

    override fun getFileType() = throw UnsupportedOperationException()

    override fun getPreviewText(): String? = null

    override fun apply(settings: CodeStyleSettings) {
        settings.cangjieCustomSettings.ALLOW_TRAILING_COMMA = cbTrailingComma.isSelected
    }

    override fun isModified(settings: CodeStyleSettings): Boolean {
        return settings.cangjieCustomSettings.ALLOW_TRAILING_COMMA != cbTrailingComma.isSelected
    }

    override fun getPanel() = cPanel

    override fun resetImpl(settings: CodeStyleSettings) {
        cbTrailingComma.isSelected = settings.cangjieCustomSettings.ALLOW_TRAILING_COMMA
    }

    override fun getTabTitle(): String = CangJieBundle.message("formatter.title.other")

//    private val cjPanel = JPanel().apply {
//        layout = BoxLayout(this, BoxLayout.Y_AXIS)
//
//        add(
//            JBScrollPane(
//                JPanel(VerticalLayout(JBUI.scale(5))).apply {
//                    border = BorderFactory.createEmptyBorder(0, UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
//                    add(
//                        OptionGroup(CangJieBundle.message("formatter.title.trailing.comma")).apply {
//                            add(cbTrailingComma)
//                        }.createPanel()
//                    )
//                }
//            )
//        )
//    }


    private val cPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(
            JBScrollPane(
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    border = BorderFactory.createEmptyBorder(0, UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
                    add(JLabel(CangJieBundle.message("formatter.title.trailing.comma")))
                    add(cbTrailingComma)
                }
            )
        )
    }
}
