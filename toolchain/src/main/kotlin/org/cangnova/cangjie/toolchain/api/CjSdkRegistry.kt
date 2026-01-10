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

package org.cangnova.cangjie.toolchain.api

import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.toolchain.impl.CjSdkRegistryImpl
import java.nio.file.Path

/**
 * 仓颉 SDK 注册中心
 *
 * 负责管理所有已注册的仓颉 SDK,提供注册、查询、删除等功能
 */
interface CjSdkRegistry {
    /**
     * 获取所有已注册的 SDK
     *
     * @return SDK 列表
     */
    fun getAllSdks(): List<CjSdk>

    /**
     * 根据 ID 获取 SDK
     *
     * @param id SDK 唯一标识符
     * @return SDK 实例,如果不存在则返回 null
     */
    fun getSdk(id: String): CjSdk?

    /**
     * 根据路径获取 SDK
     *
     * @param homePath SDK 根目录路径
     * @return SDK 实例,如果不存在则返回 null
     */
    fun getSdkByPath(homePath: Path): CjSdk?

    /**
     * 注册一个新的 SDK
     *
     * @param sdk SDK 实例
     * @return 注册成功返回 true,如果 ID 已存在则返回 false
     */
    fun registerSdk(sdk: CjSdk): Boolean

    /**
     * 注册一个 SDK 路径
     *
     * 自动检测并创建 SDK 实例
     *
     * @param homePath SDK 根目录路径
     * @param customName 自定义名称(可选)
     * @return 注册的 SDK 实例,如果路径无效则返回 null
     */
    fun registerSdkPath(homePath: Path, customName: String? = null): CjSdk?

    /**
     * 取消注册 SDK
     *
     * @param id SDK 唯一标识符
     * @return 取消注册成功返回 true,如果 SDK 不存在则返回 false
     */
    fun unregisterSdk(id: String): Boolean

    /**
     * 检查 SDK 是否已注册
     *
     * @param id SDK 唯一标识符
     * @return 已注册返回 true
     */
    fun isSdkRegistered(id: String): Boolean

    /**
     * 检查路径对应的 SDK 是否已注册
     *
     * @param homePath SDK 根目录路径
     * @return 已注册返回 true
     */
    fun isSdkPathRegistered(homePath: Path): Boolean

    /**
     * 刷新所有 SDK 的信息
     *
     * 重新检测版本和有效性
     */
    fun refreshAllSdks()

    /**
     * 刷新指定 SDK 的信息
     *
     * @param id SDK 唯一标识符
     * @return 刷新后的 SDK 实例,如果不存在则返回 null
     */
    fun refreshSdk(id: String): CjSdk?

    companion object {
        /**
         * 获取应用级别的 SDK 注册中心实例
         */
        @JvmStatic
        fun getInstance(): CjSdkRegistry {
            return ApplicationManager.getApplication()
                .getService(CjSdkRegistryImpl::class.java)
        }
    }
}
