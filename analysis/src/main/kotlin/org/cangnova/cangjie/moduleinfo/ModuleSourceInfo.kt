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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer.isDisposed
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.NlsSafe
import com.intellij.serviceContainer.AlreadyDisposedException
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServicesImpl

/**
 * 模块源码信息接口
 *
 * 该接口表示项目源码模块的信息，区别于库模块 ([LibraryModuleInfo])。
 * 它代表项目自己的可编辑源代码模块。
 *
 * ## 设计目的
 *
 * [ModuleSourceInfo] 为项目源码模块提供完整的元数据：
 * 1. **模块标识**: 通过 IntelliJ 模块对象标识源码模块
 * 2. **分析支持**: 提供完整的语法语义分析能力
 * 3. **多平台支持**: 通过 expectedBy 支持多平台项目结构
 * 4. **项目集成**: 与 IntelliJ 项目模型深度集成
 *
 * ## 核心特性
 *
 * ### 1. 源码模块标识
 * - [moduleOrigin] 总是返回 [ModuleOrigin.MODULE]
 * - 表示这是项目自己的源码，可以编辑和重构
 *
 * ### 2. 多平台支持
 * - 通过 [expectedBy] 支持 Kotlin 多平台的 expect/actual 机制
 * - 平台特定模块可以声明实现哪些 common 模块
 *
 * ### 3. 可追踪性
 * - 实现 [TrackableModuleInfo]，支持模块变更追踪
 * - 可以监听模块配置的修改
 *
 * ### 4. 有效性检查
 * - 提供 [checkValidity] 方法检查模块是否已释放
 * - 防止在已释放的模块上执行操作
 *
 * ## 继承层次
 *
 * ```
 * ModuleInfo (基础接口)
 *   ↓
 * IdeaModuleInfo (IDE 特定接口)
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
 * ### 获取模块信息
 * ```kotlin
 * fun getModuleInfo(psiFile: PsiFile): ModuleSourceInfo? {
 *     val module = ModuleUtilCore.findModuleForFile(psiFile.virtualFile, psiFile.project)
 *         ?: return null
 *
 *     return module.sourceModuleInfo
 * }
 * ```
 *
 * ### 检查模块类型
 * ```kotlin
 * fun isProjectModule(moduleInfo: IdeaModuleInfo): Boolean {
 *     return moduleInfo is ModuleSourceInfo
 * }
 * ```
 *
 * ### 多平台项目
 * ```kotlin
 * // Common 模块
 * interface CommonModule : ModuleSourceInfo {
 *     override val expectedBy: List<ModuleSourceInfo>
 *         get() = emptyList()  // common 不实现任何模块
 * }
 *
 * // JVM 平台模块
 * interface JvmModule : ModuleSourceInfo {
 *     val commonModule: CommonModule
 *
 *     override val expectedBy: List<ModuleSourceInfo>
 *         get() = listOf(commonModule)  // 实现 common 模块
 * }
 * ```
 *
 * ### 有效性检查
 * ```kotlin
 * fun analyzeModule(moduleInfo: ModuleSourceInfo) {
 *     try {
 *         moduleInfo.checkValidity()
 *         // 模块有效，执行分析
 *         performAnalysis(moduleInfo)
 *     } catch (e: AlreadyDisposedException) {
 *         // 模块已释放，跳过分析
 *         log.warn("Module ${moduleInfo.displayedName} is disposed")
 *     }
 * }
 * ```
 *
 * ## 与其他接口的区别
 *
 * - **ModuleSourceInfo**: 项目源码模块（可编辑）
 * - **LibraryModuleInfo**: 依赖的库模块（只读）
 * - **BinaryModuleInfo**: 二进制模块（编译产物）
 * - **SourceForBinaryModuleInfo**: 库的源码（库源码）
 *
 * ## 实现注意事项
 *
 * 实现类应该：
 * 1. 提供正确的 [module] 引用
 * 2. 实现 [expectedBy] 以支持多平台
 * 3. 提供准确的内容作用域 ([contentScope])
 * 4. 正确管理依赖关系 ([dependencies])
 *
 * @see IdeaModuleInfo
 * @see TrackableModuleInfo
 * @see LibraryModuleInfo
 */
