package com.linqingying.cangjie.configurable

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.configurable.CjConfigurableBase
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.cjpm.project.pathAsPath
import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.linqingying.cangjie.cjpm.project.settings.ui.CangJieProjectSettingsPanel
import com.linqingying.cangjie.configurable.state.PluginLanguageState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Paths
import javax.swing.SwingUtilities


class CangJieConfigurable(override val project: Project) : CjConfigurableBase(project, CangJieBundle.message("CangJie")), Configurable.NoScroll {
    private val projectDir = project.cjpmProjects.allProjects.firstOrNull()?.rootDir?.pathAsPath ?: Paths.get(".")


    private val cangjieProjectSettings by lazy { CangJieProjectSettingsPanel(projectDir) }
    @Throws(ConfigurationException::class)
    override fun apply() {
        cangjieProjectSettings.validateSettings()
        super.apply()
    }
    private fun refreshUI() {
        SwingUtilities.invokeLater {
            // 在事件调度线程中刷新UI，这里可以根据需要刷新具体的组件或界面

            // 例如，重新加载一个面板或刷新整个插件窗口：
            panel.revalidate()  // 重新验证布局
            panel .repaint()     // 重新绘制界面

            // 如果有其他窗口或组件需要更新，可以在这里调用相关方法
        }
    }
    val panel = panel {
        val settings = project.cangjieSettings
        val state = settings.state.copy()
        cangjieProjectSettings.attachTo(this)


//语言组
        group(CangJieBundle.message("cangjie.ide.language")) {
            indent {
                row {

                    comboBox(LanguageOption.entries)
                        .label(CangJieBundle.message("cangjie.ide.language.select"))
                        .bindItem(
                            { PluginLanguageState.instance.language },
                            { selectedLanguage ->


                                PluginLanguageState.instance.language = selectedLanguage!!

                                // 调用刷新UI的方法
                                refreshUI()
                            }
                        )

                }
            }
        }

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
            data.toolchain?.location != settings.toolchain?.location

        }

    }
    override fun disposeUIResources() {
        super.disposeUIResources()
        Disposer.dispose(cangjieProjectSettings)
    }
    override fun createPanel(): DialogPanel  =  panel
}
