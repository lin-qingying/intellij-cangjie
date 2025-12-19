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
import com.intellij.openapi.roots.TestModuleProperties
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import org.cangnova.cangjie.cache.cacheByClassInvalidatingOnRootModifications
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.CangJieResolveScopeEnlarger
import org.cangnova.cangjie.utils.addIfNotNull

/**
 * 模块测试源码信息
 *
 * 该数据类表示模块的测试源码信息，继承自 [ModuleSourceInfoWithExpectedBy]。
 * 它代表模块的测试代码部分。
 *
 * ## 设计目的
 *
 * [ModuleTestSourceInfo] 为模块的测试源码提供完整的元数据：
 * 1. **测试作用域**: 提供测试源码的搜索范围
 * 2. **依赖管理**: 自动包含生产源码的依赖
 * 3. **内部可见性**: 可以访问生产源码和测试模块的 internal 声明
 * 4. **生产模块关联**: 支持与生产模块的关联
 *
 * ## 核心特性
 *
 * ### 1. 测试源码作用域
 * - [contentScope] 返回测试源码的搜索范围
 * - 通过 [CangJieResolveScopeEnlarger] 扩展作用域
 *
 * ### 2. 内部可见性
 * - 可以访问本模块生产源码的 internal 声明
 * - 可以访问关联生产模块的 internal 声明
 * - 可以访问额外可见模块的 internal 声明
 *
 * ## 使用场景
 *
 * ### 获取测试源码信息
 * ```kotlin
 * val module: Module = getModule()
 * val testInfo = module.testSourceInfo
 * ```
 *
 * ### 检查内部可见性
 * ```kotlin
 * val testInfo = ModuleTestSourceInfo(module)
 * val visibleModules = testInfo.modulesWhoseInternalsAreVisible()
 * // 包含：本模块生产源码、关联生产模块等
 * ```
 *
 * @param module IntelliJ 模块对象
 *
 * @see ModuleSourceInfoWithExpectedBy
 * @see ModuleProductionSourceInfo
 * @see TestModuleProperties
 */
data class ModuleTestSourceInfo internal constructor(
    override val module: Module
) : ModuleSourceInfoWithExpectedBy(forProduction = false), IdeaModuleInfo {

    /**
     * 模块名称
     *
     * 使用特殊名称格式 `<test sources for module 模块名>`，用于内部标识。
     */
    override val name: Name
        get() = Name.special("<test sources for module ${module.name}>")

    /**
     * 内容搜索作用域
     *
     * 返回测试源码的全局搜索范围，包含所有测试源码文件。
     *
     * ## 作用域扩展
     *
     * 通过 [CangJieResolveScopeEnlarger] 扩展作用域，
     * 可能包含额外的测试相关文件。
     *
     * @return 测试源码的全局搜索范围
     * @see CangJieResolveScopeEnlarger
     */
    override val contentScope: GlobalSearchScope
        get() = CangJieResolveScopeEnlarger.enlargeScope(module.cangjieTestSourceScope, module, isTestScope = true)

    /**
     * 获取仓颉测试源码作用域
     *
     * 扩展属性，返回模块测试源码的基础搜索范围。
     *
     * @return 测试源码的全局搜索范围
     */
    private val Module.cangjieTestSourceScope: GlobalSearchScope
        get() = ModuleSourcesScope.tests(module)

    /**
     * 可以访问 internal 声明的模块列表
     *
     * 返回测试源码可以访问其 internal 声明的模块列表。
     *
     * ## 可见性规则
     *
     * 测试源码可以访问以下模块的 internal 声明：
     * 1. **本模块的生产源码**: 测试代码可以访问被测试代码的 internal 成员
     * 2. **关联生产模块**: 如果当前是测试模块，可以访问关联生产模块的 internal 成员
     * 3. **额外可见模块**: 通过配置指定的额外可见模块
     *
     * ## 缓存机制
     *
     * 结果通过 [cacheByClassInvalidatingOnRootModifications] 缓存，
     * 仅在模块根目录变更时重新计算。
     *
     * @return 可以访问 internal 声明的模块集合
     * @see modulesWhoseInternalsAreVisible
     */
    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> =
        module.cacheByClassInvalidatingOnRootModifications(KeyForModulesWhoseInternalsAreVisible::class.java) {
            val list = SmartList<ModuleInfo>()

            // 添加本模块的生产源码
            list.addIfNotNull(module.productionSourceInfo)

            // 添加关联生产模块的生产源码
            TestModuleProperties.getInstance(module).productionModule?.let {
                list.addIfNotNull(it.productionSourceInfo)
            }

            // 添加额外可见模块
            list.addAll(module.additionalVisibleModules.mapNotNull { additionalVisibleModule ->
                additionalVisibleModule.productionSourceInfo ?:
                // 对于测试源码，也要考虑 testFixture 作为额外可见模块
                module.testSourceInfo?.let { additionalVisibleModule.testSourceInfo }
            })

            list.toHashSet()
        }



    /**
     * 缓存键对象
     *
     * 用于 [modulesWhoseInternalsAreVisible] 方法的缓存键。
     */
    private object KeyForModulesWhoseInternalsAreVisible
}

/**
 * 获取模块的额外可见模块列表
 *
 * 扩展属性，返回通过配置指定为对当前模块可见的其他模块列表。
 * 仓颉语言当前不使用 facet 配置，因此返回空列表。
 *
 *
 * @return 额外可见的模块列表（当前为空）
 */
val Module.additionalVisibleModules: List<Module>
    get() = emptyList()
