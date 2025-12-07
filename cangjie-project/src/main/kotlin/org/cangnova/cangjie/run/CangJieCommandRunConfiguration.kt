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

package org.cangnova.cangjie.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.jdom.Element

/**
 * 仓颉命令运行配置
 *
 * 用于执行仓颉构建命令，如 build、test、clean 等
 */
class CangJieCommandRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : CangJieRunConfigurationBase(project, factory, name) {

    /**
     * 要执行的命令（例如："run"、"build"、"test"）
     */
    var command: String = "run"

    /**
     * 命令的附加参数
     */
    var args: String = ""

    /**
     * 创建运行状态
     *
     * @param environment 执行环境
     * @param commandExecutor 命令执行器
     * @return 仓颉命令运行状态实例
     */
    override fun createRunState(
        environment: ExecutionEnvironment,
        commandExecutor: CangJieCommandExecutor
    ) = CangJieCommandRunState(environment, this, commandExecutor)

    /**
     * 获取配置编辑器
     *
     * @return 仓颉运行配置编辑器实例
     */
    override fun getConfigurationEditor() = CangJieRunConfigurationEditor(project)

    /**
     * 生成建议的配置名称
     *
     * @return 格式为 "命令 项目名" 的配置名称
     */
    override fun suggestedName(): String {
        return "$command ${project.name}"
    }

    /**
     * 检查配置的有效性
     *
     * 验证命令是否为空，构建系统是否可用，以及配置是否有效
     *
     * @throws RuntimeConfigurationError 当配置无效时抛出异常
     */
    override fun checkConfiguration() {
        super.checkConfiguration()

        // 检查命令是否为空
        if (command.isBlank()) {
            throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.command.empty"))
        }

        // 检测构建系统
        val systemId = org.cangnova.cangjie.project.service.CjProjectBuildSystemService.getInstance().getBuildSystem()
            ?: throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.cannot.detect.build.system"))

        // 查找命令执行器
        val executor = CangJieCommandExecutor.findExecutor()
            ?: throw RuntimeConfigurationError(
                CjProjectBundle.message(
                    "run.configuration.error.no.command.executor",
                    systemId.id
                )
            )

        // 使用子系统执行器验证配置
        val error = executor.validateConfiguration(this)
        if (error != null) {
            throw RuntimeConfigurationError(error)
        }
    }

    /**
     * 将配置序列化到 XML 元素
     *
     * @param element 用于保存配置的 XML 元素
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("command", command)
        args.let { element.writeString("args", it) }
    }

    /**
     * 从 XML 元素反序列化配置
     *
     * @param element 包含配置数据的 XML 元素
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("command")?.let { command = it }
        element.readString("args")?.let { args = it }
    }
}