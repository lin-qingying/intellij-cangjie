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

    /**
     * 检查是否在指定模块的内容中搜索
     *
     * 用于判断搜索范围是否包含指定模块的内容。
     *
     * @param aModule 要检查的模块
     * @return 如果 aModule 是当前模块则返回 true，否则返回 false
     */
    override fun isSearchInModuleContent(aModule: Module): Boolean = aModule == module

    /**
     * 检查是否在库中搜索
     *
     * 源码作用域不包含库文件，只包含模块的源码文件。
     *
     * @return 总是返回 false
     */
    override fun isSearchInLibraries(): Boolean = false

    /**
     * 获取作用域的显示名称
     *
     * 用于在 IDE 中显示此作用域的名称，通过国际化消息获取。
     *
     * @return 格式化的作用域显示名称
     * @see CangJieModuleInfoBundle
     */
    override fun getDisplayName(): String = CangJieModuleInfoBundle.message("module.sources.scope.0", module.name)

    /**
     * 判断是否与另一个对象相等
     *
     * 两个 [ModuleSourcesScope] 相等当且仅当它们的模块和源码根类型都相同。
     *
     * @param other 要比较的对象
     * @return 如果对象相等则返回 true
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ModuleSourcesScope) return false

        return module == other.module && sourceRootKind == other.sourceRootKind
    }

    /**
     * 计算哈希码
     *
     * 基于源码根类型和模块的哈希码计算。
     *
     * @return 作用域的哈希码
     */
    override fun calcHashCode(): Int = sourceRootKind.hashCode() + 31 * module.hashCode()

    /**
     * 获取字符串表示
     *
     * 返回易于调试的字符串表示形式。
     *
     * @return 格式为 "{sourceRootKind} sources of module:{moduleName}" 的字符串
     */
    override fun toString(): String = "$sourceRootKind sources of module:${module.name}"

    companion object {
        /**
         * 创建生产源码作用域
         *
         * 工厂方法，用于创建包含模块所有生产源码的作用域。
         *
         * ## 使用示例
         * ```kotlin
         * val module: Module = getModule()
         * val productionScope = ModuleSourcesScope.production(module)
         * // 在此作用域中搜索只会包含生产源码文件
         * ```
         *
         * @param module 模块对象
         * @return 生产源码作用域
         */
        fun production(module: Module): ModuleSourcesScope =
            ModuleSourcesScope(module, SourceRootKind.PRODUCTION)

        /**
         * 创建测试源码作用域
         *
         * 工厂方法，用于创建包含模块所有测试源码的作用域。
         *
         * ## 使用示例
         * ```kotlin
         * val module: Module = getModule()
         * val testScope = ModuleSourcesScope.tests(module)
         * // 在此作用域中搜索只会包含测试源码文件
         * ```
         *
         * @param module 模块对象
         * @return 测试源码作用域
         */
        fun tests(module: Module): ModuleSourcesScope =
            ModuleSourcesScope(module, SourceRootKind.TESTS)
    }
}

/**
 * 计算模块的源码根目录集合
 *
 * 遍历模块的所有内容条目，提取符合指定类型的源码根目录。
 *
 * ## 过滤规则
 *
 * 1. **排除资源目录**: 不包含 JavaResourceRootType 类型的目录
 * 2. **生产源码**: 当 sourceRootKind 为 PRODUCTION 时，只包含非测试源码目录
 * 3. **测试源码**: 当 sourceRootKind 为 TESTS 时，只包含测试源码目录
 *
 * ## 实现细节
 *
 * - 使用 [ModuleRootManager] 获取模块的内容条目
 * - 遍历每个 contentEntry 的 sourceFolders
 * - 根据 sourceRootKind 过滤合适的源码文件夹
 * - 提取虚拟文件对象并收集到列表中
 *
 * @param module 要计算根目录的模块
 * @param sourceRootKind 源码根类型（生产或测试）
 * @return 包含所有符合条件的源码根目录的列表
 *
 * @see ModuleRootManager
 * @see JavaResourceRootType
 */
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

