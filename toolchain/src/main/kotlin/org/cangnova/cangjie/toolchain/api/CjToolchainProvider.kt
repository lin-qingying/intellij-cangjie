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
 * CangJie工具链提供者接口
 *
 * 用于发现和创建工具链实例
 */
interface CjToolchainProvider {
    /**
     * 提供者唯一标识符
     */
    val id: String

    /**
     * 提供者显示名称
     */
    val displayName: String

    /**
     * 自动检测系统中可用的工具链
     *
     * @return 检测到的工具链列表
     */
    fun detectToolchains(): List<CjToolchain>

    /**
     * 根据路径创建工具链实例
     *
     * @param homePath 工具链主目录
     * @return 工具链实例，如果路径无效则返回null
     */
    fun createToolchain(homePath: Path): CjToolchain?


    companion object {
        private val EP_NAME: ExtensionPointName<CjToolchainProvider> =
            ExtensionPointName.create("org.cangnova.cangjie.toolchain.toolchainProvider")

        fun getToolchains(): List<CjToolchain> {
            return EP_NAME.extensionList.flatMap { it.detectToolchains() }
        }

        fun getToolchain(homePath: Path): CjToolchain? =
            EP_NAME.extensionList.asSequence()
                .mapNotNull { it.createToolchain(homePath) }
                .firstOrNull()
    }
}
