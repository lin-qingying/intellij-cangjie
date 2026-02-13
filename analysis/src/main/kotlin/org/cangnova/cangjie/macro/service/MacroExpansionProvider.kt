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
 * 宏展开提供者接口
 *
 * 定义了宏展开的基本能力，由编译器实现
 */
interface MacroExpansionProvider {
    /**
     * 提供者名称
     */
    val name: String

    /**
     * 展开来源标识
     */
    val source: ExpansionSource

    /**
     * 检查提供者是否可用
     *
     * @param project 项目
     * @return 是否可用
     */
    fun isAvailable(project: Project): Boolean

    /**
     * 展开指定位置的宏
     *
     * @param project 项目
     * @param file 源文件
     * @param offset 宏表达式的偏移量
     * @param options 展开选项
     * @return 展开结果
     */
    suspend fun expandMacroAtOffset(
        project: Project,
        file: VirtualFile,
        offset: Int,
        options: MacroExpansionOptions
    ): CjResult<MacroExpansionResult, MacroExpansionError>

    /**
     * 展开文件中的所有宏
     *
     * @param project 项目
     * @param file 源文件
     * @param options 展开选项
     * @return 所有宏的展开结果列表
     */
    suspend fun expandAllMacrosInFile(
        project: Project,
        file: VirtualFile,
        options: MacroExpansionOptions
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError>

    /**
     * 展开指定范围内的宏
     *
     * @param project 项目
     * @param file 源文件
     * @param startOffset 起始偏移量
     * @param endOffset 结束偏移量
     * @param options 展开选项
     * @return 范围内宏的展开结果列表
     */
    suspend fun expandMacrosInRange(
        project: Project,
        file: VirtualFile,
        startOffset: Int,
        endOffset: Int,
        options: MacroExpansionOptions
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError>
}
