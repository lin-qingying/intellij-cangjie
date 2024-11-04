package com.linqingying.cangjie.ide.newProject.ui

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.settings.ui.CangJieProjectSettingsPanel
import com.linqingying.cangjie.cjpm.toolchain.cjpm
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm
import com.linqingying.cangjie.ide.project.settings.ui.UiDebouncer
import com.linqingying.cangjie.ide.project.tools.projectWizard.CangJieProjectTypeItem
import com.linqingying.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.columns
import java.nio.file.Path
import java.nio.file.Paths

class CjNewProjectPanel(
    private val showProjectTypeSelection: Boolean,
    cjpmProjectDir: Path = Paths.get("."),
    private val updateListener: (() -> Unit)? = null
) : Disposable {
    //        项目类型
    private val projectTypeComboBox: ComboBox<CangJieProjectTypeItem> = ComboBox<CangJieProjectTypeItem>().apply {

//            添加条目
        addItem(CangJieProjectTypeItem("executable", CangJieBundle.message("cangjie.project.type.executable")))
        addItem(CangJieProjectTypeItem("static", CangJieBundle.message("cangjie.project.type.static")))
        addItem(CangJieProjectTypeItem("dynamic", CangJieBundle.message("cangjie.project.type.dynamic")))


    }
    private val cangjieProjectSettings = CangJieProjectSettingsPanel(cjpmProjectDir, updateListener)
    private val cjpm: Cjpm?
        get() = cangjieProjectSettings.data.toolchain?.cjpm()


    private val updateDebouncer = UiDebouncer(this)
    private var needInstallCjpmGenerate = false

    val data: ConfigurationData
        get() = ConfigurationData(
            cangjieProjectSettings.data,
            (projectTypeComboBox.selectedItem as CangJieProjectTypeItem).type
        )

    fun update() {
        updateDebouncer.run(
            onPooledThread = {

            },
            onUiThread = { needInstall ->


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


    fun attachTo(panel: Panel) = with(panel) {
        cangjieProjectSettings.attachTo(this)

        if (showProjectTypeSelection) {

            row(CangJieUiBundle.message("action.new.project.projecttype.title")) {
                cell(projectTypeComboBox)
                    .columns(COLUMNS_MEDIUM)
                    .component
            }.bottomGap(BottomGap.SMALL)

        }

        update()
    }


    override fun dispose() {
        Disposer.dispose(cangjieProjectSettings)

    }
}

data class ConfigurationData(
    val settings: CangJieProjectSettingsPanel.Data,
    val projectType: String
)
