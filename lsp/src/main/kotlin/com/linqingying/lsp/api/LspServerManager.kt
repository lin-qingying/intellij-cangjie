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

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * 跟踪启动的 LSP 服务器，允许启动、重启和停止 LSP 服务器。
 *
 * 希望启动 LSP 服务器的插件应实现 [LspServerSupportProvider]。
 *
 * 有关启动 LSP 服务器的信息，请参见 [LspServerSupportProvider.fileOpened] 函数的文档。
 */

interface LspServerManager {
    companion object {
        /**
         * 返回给定项目的 `LspServerManager` 实例。传递的 `project` 参数不能是“默认项目”（即用于管理新项目设置的虚拟项目）。
         * 因此，某些调用者在调用 `LspServerManager.getInstance(project)` 之前可能需要检查 [Project.isDefault]。
         */
        @JvmStatic
        fun getInstance(project: Project): LspServerManager = project.service()
    }

    fun getServersForProvider(providerClass: Class<out LspServerSupportProvider>): Collection<LspServer>

    /**
     * 这个函数适用于“用户在设置中启用了某些框架支持”这样的情况。它通过调度 [LspServerSupportProvider.fileOpened] 函数调用来通知 `providerClass`
     * 打开编辑器中的文件。所以，如果按照文档实现了 [fileOpened][LspServerSupportProvider.fileOpened] 函数， 这个函数可以确保启动所有
     * 当前打开文件所需的 [LSP 服务器][LspServer]。
     *
     * 如果一个或多个 [LSP 服务器][LspServer] 已经在运行，并且所有在编辑器中打开的文件都位于运行中的服务器的根目录中，
     * 那么这个函数将不执行任何操作。
     *
     * 这个函数可以从任何线程调用。
     *
     * @see [LspServerSupportProvider.fileOpened]
     */
    fun startServersIfNeeded(providerClass: Class<out LspServerSupportProvider>)

    /**
     * 这个函数即使当前编辑器中没有文件打开，也会启动 LSP 服务器，
     * 因此建议考虑使用 [startServersIfNeeded]。
     *
     * 如果一个 [LSP 服务器][LspServer] 已经在运行，并且它与传递的 [descriptor] 具有相同的根目录，
     * 那么这个函数不会执行任何操作。
     *
     * 这个函数可以从任何线程调用。
     */
    fun ensureServerStarted(providerClass: Class<out LspServerSupportProvider>, descriptor: LspServerDescriptor)

    /**
     * 这个函数适用于“用户在设置中禁用了某些框架支持”这样的情况。
     * 它会停止所有与 `providerClass` 相关的运行中的 LSP 服务器。
     */
    fun stopServers(providerClass: Class<out LspServerSupportProvider>)

    /**
     * 只是 [stopServers] 后跟 [startServersIfNeeded] 的简写。通常，当插件调用此函数时，举例来说，当用户更改了某些框架特定的设置，
     * 因此需要使用其他参数重新启动 LSP 服务器。
     */
    fun stopAndRestartIfNeeded(providerClass: Class<out LspServerSupportProvider>)

    fun addLspServerManagerListener(
        listener: LspServerManagerListener,
        parentDisposable: Disposable,
        sendEventsForExistingServers: Boolean = false,
    )
}
