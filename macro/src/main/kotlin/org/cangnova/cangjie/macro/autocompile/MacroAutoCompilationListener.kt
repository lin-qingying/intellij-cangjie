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

package org.cangnova.cangjie.macro.autocompile

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.cangnova.cangjie.macro.service.MacroCompilationOptions
import org.cangnova.cangjie.macro.service.MacroCompilationService
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.result.CjResult

/**
 * 宏自动编译监听器
 *
 * 在项目同步完成后自动检测并编译项目中的 macro package，
 * 无需用户手动触发。编译在后台协程中执行，不阻塞同步流程。
 *
 * 编译失败仅记录日志，不影响项目正常使用。
 */
class MacroAutoCompilationListener(
    private val project: Project
) : CjProjectListener {

    companion object {
        private val LOG = logger<MacroAutoCompilationListener>()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun projectSynced(event: CjProjectEvent) {
        val service = MacroCompilationService.getInstance(project)
        if (!service.isAvailable()) {
            LOG.info("宏编译服务不可用，跳过自动编译")
            return
        }

        LOG.info("项目同步完成，开始自动编译宏包: ${event.project.name}")

        scope.launch {
            try {
                val options = MacroCompilationOptions(
                    forceRecompile = false
                )
                val result = service.compileAllMacrosInProject(options)
                when (result) {
                    is CjResult.Ok -> {
                        val data = result.ok
                        LOG.info(
                            "宏包自动编译完成: 编译了 ${data.compiledFiles.size} 个文件, " +
                                    "生成 ${data.outputFiles.size} 个输出文件, " +
                                    "耗时 ${data.compilationTimeMs}ms"
                        )
                    }

                    is CjResult.Err -> {
                        LOG.warn("宏包自动编译失败: ${result.err.message}")
                    }
                }
            } catch (e: Exception) {
                LOG.warn("宏包自动编译过程中发生异常", e)
            }
        }
    }
}
