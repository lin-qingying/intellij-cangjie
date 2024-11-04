package com.linqingying.cangjie.configurable

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.configurable.CjConfigurableBase
import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected


class CangJieAstAnalyzerConfigurable(override val project: Project) :
    CjConfigurableBase(project, CangJieBundle.message("CangJie.languageServer.astanalyzer")), Configurable.NoScroll {

    private val astConfig = CangJieLanguageServerServices.getInstance().astConfig

    override fun createPanel(): DialogPanel {
        return panel {
            lateinit var mainSwitch: Cell<JBCheckBox>
            row {
                mainSwitch = checkBox(CangJieUiBundle.message("cangJie.languageServer.ast.enable.title"))
                    .bindSelected(
                        getter = { astConfig.enabled },
                        setter = {

                            astConfig.enabled = it
                        }
                    )
            }
            indent {
//                诊断信息
                panel {
                    lateinit var diagnostics: Cell<JBCheckBox>

                    row {
                        diagnostics = checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.diagnostic.title"))
                            .bindSelected(
                                getter = { astConfig.diagnostics },
                                setter = { astConfig.diagnostics = it })
                    }
                    indent {
                        row {
                            checkBox(CangJieUiBundle.message("cangJie.languageServer.ast.library.diagnostic.title"))
                                .bindSelected(
                                    getter = { astConfig.libraryDiagnostics },
                                    setter = { astConfig.libraryDiagnostics = it })
                        }
                        //                快速修复
                        row {
                            checkBox(CangJieUiBundle.message("cangJie.languageServer.ast.quickfix.title"))
                                .bindSelected(
                                    getter = { astConfig.quickFix },
                                    setter = { astConfig.quickFix = it })
                        }
                    }.enabledIf(diagnostics.selected)
                }

//                引用解析
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.ast.reference.title"))
                        .bindSelected(
                            getter = { astConfig.references },
                            setter = { astConfig.references = it })
                }


            }.enabledIf(mainSwitch.selected)
        }
    }
}
