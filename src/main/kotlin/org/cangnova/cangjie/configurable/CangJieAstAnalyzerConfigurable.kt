/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.configurable

import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.configurable.CjConfigurableBase
import org.cangnova.cangjie.configurable.services.CangJieLanguageServerServices
import org.cangnova.cangjie.messages.CangJieUiBundle
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
                //              悬浮信息
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.hoverinfo.title"))
                        .bindSelected(
                            getter = { astConfig.hoverInfo },
                            setter = { astConfig.hoverInfo = it })
                }
//                引用解析
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.ast.reference.title"))
                        .bindSelected(
                            getter = { astConfig.references },
                            setter = { astConfig.references = it })
                }
//               补全触发
                row {
                    checkBox(CangJieUiBundle.message("cangJie.languageServer.lsp.autoComplete.title"))
                        .bindSelected(
                            getter = { astConfig.autoComplete },
                            setter = { astConfig.autoComplete = it })
                }


            }.enabledIf(mainSwitch.selected)
        }
    }
}
