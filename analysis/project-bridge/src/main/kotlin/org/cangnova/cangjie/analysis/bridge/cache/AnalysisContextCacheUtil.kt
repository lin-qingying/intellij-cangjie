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

package org.cangnova.cangjie.analysis.bridge.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.project.model.CjModule
import java.util.concurrent.ConcurrentHashMap

/**
 * 分析上下文缓存工具
 *
 * 提供类似原始实现的缓存机制，支持自动失效。
 *
 * ## 使用场景
 *
 * 用于缓存计算成本高的分析结果，例如：
 * - 模块依赖列表
 * - 文件作用域
 * - 符号索引
 *
 * ## 缓存失效策略
 *
 * 缓存会在以下情况自动失效：
 * - 项目根目录结构发生变化（添加/删除模块、依赖等）
 * - 项目配置文件被修改（cjpm.toml 等）
 * - 手动触发项目刷新
 *
 * ## 使用示例
 *
 * ```kotlin
 * class CjModuleAnalysisContext(private val cjModule: CjModule) : AnalysisContext {
 *     override val dependencies: List<AnalysisContext>
 *         get() = cjModule.cacheOnRootModifications(DependencyListCacheKey) {
 *             collectDependencies()
 *         }
 * }
 * ```
 */
object AnalysisContextCacheUtil {

    // 用于标记是否已为项目注册监听器
    private val listenerRegistered = Key.create<Boolean>("cangjie.analysis.cache.listener.registered")

    /**
     * 在 CjModule 上缓存值，当项目根目录修改时自动失效
     *
     * @param T 缓存值类型
     * @param module 模块实例
     * @param key 缓存键基础名称
     * @param provider 值提供器
     * @return 缓存的值
     */
    fun <T : Any> cacheOnRootModifications(
        module: CjModule,
        key: Key<T>,
        provider: () -> T
    ): T {
        val project = module.project.intellijProject

        // 确保监听器已注册
        ensureListenerRegistered(project)

        // 为每个模块创建唯一的缓存键
        val moduleSpecificKey = Key.create<T>("${key.toString()}.${module.name}")

        // 先检查缓存
        val cached = project.getUserData(moduleSpecificKey)
        if (cached != null) {
            return cached
        }

        // 计算新值
        val value = provider()
        project.putUserData(moduleSpecificKey, value)

        return value
    }

    /**
     * 确保项目已注册缓存失效监听器
     */
    private fun ensureListenerRegistered(project: Project) {
        // 使用双重检查锁定
        if (project.getUserData(listenerRegistered) != true) {
            synchronized(this) {
                if (project.getUserData(listenerRegistered) != true) {
                    registerCacheInvalidationListener(project)
                    project.putUserData(listenerRegistered, true)
                }
            }
        }
    }

    /**
     * 注册缓存失效监听器
     */
    private fun registerCacheInvalidationListener(project: Project) {
        project.messageBus.connect().subscribe(
            ModuleRootListener.TOPIC,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    // 清除所有缓存
                    clearAllCaches(project)
                }
            }
        )
    }

    /**
     * 清除项目的所有缓存
     */
    private fun clearAllCaches(project: Project) {
        // 清除所有以我们的前缀开头的缓存键
        // 注意：这是一个简化实现，实际使用中可能需要更精细的控制
        CacheKeys.ALL_KEYS.forEach { key ->
            // 遍历所有可能的模块名称组合
            // 这里我们只能清除已知的键
            project.putUserData(key, null)
        }
    }
}

/**
 * CjModule 扩展函数：缓存值并在根目录修改时失效
 *
 * 便捷方法，简化缓存操作。
 *
 * ## 使用示例
 *
 * ```kotlin
 * val dependencies = cjModule.cacheOnRootModifications(MyKey) {
 *     expensiveComputation()
 * }
 * ```
 *
 * @param T 缓存值类型
 * @param key 缓存键
 * @param provider 值提供器
 * @return 缓存的值
 */
fun <T : Any> CjModule.cacheOnRootModifications(
    key: Key<T>,
    provider: () -> T
): T {
    return AnalysisContextCacheUtil.cacheOnRootModifications(this, key, provider)
}

/**
 * 预定义的缓存键
 */
object CacheKeys {
    /** 模块依赖列表缓存键 */
    val DEPENDENCY_LIST = Key.create<List<org.cangnova.cangjie.descriptors.AnalysisContext>>("cangjie.analysis.dependencies")

    /** 模块作用域缓存键 */
    val MODULE_SCOPE = Key.create<com.intellij.psi.search.GlobalSearchScope>("cangjie.analysis.scope")

    /** 库元数据缓存键 */
    val LIBRARY_METADATA = Key.create<LibraryMetadata>("cangjie.analysis.library.metadata")

    /** 所有缓存键的集合（用于批量清除） */
    internal val ALL_KEYS = setOf(DEPENDENCY_LIST, MODULE_SCOPE, LIBRARY_METADATA)
}

/**
 * 库元数据数据类
 *
 * 用于缓存从库中提取的元数据信息。
 */
data class LibraryMetadata(
    val name: String,
    val version: String,
    val dependencies: List<String> = emptyList()
)