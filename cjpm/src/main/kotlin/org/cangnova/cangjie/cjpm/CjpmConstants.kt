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

package org.cangnova.cangjie.cjpm

/**
 * Cjpm 包管理器常量
 *
 * 定义仓颉包管理器（Cjpm）相关的常量，包括：
 * - 配置文件名称
 * - 项目布局路径
 * - 构建相关路径
 *
 * 这些常量用于统一管理 Cjpm 项目的文件和目录结构。
 */
object CjpmConstants {

    /**
     * 锁文件名称列表
     *
     * Cjpm 支持的依赖锁定文件名称，用于锁定依赖版本：
     * - `cjpm.lock`: 新版锁文件
     * - `module-lock.json`: 旧版锁文件（向后兼容）
     */
    val LOCK_FILE = listOf("cjpm.lock", "module-lock.json")
//        get() {
//            return CjpmConstantsService.getInstance().LOCK_FILE
//        }

    /**
     * 构建脚本文件名
     *
     * Cjpm 项目的构建脚本文件名称，通常包含自定义构建逻辑。
     */
    const val BUILD_FILE = "build.cj"

    /**
     * 清单文件名
     *
     * Cjpm 项目的主配置文件，包含项目元数据、依赖声明等信息。
     */
    val MANIFEST_FILE = "cjpm.toml"

    /**
     * 项目布局常量
     *
     * 定义 Cjpm 项目的标准目录结构。
     */
    object ProjectLayout {
        /**
         * 源代码目录列表
         *
         * 包含项目源代码和示例代码的目录：
         * - `src`: 主源代码目录
         * - `examples`: 示例代码目录
         */
        val sources = listOf("src", "examples")

        /**
         * 测试代码目录列表
         *
         * 包含测试代码和基准测试的目录：
         * - `tests`: 单元测试和集成测试目录
         * - `benches`: 性能基准测试目录
         */
        val tests = listOf("tests", "benches")

        /**
         * 构建输出目录
         *
         * 所有构建产物（编译输出、中间文件等）的输出目录。
         */
        const val target = "target"
    }
}


