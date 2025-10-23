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

package org.cangnova.cangjie.toolchain.api

import com.intellij.openapi.extensions.ExtensionPointName
import java.nio.file.Path

/**
 * 仓颉 SDK 发现器
 *
 * 负责在系统中自动发现和扫描 SDK 路径
 */
interface CjSdkDiscoverer {
    /**
     * 发现器的优先级
     * 数值越小优先级越高，默认为 100
     */
    val priority: Int
        get() = 100

    /**
     * 发现器的显示名称
     */
    val displayName: String

    /**
     * 发现系统中所有可能的 SDK 路径
     *
     * @return SDK 路径列表
     */
    fun discoverSdkPaths(): List<Path>

    /**
     * 在指定路径下搜索 SDK
     *
     * @param searchPath 搜索根路径
     * @param recursive 是否递归搜索子目录
     * @param maxDepth 最大递归深度（仅在 recursive=true 时有效）
     * @return 找到的 SDK 路径列表
     */
    fun searchSdkPaths(searchPath: Path, recursive: Boolean = true, maxDepth: Int = 3): List<Path>

    companion object {
        /**
         * 扩展点名称
         */
        @JvmField
        val EP_NAME = ExtensionPointName<CjSdkDiscoverer>("org.cangnova.cangjie.toolchain.sdkDiscoverer")

        /**
         * 获取默认的 SDK 发现器实例
         *
         * @return 第一个注册的发现器，如果没有则返回 null
         */
        @JvmStatic
        fun getInstance(): CjSdkDiscoverer? {
            return EP_NAME.extensionList.firstOrNull()
        }

        /**
         * 获取所有注册的 SDK 发现器（按优先级排序）
         */
        @JvmStatic
        fun getAllDiscoverers(): List<CjSdkDiscoverer> {
            return EP_NAME.extensionList.sortedBy { it.priority }
        }

        /**
         * 使用所有发现器发现 SDK 路径（去重）
         */
        @JvmStatic
        fun discoverAllSdkPaths(): List<Path> {
            return getAllDiscoverers()
                .flatMap { it.discoverSdkPaths() }
                .distinct()
        }
    }
}
