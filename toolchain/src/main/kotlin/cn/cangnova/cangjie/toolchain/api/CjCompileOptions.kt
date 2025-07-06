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
