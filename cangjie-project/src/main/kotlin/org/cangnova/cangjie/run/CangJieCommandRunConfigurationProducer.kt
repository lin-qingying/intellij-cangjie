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

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import java.nio.file.Paths

/**
 * 仓颉运行配置生产者
 *
 * 根据项目上下文自动创建运行配置
 */
class CangJieCommonRunConfigurationProducer : LazyRunConfigurationProducer<CangJieCommandRunConfiguration>() {

    /**
     * 获取配置工厂
     *
     * @return 仓颉命令运行配置类型的工厂实例
     */
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CangJieCommandRunConfigurationType.instance.factory
    }

    /**
     * 从上下文设置运行配置
     *
     * 根据项目上下文自动配置运行参数，包括配置名称、运行命令和工作目录
     *
     * @param configuration 要配置的运行配置实例
     * @param context 配置上下文，包含项目信息
     * @param sourceElement 触发配置创建的源元素引用
     * @return 始终返回 true，表示配置已成功设置
     */
    override fun setupConfigurationFromContext(
        configuration: CangJieCommandRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val project = context.project

        // 设置配置名称为 "运行 项目名"
        configuration.name = "运行 ${project.name}"

        // 设置默认命令为 "run"
        configuration.command = "run"

        // 设置工作目录为项目根目录
        configuration.workingDirectory = project.basePath?.let { Paths.get(it) }

        return true
    }

    /**
     * 检查给定的配置是否与上下文匹配
     *
     * 通过比较工作目录与项目根目录来判断配置是否来自当前上下文
     *
     * @param configuration 要检查的运行配置
     * @param context 当前上下文
     * @return 如果配置的工作目录与项目根目录匹配则返回 true
     */
    override fun isConfigurationFromContext(
        configuration: CangJieCommandRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val project = context.project
        return configuration.workingDirectory?.toString() == project.basePath
    }
}