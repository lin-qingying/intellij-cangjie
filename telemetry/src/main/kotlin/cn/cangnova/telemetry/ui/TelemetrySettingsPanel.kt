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

package cn.cangnova.telemetry.ui

import cn.cangnova.telemetry.TelemetryBundle
import cn.cangnova.telemetry.TelemetrySettings
import cn.cangnova.telemetry.sender.TelemetryDataSender
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TelemetrySettingsPanel : SearchableConfigurable, Configurable {
    private var panel: DialogPanel? = null
    private val settings = TelemetrySettings.getInstance()
    private val telemetryDataSender = TelemetryDataSender.getInstance()

    override fun getId(): String = "cn.cangnova.telemetry.settings"

    override fun getDisplayName(): String = TelemetryBundle.message("settings.telemetry.display.name")

    override fun createComponent(): JComponent {
        panel = panel {
            group(TelemetryBundle.message("settings.telemetry.display.name")) {
                row {
                    label(TelemetryBundle.message("settings.telemetry.description"))
                }
                row {
                    checkBox(TelemetryBundle.message("settings.telemetry.enabled"))
                        .bindSelected(
                            { settings.telemetryEnabled },
                            { settings.telemetryEnabled = it }
                        )
                }
//                row {
//                    // 显示固定的遥测服务器地址
//                    label(TelemetryBundle.message("settings.telemetry.server.address"))
//                }
                row {
                    // 使用从配置文件获取的隐私政策URL，如果为空则使用默认值
                    val privacyPolicyUrl = telemetryDataSender.getPrivacyPolicyUrl().takeIf { it.isNotEmpty() } 
                        ?: "https://gitcode.com/OpenCangjieCommunity/intellij-cangjie"
                    
                    browserLink(
                        TelemetryBundle.message("settings.telemetry.privacy.link.text"),
                        privacyPolicyUrl
                    )
                }
            }
        }

        return panel!!
    }

    override fun isModified(): Boolean {
        return panel?.isModified() ?: false
    }

    override fun apply() {
        panel?.apply()
    }

    override fun reset() {
        panel?.reset()
    }
} 