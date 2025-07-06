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

package cn.cangnova.cangjie.toolchain

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
     * @param exec 可执行文件名称
     * @return 如果存在则返回true，否则返回false
     */
    fun hasExecutable(exec: String): Boolean

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
     * 获取代码格式化工具实例
     *
     * @return 代码格式化工具实例
     */
    fun getFormatter(): CjFormatter

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
}

/**
 * CangJie工具链提供者接口
 *
 * 用于发现和创建工具链实例
 */
interface CjToolchainProvider {
    /**
     * 根据路径获取工具链实例
     *
     * @param homePath 工具链主目录
     * @return 工具链实例，如果路径无效则返回null
     */
    fun getToolchain(homePath: Path): CjToolchain?

    /**
     * 获取所有可用的工具链
     *
     * @return 可用工具链列表
     */
    fun getToolchains(): List<CjToolchain>
}

/**
 * CangJie编译器工具接口
 */
interface CjCompiler : CjTool {
    /**
     * 编译文件
     *
     * @param sourcePath 源文件路径
     * @param outputPath 输出路径
     * @param options 编译选项
     * @return 编译结果
     */
    fun compile(sourcePath: Path, outputPath: Path, options: CjCompileOptions): CjCompileResult

    /**
     * 编译多个文件
     *
     * @param sourcePaths 源文件路径列表
     * @param outputPath 输出路径
     * @param options 编译选项
     * @return 编译结果
     */
    fun compileFiles(sourcePaths: List<Path>, outputPath: Path, options: CjCompileOptions): CjCompileResult

    /**
     * 编译项目
     *
     * @param projectPath 项目路径
     * @param options 编译选项
     * @return 编译结果
     */
    fun compileProject(projectPath: Path, options: CjCompileOptions): CjCompileResult

    companion object {
        /**
         * 编译器名称
         */
        const val NAME = "cjc"
    }
}

/**
 * CangJie包管理器工具接口
 */
interface CjPackageManager : CjTool {
    /**
     * 安装依赖包
     *
     * @param packageName 包名称
     * @param version 版本要求
     * @param options 安装选项
     * @return 安装结果
     */
    fun install(packageName: String, version: String?, options: CjPackageOptions): CjPackageResult

    /**
     * 从项目配置文件安装所有依赖
     *
     * @param projectPath 项目路径
     * @param options 安装选项
     * @return 安装结果
     */
    fun installDependencies(projectPath: Path, options: CjPackageOptions): CjPackageResult

    /**
     * 更新依赖包
     *
     * @param packageName 包名称
     * @param options 更新选项
     * @return 更新结果
     */
    fun update(packageName: String?, options: CjPackageOptions): CjPackageResult

    /**
     * 移除依赖包
     *
     * @param packageName 包名称
     * @param options 移除选项
     * @return 移除结果
     */
    fun uninstall(packageName: String, options: CjPackageOptions): CjPackageResult

    companion object {
        /**
         * 包管理器名称
         */
        const val NAME = "packageManager"
    }
}

/**
 * CangJie代码格式化工具接口
 */
interface CjFormatter : CjTool {
    /**
     * 格式化文件
     *
     * @param filePath 文件路径
     * @param options 格式化选项
     * @return 格式化结果
     */
    fun format(filePath: Path, options: CjFormatOptions): CjFormatResult

    /**
     * 格式化多个文件
     *
     * @param filePaths 文件路径列表
     * @param options 格式化选项
     * @return 格式化结果
     */
    fun formatFiles(filePaths: List<Path>, options: CjFormatOptions): CjFormatResult

    /**
     * 格式化项目
     *
     * @param projectPath 项目路径
     * @param options 格式化选项
     * @return 格式化结果
     */
    fun formatProject(projectPath: Path, options: CjFormatOptions): CjFormatResult

    companion object {
        /**
         * 格式化工具名称
         */
        const val NAME = "formatter"
    }
}

/**
 * CangJie工具基础接口
 */
interface CjTool {
    /**
     * 工具所属的工具链
     */
    val toolchain: CjToolchain

    /**
     * 工具的可执行文件路径
     */
    val executable: Path

    /**
     * 获取工具版本
     *
     * @return 工具版本信息
     */
    fun getVersion(): String
}

