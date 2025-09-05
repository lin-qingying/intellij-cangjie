///*
// * Copyright 2025 LinQingYing. and contributors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// * The use of this source code is governed by the Apache License 2.0,
// * which allows users to freely use, modify, and distribute the code,
// * provided they adhere to the terms of the license.
// *
// * The software is provided "as-is", and the authors are not responsible for
// * any damages or issues arising from its use.
// *
// */
//
//package org.cangnova.cangjie.configurable
//
//import org.cangnova.cangjie.buildsystem.api.CangJieBuildSystem.Companion.EP_NAME
//import org.cangnova.cangjie.buildsystem.state.BuildSettingsState
//import org.cangnova.cangjie.configurable.CjToolchainPathChoosingComboBox
//import org.cangnova.cangjie.configurable.CjConfigurableBase
//import org.cangnova.cangjie.ide.project.settings.ui.UiDebouncer
//import org.cangnova.cangjie.ide.project.settings.ui.fullWidthCell
//import org.cangnova.cangjie.ide.project.structure.download.SdkDownloadEp
//import org.cangnova.cangjie.ide.project.structure.download.addDownloadItem
//import org.cangnova.cangjie.messages.CangJieBundle
//import org.cangnova.cangjie.toolchain.CangJieToolchain
//import org.cangnova.cangjie.toolchain.CjToolchainServices
//import org.cangnova.cangjie.toolchain.state.ToolchainSettingsState
//import com.intellij.openapi.Disposable
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.options.Configurable
//import com.intellij.openapi.options.ConfigurationException
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.DialogPanel
//import com.intellij.openapi.util.Disposer
//import com.intellij.ui.JBColor
//import com.intellij.ui.dsl.builder.Panel
//import com.intellij.ui.dsl.builder.bindItem
//import com.intellij.ui.dsl.builder.panel
//import javax.swing.JButton
//import javax.swing.JLabel
//
//class BuildSystemConfigurable(override val project: Project) :
//    CjConfigurableBase(project, CangJieBundle.message("CangJie.buildSystem")), Configurable.NoScroll {
//
//    private val cangjieProjectSettings by lazy { CangJieProjectSettingsPanel1() }
//    private val state = BuildSettingsState.getInstance(project)
//
//    override fun createPanel(): DialogPanel = panel {
//        cangjieProjectSettings.attachTo(this)
//
//        row("默认构建系统:") {
//            val applicable = EP_NAME.extensionList
//
//            if (applicable.size == 0) {
//                text("无可用的构建系统")
//            } else {
//                comboBox(applicable)
//
//                    .bindItem(
//                        { applicable.find { it.id == state.defaultBuildSystemId } },
//                        {
//                            state.defaultBuildSystemId = it?.id ?: ""
//                        }
//                    )
//            }
//
//        }
//    }
//
//
//
//}
//
//
//class CangJieProjectSettingsPanel1(
//
//    private val updateListener: (() -> Unit)? = null
//) : Disposable {
//    data class Data(
//        val toolchain: CangJieToolchain?,
////        val explicitPathToStdlib: String?
//    )
//
//    override fun dispose() {
//        Disposer.dispose(pathToToolchainComboBox)
//    }
//
//    var data: Data
//        get() {
//            val toolchain = pathToToolchainComboBox.selectedPath?.let { CangJieToolchain.create(it) }
//            return Data(
//                toolchain = toolchain,
//
//                )
//        }
//        set(value) {
//
//            pathToToolchainComboBox.selectedPath = value.toolchain?.homePath
//
//            update()
//        }
//    private val versionUpdateDebouncer = UiDebouncer(this)
//
//    private val pathToToolchainComboBox = CjToolchainPathChoosingComboBox {
//
//        update()
//    }
//
//
//    private val toolchainVersion = JLabel()
//    private val compilerType = JLabel()
//
//
//    //    所有历史工具链路径
//    private val toolchainsService = ApplicationManager.getApplication().getService(CjToolchainServices::class.java)
//
//    private val toolchainPaths: List<String> = toolchainsService.getToolchainPaths()
//
//
//    private val toolchainPath = CangJieToolchain.getToolchain().homePath
//
//    // 抛出ConfigurationException异常
//    @Throws(ConfigurationException::class)
//    fun validateSettings() {
//        // 获取data中的toolchain
//        val toolchain = data.toolchain ?: return
//        // 如果toolchain不是有效的toolchain，则移除toolchain并抛出ConfigurationException异常
//        if (!toolchain.isAvailable) {
//            toolchainsService.removeToolchain(toolchain.homePath.toString())
//            throw ConfigurationException(
//                CangJieBundle.message(
//                    "settings.cangjie.toolchain.invalid.toolchain.error",
//                    toolchain.homePath
//                )
//            )
//        }
//    }
//
//    fun attachTo(panel: Panel) = with(panel) {
//        data = Data(
//            toolchain = CangJieToolchain.getToolchain()
//
//
//        )
//
//        row(CangJieBundle.message("settings.cangjie.sdk.home.label")) {
//            fullWidthCell(pathToToolchainComboBox)
//
//
////            显示下载按钮
//            val downloadEp = SdkDownloadEp.EP_NAME.findFirstSafe { it.supportsDownload() }
//
//            if (/*toolchainPaths.isEmpty() &&*/ downloadEp != null) {
//                val downloadButton = JButton(CangJieBundle.message("settings.cangjie.download.toolchain.button"))
//                downloadButton.addActionListener {
//
//                    addDownloadItem(downloadEp, pathToToolchainComboBox) {
//
////                        update()
//                    }
//
//                }
//                cell(downloadButton)
//            }
//
//        }
//        row(CangJieBundle.message("settings.cangjie.toolchain.version.label")) {
//            cell(toolchainVersion)
//        }
//        row(CangJieBundle.message("settings.cangjie.toolchain.compiler.type.label")) {
//            cell(compilerType)
//        }
//
//        pathToToolchainComboBox.addToolchainsAsync {
////            toolchainPaths
//            listOf(toolchainPath)
//        }
//
//
//    }
//
//
//    private fun update() {
//        val pathToToolchain = pathToToolchainComboBox.selectedPath
//
//        versionUpdateDebouncer.run(onPooledThread = {
//            val toolchain = pathToToolchain?.let { CangJieToolchain.create(it) }
//
//
//            val cjcVersion = toolchain?.version
//
//            Triple(cjcVersion?.semver, cjcVersion?.type, false)
//        }, onUiThread = { (cjcVersion, type, hasCangJieup) ->
//
//            if (cjcVersion == null) {
//                toolchainVersion.text =
//                    CangJieBundle.message("settings.cangjie.toolchain.not.applicable.version.text")
//                toolchainVersion.foreground = JBColor.RED
//                compilerType.text = CangJieBundle.message("settings.cangjie.toolchain.not.applicable.version.text")
//                compilerType.foreground = JBColor.RED
//
//            } else {
//                toolchainVersion.text = cjcVersion.parsedVersion
//                toolchainVersion.foreground = JBColor.foreground()
//
//                compilerType.text = type
//                compilerType.foreground = JBColor.foreground()
//
//                //更新
//                ToolchainSettingsState.getInstance().path = pathToToolchain.toString()
//            }
//            updateListener?.invoke()
//        })
//    }
//
//}
//
//
//
//
