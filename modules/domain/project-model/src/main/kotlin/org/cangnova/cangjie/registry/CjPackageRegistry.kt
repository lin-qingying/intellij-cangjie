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

package org.cangnova.cangjie.registry

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.model.CjPackageMetadata
import org.cangnova.cangjie.project.model.CjVersion

/**
 * 包注册表接口
 *
 * 管理已下载和已安装的包
 */
@Service(Service.Level.PROJECT)
interface CjPackageRegistry {
    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): CjPackageRegistry {
            return project.getService(CjPackageRegistry::class.java)
        }
    }

    /**
     * 注册包
     *
     * @param pkg 要注册的包
     */
    fun registerPackage(pkg: CjPackageMetadata)

    /**
     * 取消注册包
     *
     * @param name 包名称
     * @param version 包版本
     */
    fun unregisterPackage(name: String, version: CjVersion)

    /**
     * 查找包
     *
     * @param name 包名称
     * @param version 包版本
     * @return 找到的包，如果不存在返回 null
     */
    fun findPackage(name: String, version: CjVersion): CjPackageMetadata?

    /**
     * 获取所有已注册的包
     */
    fun getAllPackages(): List<CjPackageMetadata>

    /**
     * 检查包是否已注册
     *
     * @param name 包名称
     * @param version 包版本
     * @return 如果已注册返回 true
     */
    fun isPackageRegistered(name: String, version: CjVersion): Boolean
}