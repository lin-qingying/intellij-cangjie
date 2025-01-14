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

/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.linqingying.cangjie.cjpm.project.settings.ui

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.CjToolchainPathChoosingComboBox
import com.linqingying.cangjie.cjpm.project.settings.CangJieProjectSettingsService
import com.linqingying.cangjie.cjpm.project.toPath
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainProvider
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainServices
import com.linqingying.cangjie.cjpm.toolchain.cjc
import com.linqingying.cangjie.ide.project.settings.ui.UiDebouncer
import com.linqingying.cangjie.ide.project.settings.ui.fullWidthCell
import com.intellij.execution.wsl.WslPath
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkListDownloader
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.linqingying.cangjie.ide.projectStructure.download.SdkDownloader
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JButton
import javax.swing.JLabel

class CangJieProjectSettingsPanel(

    private val cjpmProjectDir: Path = Paths.get("."), private val updateListener: (() -> Unit)? = null
) : Disposable {
    data class Data(
        val toolchain: CjToolchainBase?,
//        val explicitPathToStdlib: String?
    )

    override fun dispose() {
        Disposer.dispose(pathToToolchainComboBox)
    }

    var data: Data
        get() {
            val toolchain = pathToToolchainComboBox.selectedPath?.let { CjToolchainProvider.getToolchain(it) }
            return Data(
                toolchain = toolchain,

                )
        }
        set(value) {

            pathToToolchainComboBox.selectedPath = value.toolchain?.location
//            pathToStdlibField.text = value.explicitPathToStdlib ?: ""
            update()
        }
    private val versionUpdateDebouncer = UiDebouncer(this)

    private val pathToToolchainComboBox = CjToolchainPathChoosingComboBox { update() }


    private val toolchainVersion = JLabel()
    private val compilerType = JLabel()


    //    所有历史工具链路径
    private val toolchainsService = ApplicationManager.getApplication().getService(CjToolchainServices::class.java)

    //    private val toolchainPaths: List<CjToolchainBase> = toolchainsService.getToolchainPaths()
    private val toolchainPaths: List<String> = toolchainsService.getToolchainPaths()

    @Throws(ConfigurationException::class)
    fun validateSettings() {
        val toolchain = data.toolchain ?: return
        if (!toolchain.looksLikeValidToolchain()) {
            toolchainsService.removeToolchain(toolchain.location.toString())
            throw ConfigurationException(
                CangJieBundle.message(
                    "settings.cangjie.toolchain.invalid.toolchain.error",
                    toolchain.location
                )
            )
        }
    }

    fun attachTo(panel: Panel) = with(panel) {
        data = Data(
            toolchain = ProjectManager.getInstance().defaultProject
                .getService(CangJieProjectSettingsService::class.java)?.toolchain
                ?: CjToolchainBase.suggest(
                    cjpmProjectDir
                ) ?: toolchainPaths.getOrNull(0)?.toPath()?.let { CjToolchainProvider.getToolchain(it) }


        )

        row(CangJieBundle.message("settings.cangjie.sdk.home.label")) {
            fullWidthCell(pathToToolchainComboBox)


//            显示下载按钮
            if (toolchainPaths.isEmpty()) {
                val downloadButton = JButton(CangJieBundle.message("settings.cangjie.download.toolchain.button"))
                downloadButton.addActionListener {
                    downloadSdk()
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
//        pathToToolchainComboBox.addToolchainsAsync  {
//            CjToolchainFlavor.getApplicableFlavors()
//                .flatMap {
//                    it.suggestHomePaths()
//
//                }.distinct()
//        }
//            TODO 修改为历史可用的工具链路径

        pathToToolchainComboBox.addToolchainsAsync {
            toolchainPaths
        }


    }

    private inline fun <T : Any?> computeInBackground(
        project: Project?,
        @NlsContexts.DialogTitle title: String,
        crossinline action: (ProgressIndicator) -> T
    ): T =
        ProgressManager.getInstance().run(object : Task.WithResult<T, Exception>(project, title, true) {
            override fun compute(indicator: ProgressIndicator) = action(indicator)
        })

    private fun downloadSdk() {
        println("下载sdk")
        computeInBackground(null, CangJieBundle.message("progress.title.downloading.sdk.list")) {
//         休眠5秒

            SdkDownloader.getInstance().downloadForUI(it)
        }


    }

    private fun update() {
        val pathToToolchain = pathToToolchainComboBox.selectedPath

        versionUpdateDebouncer.run(onPooledThread = {
            val toolchain = pathToToolchain?.let { CjToolchainProvider.getToolchain(it) }
            val cjc = toolchain?.cjc()

//                TODO 版本管理工具
//                val cangjieup = toolchain?.cangjieup
            val cjcVersion = cjc?.version

//                val stdlibLocation = cjc?.getStdlibFromSysroot(cjpmProjectDir)?.presentableUrl
            Triple(cjcVersion?.semver, cjcVersion?.type, false)
        }, onUiThread = { (cjcVersion, type, hasCangJieup) ->
//                downloadStdlibLink.isVisible = hasCangJieup && stdlibLocation == null

//                pathToStdlibField.isEditable = !hasCangJieup
//                pathToStdlibField.setButtonEnabled(!hasCangJieup)
//                if (stdlibLocation != null && (pathToStdlibField.text.isBlank() || hasCangJieup) ||
//                    !isStdlibLocationCompatible(pathToToolchain?.toString().orEmpty(), pathToStdlibField.text)
//                ) {
//                    pathToStdlibField.text = stdlibLocation.orEmpty()
//                }
//                fetchedSysroot = stdlibLocation


//                pathToToolchainComboBox.selectedPath = data.toolchain?.location

            if (cjcVersion == null) {
                toolchainVersion.text = CangJieBundle.message("settings.cangjie.toolchain.not.applicable.version.text")
                toolchainVersion.foreground = JBColor.RED
                compilerType.text = CangJieBundle.message("settings.cangjie.toolchain.not.applicable.version.text")
                compilerType.foreground = JBColor.RED

            } else {
                toolchainVersion.text = cjcVersion.parsedVersion
                toolchainVersion.foreground = JBColor.foreground()

                compilerType.text = type
                compilerType.foreground = JBColor.foreground()
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
