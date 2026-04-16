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

import com.intellij.openapi.project.Project

/**
 * 项目SDK配置接口
 *
 * 管理项目级别的CangJie SDK配置
 */
interface CjProjectSdkConfig {
    /**
     * 获取项目关联的SDK ID
     *
     * @return SDK ID，如果未配置则返回null
     */
    fun getProjectSdkId(): String?

    /**
     * 设置项目关联的SDK ID
     *
     * @param sdkId SDK ID，设置为null表示清除配置
     */
    fun setProjectSdkId(sdkId: String?)

    /**
     * 获取项目关联的SDK实例
     *
     * @return SDK实例，如果未配置或SDK不存在则返回null
     */
    fun getProjectSdk(): CjSdk?

    /**
     * 检查项目是否已配置SDK
     *
     * @return 如果已配置返回true，否则返回false
     */
    fun hasProjectSdk(): Boolean

    companion object {
        /**
         * 获取项目SDK配置实例
         *
         * @param project 项目实例
         * @return 项目SDK配置实例
         */
        @JvmStatic
        fun getInstance(project: Project): CjProjectSdkConfig {
            return project.getService(CjProjectSdkConfig::class.java)
        }
    }
}
