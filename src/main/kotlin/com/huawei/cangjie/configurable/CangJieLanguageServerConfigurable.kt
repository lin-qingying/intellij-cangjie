package com.huawei.cangjie.configurable

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.configurable.CjConfigurableBase
import com.huawei.cangjie.configurable.services.CangJieLanguageServerServices
import com.huawei.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel


class CangJieLanguageServerConfigurable(override val project: Project) :
    CjConfigurableBase(project, CangJieBundle.message("CangJie.languageServer")), Configurable.NoScroll {

    private val lspConfig = CangJieLanguageServerServices.getInstance().lspConfig
    private val astConfig = CangJieLanguageServerServices.getInstance().astConfig



    override fun createPanel(): DialogPanel {
        return panel {
            group(CangJieUiBundle.message("cangJie.languageServer.lsp.title")) {
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.enable.title"))
                        .bindSelected(
                            getter = { lspConfig.enabled },
                            setter = {

                                lspConfig.enabled = it
                            }
                        )
                }
            }
            group(CangJieUiBundle.message("cangJie.languageServer.ast.title")) {
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.ast.enable.title"))
                        .bindSelected(
                            getter = { astConfig.enabled },
                            setter = {

                                astConfig.enabled = it
                            }
                        )
                }
            }
        }
    }
}
