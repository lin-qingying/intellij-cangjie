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

package org.cangnova.cangjie.debugger

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.debugger.toolchain.DebuggerDownloadInfo
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.nio.file.Path


/**
 * 调试器提供者接口
 *
 * 定义获取调试器可执行文件的方式。
 * 后期可扩展实现从网络下载、从SDK获取等方式。
 */
interface DebuggerProvider {
    val name: String

    /**
     * 调试器类型标识
     */
    val debuggerType: String

    /**
     * 获取调试服务器可执行文件路径
     *
     * @return 服务器可执行文件的绝对路径
     * @throws DebuggerNotFoundException 如果调试器未找到
     */
    suspend fun getServerPath(): Path

    /**
     * 检查调试器是否可用
     *
     * @return true 如果调试器存在且可执行
     */
    suspend fun isAvailable(): Boolean

    /**
     * 确保调试器已准备就绪
     *
     * 此方法不包括下载，只负责设置权限、验证文件等操作
     *
     * @return Result.success 如果准备成功
     */
    suspend fun ensureReady(): Result<Path>

    /**
     * 获取调试器环境变量
     *
     * 默认实现返回工具链的环境变量。
     * 子类可以重写此方法以添加额外的环境变量配置。
     *
     * @param project 当前项目
     * @return 环境变量Map，键为变量名，值为变量值
     */
    fun getEnvironmentVariables(project: Project): Map<String, String> {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return emptyMap()
        return sdk.getEnvironment()
    }

    /**
     * 获取下载信息
     *
     * 每个 Provider 实现此方法以提供特定的下载链接和配置
     * 链接格式: https://downloads.sourceforge.net/project/intellij-cangjie-debugger/[文件路径]
     *
     * @return 下载信息，如果不支持下载则返回 null
     */
    fun getDownloadInfo(): DebuggerDownloadInfo
}

/**
 * 调试器未找到异常
 */
class DebuggerNotFoundException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * 调试器下载失败异常
 */
class DebuggerDownloadException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)