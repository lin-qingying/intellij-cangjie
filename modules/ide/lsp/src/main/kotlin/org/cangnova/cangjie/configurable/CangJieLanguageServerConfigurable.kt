/*
 * Copyright 2026 LinQingYing. and contributors.
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
