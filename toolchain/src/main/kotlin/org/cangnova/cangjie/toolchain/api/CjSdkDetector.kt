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
import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import java.nio.file.Path

/**
 * 仓颉 SDK 检测器
 *
 * 负责检测指定路径是否为有效的仓颉 SDK,并提取版本信息
 */
interface CjSdkDetector {
    /**
     * 获取必需的可执行文件列表
     *
     * @return 必需的可执行文件名称列表(不含扩展名)
     */
    val requiredExecutables: List<String>

    /**
     * 检测指定路径是否为有效的仓颉 SDK
     *
     * @param homePath SDK 根目录路径
     * @return 如果是有效的 SDK 则返回 true
     */
    fun isValidSdk(homePath: Path): Boolean

    /**
     * 从 SDK 路径中检测版本信息
     *
     * @param homePath SDK 根目录路径
     * @return SDK 版本信息,如果无法检测则返回 null
     */
    fun detectVersion(homePath: Path): CangJieSdkVersion?

    /**
     * 从指定路径创建 SDK 实例
     *
     * @param homePath SDK 根目录路径
     * @param customName 自定义名称(可选)
     * @return SDK 实例,如果路径无效则返回 null
     */
    fun createSdk(homePath: Path, customName: String? = null): CjSdk?

    /**
     * 检查 SDK 的必需可执行文件是否存在
     *
     * @param homePath SDK 根目录路径
     * @return 缺失的可执行文件列表,如果全部存在则返回空列表
     */
    fun checkRequiredExecutables(homePath: Path): List<String>

    companion object {
        /**
         * 扩展点名称
         */

        val EP_NAME = ExtensionPointName<CjSdkDetector>("org.cangnova.cangjie.toolchain.sdkDetector")

        /**
         * 获取默认的 SDK 检测器实例
         *
         * @return 第一个注册的检测器，如果没有则返回 null
         */
        @JvmStatic
        fun getInstance(): CjSdkDetector? {
            return EP_NAME.extensionList.firstOrNull()
        }

        /**
         * 获取所有注册的 SDK 检测器
         */
        @JvmStatic
        fun getAllDetectors(): List<CjSdkDetector> {
            return EP_NAME.extensionList
        }
    }
}
