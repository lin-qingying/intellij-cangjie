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

package org.cangnova.cangjie.model

import java.nio.file.Path

/**
 * 构建配置接口
 */
interface CjBuildConfiguration {
    /**
     * 配置名称
     */
    val name: String

    /**
     * 源目录列表
     */
    val sourceDirs: List<Path>

    /**
     * 资源目录列表
     */
    val resourceDirs: List<Path>

    /**
     * 输出目录
     */
    val outputDir: Path

    /**
     * 测试源目录列表
     */
    val testSourceDirs: List<Path>

    /**
     * 测试资源目录列表
     */
    val testResourceDirs: List<Path>

    /**
     * 测试输出目录
     */
    val testOutputDir: Path

    /**
     * 编译器选项
     */
    val compilerOptions: Map<String, String>

    /**
     * 链接器选项
     */
    val linkerOptions: Map<String, String>

    /**
     * 是否启用优化
     */
    val optimizationEnabled: Boolean
        get() = false

    /**
     * 优化级别 (0-3)
     */
    val optimizationLevel: Int
        get() = 0

    /**
     * 是否生成调试信息
     */
    val debugInfoEnabled: Boolean
        get() = true

    /**
     * 验证配置是否有效
     */
    fun validate(): Boolean
}