/**
 * CangJie工具工厂接口
 */
interface CjToolFactory<T : CjTool> {
    /**
     * 创建工具实例
     *
     * @param toolchain 工具链实例
     * @return 工具实例
     */
    fun create(toolchain: CjToolchain): T
}

/**
 * CangJie编译选项
 */
interface CjCompileOptions {
    /**
     * 是否生成调试信息
     */
    val debug: Boolean

    /**
     * 优化级别
     */
    val optimizationLevel: OptimizationLevel

    /**
     * 警告级别
     */
    val warningLevel: WarningLevel

    /**
     * 目标平台
     */
    val targetPlatform: TargetPlatform?

    /**
     * 额外的编译器参数
     */
    val extraArgs: List<String>

    /**
     * 警告级别枚举
     */
    enum class WarningLevel {
        /**
         * 不显示任何警告
         */
        NONE,

        /**
         * 显示常规警告
         */
        NORMAL,

        /**
         * 显示所有警告
         */
        ALL,

        /**
         * 将警告视为错误
         */
        ERROR
    }

    /**
     * 优化级别枚举
     */
    enum class OptimizationLevel {
        /**
         * 不进行优化，保留调试信息
         */
        NONE,

        /**
         * 基本优化
         */
        BASIC,

        /**
         * 中等优化
         */
        MEDIUM,

        /**
         * 完全优化，可能会影响调试
         */
        FULL
    }

    /**
     * 目标平台枚举
     */
    enum class TargetPlatform {
        /**
         * JVM 平台
         */
        JVM,

        /**
         * 原生平台
         */
        NATIVE,

        /**
         * WebAssembly 平台
         */
        WASM,

        /**
         * JavaScript 平台
         */
        JS
    }
}

/**
 * CangJie编译结果
 */
interface CjCompileResult {
    /**
     * 编译是否成功
     */
    val success: Boolean

    /**
     * 编译输出的文件路径
     */
    val outputFiles: List<Path>

    /**
     * 编译过程中的诊断信息
     */
    val diagnostics: List<CjDiagnostic>

    /**
     * 编译器输出信息
     */
    val output: String

    /**
     * 编译结果状态
     */
    val status: CompilationStatus

    /**
     * 编译结果状态枚举
     */
    enum class CompilationStatus {
        /**
         * 成功
         */
        SUCCESS,

        /**
         * 有警告
         */
        WARNING,

        /**
         * 失败
         */
        FAILURE,

        /**
         * 取消
         */
        CANCELLED,

        /**
         * 超时
         */
        TIMEOUT
    }
}

/**
 * CangJie诊断信息
 */
interface CjDiagnostic {
    /**
     * 诊断级别
     */
    val level: Level

    /**
     * 诊断消息
     */
    val message: String

    /**
     * 源文件路径
     */
    val filePath: Path?

    /**
     * 行号
     */
    val line: Int?

    /**
     * 列号
     */
    val column: Int?

    /**
     * 诊断类型
     */
    val type: DiagnosticType

    /**
     * 诊断级别枚举
     */
    enum class Level {
        /**
         * 错误
         */
        ERROR,

        /**
         * 警告
         */
        WARNING,

        /**
         * 信息
         */
        INFO,

        /**
         * 提示
         */
        HINT
    }

    /**
     * 诊断类型枚举
     */
    enum class DiagnosticType {
        /**
         * 语法错误
         */
        SYNTAX,

        /**
         * 类型错误
         */
        TYPE,

        /**
         * 名称解析错误
         */
        NAME_RESOLUTION,

        /**
         * 编译器内部错误
         */
        INTERNAL,

        /**
         * 其他错误
         */
        OTHER
    }
}

/**
 * CangJie包管理选项
 */
interface CjPackageOptions {
    /**
     * 安装范围
     */
    val scope: InstallScope

    /**
     * 依赖类型
     */
    val dependencyType: DependencyType

    /**
     * 是否保存到项目配置
     */
    val saveMode: SaveMode

    /**
     * 额外的包管理器参数
     */
    val extraArgs: List<String>

    /**
     * 安装范围枚举
     */
    enum class InstallScope {
        /**
         * 全局安装
         */
        GLOBAL,

        /**
         * 项目安装
         */
        PROJECT
    }

