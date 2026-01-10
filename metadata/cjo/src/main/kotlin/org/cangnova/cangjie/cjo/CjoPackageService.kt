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

package org.cangnova.cangjie.cjo

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.FqName

/**
 * CJO 包元数据服务接口
 *
 * 项目级服务，提供统一的包元数据访问接口。
 * 通过扩展点系统支持多种 CJO 来源。
 *
 * ## 核心功能
 *
 * 1. **按需加载**: 只在需要时解析 CJO 文件
 * 2. **自动缓存**: 使用 IntelliJ 缓存系统管理包元数据
 * 3. **缓存失效**: 文件修改或项目结构变化时自动清除
 * 4. **跨包查找**: 支持完整的 FullId 解析所需的包查找
 * 5. **扩展点支持**: 通过 CjoProvider 扩展点支持多种来源
 * 6. **库支持**: 从工作空间库的 classes 中查找 CJO 文件
 *
 * ## 使用方式
 *
 * ```kotlin
 * val service = CjoPackageService.getInstance(project)
 *
 * // 通过包名获取（会查询所有 CjoProvider）
 * val wrapper = service.getPackage(FqName("std.collection"))
 *
 * // 通过文件获取
 * val wrapper = service.getPackageFromFile(virtualFile)
 * ```
 *
 * ## 服务初始化
 *
 * 该服务通过 XML 配置显式注册（非轻量级服务），确保在项目启动时主动初始化，
 * 以便及时监听 WorkspaceModel 变更事件。
 *
 * @see PackageWrapper
 * @see CjoFileLoader
 * @see CjoProvider
 */
interface CjoPackageService {

    /**
     * 获取包元数据
     *
     * 查找顺序：
     * 1. 直接缓存
     * 2. 已注册的文件映射
     * 3. CjoProvider 扩展点
     *
     * @param packageFqName 包的完全限定名
     * @return 包装器，如果找不到返回 null
     */
    fun getPackage(packageFqName: FqName): PackageWrapper?

    /**
     * 通过虚拟文件获取包元数据
     *
     * @param virtualFile CJO/CJB 文件
     * @return 包装器，如果加载失败返回 null
     */
    fun getPackageFromFile(virtualFile: VirtualFile): PackageWrapper?

    /**
     * 注册包文件位置
     *
     * @param packageFqName 包的完全限定名
     * @param virtualFile 包文件
     */
    fun registerPackageFile(packageFqName: FqName, virtualFile: VirtualFile)

    /**
     * 注册已加载的包
     *
     * @param packageWrapper 包装器
     */
    fun registerPackage(packageWrapper: PackageWrapper)

    /**
     * 获取所有已注册的包名
     *
     * @return 所有已注册的包名集合
     */
    fun getAllPackageNames(): Set<FqName>

    /**
     * 检查包是否存在
     *
     * @param packageFqName 包的完全限定名
     * @return true 如果包存在
     */
    fun hasPackage(packageFqName: FqName): Boolean

    /**
     * 使指定包的缓存失效
     *
     * @param packageFqName 包的完全限定名
     */
    fun invalidateCache(packageFqName: FqName)

    /**
     * 清空所有缓存
     */
    fun clearCache()

    /**
     * 解析 FullId
     *
     * 快捷方法，委托给 CjoFullIdResolver 服务
     *
     * @param fullId 完整ID
     * @param currentPackage 当前包上下文
     * @return 解析结果
     */
    fun resolveFullId(
        fullId: org.cangnova.cangjie.metadata.model.fb.FbFullId,
        currentPackage: org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
    ): org.cangnova.cangjie.cjo.NameResolveResult

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目实例
         * @return 服务实例
         */
        fun getInstance(project: Project): CjoPackageService {
            return project.getService(CjoPackageService::class.java)
        }
    }
}