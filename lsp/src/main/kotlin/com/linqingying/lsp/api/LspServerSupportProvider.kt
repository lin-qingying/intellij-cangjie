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

package com.linqingying.lsp.api

import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lang.lsWidget.LanguageServiceWidgetItem
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.linqingying.lsp.api.lsWidget.LspServerWidgetItem
import org.jetbrains.annotations.ApiStatus

/**
 * 插件通过注册 `LspServerSupportProvider` 的实现来为某些编程语言或框架添加基于 LSP 服务器的支持。
 *
 * @see [fileOpened]
 * @see [https://microsoft.github.io/language-server-protocol/](https://microsoft.github.io/language-server-protocol/)
 */

interface LspServerSupportProvider {
    /**
     * [LspServerStarter] 实例被传递给插件实现的 [LspServerSupportProvider.fileOpened] 函数。
     * 插件在需要时可以调用 [LspServerStarter.ensureServerStarted]。
     *
     * 注意：在 [LspServerSupportProvider.fileOpened] 函数退出后调用 [LspServerStarter.ensureServerStarted] 将无效。
     * 因此，插件不应存储 [LspServerStarter] 实例的引用。
     *
     * @see [LspServerSupportProvider.fileOpened]
     */
    interface LspServerStarter {
        /**
         * 查找是否有与传递的 [LspServerDescriptor][descriptor] 具有相同根目录的已运行的 [LSP 服务器][LspServer]。
         * 如果找到，则此函数不执行任何操作；如果未找到，则创建并启动一个新的 [LspServer] 实例，
         * 并使用传递的 [LspServerDescriptor][descriptor] 来控制服务器的启动和行为。
         *
         * 注意：在 [LspServerSupportProvider.fileOpened] 函数退出后调用 [LspServerStarter.ensureServerStarted] 将无效。
         * 因此，插件不应存储 [LspServerStarter] 实例的引用。
         *
         * 提示：对于运行的 [LspServer]，启动时使用的 [descriptor] 对象可通过 [LspServer.descriptor] 获取。
         */
        fun ensureServerStarted(descriptor: LspServerDescriptor)
    }

    /**
     * 此函数为 `LspServerSupportProvider` 实现提供了一种延迟启动 LSP 服务器的便捷方式，仅在需要时启动。
     * 每当在编辑器中打开文件时，都会调用 `fileOpened()`，除非已存在运行的 [LspServer] 且打开的文件位于服务器根目录内。
     *
     * 实现可以检查文件类型、插件特定设置，并在需要时调用 [LspServerStarter.ensureServerStarted]。
     *
     * 典型实现：
     * ```kotlin
     * if (file.extension == "foo" && isFooSdkConfigured(project)) {
     *    serverStarter.ensureServerStarted(FooLspServerDescriptor(project))
     * }
     * ```
     *
     * 注意：在 [fileOpened] 函数退出后调用 [LspServerStarter.ensureServerStarted] 将无效。
     * 因此，插件不应存储 [LspServerStarter] 实例的引用。
     *
     * 如果插件希望在其他事件（例如，在设置中启用插件特定的框架支持）而非“在编辑器中打开文件”事件上启动 LSP 服务器，
     * 插件可以调用 [LspServerManager.startServersIfNeeded] 函数。
     *
     * 如果需要在启动 LSP 服务器之前执行耗时操作（例如，从互联网下载服务器二进制文件），
     * [fileOpened] 函数的实现不应耗时，因为这可能会冻结 UI。
     * 以下示例展示了在后台线程中运行耗时任务，并稍后通过 [LspServerManager.startServersIfNeeded] 启动 LSP 服务器：
     *
     * ```kotlin
     * override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerStarter) {
     *   val fooService = FooService.getInstance(project)
     *   if (file.extension != "foo" || !fooService.isFooSupportEnabled) return
     *
     *   if (fooService.isLspServerDownloaded) {
     *     serverStarter.ensureServerStarted(FooLspServerDescriptor(project))
     *   } else if (!fooService.isDownloadAlreadyScheduled) {
     *     fooService.scheduleDownload()
     *   }
     * }
     * ```
     *
     * 示例：FooService.scheduleDownload() 的实现：
     * ```kotlin
     * fun scheduleDownload() {
     *   if (isLspServerDownloaded || !isDownloadAlreadyScheduled.compareAndSet(false, true)) return
     *
     *   ApplicationManager.getApplication().executeOnPooledThread {
     *     downloadLspServer() // 下载成功时将 `isLspServerDownloaded` 设置为 `true`
     *     isDownloadAlreadyScheduled.set(false)
     *     LspServerManager.getInstance(project).startServersIfNeeded(FooLspSupportProvider::class.java)
     *   }
     * }
     * ```
     *
     * @param file 项目根目录内的有效本地文件
     */
    @RequiresReadLock
    @RequiresBackgroundThread
    fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerStarter)

    fun createLspWidgetItems(project: Project, currentFile: VirtualFile?): List<LanguageServiceWidgetItem> =
        LspServerManager.getInstance(project).getServersForProvider(javaClass)
            .mapNotNull { createLspServerWidgetItem(it, currentFile) }

    /**
     * 在“语言服务”状态栏小部件中创建特定于 LSP 服务器的条目。
     * 强烈建议插件覆盖此函数，以便设置正确的图标以及点击时打开插件页面的操作。
     *
     * 典型实现：
     * ```kotlin
     * override fun getLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?) =
     *   LspServerWidgetItem(lspServer, currentFile, fooIcon, FooConfigurable::class.java)
     * ```
     */
    fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem? =
        LspServerWidgetItem(lspServer, currentFile)

    companion object {
        val EP_NAME = create<LspServerSupportProvider>("com.linqingying.lsp.serverSupportProvider")
    }
}
