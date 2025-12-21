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

package org.cangnova.cangjie.cjo

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.name.FqName

/**
 * CJO 文件提供者接口
 *
 * 扩展点接口，用于提供 CJO/CJB 元数据文件的位置。
 * 不同的实现可以从不同来源提供 CJO 文件：
 *
 * - 标准库提供者：从 SDK 目录获取
 * - 项目依赖提供者：从项目依赖中获取
 * - 外部库提供者：从环境变量或配置路径获取
 *
 * ## 扩展点注册
 *
 * ```xml
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *     <cjoProvider implementation="...StandardLibraryCjoProvider"/>
 * </extensions>
 * ```
 *
 * @see CjoPackageService
 */
interface CjoProvider {

    /**
     * 提供者的显示名称（用于调试和日志）
     */
    val displayName: String

    /**
     * 提供者的优先级
     *
     * 较高优先级的提供者会被优先查询。
     * 标准库通常有最高优先级。
     */
    val priority: Int get() = PRIORITY_NORMAL

    /**
     * 检查提供者是否适用于当前项目
     *
     * @param project 项目实例
     * @return 如果提供者可以为此项目提供 CJO 文件，返回 true
     */
    fun isApplicable(project: Project): Boolean = true

    /**
     * 获取指定包的 CJO 文件
     *
     * @param project 项目实例
     * @param packageFqName 包的完全限定名
     * @return CJO/CJB 文件，如果找不到返回 null
     */
    fun getPackageFile(project: Project, packageFqName: FqName): VirtualFile?

    /**
     * 获取此提供者可以提供的所有包名
     *
     * 此方法用于预加载或索引场景。如果无法高效地列举所有包，
     * 可以返回空集合，依赖按需查找。
     *
     * @param project 项目实例
     * @return 可用的包名集合
     */
    fun getAvailablePackages(project: Project): Set<FqName> = emptySet()

    /**
     * 获取此提供者的所有 CJO 文件根目录
     *
     * @param project 项目实例
     * @return CJO 文件根目录列表
     */
    fun getRoots(project: Project): List<VirtualFile> = emptyList()

    companion object {
        val EP_NAME: ExtensionPointName<CjoProvider> =
            ExtensionPointName.create("org.cangnova.cangjie.cjoProvider")

        /**
         * 优先级常量
         */
        const val PRIORITY_HIGHEST = 1000
        const val PRIORITY_HIGH = 750
        const val PRIORITY_NORMAL = 500
        const val PRIORITY_LOW = 250
        const val PRIORITY_LOWEST = 0
    }
}

/**
 * CJO 提供者类型枚举
 *
 * 标识不同类型的 CJO 文件来源
 */
enum class CjoProviderType {
    /**
     * 标准库（SDK 内置）
     */
    STANDARD_LIBRARY,

    /**
     * 项目依赖（通过 cjpm 管理）
     */
    PROJECT_DEPENDENCY,

    /**
     * 外部库（环境变量或配置路径）
     */
    EXTERNAL_LIBRARY,

    /**
     * 项目模块（当前项目编译产物）
     */
    PROJECT_MODULE
}

/**
 * 带类型的 CJO 提供者接口
 *
 * 扩展 CjoProvider，增加类型标识
 */
interface TypedCjoProvider : CjoProvider {
    /**
     * 提供者类型
     */
    val providerType: CjoProviderType
}
