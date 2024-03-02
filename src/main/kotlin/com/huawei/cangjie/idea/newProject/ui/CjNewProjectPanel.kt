package com.huawei.cangjie.idea.newProject.ui

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.toolchain.cjpm
import com.huawei.cangjie.cjpm.toolchain.tools.Cjpm

import com.huawei.cangjie.idea.newProject.CjCustomTemplate
import com.huawei.cangjie.idea.newProject.CjGenericTemplate
import com.huawei.cangjie.idea.newProject.CjProjectTemplate
import com.huawei.cangjie.idea.newProject.state.CjUserTemplatesState
import com.huawei.cangjie.idea.project.settings.ui.CangJieProjectSettingsPanel
import com.huawei.cangjie.idea.project.settings.ui.UiDebouncer
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.ui.JBUI
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.ListSelectionModel
import kotlin.math.min

class CjNewProjectPanel(
    private val showProjectTypeSelection: Boolean,
    cjpmProjectDir: Path = Paths.get("."),
    private val updateListener: (() -> Unit)? = null
) : Disposable {

    private val cangjieProjectSettings = CangJieProjectSettingsPanel(cjpmProjectDir, updateListener)
    private val cjpm: Cjpm?
        get() = cangjieProjectSettings.data.toolchain?.cjpm()
    private val defaultTemplates: List<CjProjectTemplate> = listOf(
        CjGenericTemplate.CjpmBinaryTemplate,
        CjGenericTemplate.CjpmLibraryTemplate,

    )
    private val userTemplates: List<CjCustomTemplate>
        get() = CjUserTemplatesState.getInstance().templates.map {
            CjCustomTemplate(it.name, it.url)
        }
    private val templateListModel: DefaultListModel<CjProjectTemplate> =
        JBList.createDefaultListModel(defaultTemplates + userTemplates)

    private val templateList: JBList<CjProjectTemplate> = JBList(templateListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        selectedIndex = 0
        addListSelectionListener { update() }
        cellRenderer = object : ColoredListCellRenderer<CjProjectTemplate>() {
            override fun customizeCellRenderer(
                list: JList<out CjProjectTemplate>,
                value: CjProjectTemplate,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                icon = value.icon
                append(value.name)

                if (value is CjCustomTemplate) {
                    append(" ")
                    append(value.shortLink, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }

    private val selectedTemplate: CjProjectTemplate
        get() = templateList.selectedValue

    private val updateDebouncer = UiDebouncer(this)
    private var needInstallCjpmGenerate = false

    val data: ConfigurationData get() = ConfigurationData(cangjieProjectSettings.data, selectedTemplate)
    fun update() {
        updateDebouncer.run(
            onPooledThread = {
                when (selectedTemplate) {
                    is CjGenericTemplate -> false
                    is CjCustomTemplate -> cjpm?.checkNeedInstallCjpmGenerate() ?: false
                }
            },
            onUiThread = { needInstall ->

                needInstallCjpmGenerate = needInstall
                updateListener?.invoke()
            }
        )
    }

    @Throws(ConfigurationException::class)
    fun validateSettings() {
        cangjieProjectSettings.validateSettings()

        if (needInstallCjpmGenerate) {
            @Suppress("DialogTitleCapitalization")
            throw ConfigurationException(CangJieBundle.message("dialog.message.cjpm.generate.needed.to.create.project.from.custom.template"))
        }
    }

    private val templateToolbar: ToolbarDecorator = ToolbarDecorator.createDecorator(templateList)
        .setToolbarPosition(ActionToolbarPosition.BOTTOM)
        .setPreferredSize(JBUI.size(0, 125))
        .disableUpDownActions()
        .setAddAction {
            AddUserTemplateDialog().show()
            updateTemplatesList()
        }
        .setRemoveAction {
            val customTemplate = selectedTemplate as? CjCustomTemplate ?: return@setRemoveAction
            CjUserTemplatesState.getInstance().templates
                .removeIf { it.name == customTemplate.name }
            updateTemplatesList()
        }
        .setRemoveActionUpdater { selectedTemplate !in defaultTemplates }

    fun attachTo(panel: Panel) = with(panel) {
        cangjieProjectSettings.attachTo(this)



        update()
    }

    private fun updateTemplatesList() {
        val index: Int = templateList.selectedIndex

        with(templateListModel) {
            removeAllElements()
            defaultTemplates.forEach(::addElement)
            userTemplates.forEach(::addElement)
        }

        templateList.selectedIndex = min(index, templateList.itemsCount - 1)
    }

    override fun dispose() {
        Disposer.dispose(cangjieProjectSettings)

    }
}

data class ConfigurationData(
    val settings: CangJieProjectSettingsPanel.Data,
    val template: CjProjectTemplate
)
