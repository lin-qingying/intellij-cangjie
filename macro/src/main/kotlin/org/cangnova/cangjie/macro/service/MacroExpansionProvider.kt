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
import org.cangnova.cangjie.macro.engine.MacroExpansionEngine
import org.cangnova.cangjie.result.CjResult

/**
 * 宏展开提供者接口
 *
 * 定义宏展开的基本能力。每个实现对应一种展开引擎（如 `cjc-frontend` 或 `LSPMacroServer`）。
 *
 * ## 设计原则：由来源者实现引擎信息
 *
 * 实现者通过 [engine] 属性描述自身，推荐直接实现 [org.cangnova.cangjie.macro.engine.MacroExpansionEngine]：
 *
 * ```kotlin
 * class MyProvider : MacroExpansionProvider, MacroExpansionEngine {
 *     override val engine: MacroExpansionEngine get() = this
 *     override val id = "my-provider"
 *     // ...
 * }
 * ```
 *
 * ## 内置实现
 *
 * - [org.cangnova.cangjie.macro.compiler.CompilerMacroExpansionProvider]
 *   — 通过 `cjc-frontend --debug-macro` 展开（文件级）
 * - [org.cangnova.cangjie.macro.server.LspMacroServerProvider]
 *   — 通过 `LSPMacroServer` 常驻进程展开（宏调用级，需进程通信）
 */
interface MacroExpansionProvider {

    /** 提供者名称（显示在日志和 UI 中） */
    val name: String

    /**
     * 展开引擎描述
     *
     * 描述该提供者使用的宏展开引擎，包括协议、粒度等元信息。
     * 引擎信息由提供者自身实现，而非集中定义在枚举中。
     */
    val engine: MacroExpansionEngine

    /**
     * 检查提供者是否可用（SDK 存在、可执行文件存在等）
     */
    fun isAvailable(project: Project): Boolean

    /**
     * 展开指定偏移量处的宏
     *
     * @param project 项目
     * @param file 源文件
     * @param offset 宏表达式的文本偏移量
     * @param options 展开选项
     */
    suspend fun expandMacroAtOffset(
        project: Project,
        file: VirtualFile,
        offset: Int,
        options: MacroExpansionOptions,
    ): CjResult<MacroExpansionResult, MacroExpansionError>

    /**
     * 展开文件中的所有宏
     *
     * @param project 项目
     * @param file 源文件
     * @param options 展开选项
     * @return 文件中所有宏的展开结果列表
     */
    suspend fun expandAllMacrosInFile(
        project: Project,
        file: VirtualFile,
        options: MacroExpansionOptions,
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError>

    /**
     * 展开指定范围内的宏
     *
     * @param project 项目
     * @param file 源文件
     * @param startOffset 范围起始偏移量
     * @param endOffset 范围结束偏移量
     * @param options 展开选项
     * @return 范围内所有宏的展开结果列表
     */
    suspend fun expandMacrosInRange(
        project: Project,
        file: VirtualFile,
        startOffset: Int,
        endOffset: Int,
        options: MacroExpansionOptions,
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError>
}