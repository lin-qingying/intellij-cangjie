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

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.util.PathUtil
import org.cangnova.cangjie.descriptors.ModuleOrigin
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.projectStructure.scope.CombinableSourceAndClassRootsScope
import org.cangnova.cangjie.projectStructure.scope.PoweredLibraryScopeBase
import org.cangnova.cangjie.projectStructure.scope.calculateEntriesVirtualFileSystems
import org.cangnova.cangjie.projectStructure.scope.calculateTopPackageNames
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServicesImpl

/**
 * 库信息抽象类
 *
 * 该类是库模块信息的具体实现，整合了多个接口的功能：
 * - [IdeaModuleInfo]: IDE 特定的模块信息
 * - [LibraryModuleInfo]: 库模块的根目录管理
 * - [BinaryModuleInfo]: 二进制模块的源码关联
 *
 * ## 设计目的
 *
 * [LibraryInfo] 提供了库模块的完整实现：
 * 1. **库标识**: 通过库名称和显示名称标识库
 * 2. **作用域管理**: 提供优化的库文件搜索作用域
 * 3. **依赖管理**: 管理库之间的依赖关系
 * 4. **源码关联**: 支持附加源码用于导航
 * 5. **性能优化**: 预计算包名和文件系统信息
 *
 * ## 核心特性
 *
 * ### 1. 预计算优化
 * 在初始化时计算并缓存：
 * - 类文件的顶层包名 (`topClassesPackageNames`)
 * - 类文件的虚拟文件系统 (`classesEntriesVirtualFileSystems`)
 * - 源码文件的顶层包名 (`topSourcesPackageNames`)
 * - 源码文件的虚拟文件系统 (`sourcesEntriesVirtualFileSystems`)
 *
 * ### 2. 懒加载源码
 * 源码模块信息使用 `lazy` 延迟初始化，只在需要时创建。
 *
 * ### 3. 优化的作用域
 * `contentScope` 返回 `LibraryWithoutSourceScope`，利用预计算的包名加速文件查找。
 *
 * ## 使用场景
 *
 * ### 创建库信息
 * ```kotlin
 * class MyLibraryInfo(
 *     project: Project,
 *     library: LibraryEx
 * ) : LibraryInfo(project, library) {
 *     // 子类可以添加额外的功能
 * }
 * ```
 *
 * ### 访问库依赖
 * ```kotlin
 * val libraryInfo: LibraryInfo = getLibraryInfo(library)
 *
 * // 获取所有依赖（包含自身）
 * val allDeps = libraryInfo.dependencies
 *
 * // 获取外部依赖（不含自身）
 * val externalDeps = libraryInfo.dependenciesWithoutSelf().toList()
 * ```
 *
 * ### 检查源码附加
 * ```kotlin
 * val libraryInfo: LibraryInfo = getLibraryInfo(library)
 *
 * if (libraryInfo.sourcesModuleInfo != null) {
 *     println("库有附加源码")
 * } else {
 *     println("库没有源码，将使用反编译")
 * }
 * ```
 *
 * ### 在作用域中查找文件
 * ```kotlin
 * val libraryInfo: LibraryInfo = getLibraryInfo(library)
 * val scope = libraryInfo.contentScope
 *
 * // 在库中查找类
 * val classes = CangJieClassShortNameIndex.getInstance()
 *     .get(className, project, scope)
 * ```
 *
 * ## 性能优化说明
 *
 * ### 包名预计算
 * ```kotlin
 * init {
 *     val (classes, sources) = runReadAction {
 *         library.getFiles(OrderRootType.CLASSES) to library.getFiles(OrderRootType.SOURCES)
 *     }
 *
 *     topClassesPackageNames = classes.calculateTopPackageNames()
 *     // ...
 * }
 * ```
 *
 * 这样在创建 `LibraryWithoutSourceScope` 时可以直接使用预计算的包名，
 * 避免每次都扫描文件系统。
 *
 * ### 文件系统缓存
 * 预计算 `entriesVirtualFileSystems`，用于快速判断文件是否属于此库。
 *
 * ## 生命周期
 *
 * ```
 * 1. 创建 LibraryInfo 实例
 *   ↓
 * 2. init 块执行：预计算包名和文件系统信息
 *   ↓
 * 3. 使用 contentScope：创建优化的 LibraryWithoutSourceScope
 *   ↓
 * 4. 首次访问 sourcesModuleInfo：懒加载源码模块（如果有）
 *   ↓
 * 5. 使用完毕，检查 isDisposed
 * ```
 *
 * @param project IntelliJ 项目实例
 * @param library IntelliJ 库对象（LibraryEx）
 *
 * @see IdeaModuleInfo
 * @see LibraryModuleInfo
 * @see BinaryModuleInfo
 * @see LibraryWithoutSourceScope
 */
