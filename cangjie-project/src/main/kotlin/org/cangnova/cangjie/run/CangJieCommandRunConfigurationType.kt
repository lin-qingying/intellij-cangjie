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
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.cangnova.cangjie.project.CjProjectBundle


/**
 * 仓颉命令运行配置类型
 *
 * 用于运行特定命令，如 build、test 等
 */
class CangJieCommandRunConfigurationType : ConfigurationTypeBase(
    "CangJieCommandRunConfigurationType",
    CjProjectBundle.message("run.configuration.type.command.display.name"),
    CjProjectBundle.message("run.configuration.type.command.description"),
    NotNullLazyValue.createValue { AllIcons.RunConfigurations.Application }
) {
    /**
     * 获取配置工厂实例
     */
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(CangJieConfigurationFactory(this))
    }

    companion object {
        /**
         * 获取仓颉命令运行配置类型的单例实例
         */
        val instance: CangJieCommandRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CangJieCommandRunConfigurationType::class.java)
    }
}

/**
 * 仓颉命令运行配置工厂
 *
 * 负责创建仓颉命令运行配置实例
 */
class CangJieConfigurationFactory(type: CangJieCommandRunConfigurationType) : ConfigurationFactory(type) {

    /**
     * 获取工厂 ID
     *
     * @return 工厂的唯一标识符
     */
    override fun getId(): String = ID

    /**
     * 创建模板配置
     *
     * @param project 当前项目
     * @return 新创建的仓颉命令运行配置实例
     */
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return CangJieCommandRunConfiguration(project, this, "CangJie Command")
    }

    companion object {
        /**
         * 工厂的唯一标识符
         */
        const val ID: String = "CangJie Command"
    }
}