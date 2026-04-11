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


import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import org.cangnova.cangjie.ide.project.CangJieProjectSettingsPanel
import org.cangnova.cangjie.ide.project.cangjieSettings
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.project.wizard.ConfigurationData
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import org.cangnova.cangjie.utils.pathAsPath
import java.nio.file.Paths
import javax.swing.SwingUtilities


class CangJieConfigurable(override val project: Project) :
    CjConfigurableBase(project, CangJieBundle.message("CangJie")), Configurable.NoScroll {

    private val projectDir =
        CjProjectsService.getInstance(project).cjProject.takeIf { it.isValid }?.rootDir?.pathAsPath ?: Paths.get(".")

    private val cangjieProjectSettings by lazy { CangJieProjectSettingsPanel(project) }

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

                    cangjieProjectSettings.attachTo(this)



                    onApply {


                        settings.modify {
                            it.toolchain = cangjieProjectSettings.data.toolchain
                        }

                    }

                    onReset {
                        val sdkConfig = CjProjectSdkConfig.getInstance(project)
                        val currentSdk =
                            sdkConfig.getProjectSdk() ?: CjSdkRegistry.getInstance().getSdkByPath(projectDir)
                        val newData = ConfigurationData.Data(
                            toolchain = currentSdk
                        )
                        if (cangjieProjectSettings.data != newData) {
                            cangjieProjectSettings.data = newData
                        }
                    }

                    onIsModified {
                        val sdkConfig = CjProjectSdkConfig.getInstance(project)
                        val currentSdk = sdkConfig.getProjectSdk()
                        val data = cangjieProjectSettings.data
                        data.toolchain?.homePath != currentSdk?.homePath

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
