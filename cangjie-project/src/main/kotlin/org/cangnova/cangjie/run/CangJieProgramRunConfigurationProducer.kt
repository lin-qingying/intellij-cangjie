/* * Copyright 2025 LinQingYing. and contributors. * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. * * The use of this source code is governed by the Apache License 2.0, * which allows users to freely use, modify, and distribute the code, * provided they adhere to the terms of the license. * * The software is provided "as-is", and the authors are not responsible for * any damages or issues arising from its use. * */
package org.cangnova.cangjie.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjOutputType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.project.service.CjProjectsService
import java.nio.file.Paths

/**
 * 仓颉模块运行配置生产者
 *
 * 当右键点击仓颉文件时，自动创建运行配置
 */
class CangJieProgramRunConfigurationProducer : LazyRunConfigurationProducer<CangJieProgramRunConfiguration>() {

    /**
     * 获取配置工厂
     */
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CangJieProgramRunConfigurationType.instance.factory
    }

    /**
     * 从上下文设置运行配置
     *
     * @param configuration 要配置的运行配置实例
     * @param context 配置上下文，包含源元素和项目信息
     * @param sourceElement 触发配置创建的源元素引用
     * @return 如果成功设置配置则返回 true，否则返回 false
     */
    override fun setupConfigurationFromContext(
        configuration: CangJieProgramRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        // 获取源元素
        val element = sourceElement.get() ?: return false

        // 确保是仓颉文件
        val psiFile = element.containingFile as? CjFile ?: return false

        val project = context.project

        // 获取仓颉项目服务
        val projectsService = CjProjectsService.getInstance(project)

        // 通过文件查找所属的 CjModule
        val virtualFile = psiFile.virtualFile ?: return false
        val cjModule = projectsService.findModuleForFile(virtualFile) ?: return false

        // 检查模块的 output-type 是否为可执行类型
        if (!isExecutableModule(cjModule)) {
            return false
        }

        // 设置模块名
        configuration.moduleName = cjModule.name

        // 设置配置名称
        configuration.name = "run ${cjModule.name}"

        // 设置工作目录为模块根目录
        configuration.workingDirectory = Paths.get(cjModule.rootDir.path)

        return true
    }

    /**
     * 检查模块是否为可执行类型
     *
     * @param module 要检查的模块
     * @return 如果模块的 output-type 为 executable 则返回 true，否则返回 false
     */
    private fun isExecutableModule(module: CjModule): Boolean {
        // 通过 metadata.outputType 获取输出类型
        return module.metadata.outputType ==  CjOutputType.EXECUTABLE
    }

    /**
     * 检查给定的配置是否与上下文匹配
     *
     * @param configuration 要检查的运行配置
     * @param context 当前上下文
     * @return 如果配置与上下文匹配则返回 true，否则返回 false
     */
    override fun isConfigurationFromContext(
        configuration: CangJieProgramRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        // 获取上下文中的元素
        val element = context.location?.psiElement ?: return false

        // 确保是仓颉文件
        val psiFile = element.containingFile as? CjFile ?: return false

        // 获取仓颉项目服务
        val projectsService = CjProjectsService.getInstance(context.project)

        // 通过文件查找所属的 CjModule
        val virtualFile = psiFile.virtualFile ?: return false
        val cjModule = projectsService.findModuleForFile(virtualFile) ?: return false

        // 检查模块的 output-type 是否为可执行类型
        if (!isExecutableModule(cjModule)) {
            return false
        }

        // 比较模块名和工作目录
        return configuration.moduleName == cjModule.name &&
                configuration.workingDirectory?.toString() == cjModule.rootDir.path
    }
}