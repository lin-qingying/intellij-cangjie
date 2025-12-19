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

import com.intellij.openapi.util.ModificationTracker

/**
 * 可追踪的模块信息接口
 *
 * 该接口扩展了 [ModuleInfo]，添加了模块变更追踪的能力。
 * 实现此接口的模块可以提供修改追踪器，用于监听模块的变更。
 *
 * ## 设计目的
 *
 * 在 IntelliJ 平台中，许多缓存和分析结果需要在模块变更时失效。
 * [TrackableModuleInfo] 提供了一种标准化的方式来追踪模块的变更：
 *
 * 1. **缓存失效**: 当模块配置改变时，相关的缓存可以自动失效
 * 2. **增量分析**: 只重新分析发生变化的部分，提升性能
 * 3. **依赖追踪**: 当一个模块变更时，可以找到所有依赖它的模块
 * 4. **智能重新分析**: 在适当的时机触发重新分析，避免不必要的计算
 *
 * ## 核心职责
 *
 * - **提供修改追踪器**: 创建并返回能够追踪模块变更的追踪器对象
 * - **变更通知**: 当模块发生变更时，追踪器的修改计数会增加
 * - **缓存集成**: 追踪器可以与 IntelliJ 的缓存系统集成
 *
 * ## 继承层次
 *
 * ```
 * ModuleInfo (基础接口)
 *   ↓
 * TrackableModuleInfo (可追踪接口)
 *   ↓
 * ModuleSourceInfo (源码模块接口)
 *   ↓
 * 具体实现类（如 ModuleProductionSourceInfo, ModuleTestSourceInfo）
 * ```
 *
 * ## 使用场景
 *
 * ### 缓存失效
 * ```kotlin
 * class ModuleAnalysisCache {
 *     private val cache = CachedValuesManager.getManager(project)
 *         .createCachedValue {
 *             val moduleInfo = getModuleInfo() as TrackableModuleInfo
 *             val result = performExpensiveAnalysis(moduleInfo)
 *
 *             // 当模块变更时，缓存自动失效
 *             CachedValueProvider.Result.create(
 *                 result,
 *                 moduleInfo.createModificationTracker()
 *             )
 *         }
 *
 *     fun getAnalysisResult() = cache.value
 * }
 * ```
 *
 * ### 增量编译
 * ```kotlin
 * class IncrementalCompiler {
 *     private val lastModificationCounts = mutableMapOf<TrackableModuleInfo, Long>()
 *
 *     fun compile(module: TrackableModuleInfo) {
 *         val tracker = module.createModificationTracker()
 *         val currentCount = tracker.modificationCount
 *         val lastCount = lastModificationCounts[module]
 *
 *         if (lastCount == null || currentCount != lastCount) {
 *             // 模块已变更，需要重新编译
 *             performCompilation(module)
 *             lastModificationCounts[module] = currentCount
 *         } else {
 *             // 模块未变更，跳过编译
 *             println("Module ${module.name} hasn't changed, skipping compilation")
 *         }
 *     }
 * }
 * ```
 *
 * ### 依赖变更检测
 * ```kotlin
 * class DependencyChangeDetector {
 *     fun checkDependenciesChanged(module: TrackableModuleInfo): Boolean {
 *         return module.dependencies.filterIsInstance<TrackableModuleInfo>()
 *             .any { dependency ->
 *                 val tracker = dependency.createModificationTracker()
 *                 val cachedCount = getCachedModificationCount(dependency)
 *                 tracker.modificationCount != cachedCount
 *             }
 *     }
 * }
 * ```
 *
 * ## 实现注意事项
 *
 * 实现类应该：
 *
 * 1. **返回稳定的追踪器**: 对同一个模块，应该返回相同的追踪器实例或具有相同语义的追踪器
 * 2. **准确的变更检测**: 追踪器应该在模块真正发生变更时才增加计数
 * 3. **合理的粒度**: 变更粒度应该适中，既不过于敏感也不过于迟钝
 * 4. **性能考虑**: 创建追踪器的操作应该是轻量级的
 *
 * ## 与 ModificationTracker 的关系
 *
 * [ModificationTracker] 是 IntelliJ 平台提供的通用变更追踪机制：
 * - **modificationCount**: 一个单调递增的长整型计数器
 * - **变更时递增**: 每当被追踪的对象发生变更时，计数器递增
 * - **缓存依赖**: 缓存系统可以依赖此计数器来判断缓存是否失效
 *
 * ## 示例实现
 *
 * ```kotlin
 * class MyModuleInfo : TrackableModuleInfo {
 *     override val module: Module = ...
 *
 *     override fun createModificationTracker(): ModificationTracker {
 *         // 使用 CangJieModificationTrackerProvider 创建追踪器
 *         return CangJieModificationTrackerProvider
 *             .getInstance(module.project)
 *             .createModuleModificationTracker(module)
 *     }
 * }
 * ```
 *
 * @see ModuleInfo
 * @see ModificationTracker
 * @see ModuleSourceInfo
 * @see CangJieModificationTrackerProvider
 */
interface TrackableModuleInfo : ModuleInfo {
    /**
     * 创建模块的修改追踪器
     *
     * 返回一个能够追踪此模块变更的 [ModificationTracker] 对象。
     * 当模块的配置、源码或依赖发生变化时，追踪器的修改计数应该增加。
     *
     * ## 追踪的变更类型
     *
     * 追踪器应该监听以下类型的变更：
     * - **模块配置**: 如 SDK 变更、语言级别变更
     * - **源码根目录**: 源码目录的添加、删除或修改
     * - **依赖关系**: 模块依赖的添加、删除或修改
     * - **模块属性**: 模块名称、类型等基本属性的变更
     *
     * ## 实现建议
     *
     * 推荐使用 [CangJieModificationTrackerProvider] 来创建追踪器：
     * ```kotlin
     * override fun createModificationTracker(): ModificationTracker {
     *     return CangJieModificationTrackerProvider
     *         .getInstance(module.project)
     *         .createModuleModificationTracker(module)
     * }
     * ```
     *
     * ## 使用示例
     *
     * ### 与缓存集成
     * ```kotlin
     * val moduleInfo: TrackableModuleInfo = getModuleInfo()
     * val tracker = moduleInfo.createModificationTracker()
     *
     * val cachedValue = CachedValuesManager.getCachedValue(file) {
     *     val result = computeExpensiveValue(moduleInfo)
     *     CachedValueProvider.Result.create(result, tracker)
     * }
     * ```
     *
     * ### 手动检查变更
     * ```kotlin
     * val moduleInfo: TrackableModuleInfo = getModuleInfo()
     * val tracker = moduleInfo.createModificationTracker()
     *
     * val initialCount = tracker.modificationCount
     * // ... 执行一些操作 ...
     * val currentCount = tracker.modificationCount
     *
     * if (currentCount != initialCount) {
     *     println("模块已发生变更")
     * }
     * ```
     *
     * @return 模块的修改追踪器
     * @see ModificationTracker
     * @see CangJieModificationTrackerProvider
     */
    fun createModificationTracker(): ModificationTracker
}
