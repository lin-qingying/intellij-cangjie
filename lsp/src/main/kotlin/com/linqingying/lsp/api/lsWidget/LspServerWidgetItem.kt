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

package com.linqingying.lsp.api.lsWidget

import com.intellij.icons.AllIcons
import com.intellij.lang.LangBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lang.lsWidget.LanguageServicePopupSection
import com.intellij.platform.lang.lsWidget.LanguageServicePopupSection.ForCurrentFile
import com.intellij.platform.lang.lsWidget.LanguageServicePopupSection.Other
import com.intellij.platform.lang.lsWidget.LanguageServiceWidgetItem
import com.intellij.platform.lang.lsWidget.OpenSettingsAction
import com.linqingying.lsp.api.LspBundle
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.LspServerManager
import com.linqingying.lsp.api.LspServerState.*

import javax.swing.Icon

/**
 * @param icon 用于语言服务弹出窗口和状态栏，
 * 但在后者的情况下，平台会根据当前 UI 主题对图标进行颜色调整，以适应状态栏。
 * 如果实现希望为状态栏和弹出项提供不同的图标，可以重写 [statusBarIcon]。
 */

open class LspServerWidgetItem(
    protected val lspServer: LspServer,
    currentFile: VirtualFile?,
    private val icon: Icon = AllIcons.Json.Object,
    private val settingsPageClass: Class<out Configurable>? = null,
) : LanguageServiceWidgetItem() {

    override val statusBarIcon: Icon = icon

    override val statusBarTooltip: String
        get() = lspServer.descriptor.presentableName + versionPostfix

    override val isError: Boolean = lspServer.state == ShutdownUnexpectedly

    override val widgetActionLocation: LanguageServicePopupSection by lazy {
        if (currentFile != null &&
            currentFile.isInLocalFileSystem &&
            lspServer.descriptor.isSupportedFile(currentFile) &&
            lspServer.descriptor.roots.any { root -> VfsUtil.isAncestor(root, currentFile, true) } &&
            ProjectFileIndex.getInstance(lspServer.project).isInContent(currentFile)
        ) {
            ForCurrentFile
        } else Other
    }

    protected open val widgetActionText: @NlsActions.ActionText String
        get() = when (lspServer.state) {
            Initializing -> LangBundle.message("language.services.widget.item.initializing", serverLabel)
            Running -> serverLabel
            ShutdownNormally -> LangBundle.message("language.services.widget.item.shutdown.normally", serverLabel)
            ShutdownUnexpectedly -> LangBundle.message(
                "language.services.widget.item.shutdown.unexpectedly",
                serverLabel
            )
        }

    protected open val serverLabel: @NlsSafe String
        get() = lspServer.descriptor.presentableName + versionPostfix + rootPostfix

    protected open val versionPostfix: @NlsSafe String
        // 也许可以尝试自动缩短长版本字符串？例如 `1.36.4 (release, aarch64-apple-darwin)` -> `1.36.4…`
        get() = lspServer.initializeResult?.serverInfo?.version?.let { " $it" } ?: ""

    protected open val rootPostfix: @NlsSafe String
        get() {
            val roots = lspServer.descriptor.roots
            val lspServers =
                LspServerManager.getInstance(lspServer.project).getServersForProvider(lspServer.providerClass)
            return if (lspServers.size >= 2 && roots.size == 1) " …/${roots[0].name}" else ""
        }

    override fun createWidgetMainAction(): AnAction =
        settingsPageClass?.let {
            OpenSettingsAction(it, widgetActionText, icon)
        }
            ?: object : AnAction(widgetActionText, null, icon) {
                override fun actionPerformed(e: AnActionEvent) {
                    // 对于每个基于 LSP API 的插件，没有单一的合理操作。
                    // 强烈建议插件重写 `LspServerSupportProvider.getLspServerWidgetItem()`。
                    // 典型实现：
                    //     override fun getLspServerWidgetItem(...) = LspServerWidgetItem(context, lspServer, fooIcon, FooConfigurable::class.java)
                }
            }

    override fun createWidgetInlineActions(): List<AnAction> {
        val actions = mutableListOf<AnAction>()

        actions.addAll(createAdditionalInlineActions())

        if (widgetActionLocation == ForCurrentFile) {
            actions.add(RestartLspServerAction(lspServer))
        } else {
            when (lspServer.state) {
                Initializing, Running -> actions.add(StopLspServerAction(lspServer))
                ShutdownNormally -> Unit // 什么都不做
                ShutdownUnexpectedly -> actions.add(RestartLspServerAction(lspServer))
            }
        }

        settingsPageClass?.let { actions.add(OpenSettingsAction(it)) }

        return actions
    }

    open fun createAdditionalInlineActions(): List<AnAction> {
        if (lspServer.state != ShutdownUnexpectedly) return emptyList()
        val stderrAction = LspWidgetInternalService.getInstance().createShowErrorOutputAction(lspServer)
        return if (stderrAction != null) listOf(stderrAction) else emptyList()
    }
}


private class RestartLspServerAction(
    private val lspServer: LspServer,
) : AnAction(LspBundle.message("action.RestartLspServerAction.text"), null, AllIcons.Actions.StopAndRestart),
    DumbAware {
    override fun actionPerformed(e: AnActionEvent) = LspWidgetInternalService.getInstance().restartLspServer(lspServer)
}


private class StopLspServerAction(
    private val lspServer: LspServer,
) : AnAction(LspBundle.message("action.StopLspServerAction.text"), null, AllIcons.Actions.StopAndRestart), DumbAware {
    override fun actionPerformed(e: AnActionEvent) = LspWidgetInternalService.getInstance().stopLspServer(lspServer)
}
