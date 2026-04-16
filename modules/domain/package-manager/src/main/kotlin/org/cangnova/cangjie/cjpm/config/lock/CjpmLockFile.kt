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

package org.cangnova.cangjie.cjpm.config.lock

/**
 * cjpm.lock 文件的数据模型
 *
 * cjpm.lock 文件格式示例:
 * ```toml
 * version = 0
 *
 * [requires]
 *   CJson = {branch = "master", commitId = "592150aac84940e90425ca6879f1af5952311d42", git = "https://gitcode.com/Cangjie-TPC/CJson.git"}
 *   MyLib = {version = "1.0.0", registry = "default"}
 * ```
 */
data class CjpmLockFile(
    /**
     * Lock 文件版本
     */
    val version: Int = 0,

    /**
     * 依赖信息映射
     * Key: 依赖名称
     * Value: 锁定的依赖信息
     */
    val requires: Map<String, LockedDependency> = emptyMap()
)

/**
 * 锁定的依赖信息
 */
sealed class LockedDependency {
    abstract val name: String

    /**
     * Git 依赖锁定信息
     */
    data class Git(
        override val name: String,
        val git: String,
        val commitId: String,
        val branch: String? = null,
        val tag: String? = null,
        val rev: String? = null
    ) : LockedDependency()

    /**
     * 注册表依赖锁定信息
     */
    data class Registry(
        override val name: String,
        val version: String,
        val registry: String = "default",
        val checksum: String? = null
    ) : LockedDependency()

    /**
     * 路径依赖锁定信息
     */
    data class Path(
        override val name: String,
        val path: String
    ) : LockedDependency()
}