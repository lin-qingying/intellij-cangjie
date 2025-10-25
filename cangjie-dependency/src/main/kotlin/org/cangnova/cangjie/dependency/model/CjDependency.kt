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

package org.cangnova.cangjie.dependency.model

/**
 * 依赖范围
 */
enum class CjDependencyScope {
    /**
     * 编译时依赖
     */
    COMPILE,

    /**
     * 运行时依赖
     */
    RUNTIME,

    /**
     * 测试依赖
     */
    TEST,

    /**
     * 已提供依赖 (编译时需要，运行时由环境提供)
     */
    PROVIDED
}

/**
 * 依赖类型
 */
enum class CjDependencyType {
    /**
     * 外部库依赖
     */
    LIBRARY,

    /**
     * 项目内模块依赖
     */
    MODULE,

    /**
     * 系统依赖 (如标准库)
     */
    SYSTEM
}

/**
 * 依赖抽象接口
 */
interface CjDependency {
    /**
     * 依赖名称
     */
    val name: String

    /**
     * 依赖组 (可选，类似 Maven 的 groupId)
     */
    val group: String?

    /**
     * 依赖版本
     */
    val version: CjVersion

    /**
     * 依赖范围
     */
    val scope: CjDependencyScope

    /**
     * 依赖类型
     */
    val type: CjDependencyType

    /**
     * 是否传递依赖
     */
    val transitive: Boolean
        get() = true

    /**
     * 排除的传递依赖列表
     */
    val excludes: List<String>
        get() = emptyList()
}