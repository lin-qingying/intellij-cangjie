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

package org.cangnova.cangjie.descriptors

import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

/**
 * AnalysisContext 提供者接口
 *
 * 这是一个项目扩展点接口，定义了从不同来源获取分析上下文的标准方法。
 * 具体实现由项目模型模块（如 cangjie-project）提供。
 *
 * ## 设计原则
 *
 * **接口在 analysis，实现在 project-bridge**：
 * - analysis 模块定义接口，不依赖具体项目模型
 * - project-bridge 模块提供实现，桥接 CjModule 和 AnalysisContext
 * - 通过 IntelliJ 扩展点机制解耦
 *
 * **扩展点模式**：
 * - 不同的项目类型（CJPM、KCJPM 等）可以提供不同的实现
 * - 支持多种构建系统和项目结构
 * - 通过 isApplicable 方法判断是否适用于当前项目
 *
 * ## 核心功能
 *
 * ### 1. 适用性判断
 * 通过 isApplicable 方法判断当前提供者是否适用于给定项目。
 *
 * ### 2. 文件到上下文的映射
 * 根据文件所在位置，找到其所属的分析上下文（模块）。
 *
 * ### 3. 全局上下文管理
 * 提供项目中所有可分析上下文的视图。
 *
 * ### 4. 缓存管理
 * 缓存已创建的上下文实例，避免重复创建。
 *
 * ## 使用示例
 *
 * ### 获取文件的上下文
 * ```kotlin
 * val provider = AnalysisContextProvider.getInstance(project)
 * val context = provider.getContextForFile(psiFile)
 *
 * if (context != null) {
 *     // 使用上下文进行分析
 *     analyzeWithContext(context)
 * }
 * ```
 *
 * ### 获取所有上下文
 * ```kotlin
 * val provider = AnalysisContextProvider.getInstance(project)
 * val allContexts = provider.getAllContexts()
 *
 * for (context in allContexts) {
 *     println("模块: ${context.contextId}")
 * }
 * ```
 *
 * ### 清除缓存
 * ```kotlin
 * val provider = AnalysisContextProvider.getInstance(project)
 * provider.clearCache() // 项目结构变化时调用
 * ```
 *
 * ## 实现指南
 *
 * 实现此接口时需要：
 *
 * 1. **适用性判断**：实现 isApplicable 方法，判断是否适用于当前项目
 * 2. **文件定位逻辑**：根据文件路径找到所属模块
 * 3. **上下文创建**：将项目模型转换为 AnalysisContext
 * 4. **缓存策略**：避免重复创建相同的上下文
 * 5. **线程安全**：支持多线程并发访问
 *
 * ### 实现示例
 * ```kotlin
 * class CjAnalysisContextProvider : AnalysisContextProvider {
 *
 *     private val cache = ConcurrentHashMap<CjModule, AnalysisContext>()
 *
 *     override fun isApplicable(project: Project): Boolean {
 *         // 判断项目是否是仓颉项目
 *         val projectsService = CjProjectsService.getInstanceIfCreated(project) ?: return false
 *         return projectsService.cjProject != null
 *     }
 *
 *     override fun getContextForFile(file: PsiFile): AnalysisContext? {
 *         val module = findModuleForFile(file) ?: return null
 *         return cache.getOrPut(module) {
 *             CjModuleAnalysisContext(module)
 *         }
 *     }
 *
 *     override fun getAllContexts(): List<AnalysisContext> {
 *         return project.modules.map { module ->
 *             cache.getOrPut(module) {
 *                 CjModuleAnalysisContext(module)
 *             }
 *         }
 *     }
 *
 *     override fun clearCache() {
 *         cache.clear()
 *     }
 * }
 * ```
 *
 * @see AnalysisContext
 */
interface AnalysisContextProvider {

    /**
     * 判断此提供者是否适用于给定的项目
     *
     * 此方法用于确定当前提供者是否能够为指定项目提供分析上下文。
     * 多个提供者可以注册到扩展点，系统会选择第一个返回 true 的提供者。
     *
     * **实现建议**：
     * - 检查项目是否包含特定的配置文件（如 cjpm.toml）
     * - 检查项目服务是否已初始化
     * - 避免执行耗时操作，此方法可能被频繁调用
     *
     * **使用场景**：
     * - 系统自动选择合适的提供者
     * - 支持多种项目类型共存
     * - 插件化扩展
     *
     * @param project IntelliJ 项目实例
     * @return true 如果此提供者适用于该项目，否则 false
     */
    fun isApplicable(project: Project): Boolean

    /**
     * 从 PSI 文件获取分析上下文
     *
     * 根据文件所在的模块返回对应的分析上下文。
     * 如果文件不属于任何可识别的模块，返回 null。
     *
     * **使用场景**：
     * - 代码分析：分析特定文件时获取其上下文
     * - 符号解析：在文件的模块作用域内解析符号
     * - 代码补全：提供当前文件可访问的所有符号
     *
     * @param file PSI 文件实例
     * @return 对应的分析上下文，如果无法确定返回 null
     */
    fun getContextForFile(file: PsiFile): AnalysisContext?

    /**
     * 从虚拟文件获取分析上下文
     *
     * 类似于 [getContextForFile]，但直接接受 VirtualFile。
     * 适用于尚未加载 PSI 的场景。
     *
     * @param project 项目实例
     * @param file 虚拟文件实例
     * @return 对应的分析上下文，如果无法确定返回 null
     */
    fun getContextForFile(project: Project, file: VirtualFile): AnalysisContext?

    /**
     * 获取项目中所有的分析上下文
     *
     * 返回当前项目中所有可分析的模块上下文。
     * 包括：
     * - 所有源码模块
     * - 不包括库依赖（库依赖通过模块的 dependencies 属性访问）
     *
     * **使用场景**：
     * - 全局分析：分析整个项目
     * - 索引构建：为所有模块建立索引
     * - 批量操作：对所有模块执行某个操作
     *
     * @param project 项目实例
     * @return 所有模块的分析上下文列表
     */
    fun getAllContexts(project: Project): List<AnalysisContext>

    /**
     * 清除所有缓存的上下文
     *
     * 在以下情况下应该调用：
     * - 项目结构变化（模块添加/删除）
     * - 依赖关系更新
     * - 配置文件修改
     *
     * 清除缓存后，下次访问将重新创建上下文实例。
     */
    fun clearCache()

    companion object {
        val EP_NAME = ExtensionPointName.create<AnalysisContextProvider>("org.cangnova.cangjie.analysisContextProvider")
        /**
         * 获取适用于当前项目的提供者实例
         *
         * 使用 IntelliJ Platform 的扩展点机制获取当前项目的提供者实例。
         * 系统会遍历所有注册的提供者，返回第一个 isApplicable 返回 true 的提供者。
         *
         * **注意**：如果没有找到适用的提供者，此方法会抛出异常。
         *
         * @param project IntelliJ 项目实例
         * @return AnalysisContextProvider 实例
         * @throws IllegalStateException 如果没有找到适用的提供者
         */
        @JvmStatic
        fun getInstance(project: Project): AnalysisContextProvider {

            return EP_NAME.extensionList.firstOrNull { it.isApplicable(project) }
                ?: throw IllegalStateException("No applicable AnalysisContextProvider found for project: ${project.name}")
        }
    }
}

/**
 * Project 扩展属性：便捷访问 AnalysisContextProvider
 *
 * 使用示例：
 * ```kotlin
 * val context = project.analysisContextProvider.getContextForFile(file)
 * ```
 */
val Project.analysisContextProvider: AnalysisContextProvider
    get() = AnalysisContextProvider.getInstance(this)
