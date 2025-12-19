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

package org.cangnova.cangjie.moduleinfo

import com.intellij.openapi.module.Module
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.cache.cacheByClassInvalidatingOnRootModifications
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.CangJieResolveScopeEnlarger

/**
 * 模块生产源码信息
 *
 * 该数据类表示模块的生产源码信息，继承自 [ModuleSourceInfoWithExpectedBy]。
 * 它代表模块的主要源代码部分（非测试代码）。
 *
 * ## 设计目的
 *
 * [ModuleProductionSourceInfo] 为模块的生产源码提供完整的元数据：
 * 1. **生产作用域**: 提供生产源码的搜索范围
 * 2. **依赖管理**: 管理生产代码的依赖关系
 * 3. **内部可见性**: 控制哪些模块可以访问本模块的 internal 声明
 * 4. **符号解析**: 为代码分析提供正确的解析上下文
 *
 * ## 核心特性
 *
 * ### 1. 生产源码作用域
 * - [contentScope] 返回生产源码的搜索范围
 * - 通过 [CangJieResolveScopeEnlarger] 扩展作用域
 * - 不包含测试源码文件
 *
 * ### 2. 内部可见性
 * - 只有配置为额外可见的模块才能访问本模块的 internal 声明
 * - 测试源码自动可以访问生产源码的 internal 声明
 *
 * ## 使用场景
 *
 * ### 获取生产源码信息
 * ```kotlin
 * val module: Module = getModule()
 * val productionInfo = module.productionSourceInfo
 * ```
 *
 * ### 检查内部可见性
 * ```kotlin
 * val productionInfo = ModuleProductionSourceInfo(module)
 * val visibleModules = productionInfo.modulesWhoseInternalsAreVisible()
 * // 返回可以访问本模块 internal 声明的模块列表
 * ```
 *
 * ### 获取内容作用域
 * ```kotlin
 * val productionInfo = ModuleProductionSourceInfo(module)
 * val scope = productionInfo.contentScope
 * // 在此作用域中搜索符号
 * ```
 *
 * @param module IntelliJ 模块对象
 *
 * @see ModuleSourceInfoWithExpectedBy
 * @see ModuleTestSourceInfo
 * @see CangJieResolveScopeEnlarger
 */
data class ModuleProductionSourceInfo internal constructor(
    override val module: Module
) : ModuleSourceInfoWithExpectedBy(forProduction = true) {

    /**
     * 模块名称
     *
     * 使用特殊名称格式 `<production sources for module 模块名>`，用于内部标识。
     *
     * @return 模块的内部名称
     */
    override val name: Name
        get() = Name.special("<production sources for module ${module.name}>")

    /**
     * 内容搜索作用域
     *
     * 返回生产源码的全局搜索范围，包含所有生产源码文件（不包含测试文件）。
     *
     * ## 作用域扩展
     *
     * 通过 [CangJieResolveScopeEnlarger] 扩展作用域，
     * 可能包含额外的生产相关文件。
     *
     * ## 测试源码隔离
     *
     * 此作用域不包含测试源码文件，确保生产代码和测试代码的正确隔离。
     *
     * @return 生产源码的全局搜索范围
     * @see CangJieResolveScopeEnlarger
     */
    override val contentScope: GlobalSearchScope
        get() = CangJieResolveScopeEnlarger.enlargeScope(module.cangjieProductionSourceScope, module, isTestScope = false)

    /**
     * 获取仓颉生产源码作用域
     *
     * 扩展属性，返回模块生产源码的基础搜索范围。
     *
     * @return 生产源码的全局搜索范围
     */
    private val Module.cangjieProductionSourceScope: GlobalSearchScope
        get() = ModuleSourcesScope.production(module)

    /**
     * 可以访问 internal 声明的模块列表
     *
     * 返回可以访问本模块 internal 声明的其他模块列表。
     *
     * ## 可见性规则
     *
     * 以下模块可以访问本模块的 internal 声明：
     * 1. **本模块的测试源码**: 自动可见（在 [ModuleTestSourceInfo] 中处理）
     * 2. **额外可见模块**: 通过配置指定的模块
     *
     * ## 缓存机制
     *
     * 结果通过 [cacheByClassInvalidatingOnRootModifications] 缓存，
     * 仅在模块根目录变更时重新计算。
     *
     * ## 当前实现
     *
     * 仓颉语言当前不使用额外可见模块功能（[Module.additionalVisibleModules] 返回空列表），
     * 因此此方法总是返回空集合。
     *
     * @return 可以访问 internal 声明的模块集合
     * @see modulesWhoseInternalsAreVisible
     * @see Module.additionalVisibleModules
     */
    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> {
        return module.cacheByClassInvalidatingOnRootModifications(KeyForModulesWhoseInternalsAreVisible::class.java) {
            // 获取额外可见模块的生产源码信息
            module.additionalVisibleModules.mapNotNull { it.productionSourceInfo }
        }
    }

    /**
     * 缓存键对象
     *
     * 用于 [modulesWhoseInternalsAreVisible] 方法的缓存键。
     */
    private object KeyForModulesWhoseInternalsAreVisible
}
