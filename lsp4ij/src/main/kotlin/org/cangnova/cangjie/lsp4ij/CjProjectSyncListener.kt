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

package org.cangnova.cangjie.lsp4ij

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerManager
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectListener

/**
 * 仓颉项目同步监听器
 *
 * 监听项目同步完成事件，在项目刷新成功后重启 LSP 服务器。
 * 这样可以确保 LSP 服务器使用最新的项目配置和依赖信息。
 */
class CjProjectSyncListener(
    private val project: Project
) : CjProjectListener {

    companion object {
        private val LOG = logger<CjProjectSyncListener>()
    }

    /**
     * 项目同步完成时的回调
     *
     * 当项目刷新成功后，重启 LSP 服务器以加载最新的项目配置。
     */
    override fun projectSynced(event: CjProjectEvent) {
        LOG.info("Project synced, restarting LSP server: ${event.project.name}")

        try {
            // 创建 LSP 服务器重启选项
            val options = LanguageServerManager.StartOptions()
            options.isForceRestart = true

            // 重启仓颉 LSP 服务器
            LanguageServerManager.getInstance(project).start("CangJie", options)

            LOG.info("LSP server restart requested successfully")
        } catch (e: Exception) {
            LOG.error("Failed to restart LSP server", e)
        }
    }
}