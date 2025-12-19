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
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.projectStructure.scope.CombinableSourceAndClassRootsScope
import org.cangnova.cangjie.scope.AbstractVirtualFileRootsScope
import org.jetbrains.jps.model.java.JavaResourceRootType

/**
 * 模块源码作用域
 *
 * 该类表示模块的源码搜索范围，可以是生产源码或测试源码。
 * 它可以与其他作用域组合成 CombinedSourceAndClassRootsScope。
 *
 * ## 设计目的
 *
 * [ModuleSourcesScope] 为模块源码提供精确的搜索范围：
 * 1. **源码类型区分**: 区分生产源码和测试源码
 * 2. **根目录管理**: 管理模块的源码根目录列表
 * 3. **文件查找**: 高效判断文件是否在作用域内
 * 4. **可组合性**: 支持与其他作用域组合
 *
 * ## 使用场景
 *
 * ### 创建生产源码作用域
 * ```kotlin
 * val productionScope = ModuleSourcesScope.production(module)
 * ```
 *
 * ### 创建测试源码作用域
 * ```kotlin
 * val testScope = ModuleSourcesScope.tests(module)
 * ```
 *
 * @param module 模块对象
 * @param sourceRootKind 源码根类型（生产或测试）
 *
 * @see AbstractVirtualFileRootsScope
 * @see CombinableSourceAndClassRootsScope
 */
class ModuleSourcesScope(
    private val module: Module,
    private val sourceRootKind: SourceRootKind,
) : AbstractVirtualFileRootsScope(module.project), CombinableSourceAndClassRootsScope {

    /**
     * 源码根类型枚举
     *
     * 定义 [ModuleSourcesScope] 包含的源码根类型。
     */
    enum class SourceRootKind {
        /** 生产源码 */
        PRODUCTION,

        /** 测试源码 */
        TESTS,
    }

    /**
     * 源码根目录列表
     *
     * 从模块配置中计算得出的源码根目录列表。
     * 根据 [sourceRootKind] 过滤生产或测试源码。
     */
    override val roots: List<VirtualFile> = calculateRootsSet(module, sourceRootKind)

    /**
     * 检查作用域是否为空
     *
     * @return 如果模块没有源码根目录则返回 true
     */
    fun isEmpty(): Boolean = roots.isEmpty()

    /**
     * 包含的模块集合
     *
     * @return 只包含当前模块
     */
    override val modules: Set<Module> get() = setOf(module)

    /**
     * 是否包含库类根目录
     *
     * @return false，源码作用域不包含库类
     */
    override val includesLibraryClassRoots: Boolean get() = false

    /**
     * 是否包含库源码根目录
     *
     * @return false，源码作用域不包含库源码
     */
    override val includesLibrarySourceRoots: Boolean get() = false

    /**
     * 获取文件的根目录
     *
     * 对于源码文件，返回其所属的源码根目录。
     *
     * ## 实现说明
     *
     * 使用稳定的公共 API `getSourceRootForFile` 替代内部 API `getModuleSourceOrLibraryClassesRoot`。
     * 因为 [ModuleSourcesScope] 只包含源码根目录，所以只需要获取源码根即可。
     *
     * @param file 要查询的文件
     * @return 文件所属的源码根目录，如果文件不在任何源码根目录下则返回 null
     */
    override fun getFileRoot(file: VirtualFile): VirtualFile? = myProjectFileIndex.getSourceRootForFile(file)

    override fun isSearchInModuleContent(aModule: Module): Boolean = aModule == module

    override fun isSearchInLibraries(): Boolean = false

    override fun getDisplayName(): String = CangJieModuleInfoBundle.message("module.sources.scope.0", module.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ModuleSourcesScope) return false

        return module == other.module && sourceRootKind == other.sourceRootKind
    }

    override fun calcHashCode(): Int = sourceRootKind.hashCode() + 31 * module.hashCode()

    override fun toString(): String = "$sourceRootKind sources of module:${module.name}"

    companion object {
        fun production(module: Module): ModuleSourcesScope =
            ModuleSourcesScope(module, SourceRootKind.PRODUCTION)

        fun tests(module: Module): ModuleSourcesScope =
            ModuleSourcesScope(module, SourceRootKind.TESTS)
    }
}
private fun calculateRootsSet(module: Module, sourceRootKind: ModuleSourcesScope.SourceRootKind): ArrayList<VirtualFile> {
    val roots = ArrayList<VirtualFile>()
    val moduleRootManager = ModuleRootManager.getInstance(module)

    for (contentEntry in moduleRootManager.contentEntries) {
        contentEntry
            .sourceFolders
            .filter { sourceFolder ->
                when {
                    sourceFolder.rootType is JavaResourceRootType -> false
                    sourceRootKind == ModuleSourcesScope.SourceRootKind.PRODUCTION -> !sourceFolder.isTestSource
                    sourceRootKind == ModuleSourcesScope.SourceRootKind.TESTS -> sourceFolder.isTestSource
                    else -> false
                }
            }
            .mapNotNullTo(roots) { it.file }
    }

    return roots
}

