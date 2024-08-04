

package com.linqingying.cangjie.ide.formatter

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.lang.CangJieLanguage
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil

import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class CangJieSaveStylePanel(settings: CodeStyleSettings) : CodeStyleAbstractPanel(CangJieLanguage, null, settings) {
    override fun getRightMargin() = throw UnsupportedOperationException()
    override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()
    override fun getFileType() = throw UnsupportedOperationException()
    override fun getPreviewText(): String? = null

    override fun getTabTitle(): String = CangJieBundle.message("formatter.title.load.save")

    private data class SaveItem(val label: String, val id: String?)

    private val saveDefaultsComboBox = ComboBox<SaveItem>()
    private val saveDefaultsItems = listOf(
        SaveItem("<ide defaults>", null),
        SaveItem(CangJieStyleGuideCodeStyle.CODE_STYLE_TITLE, CangJieStyleGuideCodeStyle.CODE_STYLE_ID),
        SaveItem(CangJieObsoleteCodeStyle.CODE_STYLE_TITLE, CangJieObsoleteCodeStyle.CODE_STYLE_ID),
    )

    private var selectedId: String?
        get() {
            val (_, id) = saveDefaultsComboBox.selectedItem as SaveItem
            return id
        }
        set(value) {
            saveDefaultsComboBox.selectedItem = saveDefaultsItems.firstOrNull { (_, id) -> id == value } ?: saveDefaultsItems.first()
        }

    private val jPanel = JPanel(BorderLayout()).apply {
        add(
            JBScrollPane(
                JPanel(VerticalLayout(JBUI.scale(5))).apply {
                    border = BorderFactory.createEmptyBorder(0, UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
                    add(JPanel(HorizontalLayout(JBUI.scale(5))).apply {
                        saveDefaultsItems.forEach {
                            saveDefaultsComboBox.addItem(it)
                        }

                        saveDefaultsComboBox.renderer = SimpleListCellRenderer.create("") {
                            it.label
                        }

                        add(JLabel(CangJieBundle.message("formatter.text.use.defaults.from")))
                        add(saveDefaultsComboBox)
                    })
                }
            )
        )
    }

    override fun apply(settings: CodeStyleSettings) {
        settings.cangjieCustomSettings.CODE_STYLE_DEFAULTS = selectedId
        settings.cangjieCommonSettings.CODE_STYLE_DEFAULTS = selectedId
    }

    override fun isModified(settings: CodeStyleSettings): Boolean {
        return selectedId != settings.cangjieCustomSettings.CODE_STYLE_DEFAULTS ||
                selectedId != settings.cangjieCommonSettings.CODE_STYLE_DEFAULTS
    }

    override fun getPanel() = jPanel

    override fun resetImpl(settings: CodeStyleSettings) {
        selectedId = settings.cangjieCustomSettings.CODE_STYLE_DEFAULTS ?: settings.cangjieCommonSettings.CODE_STYLE_DEFAULTS
    }

    override fun onSomethingChanged() {

    }
}
