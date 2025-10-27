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

package org.cangnova.cangjie.project.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * 构建目标类型
 */
enum class CjTargetType {
    /**
     * 可执行文件
     */
    EXECUTABLE,

    /**
     * 静态库
     */
    STATIC_LIBRARY,

    /**
     * 动态库
     */
    DYNAMIC_LIBRARY,

    /**
     * 测试目标
     */
    TEST
}

/**
 * 构建目标抽象
 *
 * 代表一个可构建的目标(如可执行文件、库等)
 */
interface CjTarget {
    /**
     * 目标名称
     */
    val name: String

    /**
     * 目标类型
     */
    val type: CjTargetType

    /**
     * 所属模块
     */
    val module: CjModule

    /**
     * 源码集
     */
    val sourceSets: List<CjSourceSet>

    /**
     * 输出目录（如果有）
     */
    val outputDirectory: VirtualFile?
        get() = null
}