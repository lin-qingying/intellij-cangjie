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

package org.cangnova.cangjie.macro.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.result.CjResult

/**
 * 宏编译服务接口
 *
 * 提供编译宏声明的能力，通过 `cjc-frontend --compile-macro` 命令实现。
 * 宏展开前需要先编译宏声明，生成 `.cjo` 文件。
 *
 * ## 使用场景
 *
 * 1. 自动编译模式：在宏展开前自动检测并编译宏声明
 * 2. 手动编译模式：用户手动触发宏编译
 *
 * ## 使用示例
 *
 * ```kotlin
 * val service = MacroCompilationService.getInstance(project)
 *
 * // 编译文件中的宏声明
 * val result = service.compileMacros(file, options)
 *
 * // 编译项目中的所有宏声明
 * val result = service.compileAllMacrosInProject(options)
 * ```
 */
interface MacroCompilationService {

    /**
     * 编译指定文件中的宏声明
     *
     * @param file 包含宏声明的源文件
     * @param options 编译选项
     * @return 编译结果
     */
    suspend fun compileMacros(
        file: VirtualFile,
        options: MacroCompilationOptions = MacroCompilationOptions.DEFAULT
    ): CjResult<MacroCompilationResult, MacroCompilationError>

    /**
     * 编译项目中的所有宏声明
     *
     * @param options 编译选项
     * @return 编译结果
     */
    suspend fun compileAllMacrosInProject(
        options: MacroCompilationOptions = MacroCompilationOptions.DEFAULT
    ): CjResult<MacroCompilationResult, MacroCompilationError>

    /**
     * 检查文件是否需要重新编译宏
     *
     * @param file 源文件
     * @return 是否需要重新编译
     */
    fun needsRecompilation(file: VirtualFile): Boolean

    /**
     * 检查服务是否可用
     *
     * @return 编译器是否可用
     */
    fun isAvailable(): Boolean

    /**
     * 获取或创建文件的编译输出目录
     *
     * @param file 源文件
     * @return 输出目录路径
     */
    fun getOutputDirectory(file: VirtualFile): java.nio.file.Path?

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目
         * @return 宏编译服务实例
         */
        @JvmStatic
        fun getInstance(project: Project): MacroCompilationService {
            return project.getService(MacroCompilationService::class.java)
        }
    }
}

/**
 * 宏编译选项
 */
data class MacroCompilationOptions(
    /**
     * 是否强制重新编译（忽略缓存）
     */
    val forceRecompile: Boolean = false,

    /**
     * 编译超时时间（毫秒），0 表示无超时
     */
    val timeoutMs: Long = 30000L,

    /**
     * 是否并行编译
     */
    val parallel: Boolean = true,

    /**
     * 自定义输出目录，null 表示使用默认目录
     */
    val outputDir: java.nio.file.Path? = null
) {
    companion object {
        val DEFAULT = MacroCompilationOptions()
    }
}

/**
 * 宏编译结果
 */
data class MacroCompilationResult(
    /**
     * 编译的文件列表
     */
    val compiledFiles: List<String>,

    /**
     * 生成的 .cjo 文件路径列表
     */
    val outputFiles: List<java.nio.file.Path>,

    /**
     * 输出目录
     */
    val outputDirectory: java.nio.file.Path,

    /**
     * 编译耗时（毫秒）
     */
    val compilationTimeMs: Long,

    /**
     * 编译器输出信息
     */
    val compilerOutput: String = ""
)

/**
 * 宏编译错误
 */
sealed class MacroCompilationError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * SDK 未配置
     */
    class SdkNotConfigured : MacroCompilationError("项目未配置仓颉 SDK")

    /**
     * 编译器不可用
     */
    class CompilerUnavailable(
        val reason: String
    ) : MacroCompilationError("编译器不可用: $reason")

    /**
     * 编译失败
     */
    class CompilationFailed(
        val commandLine: String,
        val exitCode: Int,
        val stderr: String,
        cause: Throwable? = null
    ) : MacroCompilationError("宏编译失败 (退出码: $exitCode): $stderr", cause)

    /**
     * 超时
     */
    class Timeout(
        val timeoutMs: Long
    ) : MacroCompilationError("宏编译超时 (${timeoutMs}ms)")

    /**
     * 没有找到宏声明
     */
    class NoMacrosFound(
        val filePath: String
    ) : MacroCompilationError("文件中没有找到宏声明: $filePath")

    /**
     * 内部错误
     */
    class InternalError(
        val details: String,
        cause: Throwable? = null
    ) : MacroCompilationError("内部错误: $details", cause)
}
