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

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.*
import org.cangnova.cangjie.config.CangJieSourceRootTypes
import org.cangnova.cangjie.moduleinfo.cache.LibraryInfoCache
import org.jetbrains.jps.model.module.UnknownSourceRootType

/**
 * 模块依赖收集器
 *
 * 该服务用于收集模块的依赖关系，包括：
 * 1. 其他模块依赖
 * 2. 库依赖
 * 3. 传递依赖（可选）
 *
 * ## 核心功能
 *
 * - **依赖遍历**: 按照依赖顺序遍历所有模块依赖
 * - **作用域区分**: 区分生产源码和测试源码的依赖
 * - **缓存支持**: 依赖信息通常会被上层缓存
 *
 * ## 使用场景
 *
 * ```kotlin
 * val collector = ModuleDependencyCollector.getInstance(project)
 * val dependencies = collector.collectModuleDependencies(
 *     module = module,
 *     rootTypeId = CangJieSourceRootTypes.SOURCE,
 *     includeExportedDependencies = true
 * )
 * ```
 *
 * @param project 当前项目实例
 *
 * @see ModuleSourceInfo
 * @see IdeaModuleInfo
 */
@Service(Service.Level.PROJECT)
class ModuleDependencyCollector(private val project: Project) {
    companion object {
        private val LOG = Logger.getInstance(ModuleDependencyCollector::class.java)

        /**
         * 获取项目的 ModuleDependencyCollector 实例
         *
         * @param project 项目对象
         * @return ModuleDependencyCollector 服务实例
         */
        fun getInstance(project: Project): ModuleDependencyCollector = project.service()
    }

    /**
     * 收集模式枚举
     *
     * 控制收集哪些依赖：
     * - **COLLECT_NON_IGNORED**: 收集未被忽略的依赖（默认）
     * - **COLLECT_IGNORED**: 收集被忽略的依赖
     */
    enum class CollectionMode {
        /** 收集被忽略的依赖 */
        COLLECT_IGNORED,

        /** 收集未被忽略的依赖 */
        COLLECT_NON_IGNORED;
    }


    /**
     * 收集模块依赖
     *
     * 遍历模块的所有依赖项（模块、库），并返回对应的 ModuleInfo 列表。
     *
     * ## 依赖顺序
     *
     * 返回的依赖列表按照以下顺序排列：
     * 1. 当前模块本身
     * 2. 直接模块依赖
     * 3. 库依赖
     * 4. 传递依赖（如果 includeExportedDependencies = true）
     *
     * ## 生产/测试区分
     *
     * - **生产源码** (`CangJieSourceRootTypes.SOURCE`): 只收集生产依赖
     * - **测试源码** (`CangJieSourceRootTypes.TEST`): 收集生产 + 测试依赖
     *
     * @param module 要收集依赖的模块
     * @param rootTypeId 源码根类型标识符（生产或测试）
     * @param includeExportedDependencies 是否包含导出的传递依赖
     * @param collectionMode 收集模式，默认收集未被忽略的依赖
     * @return 依赖的 ModuleInfo 集合
     *
     * @see CangJieSourceRootTypes
     * @see IdeaModuleInfo
     */
    fun collectModuleDependencies(
        module: Module,
        rootTypeId: String,
        includeExportedDependencies: Boolean,
        collectionMode: CollectionMode = CollectionMode.COLLECT_NON_IGNORED,
    ): Collection<IdeaModuleInfo> {
        val debugInfo = if (LOG.isDebugEnabled) ArrayList<String>() else null
        val isForTests = CangJieSourceRootTypes.isTestSource(rootTypeId)

        val orderEnumerator = getOrderEnumerator(module, isForTests, includeExportedDependencies)

        val result = LinkedHashSet<IdeaModuleInfo>()

        orderEnumerator.forEach { orderEntry ->
            if (isApplicable(orderEntry, isForTests)) {
                debugInfo?.add("Add constructor ${orderEntry.presentableName}")
                for (moduleInfo in collectModuleDependenciesForOrderEntry(orderEntry, isForTests)) {
                    debugInfo?.add("Add module ${moduleInfo.displayedName}")
                    result.add(moduleInfo)
                }
            } else {
                debugInfo?.add("Skip constructor ${orderEntry.presentableName}")
            }

            return@forEach true
        }

        if (debugInfo != null) {
            val debugString = buildString {
                appendLine("Building dependency list for module ${module.name}")
                appendLine("isForTests = $isForTests")
                debugInfo.joinTo(this, separator = "; ", prefix = "[", postfix = "]")
            }
            LOG.debug(debugString)
        }

        return result
    }

    /**
     * 获取依赖顺序枚举器
     *
     * 根据源码类型和配置创建合适的依赖枚举器。
     *
     * @param module 模块
     * @param isForTests 是否为测试源码
     * @param includeExportedDependencies 是否包含导出的依赖
     * @return 依赖顺序枚举器
     */
    private fun getOrderEnumerator(
        module: Module,
        isForTests: Boolean,
        includeExportedDependencies: Boolean,
    ): OrderEnumerator {
        val rootManager = ModuleRootManager.getInstance(module)

        val dependencyEnumerator = rootManager.orderEntries().compileOnly()
        if (includeExportedDependencies) {
            dependencyEnumerator.recursively().exportedOnly()
        }

        // 对于生产源码，只包含生产依赖
        if (!isForTests) {
            dependencyEnumerator.productionOnly()
        }

        return dependencyEnumerator
    }

