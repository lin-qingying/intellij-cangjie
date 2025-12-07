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

import com.intellij.execution.Executor
import com.intellij.execution.ExternalizablePath
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.*
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.run.target.CjLanguageRuntimeType
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 获取运行配置的目标环境配置
 *
 * @return 目标环境配置，如果未启用远程目标或未设置目标名称则返回 null
 */
val CangJieRunConfigurationBase.targetEnvironment: TargetEnvironmentConfiguration?
    get() {
        if (!RunTargetsEnabled.get()) return null
        val targetName = defaultTargetName ?: return null
        return TargetEnvironmentsManager.getInstance(project).targets.findByName(targetName)
    }

/**
 * 仓颉运行配置基类
 *
 * 为命令运行配置和程序运行配置提供通用功能
 */
abstract class CangJieRunConfigurationBase(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<RunProfileState>(project, factory, name),
    RunConfigurationWithSuppressedDefaultDebugAction,
    TargetEnvironmentAwareRunProfile {

    /**
     * 命令执行的工作目录
     */
    var workingDirectory: Path? = if (!project.isDefault) {
        project.basePath?.let { Paths.get(it) }
    } else {
        null
    }

    /**
     * 运行配置选项
     */
    val runConfigurationOptions get() = options

    /**
     * 获取默认目标名称
     *
     * @return 远程目标的名称，如果未设置则返回 null
     */
    override fun getDefaultTargetName(): String? = options.remoteTarget

    /**
     * 设置默认目标名称
     *
     * @param targetName 要设置的目标名称
     */
    override fun setDefaultTargetName(targetName: String?) {
        options.remoteTarget = targetName
    }

    /**
     * 检查是否可以在指定目标上运行
     *
     * @param target 目标环境配置
     * @return 仓颉配置可以在任何目标环境上运行，始终返回 true
     */
    override fun canRunOn(target: TargetEnvironmentConfiguration): Boolean {
        // 仓颉配置可以在任何目标平台环境上运行
        return true
    }

    /**
     * 获取默认语言运行时类型
     *
     * @return 仓颉语言运行时类型
     */
    override fun getDefaultLanguageRuntimeType(): LanguageRuntimeType<*>? {
        return CjLanguageRuntimeType()
    }

    /**
     * 环境变量配置
     */
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    /**
     * 私有运行状态实例
     */
    private var _runState: CangJieRunState<CangJieRunConfigurationBase>? = null

    /**
     * 获取当前运行状态
     */
    val runState: CangJieRunState<CangJieRunConfigurationBase>?
        get() = _runState

    /**
     * 获取运行配置状态
     *
     * @param executor 执行器
     * @param environment 执行环境
     * @return 运行配置状态，如果创建失败则返回 null
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        // 查找命令执行器
        val commandExecutor = CangJieCommandExecutor.findExecutor() ?: return null

        // 创建运行状态
        val state = createRunState(environment, commandExecutor)

        // 保存运行状态引用
        if (state is CangJieRunState<*>) {
            @Suppress("UNCHECKED_CAST")
            _runState = state as CangJieRunState<CangJieRunConfigurationBase>
        }

        return state
    }

    /**
     * 为此配置创建运行状态
     *
     * 子类应重写此方法以提供其特定的运行状态实现
     *
     * @param environment 执行环境
     * @param commandExecutor 命令执行器
     * @return 运行配置状态
     */
    protected abstract fun createRunState(
        environment: ExecutionEnvironment,
        commandExecutor: CangJieCommandExecutor
    ): RunProfileState

    /**
     * 检查配置的有效性
     *
     * @throws RuntimeConfigurationError 当配置无效时抛出异常
     */
    override fun checkConfiguration() {
        super.checkConfiguration()

        // 检查工作目录是否已设置
        if (workingDirectory == null) {
            throw RuntimeConfigurationError(
                CjProjectBundle.message("run.configuration.error.working.directory.missing")
            )
        }
    }

    /**
     * 将配置序列化到 XML 元素
     *
     * @param element 用于保存配置的 XML 元素
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writePath("workingDirectory", workingDirectory)
        env.writeExternal(element)
    }

    /**
     * 从 XML 元素反序列化配置
     *
     * @param element 包含配置数据的 XML 元素
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readPath("workingDirectory")?.let { workingDirectory = it }
        env = EnvironmentVariablesData.readExternal(element)
    }
}

// XML 序列化辅助函数

/**
 * 将字符串值写入 XML 元素
 *
 * @param name 选项名称
 * @param value 选项值
 */
fun Element.writeString(name: String, value: String) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value)
    addContent(opt)
}

/**
 * 从 XML 元素读取字符串值
 *
 * @param name 选项名称
 * @return 选项值，如果不存在则返回 null
 */
fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")

/**
 * 将路径写入 XML 元素
 *
 * @param name 选项名称
 * @param value 路径值
 */
fun Element.writePath(name: String, value: Path?) {
    if (value != null) {
        val s = ExternalizablePath.urlValue(value.toString())
        writeString(name, s)
    }
}

/**
 * 从 XML 元素读取路径
 *
 * @param name 选项名称
 * @return 路径值，如果不存在则返回 null
 */
fun Element.readPath(name: String): Path? {
    return readString(name)?.let { Paths.get(ExternalizablePath.localPathValue(it)) }
}