interface ModuleSourceInfo :TrackableModuleInfo,  IdeaModuleInfo {
    /**
     * 关联的 IntelliJ 模块
     *
     * 返回此源码信息对应的 IntelliJ 模块对象。
     * 模块对象包含项目的结构信息、依赖关系和配置。
     *
     * ## 用途
     *
     * - 获取模块的源码根目录
     * - 查询模块的依赖配置
     * - 访问模块级别的设置
     * - 监听模块的变更
     *
     * ## 示例
     *
     * ```kotlin
     * val moduleInfo: ModuleSourceInfo = getModuleInfo()
     * val module = moduleInfo.module
     *
     * // 获取模块根目录
     * val rootManager = ModuleRootManager.getInstance(module)
     * val sourceRoots = rootManager.sourceRoots
     *
     * // 获取模块依赖
     * val dependencies = rootManager.dependencies
     * ```
     *
     * @return IntelliJ 模块对象
     * @see Module
     */
      val module: Module


    /**
     * 显示名称
     *
     * 用于 UI 显示的模块名称，直接使用 IntelliJ 模块的名称。
     *
     * 标注 [@NlsSafe] 表示此字符串已经过本地化处理，可以直接显示。
     *
     * @return 模块名称
     */
    override val displayedName: @NlsSafe String get() = module.name

    /**
     * 模块来源类型
     *
     * 源码模块的来源类型总是 [ModuleOrigin.MODULE]，
     * 表示这是项目自己的源码模块，可以编辑和重构。
     *
     * ## 与其他来源的区别
     *
     * - **MODULE**: 项目源码模块（可编辑）
     * - **LIBRARY**: 依赖的库模块（只读）
     * - **OTHER**: 其他类型（如库源码、SDK 等）
     *
     * @return [ModuleOrigin.MODULE]
     * @see ModuleOrigin
     */
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.MODULE

    /**
     * 所属 IntelliJ 项目
     *
     * 返回此模块所属的 IntelliJ 项目实例。
     * 直接从 [module] 获取项目引用。
     *
     * @return IntelliJ 项目实例
     * @see Project
     */
    override val project: Project
        get() = module.project


    /**
     * 平台相关的分析服务
     *
     * 返回平台分析服务实现，用于类型检查和解析。
     * 所有源码模块使用相同的分析服务实现。
     *
     * @return 分析服务实例
     * @see PlatformDependentAnalyzerServices
     */
    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = PlatformDependentAnalyzerServicesImpl


    /**
     * 检查模块的有效性
     *
     * 验证模块是否仍然有效（未被释放）。
     * 如果模块已被释放，抛出 [AlreadyDisposedException]。
     *
     * ## 使用场景
     *
     * - 在执行模块操作前检查有效性
     * - 在缓存的模块信息失效时清理
     * - 在异步操作中确保模块仍然存在
     *
     * ## 示例
     *
     * ```kotlin
     * fun analyzeModule(moduleInfo: ModuleSourceInfo) {
     *     try {
     *         moduleInfo.checkValidity()
     *         // 模块有效，继续操作
     *     } catch (e: AlreadyDisposedException) {
     *         // 模块已释放，停止操作
     *         return
     *     }
     * }
     * ```
     *
     * @throws AlreadyDisposedException 如果模块已被释放
     * @see Module.checkValidity
     */
    override fun checkValidity() {
        module.checkValidity()
    }
    override fun createModificationTracker(): ModificationTracker {
        return CangJieModificationTrackerProvider.getInstance(module.project).createModuleModificationTracker(module)
    }
}