package com.huawei.cangjie.cjpm.project.configurable

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.pathAsPath
import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.idea.project.settings.ui.CangJieProjectSettingsPanel
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Paths


class CjProjectConfigurable(override val project: Project) : CjConfigurableBase(project, CangJieBundle.message("settings.cangjie.toolchain.name")), Configurable.NoScroll {
    private val projectDir = project.cjpmProjects.allProjects.firstOrNull()?.rootDir?.pathAsPath ?: Paths.get(".")


    private val cangjieProjectSettings by lazy { CangJieProjectSettingsPanel(projectDir) }

    override fun createPanel(): DialogPanel  =  panel {
        val settings = project.cangjieSettings
        val state = settings.state.copy()
        cangjieProjectSettings.attachTo(this)
        onApply {
            settings.modify {
                it.toolchain = cangjieProjectSettings.data.toolchain
//                it.explicitPathToStdlib = cangjieProjectSettings.data.explicitPathToStdlib
//                it.macroExpansionEngine = state.macroExpansionEngine
//                it.doctestInjectionEnabled = state.doctestInjectionEnabled
            }
        }

        onReset {
            val newData = CangJieProjectSettingsPanel.Data(
                toolchain = settings.toolchain ?: CjToolchainBase.suggest(projectDir),
//                explicitPathToStdlib = settings.explicitPathToStdlib
            )
            if (cangjieProjectSettings.data != newData) {
                cangjieProjectSettings.data = newData
            }
        }

        onIsModified {
            val data = cangjieProjectSettings.data
            data.toolchain?.sdkHome != settings.toolchain?.sdkHome

        }

    }
}