    /**
     * 依赖类型枚举
     */
    enum class DependencyType {
        /**
         * 生产依赖
         */
        PRODUCTION,

        /**
         * 开发依赖
         */
        DEVELOPMENT,

        /**
         * 可选依赖
         */
        OPTIONAL,

        /**
         * 同级依赖
         */
        PEER
    }

    /**
     * 保存模式枚举
     */
    enum class SaveMode {
        /**
         * 不保存
         */
        NONE,

        /**
         * 保存精确版本
         */
        EXACT,

        /**
         * 保存兼容版本
         */
        COMPATIBLE
    }
}

/**
 * CangJie包管理结果
 */
interface CjPackageResult {
    /**
     * 操作是否成功
     */
    val success: Boolean

    /**
     * 受影响的包列表
     */
    val packages: List<CjPackageInfo>

    /**
     * 包管理器输出信息
     */
    val output: String

    /**
     * 操作类型
     */
    val operationType: OperationType

    /**
     * 操作状态
     */
    val status: OperationStatus

    /**
     * 操作类型枚举
     */
    enum class OperationType {
        /**
         * 安装
         */
        INSTALL,

        /**
         * 更新
         */
        UPDATE,

        /**
         * 卸载
         */
        UNINSTALL,

        /**
         * 列表
         */
        LIST
    }

    /**
     * 操作状态枚举
     */
    enum class OperationStatus {
        /**
         * 成功
         */
        SUCCESS,

        /**
         * 部分成功
         */
        PARTIAL_SUCCESS,

        /**
         * 失败
         */
        FAILURE,

        /**
         * 无变化
         */
        NO_CHANGE
    }
}

/**
 * CangJie包信息
 */
interface CjPackageInfo {
    /**
     * 包名称
     */
    val name: String

    /**
     * 包版本
     */
    val version: String

    /**
     * 依赖类型
     */
    val dependencyType: CjPackageOptions.DependencyType

    /**
     * 包状态
     */
    val status: PackageStatus

    /**
     * 包状态枚举
     */
    enum class PackageStatus {
        /**
         * 已安装
         */
        INSTALLED,

        /**
         * 已更新
         */
        UPDATED,

        /**
         * 已移除
         */
        REMOVED,

        /**
         * 未变化
         */
        UNCHANGED,

        /**
         * 已过时
         */
        OUTDATED
    }
}

/**
 * CangJie格式化选项
 */
interface CjFormatOptions {
    /**
     * 修改模式
     */
    val modificationMode: ModificationMode

    /**
     * 递归模式
     */
    val recursiveMode: RecursiveMode

    /**
     * 格式化风格
     */
    val style: FormatStyle

    /**
     * 额外的格式化工具参数
     */
    val extraArgs: List<String>

    /**
     * 修改模式枚举
     */
    enum class ModificationMode {
        /**
         * 就地修改文件
         */
        IN_PLACE,

        /**
         * 仅检查，不修改
         */
        CHECK_ONLY,

        /**
         * 输出到新文件
         */
        TO_NEW_FILE
    }

    /**
     * 递归模式枚举
     */
    enum class RecursiveMode {
        /**
         * 不递归
         */
        NONE,

        /**
         * 递归处理子目录
         */
        RECURSIVE
    }

    /**
     * 格式化风格枚举
     */
    enum class FormatStyle {
        /**
         * 官方风格
         */
        OFFICIAL,

        /**
         * Google风格
         */
        GOOGLE,

        /**
         * 自定义风格
         */
        CUSTOM
    }
}

/**
 * CangJie格式化结果
 */
interface CjFormatResult {
    /**
     * 格式化是否成功
     */
    val success: Boolean

    /**
     * 格式化的文件列表
     */
    val formattedFiles: List<Path>

    /**
     * 格式化工具输出信息
     */
    val output: String

    /**
     * 格式化状态
     */
    val status: FormatStatus

    /**
     * 格式化状态枚举
     */
    enum class FormatStatus {
        /**
         * 成功
         */
        SUCCESS,

        /**
         * 部分成功
         */
        PARTIAL_SUCCESS,

        /**
         * 失败
         */
        FAILURE,

        /**
         * 无变化
         */
        NO_CHANGE
    }
}