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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjSourceSet

/**
 * 分析作用域工具
 *
 * 提供用于构建和扩展 GlobalSearchScope 的工具方法。
 *
 * ## 核心功能
 *
 * - 从 CjModule 构建作用域
 * - 从 CjSourceSet 构建作用域
 * - 合并多个作用域
 * - 过滤和扩展作用域
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 创建模块作用域
 * val moduleScope = AnalysisScopeUtils.createModuleScope(cjModule)
 *
 * // 创建源码集作用域
 * val sourceSetScope = AnalysisScopeUtils.createSourceSetScope(sourceSet, project)
 *
 * // 合并作用域
 * val combinedScope = AnalysisScopeUtils.union(scope1, scope2, scope3)
 * ```
 */
object AnalysisScopeUtils {

    /**
     * 从 CjModule 创建全局搜索作用域
     *
     * 包含模块的所有源码集中的源文件。
     *
     * 使用自定义的 CjModuleScope 实现，提供更精确的模块边界检查和更好的性能。
     *
     * @param module 模块实例
     * @return 全局搜索作用域
     */
    fun createModuleScope(module: CjModule): GlobalSearchScope {
        val allSourceRoots = module.sourceSets.flatMap { it.sourceRoots }

        return when {
            allSourceRoots.isEmpty() -> GlobalSearchScope.EMPTY_SCOPE
            else -> CjModuleScope(module, includeTests = true)
        }
    }

    /**
     * 从 CjSourceSet 创建全局搜索作用域
     *
     * 仅包含指定源码集的源文件。
     *
     * 使用自定义的 CjSourceSetScope 实现，提供更精确的作用域检查。
     *
     * @param sourceSet 源码集实例
     * @param project IntelliJ 项目
     * @return 全局搜索作用域
     */
    fun createSourceSetScope(sourceSet: CjSourceSet, project: Project): GlobalSearchScope {
        val sourceRoots = sourceSet.sourceRoots

        return when {
            sourceRoots.isEmpty() -> GlobalSearchScope.EMPTY_SCOPE
            else -> CjSourceSetScope(
                project = project,
                sourceRoots = sourceRoots,
                scopeName = "SourceSet: ${sourceSet.name}"
            )
        }
    }

    /**
     * 创建包含指定文件列表的作用域
     *
     * **注意**：此方法可能需要访问工作区索引，因此允许慢操作。
     *
     * @param project IntelliJ 项目
     * @param files 文件列表
     * @return 全局搜索作用域
     */
    fun createFilesScope(project: Project, files: Collection<VirtualFile>): GlobalSearchScope {
        return when {
            files.isEmpty() -> GlobalSearchScope.EMPTY_SCOPE
            else ->
                GlobalSearchScope.filesScope(project, files)

        }
    }

    /**
     * 合并多个作用域
     *
     * 创建一个联合作用域，包含所有输入作用域的文件。
     *
     * @param scopes 要合并的作用域列表
     * @return 合并后的作用域
     */
    fun union(vararg scopes: GlobalSearchScope): GlobalSearchScope {
        val nonEmptyScopes = scopes.filter { it != GlobalSearchScope.EMPTY_SCOPE }

        return when {
            nonEmptyScopes.isEmpty() -> GlobalSearchScope.EMPTY_SCOPE
            nonEmptyScopes.size == 1 -> nonEmptyScopes.first()
            else -> GlobalSearchScope.union(nonEmptyScopes)
        }
    }

    /**
     * 过滤作用域，仅保留满足条件的文件
     *
     * @param scope 原始作用域
     * @param predicate 文件过滤条件
     * @return 过滤后的作用域
     */
    fun filter(
        scope: GlobalSearchScope,
        predicate: (VirtualFile) -> Boolean
    ): GlobalSearchScope {
        return object : GlobalSearchScope(scope.project) {
            override fun contains(file: VirtualFile): Boolean {
                return scope.contains(file) && predicate(file)
            }

            override fun isSearchInModuleContent(aModule: com.intellij.openapi.module.Module): Boolean {
                return scope.isSearchInModuleContent(aModule)
            }

            override fun isSearchInLibraries(): Boolean {
                return scope.isSearchInLibraries
            }
        }
    }

    /**
     * 从模块创建生产代码作用域
     *
     * 仅包含非测试的源码集。
     *
     * 使用自定义的 CjModuleScope 实现，自动过滤测试代码。
     *
     * @param module 模块实例
     * @return 生产代码作用域
     */
    fun createProductionScope(module: CjModule): GlobalSearchScope {
        val productionSources = module.sourceSets
            .filterNot { it.isTest }
            .flatMap { it.sourceRoots }

        return when {
            productionSources.isEmpty() -> GlobalSearchScope.EMPTY_SCOPE
            else -> CjModuleScope(module, includeTests = false)
        }
    }

    /**
     * 从模块创建测试代码作用域
     *
     * 仅包含测试源码集。
     *
     * 使用自定义的 CjSourceSetScope 实现。
     *
     * @param module 模块实例
     * @return 测试代码作用域
     */
    fun createTestScope(module: CjModule): GlobalSearchScope {
        val project = module.project.intellijProject
        val testSources = module.sourceSets
            .filter { it.isTest }
            .flatMap { it.sourceRoots }

        return when {
            testSources.isEmpty() -> GlobalSearchScope.EMPTY_SCOPE
            else -> CjSourceSetScope(
                project = project,
                sourceRoots = testSources,
                scopeName = "Test: ${module.name}"
            )
        }
    }
}

/**
 * CjModule 扩展属性：获取生产代码作用域
 */
val CjModule.productionScope: GlobalSearchScope
    get() = AnalysisScopeUtils.createProductionScope(this)

/**
 * CjModule 扩展属性：获取测试代码作用域
 */
val CjModule.testScope: GlobalSearchScope
    get() = AnalysisScopeUtils.createTestScope(this)

/**
 * CjModule 扩展属性：获取完整模块作用域
 */
val CjModule.moduleScope: GlobalSearchScope
    get() = AnalysisScopeUtils.createModuleScope(this)

