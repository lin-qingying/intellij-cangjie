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

package org.cangnova.cangjie.analysis.bridge

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.descriptors.AnalysisContextProvider
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjDependency
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.psi.CjFile
import java.util.concurrent.ConcurrentHashMap

/**
 * AnalysisContextProvider 的 CjModule 实现
 *
 * 这是 [AnalysisContextProvider] 接口的具体实现，基于 cangjie-project 模块的项目模型。
 * 负责将 CjModule、CjProject 等项目模型适配为 AnalysisContext。
 *
 * ## 功能特性
 *
 * ### 1. 智能文件定位
 * 根据文件路径快速定位所属模块，支持：
 * - 单模块项目
 * - 工作空间多模块项目
 * - 嵌套源码集
 *
 * ### 2. 高效缓存
 * - 基于模块的缓存策略
 * - 线程安全的并发访问
 * - 支持缓存失效和刷新
 *
 * ### 3. 依赖处理
 * 自动解析模块依赖并创建对应的上下文：
 * - 本地模块依赖
 * - 外部库依赖
 * - 标准库依赖
 *
 * ## 服务注册
 *
 * 此类需要在 plugin.xml 中注册为项目级服务：
 * ```xml
 * <projectService
 *     serviceInterface="org.cangnova.cangjie.descriptors.AnalysisContextProvider"
 *     serviceImplementation="org.cangnova.cangjie.analysis.bridge.CjAnalysisContextProvider"/>
 * ```
 *
 * @see AnalysisContextProvider
 * @see CjModuleAnalysisContext
 */
@Service(Service.Level.PROJECT)
class CjAnalysisContextProvider(private val project: Project) : AnalysisContextProvider {

    /**
     * 模块上下文缓存
     * Key: CjModule 实例
     * Value: 对应的 AnalysisContext
     */
    private val moduleContextCache = ConcurrentHashMap<CjModule, AnalysisContext>()

    /**
     * 依赖上下文缓存
     * Key: CjDependency 实例
     * Value: 对应的 AnalysisContext
     */
    private val dependencyContextCache = ConcurrentHashMap<CjDependency, AnalysisContext>()

    override fun getContextForFile(file: PsiFile): AnalysisContext? {
        // 只处理仓颉文件
        if (file !is CjFile) return null

        val virtualFile = file.virtualFile ?: return null
        return getContextForFile(virtualFile)
    }

    override fun getContextForFile(file: VirtualFile): AnalysisContext? {
        // 获取项目服务
        val projectsService = CjProjectsService.getInstance(project)
        val cjProject = projectsService.cjProject

        // 查找文件所属的模块
        val module = findModuleForFile(cjProject, file) ?: return null

        // 返回模块的上下文
        return getOrCreateContextForModule(module)
    }

    override fun getAllContexts(): List<AnalysisContext> {
        val projectsService = CjProjectsService.getInstance(project)
        val cjProject = projectsService.cjProject

        // 获取所有模块
        val modules = when {
            // 单模块项目
            cjProject.module != null -> listOf(cjProject.module!!)
            // 工作空间项目
            cjProject.workspace != null -> cjProject.workspace!!.modules
            else -> emptyList()
        }

        // 为每个模块创建或获取上下文
        return modules.map { module ->
            getOrCreateContextForModule(module)
        }
    }

    override fun clearCache() {
        moduleContextCache.clear()
        dependencyContextCache.clear()
    }

    /**
     * 获取或创建模块的分析上下文
     *
     * 如果缓存中已存在，直接返回；否则创建新实例并缓存。
     *
     * @param module CjModule 实例
     * @return 对应的 AnalysisContext
     */
    fun getOrCreateContextForModule(module: CjModule): AnalysisContext {
        return moduleContextCache.getOrPut(module) {
            CjModuleAnalysisContext(module)
        }
    }

    /**
     * 获取或创建依赖的分析上下文
     *
     * @param dependency 依赖实例
     * @return 对应的 AnalysisContext
     */
    fun getOrCreateContextForDependency(dependency: CjDependency): AnalysisContext {
        return dependencyContextCache.getOrPut(dependency) {
            CjLibraryAnalysisContext(dependency, project)
        }
    }

    /**
     * 清除特定模块的缓存
     *
     * 在模块配置变化时调用。
     *
     * @param module 要清除缓存的模块
     */
    fun clearCacheForModule(module: CjModule) {
        moduleContextCache.remove(module)
    }

    /**
     * 在 CjProject 中查找包含指定文件的模块
     *
     * @param cjProject CjProject 实例
     * @param file 要查找的文件
     * @return 包含该文件的模块，如果没找到返回 null
     */
    private fun findModuleForFile(
        cjProject: org.cangnova.cangjie.project.model.CjProject,
        file: VirtualFile
    ): CjModule? {
        // 获取所有模块
        val modules = when {
            cjProject.module != null -> listOf(cjProject.module!!)
            cjProject.workspace != null -> cjProject.workspace!!.modules
            else -> emptyList()
        }

        // 查找包含该文件的模块
        for (module in modules) {
            if (moduleContainsFile(module, file)) {
                return module
            }
        }

        return null
    }

    /**
     * 检查模块是否包含指定文件
     *
     * 遍历模块的所有源码集，检查文件是否在任何源码根下。
     *
     * @param module 模块
     * @param file 文件
     * @return true 如果模块包含该文件
     */
    private fun moduleContainsFile(module: CjModule, file: VirtualFile): Boolean {
        // 检查所有源码集
        for (sourceSet in module.sourceSets) {
            for (sourceRoot in sourceSet.sourceRoots) {
                if (isAncestor(sourceRoot, file)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * 检查一个文件是否是另一个文件的祖先（包含关系）
     *
     * @param ancestor 可能的祖先目录
     * @param file 要检查的文件
     * @return true 如果 ancestor 包含 file
     */
    private fun isAncestor(ancestor: VirtualFile, file: VirtualFile): Boolean {
        var current: VirtualFile? = file
        while (current != null) {
            if (current == ancestor) {
                return true
            }
            current = current.parent
        }
        return false
    }
}
