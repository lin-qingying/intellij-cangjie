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

package org.cangnova.cangjie.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.service.CjProjectBuildSystemService

/**
 * 命令执行扩展点。
 * 子系统实现此接口以提供命令执行逻辑。
 */
interface CangJieCommandExecutor {
    /**
     * 返回此执行器支持的构建系统ID（例如："cjpm", "cjc"）
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * 创建用于执行的命令行
     * @param configuration 运行配置
     * @return 准备执行的GeneralCommandLine，如果无法执行则返回null
     */
    fun createCommandLine(configuration: CangJieRunConfigurationBase): GeneralCommandLine?

    /**
     * 在执行前验证配置
     * @return 如果无效则返回错误消息，如果有效则返回null
     */
    fun validateConfiguration(configuration: CangJieRunConfigurationBase): String? = null

    /**
     * 判断命令是否需要附加构建适配器
     * 只有会产生构建产物的命令(如 build, run)才需要构建适配器
     * 其他命令(如 clean, check)不需要
     * @param configuration 运行配置
     * @return 如果需要构建适配器则返回true，否则返回false
     */
    fun shouldAttachBuildAdapter(configuration: CangJieRunConfigurationBase): Boolean = false


    companion object {
        val EP_NAME = ExtensionPointName<CangJieCommandExecutor>(
            "org.cangnova.cangjie.run.commandExecutor"
        )

        /**
         * 查找给定构建系统的适当执行器
         */
        fun findExecutor(): CangJieCommandExecutor? {
            val buildSystemId = CjProjectBuildSystemService.getInstance().getBuildSystem()?.id ?: return null
            return EP_NAME.extensionList
                .find { it.getBuildSystemId().id == buildSystemId }
        }
    }
}
