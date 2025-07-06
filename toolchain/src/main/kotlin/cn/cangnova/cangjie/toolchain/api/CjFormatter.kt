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

package cn.cangnova.cangjie.toolchain.api

import java.nio.file.Path

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
