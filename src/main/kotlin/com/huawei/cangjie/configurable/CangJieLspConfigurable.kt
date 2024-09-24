package com.huawei.cangjie.configurable

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.configurable.CjConfigurableBase
import com.huawei.cangjie.configurable.services.CangJieLanguageServerServices
import com.huawei.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
import com.intellij.ide.plugins.PluginManagerConfigurable.shutdownOrRestartApp
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.ComponentPredicate

class CangJieLspConfigurable(override val project: Project) :
    CjConfigurableBase(project, CangJieBundle.message("CangJie.languageServer.lsp")), Configurable.NoScroll {

    private val lspConfig = CangJieLanguageServerServices.getInstance().lspConfig
    private var otherMainSwitch = lspConfig.enabled
    override fun createPanel(): DialogPanel {
        return panel {
            lateinit var mainSwitch: Cell<JBCheckBox>
            row {
                mainSwitch = checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.enable.title"))
                    .bindSelected(
                        getter = { lspConfig.enabled },
                        setter = {

                            lspConfig.enabled = it
                        }
                    )
            }
            indent {
//                诊断信息
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.diagnostic.title"))
                        .bindSelected(
                            getter = { lspConfig.diagnostics },
                            setter = { lspConfig.diagnostics = it })
                }
//                转到声明
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.gotoDeclaration.title"))
                        .bindSelected(
                            getter = { lspConfig.goToDeclaration },
                            setter = { lspConfig.goToDeclaration = it })
                }
//                查找用法
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.findUsages.title"))
                        .bindSelected(
                            getter = { lspConfig.findUsages },
                            setter = { lspConfig.findUsages = it })
                }
//                自动补全
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.autoComplete.title"))
                        .bindSelected(
                            getter = { lspConfig.autoComplete },
                            setter = { lspConfig.autoComplete = it })
                }
                //              悬浮信息
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.hoverinfo.title"))
                        .bindSelected(
                            getter = { lspConfig.hoverInfo },
                            setter = { lspConfig.hoverInfo = it })
                }
                //              语义标记
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.semanticTokens.title"))
                        .bindSelected(
                            getter = { lspConfig.semanticTokens },
                            setter = { lspConfig.semanticTokens = it })
                }
            }.enabledIf(mainSwitch.selected)
        }
    }

    override fun disposeUIResources() {
        if (otherMainSwitch != lspConfig.enabled) {
            shutdownOrRestartApp()

        }
        super.disposeUIResources()
    }
}
