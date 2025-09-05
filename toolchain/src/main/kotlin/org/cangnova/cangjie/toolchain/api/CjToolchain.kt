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

package org.cangnova.cangjie.toolchain.api

import com.intellij.util.text.SemVer
import java.nio.file.Path

/**
 * CangJie语言工具链接口
 *
 * 该接口定义了CangJie语言处理工具链的基本功能。工具链负责提供对编译器、
 * 包管理器等核心工具的访问，以及处理与编译环境相关的操作。
 *
 * 工具链实现可以提供不同的编译和分析策略，以支持各种开发场景和目标平台。
 */
interface CjToolchain {
    /**
     * 获取工具链的主目录路径
     */
    val homePath: Path

    /**
     * 获取工具链的工具目录路径
     */
    val toolsPath: Path

    /**
     * 获取工具链的平台类型
     */
    val platformType: PlatformType

    /**
     * 获取工具链的版本信息
     */
    val version: String

    /**
     * 获取文件分隔符
     */
    val fileSeparator: String

    /**
     * 获取命令执行超时时间（毫秒）
     */
    val executionTimeoutInMilliseconds: Int

    /**
     * 获取指定工具的可执行文件路径
     *
     * @param toolName 工具名称
     * @return 工具的可执行文件路径
     */
    fun pathToExecutable(toolName: String): Path

    /**
     * 检查工具链是否包含指定的可执行文件
     *
     * @param path 可执行文件相对于SDK根路径的路径
     * @return 如果存在则返回true，否则返回false
     */
    fun hasExecutable(path: Path): Boolean

    /**
     * 将远程路径转换为本地路径
     *
     * @param remotePath 远程路径
     * @return 对应的本地路径
     */
    fun toLocalPath(remotePath: String): String

    /**
     * 将本地路径转换为远程路径
     *
     * @param localPath 本地路径
     * @return 对应的远程路径
     */
    fun toRemotePath(localPath: String): String

    /**
     * 展开用户主目录路径
     *
     * @param remotePath 包含用户主目录的路径
     * @return 展开后的完整路径
     */
    fun expandUserHome(remotePath: String): String

    /**
     * 获取可执行文件名称（考虑平台差异）
     *
     * @param toolName 工具名称
     * @return 平台相关的可执行文件名称
     */
    fun getExecutableName(toolName: String): String

    /**
     * 获取编译器工具实例
     *
     * @return 编译器工具实例
     */
    fun getCompiler(): CjCompiler

    /**
     * 获取包管理器工具实例
     *
     * @return 包管理器工具实例
     */
    fun getPackageManager(): CjPackageManager

    /**
     * 注册自定义工具
     *
     * @param toolId 工具ID
     * @param toolFactory 工具工厂
     */
    fun <T : CjTool> registerTool(toolId: String, toolFactory: CjToolFactory<T>)

    /**
     * 获取自定义工具
     *
     * @param toolId 工具ID
     * @return 工具实例，如果不存在则返回null
     */
    fun <T : CjTool> getTool(toolId: String): T?


}


/**
 * 平台类型枚举
 */
enum class PlatformType {
    /**
     * 本地平台
     */
    LOCAL,

    /**
     * 远程平台
     */
    REMOTE,

    /**
     * WSL (Windows Subsystem for Linux)
     */
    WSL,

    /**
     * Docker 容器
     */
    DOCKER,

    /**
     * 自定义平台
     */
    CUSTOM
}