abstract class LibraryInfo internal constructor(
    override val project: Project,
    val library: LibraryEx,
) : IdeaModuleInfo, LibraryModuleInfo, BinaryModuleInfo  {
    /**
     * 类文件的顶层包名集合
     *
     * 预计算的类文件（.cjo）根目录下的顶层包名，用于优化文件查找。
     */
    private val topClassesPackageNames: Set<String>?

    /**
     * 类文件的虚拟文件系统集合
     *
     * 预计算的类文件根目录所在的虚拟文件系统，用于快速判断文件归属。
     */
    private val classesEntriesVirtualFileSystems: Set<NewVirtualFileSystem>?

    /**
     * 源码文件的顶层包名集合
     *
     * 预计算的源码文件根目录下的顶层包名，用于优化源码查找。
     */
    private val topSourcesPackageNames: Set<String>?

    /**
     * 源码文件的虚拟文件系统集合
     *
     * 预计算的源码文件根目录所在的虚拟文件系统，用于快速判断文件归属。
     */
    private val sourcesEntriesVirtualFileSystems: Set<NewVirtualFileSystem>?

    /**
     * 初始化块：预计算包名和文件系统信息
     *
     * 在读操作中获取库的类文件和源码文件，然后预计算：
     * - 顶层包名（用于作用域过滤）
     * - 虚拟文件系统（用于文件归属判断）
     *
     * 这些信息在库的生命周期内保持不变，因此只需计算一次。
     */
    init {
        val (classes, sources) =
            runReadAction {
                library.getFiles(OrderRootType.CLASSES) to library.getFiles(OrderRootType.SOURCES)
            }

        topClassesPackageNames = classes.calculateTopPackageNames()
        classesEntriesVirtualFileSystems = classes.calculateEntriesVirtualFileSystems()

        topSourcesPackageNames = sources.calculateTopPackageNames()
        sourcesEntriesVirtualFileSystems = sources.calculateEntriesVirtualFileSystems()
    }

    /**
     * 模块来源类型
     *
     * 库模块的来源总是 [ModuleOrigin.LIBRARY]。
     *
     * @see ModuleOrigin
     */
    override val moduleOrigin: ModuleOrigin get() = ModuleOrigin.LIBRARY

    /**
     * 模块名称
     *
     * 使用特殊名称格式 `<library 库名>`，用于内部标识。
     */
    override val name: Name = Name.special("<library ${library.name}>")

    /**
     * 显示名称
     *
     * 用于 UI 显示的库名称，使用国际化消息格式 `<library {0}>`。
     *
     * @see CangJieModuleInfoBundle
     */
    override val displayedName: String
        get() = CangJieModuleInfoBundle.message("library.0", library.presentableName)

    /**
     * 模块内容的搜索作用域
     *
     * 返回优化的库作用域 [LibraryWithoutSourceScope]，只包含类文件（.cjo），
     * 不包含源码文件。使用预计算的包名和文件系统信息加速查找。
     *
     * @return 库的搜索作用域
     * @see LibraryWithoutSourceScope
     */
    override val contentScope: GlobalSearchScope
        get() = LibraryWithoutSourceScope(project, topClassesPackageNames, classesEntriesVirtualFileSystems, library)

    /**
     * 模块依赖列表
     *
     * 返回此库的所有依赖，包括：
     * 1. 库自身（第一个元素）
     * 2. 库的所有依赖库
     *
     * 依赖信息从 [LibraryDependenciesCache] 中获取并缓存。
     *
     * @return 包含自身和所有依赖的列表
     * @see LibraryDependenciesCache
     */
    override val dependencies : List<IdeaModuleInfo> get()  {
        val dependencies = LibraryDependenciesCache.getInstance(project).getLibraryDependencies(this)
        return buildList {
            add(this@LibraryInfo)

            addAll(dependencies.librariesWithoutSelf)
        }
    }

    /**
     * 获取不包含自身的依赖序列
     *
     * 返回库的外部依赖，不包含库自身。
     *
     * @return 外部依赖的序列
     */
    override fun dependenciesWithoutSelf(): Sequence<IdeaModuleInfo> {
        val dependencies = LibraryDependenciesCache.getInstance(project).getLibraryDependencies(this)
        return  dependencies.librariesWithoutSelf.asSequence()
    }


    /**
     * 平台相关的分析服务
     *
     * 返回平台分析服务实现，用于类型检查和解析。
     *
     * @return 分析服务实例
     */
    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = PlatformDependentAnalyzerServicesImpl

    /**
     * 懒加载的源码模块信息（内部字段）
     *
     * 使用 lazy 延迟创建源码模块信息，只在首次访问时初始化。
     */
    private val _sourcesModuleInfo: SourceForBinaryModuleInfo by lazy {
        LibrarySourceInfo(project, library, this, topSourcesPackageNames, sourcesEntriesVirtualFileSystems)
    }

    /**
     * 关联的源码模块信息
     *
     * 返回此库对应的源码模块（如果有附加源码的话）。
     * 使用懒加载策略，只在需要时创建。
     *
     * @return 源码模块信息，如果没有附加源码则返回 null
     * @see SourceForBinaryModuleInfo
     * @see LibrarySourceInfo
     */
    override val sourcesModuleInfo: SourceForBinaryModuleInfo
        get() = _sourcesModuleInfo

    /**
     * 获取库文件的根目录路径列表
     *
     * 返回库的所有类文件根目录的本地路径。
     *
     * @return 库根目录的路径集合
     */
    override fun getLibraryRoots(): Collection<String> = library.getFiles(OrderRootType.CLASSES).mapNotNull(PathUtil::getLocalPath)


    /**
     * 库是否已被释放
     *
     * 检查底层的 IntelliJ 库对象是否已经被释放。
     *
     * @return true 如果库已释放
     */
    val isDisposed get() = library.isDisposed

    /**
     * 检查库的有效性
     *
     * 验证库是否仍然有效（未被释放）。
     * 如果库已被释放，抛出 [AlreadyDisposedException]。
     *
     * @throws AlreadyDisposedException 如果库已被释放
     */
    override fun checkValidity() {
        if (isDisposed) {
            throw AlreadyDisposedException("Library '${name}' is already disposed")
        }
    }

    /**
     * 返回库信息的字符串表示
     *
     * 格式: `类名@哈希码(库对象)`
     *
     * @return 调试用的字符串表示
     */
    override fun toString() = "${this::class.simpleName}@${Integer.toHexString(System.identityHashCode(this))}($library)"
}