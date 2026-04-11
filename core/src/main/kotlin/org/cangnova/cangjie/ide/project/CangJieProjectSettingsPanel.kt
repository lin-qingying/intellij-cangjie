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

/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.cangnova.cangjie.ide.project

import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import org.cangnova.cangjie.ide.project.structure.download.SdkDownloadEp
import org.cangnova.cangjie.ide.project.structure.download.addDownloadItem
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.project.wizard.ConfigurationData
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import javax.swing.JButton
import javax.swing.JLabel


class CangJieProjectSettingsPanel(
    private val project: Project? = null,
    private val updateListener: (() -> Unit)? = null
) : Disposable {


    override fun dispose() {
        Disposer.dispose(pathToToolchainComboBox)
    }

    var data: ConfigurationData.Data
        get() {
            val toolchain = pathToToolchainComboBox.selectedPath?.let { sdkRegistry.getSdkByPath(it) }
            return ConfigurationData.Data(
                toolchain = toolchain,

                )
        }
        set(value) {

            pathToToolchainComboBox.selectedPath = value.toolchain?.homePath
//            pathToStdlibField.text = value.explicitPathToStdlib ?: ""
            update()
        }
    private val versionUpdateDebouncer =
        UiDebouncer(this)

    private val pathToToolchainComboBox = CjToolchainPathChoosingComboBox {

        update()
    }


    private val toolchainVersion = JLabel()
    private val compilerType = JLabel()


    //    SDK注册中心
    private val sdkRegistry = CjSdkRegistry.getInstance()

    private val allSdks: List<CjSdk>
        get() = sdkRegistry.getAllSdks()

    // 抛出ConfigurationException异常
    @Throws(ConfigurationException::class)
    fun validateSettings() {
        // 获取data中的toolchain
        val toolchain = data.toolchain ?: return
        // 如果toolchain不是有效的toolchain，则移除toolchain并抛出ConfigurationException异常
        if (!toolchain.isValid) {
            // 从注册中心移除无效的SDK
            sdkRegistry.unregisterSdk(toolchain.id)

            throw ConfigurationException(
                CangJieBundle.message(
                    "settings.cangjie.toolchain.invalid.toolchain.error",
                    toolchain.homePath
                )
            )
        }
    }

    fun attachTo(panel: Panel) = with(panel) {
        data = ConfigurationData.Data(
            toolchain = project?.let {
                CjProjectSdkConfig.getInstance(it).getProjectSdk()
            } ?: if (project == null) {
                allSdks.firstOrNull()
            } else {
                null
            }
        )

        row(CangJieBundle.message("settings.cangjie.sdk.home.label")) {
            fullWidthCell(pathToToolchainComboBox)


//            显示下载按钮
            val downloadEp = SdkDownloadEp.EP_NAME.findFirstSafe { it.supportsDownload() }

            if (downloadEp != null) {
                val downloadButton = JButton(CangJieBundle.message("settings.cangjie.download.toolchain.button"))
                downloadButton.addActionListener {

                    addDownloadItem(downloadEp, pathToToolchainComboBox) {

                        update()
                    }

                }
                cell(downloadButton)
            }

        }
        row(CangJieBundle.message("settings.cangjie.toolchain.version.label")) {
            cell(toolchainVersion)
        }
        row(CangJieBundle.message("settings.cangjie.toolchain.compiler.type.label")) {
            cell(compilerType)
        }

        pathToToolchainComboBox.addToolchainsAsync {
            allSdks.map { it.homePath.toString() }
        }


    }


    private fun update() {
        val pathToToolchain = pathToToolchainComboBox.selectedPath

        versionUpdateDebouncer.run(onPooledThread = {
            val toolchain = pathToToolchain?.let { sdkRegistry.registerSdkPath(it) }
            val cjcVersion = toolchain?.version

            Triple(cjcVersion?.semver, cjcVersion?.type, false)
        }, onUiThread = { (cjcVersion, type, hasCangJieup) ->

            if (cjcVersion == null) {
                toolchainVersion.text =
                    CangJieBundle.message("settings.cangjie.toolchain.not.applicable.version.text")
                toolchainVersion.foreground = JBColor.RED
                compilerType.text = CangJieBundle.message("settings.cangjie.toolchain.not.applicable.version.text")
                compilerType.foreground = JBColor.RED

            } else {
                toolchainVersion.text = cjcVersion.parsedVersion
                toolchainVersion.foreground = JBColor.foreground()

                compilerType.text = type
                compilerType.foreground = JBColor.foreground()

                // 注册SDK到注册中心
                pathToToolchain?.let { path ->
                    if (!sdkRegistry.isSdkPathRegistered(path)) {
                        sdkRegistry.registerSdkPath(path)
                    }
                }

            }
            updateListener?.invoke()
        })
    }

}

private fun isStdlibLocationCompatible(toolchainLocation: String, stdlibLocation: String): Boolean {
    val isWslToolchain = WslPath.isWslUncPath(toolchainLocation)
    val isWslStdlib = WslPath.isWslUncPath(stdlibLocation)
    // We should reset [pathToStdlibField] because non-WSL stdlib paths don't work with WSL toolchains
    return isWslToolchain == isWslStdlib
}

private fun String.blankToNull(): String? = ifBlank { null }
