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
