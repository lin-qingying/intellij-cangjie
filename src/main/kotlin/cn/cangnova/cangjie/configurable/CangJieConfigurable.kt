/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.configurable

import cn.cangnova.cangjie.cjpm.project.configurable.CjConfigurableBase
import cn.cangnova.cangjie.cjpm.project.model.cjpmProjects
import cn.cangnova.cangjie.cjpm.project.pathAsPath
import cn.cangnova.cangjie.cjpm.project.settings.cangjieSettings
import cn.cangnova.cangjie.cjpm.project.settings.ui.CangJieProjectSettingsPanel
import cn.cangnova.cangjie.configurable.state.PluginLanguageState
import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.toolchain.CjToolchainBase
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Paths
import javax.swing.SwingUtilities


class CangJieConfigurable(override val project: Project) :
    CjConfigurableBase(project, CangJieBundle.message("CangJie")), Configurable.NoScroll {
    private val projectDir = project.cjpmProjects.allProjects.firstOrNull()?.rootDir?.pathAsPath ?: Paths.get(".")


    private val cangjieProjectSettings by lazy { CangJieProjectSettingsPanel(projectDir) }

    @Throws(ConfigurationException::class)
    override fun apply() {
        cangjieProjectSettings.validateSettings()
        super.apply()
    }

    private fun refreshUI() {

        SwingUtilities.invokeLater {

            panel.revalidate()  // 重新验证布局
            panel.repaint()     // 重新绘制界面


        }
    }

    private var _panel: DialogPanel? = null
    val panel: DialogPanel
        get() {
            if (_panel == null) {
                _panel = panel {
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
                        }


                    }

                    onReset {
                        val newData = CangJieProjectSettingsPanel.Data(
                            toolchain = settings.toolchain ?: CjToolchainBase.suggest(projectDir),
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
            }
            return _panel!!
        }

    override fun disposeUIResources() {
        super.disposeUIResources()
        Disposer.dispose(cangjieProjectSettings)
    }

    override fun createPanel(): DialogPanel = panel
}