    /**
     * 判断依赖项是否适用于指定的源码类型
     *
     * 检查依赖项是否有效，以及作用域是否匹配。
     *
     * @param orderEntry 依赖项
     * @param isForTests 是否为测试源码
     * @return 如果依赖项适用则返回 true
     */
    private fun isApplicable(orderEntry: OrderEntry, isForTests: Boolean): Boolean {
        if (!orderEntry.isValid) {
            return false
        }

        return orderEntry !is ExportableOrderEntry
                || isForTests
                || orderEntry is ModuleOrderEntry && orderEntry.isProductionOnTestDependency
                || orderEntry.scope.isForProductionCompile
    }

    /**
     * 收集单个依赖项的模块信息
     *
     * 根据依赖项类型（模块、库）返回对应的 ModuleInfo 列表。
     *
     * ## 依赖项类型处理
     *
     * - **ModuleSourceOrderEntry**: 当前模块的源码
     * - **ModuleOrderEntry**: 依赖的其他模块
     * - **LibraryOrderEntry**: 依赖的库
     *
     * @param orderEntry 依赖项
     * @param isForTests 是否为测试源码
     * @return ModuleInfo 列表
     */
    private fun collectModuleDependenciesForOrderEntry(
        orderEntry: OrderEntry,
        isForTests: Boolean
    ): List<IdeaModuleInfo> {
        /**
         * 获取模块的源码信息列表
         *
         * 根据源码根类型返回合适的 ModuleSourceInfo：
         * - 测试源码：返回生产 + 测试源码信息
         * - 生产源码：只返回生产源码信息
         */
        fun Module.toInfos() = sourceModuleInfos.filter {
            isForTests || it is ModuleProductionSourceInfo
        }

        return when (orderEntry) {
            is ModuleSourceOrderEntry -> {
                // 当前模块的源码
                orderEntry.ownerModule.toInfos()
            }

            is ModuleOrderEntry -> {
                // 依赖的其他模块
                val module = orderEntry.module ?: return emptyList()
                if (!isForTests && orderEntry.isProductionOnTestDependency) {
                    // 生产源码依赖测试源码的特殊情况
                    listOfNotNull(module.testSourceInfo)
                } else {
                    module.toInfos()
                }
            }

            is LibraryOrderEntry -> {
                // 依赖的库
                val library = orderEntry.library ?: return listOf()
                LibraryInfoCache.getInstance(project)[library]
            }

            else -> {
                throw IllegalStateException("Unexpected order constructor $orderEntry")
            }
        }
    }
}

/**
 * 获取模块的生产源码信息
 *
 * 扩展属性，用于获取模块的生产源码 ModuleInfo。
 * 只有当模块包含仓颉生产源码根目录时才返回信息。
 *
 * @return 生产源码信息，如果模块没有生产源码根目录则返回 null
 * @see ModuleProductionSourceInfo
 */
val Module.productionSourceInfo: ModuleProductionSourceInfo?
    get() {
        val hasProductionRoots = hasRootsOfType(CangJieSourceRootTypes.SOURCE)
        return if (hasProductionRoots) ModuleProductionSourceInfo(this) else null
    }

/**
 * 获取模块的测试源码信息
 *
 * 扩展属性，用于获取模块的测试源码 ModuleInfo。
 * 只有当模块包含仓颉测试源码根目录时才返回信息。
 *
 * @return 测试源码信息，如果模块没有测试源码根目录则返回 null
 * @see ModuleTestSourceInfo
 */
val Module.testSourceInfo: ModuleTestSourceInfo?
    get() {
        val hasTestRoots = hasRootsOfType(CangJieSourceRootTypes.TEST)
        return if (hasTestRoots) ModuleTestSourceInfo(this) else null
    }

/**
 * 获取模块的所有源码信息
 *
 * 扩展属性，返回模块的所有源码 ModuleInfo 列表。
 * 包括生产源码和测试源码（如果存在）。
 *
 * @return 源码信息列表
 */
val Module.sourceModuleInfos: List<ModuleSourceInfo>
    get() = listOfNotNull(testSourceInfo, productionSourceInfo)

/**
 * 检查模块是否包含指定类型的根目录
 *
 * 私有辅助函数，用于判断模块是否有特定类型的源码根目录。
 * 使用 CangJieSourceRootTypes.findTypeById 将 rootTypeId 转换为 JpsModuleSourceRootType 进行比较。
 *
 * @param rootTypeId 要检查的根目录类型标识符
 * @return 如果模块包含指定类型的根目录则返回 true
 */
private fun Module.hasRootsOfType(rootTypeId: String): Boolean {
    val targetType = CangJieSourceRootTypes.findTypeById(rootTypeId) ?: return false
    return rootManager.contentEntries.any { contentEntry ->
        contentEntry.sourceFolders.any { it.rootType === targetType || (it.rootType as? UnknownSourceRootType)?.unknownTypeId == rootTypeId }
    }
}
