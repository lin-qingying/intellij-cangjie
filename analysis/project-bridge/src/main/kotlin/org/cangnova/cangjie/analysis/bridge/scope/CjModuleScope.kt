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

package org.cangnova.cangjie.analysis.bridge.scope

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.intellijModule

/**
 * 仓颉模块的搜索作用域
 *
 * 基于 CjModule 创建的自定义 GlobalSearchScope，提供更精确的模块边界检查。
 *
 * ## 功能特性
 *
 * - 基于源码根目录的快速文件包含检查
 * - 支持模块依赖传递
 * - 与 IntelliJ Module 集成
 * - 缓存源码根的父路径以提高性能
 *
 * ## 实现细节
 *
 * 该作用域通过检查文件是否在模块的源码根目录下来判断文件是否属于该作用域。
 * 为了提高性能，会缓存所有源码根的规范路径。
 *
 * @property cjModule 仓颉模块
 * @property includeTests 是否包含测试源码
 */
class CjModuleScope(
    private val cjModule: CjModule,
    private val includeTests: Boolean = true
) : GlobalSearchScope(cjModule.project.intellijProject) {

    /**
     * 源码根的规范路径集合（用于快速查找）
     */
    private val sourceRootPaths: Set<String> by lazy {
        cjModule.sourceSets
            .filter { includeTests || !it.isTest }
            .flatMap { it.sourceRoots }
            .mapNotNull { it.canonicalPath }
            .toSet()
    }

    /**
     * 关联的 IntelliJ Module（如果存在）
     */
    private val intellijModule: Module? by lazy {
        cjModule.intellijModule
    }

    /**
     * 项目文件索引（用于模块检查）
     */
    private val projectFileIndex: ProjectFileIndex by lazy {
        ProjectFileIndex.getInstance(project!!)
    }

    /**
     * 检查文件是否在该作用域内
     *
     * 实现策略：
     * 1. 首先检查文件是否在缓存的源码根路径下
     * 2. 如果存在关联的 IntelliJ Module，则通过 ProjectFileIndex 验证
     *
     * @param file 要检查的文件
     * @return true 如果文件在作用域内，否则 false
     */
    override fun contains(file: VirtualFile): Boolean {
        // 快速路径：检查文件是否在任何源码根目录下
        val filePath = file.canonicalPath ?: return false

        val isInSourceRoot = sourceRootPaths.any { sourceRoot ->
            filePath.startsWith(sourceRoot)
        }

        if (!isInSourceRoot) {
            return false
        }

        // 如果有关联的 IntelliJ Module，则进一步验证
        intellijModule?.let { module ->
            return projectFileIndex.isInSourceContent(file) &&
                   projectFileIndex.getModuleForFile(file) == module
        }

        return true
    }

    /**
     * 检查是否搜索指定模块的内容
     *
     * @param aModule IntelliJ 模块
     * @return true 如果该作用域包含指定模块的内容
     */
    override fun isSearchInModuleContent(aModule: Module): Boolean {
        // 只搜索关联的 IntelliJ Module
        return intellijModule == aModule
    }

    /**
     * 是否搜索库文件
     *
     * 模块作用域不包含库文件。
     *
     * @return false
     */
    override fun isSearchInLibraries(): Boolean = false

    /**
     * 作用域的显示名称
     */
    override fun getDisplayName(): String {
        return "CangJie Module: ${cjModule.name}"
    }

    override fun toString(): String {
        return "CjModuleScope(module=${cjModule.name}, includeTests=$includeTests, sourceRoots=${sourceRootPaths.size})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CjModuleScope) return false
        return cjModule == other.cjModule && includeTests == other.includeTests
    }

    override fun hashCode(): Int {
        var result = cjModule.hashCode()
        result = 31 * result + includeTests.hashCode()
        return result
    }
}

/**
 * 仓颉源码集的搜索作用域
 *
 * 针对单个源码集（SourceSet）的精确作用域。
 *
 * @property project IntelliJ 项目
 * @property sourceRoots 源码根目录列表
 * @property scopeName 作用域名称
 */
class CjSourceSetScope(
    project: Project,
    private val sourceRoots: Collection<VirtualFile>,
    private val scopeName: String
) : GlobalSearchScope(project) {

    /**
     * 源码根的规范路径集合（用于快速查找）
     */
    private val sourceRootPaths: Set<String> by lazy {
        sourceRoots.mapNotNull { it.canonicalPath }.toSet()
    }

    /**
     * 项目文件索引
     */
    private val projectFileIndex: ProjectFileIndex by lazy {
        ProjectFileIndex.getInstance(project!!)
    }

    /**
     * 检查文件是否在该作用域内
     *
     * @param file 要检查的文件
     * @return true 如果文件在作用域内，否则 false
     */
    override fun contains(file: VirtualFile): Boolean {
        val filePath = file.canonicalPath ?: return false

        return sourceRootPaths.any { sourceRoot ->
            filePath.startsWith(sourceRoot)
        }
    }

    /**
     * 检查是否搜索指定模块的内容
     *
     * 源码集作用域可能跨越多个模块，因此检查文件是否属于该模块。
     *
     * @param aModule IntelliJ 模块
     * @return true 如果该作用域包含指定模块的内容
     */
    override fun isSearchInModuleContent(aModule: Module): Boolean {
        // 检查源码根是否属于该模块
        return sourceRoots.any { root ->
            projectFileIndex.getModuleForFile(root) == aModule
        }
    }

    /**
     * 是否搜索库文件
     *
     * @return false
     */
    override fun isSearchInLibraries(): Boolean = false

    /**
     * 作用域的显示名称
     */
    override fun getDisplayName(): String = scopeName

    override fun toString(): String {
        return "CjSourceSetScope(name=$scopeName, sourceRoots=${sourceRoots.size})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CjSourceSetScope) return false
        return sourceRootPaths == other.sourceRootPaths
    }

    override fun hashCode(): Int {
        return sourceRootPaths.hashCode()
    }
}