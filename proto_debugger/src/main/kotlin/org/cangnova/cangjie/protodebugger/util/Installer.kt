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

package org.cangnova.cangjie.protodebugger.util

import com.intellij.execution.configurations.GeneralCommandLine
import java.io.File
import java.util.concurrent.ExecutionException

/**
 * 安装器接口
 *
 * 该接口定义了调试器工具安装的标准契约，用于处理调试器相关工具的安装和配置。
 * 实现该接口的类负责管理特定工具的安装过程，包括下载、配置和环境设置。
 *
 * 使用场景：
 * - 调试器工具的自动安装和配置
 * - 工具依赖管理
 * - 开发环境的自动化设置
 * - 调试器工具链的版本管理
 *
 * 主要功能：
 * - 提供工具安装的命令行配置
 * - 管理可执行文件的路径
 * - 处理安装过程中的异常
 * - 支持不同平台的安装策略
 */
interface Installer {
    /**
     * 执行安装过程
     *
     * 创建并返回用于执行安装的命令行配置。
     * 该方法负责生成完整的安装命令，包括所有必要的参数和环境设置。
     *
     * 使用场景：
     * - 调试器工具的首次安装
     * - 工具版本升级
     * - 依赖工具的安装
     * - 环境配置和初始化
     *
     * @return 配置好的GeneralCommandLine对象，用于执行安装
     * @throws ExecutionException 当安装过程中发生错误时抛出
     */
    @Throws(ExecutionException::class)
    fun install(): GeneralCommandLine

    /**
     * 获取可执行文件
     *
     * 返回安装完成后工具的可执行文件对象。
     * 该文件应该是可以直接执行的二进制文件或脚本。
     *
     * 使用场景：
     * - 验证安装是否成功
     * - 获取工具的执行路径
     * - 创建调试器进程
     * - 检查工具的可用性
     *
     * @return 工具的可执行文件对象
     */
    val executableFile: File
}

class  InstallerImpl(private val cl: GeneralCommandLine) : Installer {
    override fun install(): GeneralCommandLine {
        return cl
    }

    override val executableFile: File
        get() = File(this.cl.exePath)


}
