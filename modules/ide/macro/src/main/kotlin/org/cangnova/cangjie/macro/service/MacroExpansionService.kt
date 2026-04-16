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
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.result.CjResult

/**
 * 宏展开服务接口
 *
 * 提供宏展开功能的统一入口，通过编译器 `cjc-frontend --dump-macro` 实现宏展开，
 * 并提供缓存机制。
 *
 * ## 使用示例
 *
 * ```kotlin
 * val service = MacroExpansionService.getInstance(project)
 *
 * // 展开单个宏表达式
 * val result = service.expandMacro(macroExpression)
 *
 * // 展开文件中的所有宏
 * val results = service.expandAllMacrosInFile(file)
 * ```
 */
interface MacroExpansionService {

    /**
     * 展开宏表达式
     *
     * @param macroExpression 宏表达式 PSI 元素
     * @param options 展开选项
     * @return 展开结果
     */
    suspend fun expandMacro(
        macroExpression: CjMacroExpression,
        options: MacroExpansionOptions = MacroExpansionOptions.DEFAULT
    ): CjResult<MacroExpansionResult, MacroExpansionError>

    /**
     * 展开指定位置的宏
     *
     * @param file 源文件
     * @param offset 偏移量
     * @param options 展开选项
     * @return 展开结果
     */
    suspend fun expandMacroAtOffset(
        file: VirtualFile,
        offset: Int,
        options: MacroExpansionOptions = MacroExpansionOptions.DEFAULT
    ): CjResult<MacroExpansionResult, MacroExpansionError>

    /**
     * 展开文件中的所有宏
     *
     * @param file 源文件
     * @param options 展开选项
     * @return 所有宏的展开结果列表
     */
    suspend fun expandAllMacrosInFile(
        file: VirtualFile,
        options: MacroExpansionOptions = MacroExpansionOptions.DEFAULT
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError>

    /**
     * 检查服务是否可用
     *
     * @return 编译器是否可用
     */
    fun isAvailable(): Boolean

    /**
     * 清除缓存
     */
    fun clearCache()

    /**
     * 清除指定文件的缓存
     *
     * @param file 文件
     */
    fun clearCacheForFile(file: VirtualFile)

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目
         * @return 宏展开服务实例
         */
        @JvmStatic
        fun getInstance(project: Project): MacroExpansionService {
            return project.getService(MacroExpansionService::class.java)
        }
    }
}
