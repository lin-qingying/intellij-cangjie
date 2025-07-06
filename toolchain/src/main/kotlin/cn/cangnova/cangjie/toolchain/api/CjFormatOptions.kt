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
