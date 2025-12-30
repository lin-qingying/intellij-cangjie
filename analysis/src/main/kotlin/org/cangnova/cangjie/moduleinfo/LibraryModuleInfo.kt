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

/**
 * 库模块信息接口
 *
 * 该接口表示库模块的元信息，扩展了 [ModuleInfo] 接口，
 * 添加了库特定的功能，如获取库文件的根目录。
 *
 * ## 设计目的
 *
 * [LibraryModuleInfo] 专门用于表示依赖的库模块：
 * - 提供库文件的物理位置信息
 * - 支持库的管理和查询
 * - 为库的加载和解析提供基础信息
 *
 * ## 核心职责
 *
 * 1. **库根目录管理**: 提供库文件的根目录路径列表
 * 2. **库标识**: 通过 ModuleInfo 提供库的名称和元数据
 * 3. **依赖管理**: 管理库之间的依赖关系
 *
 * ## 继承层次
 *
 * ```
 * ModuleInfo (基础接口)
 *   ↓
 * LibraryModuleInfo (库模块接口)
 *   ↓
 * IdeaModuleInfo + BinaryModuleInfo
 *   ↓
 * LibraryInfo (具体实现)
 * ```
 *
 * ## 使用场景
 *
 * ### 库文件查找
 * ```kotlin
 * fun findLibraryFiles(library: LibraryModuleInfo): List<File> {
 *     val roots = library.getLibraryRoots()
 *     return roots.map { File(it) }.filter { it.exists() }
 * }
 * ```
 *
 * ### 库扫描
 * ```kotlin
 * fun scanLibraryClasses(library: LibraryModuleInfo): List<String> {
 *     val classNames = mutableListOf<String>()
 *
 *     for (rootPath in library.getLibraryRoots()) {
 *         val root = File(rootPath)
 *         root.walk()
 *             .filter { it.extension == "cjo" }
 *             .forEach { file ->
 *                 classNames.add(extractClassName(file))
 *             }
 *     }
 *
 *     return classNames
 * }
 * ```
 *
 * ### 库验证
 * ```kotlin
 * fun validateLibrary(library: LibraryModuleInfo): Boolean {
 *     val roots = library.getLibraryRoots()
 *
 *     // 检查所有根目录是否存在
 *     return roots.all { path ->
 *         val file = File(path)
 *         file.exists() && (file.isDirectory || file.isFile)
 *     }
 * }
 * ```
 *
 * ## 与其他接口的区别
 *
 * - **ModuleInfo**: 通用模块信息（源码和库都适用）
 * - **LibraryModuleInfo**: 专门针对库模块（提供库根目录）
 * - **BinaryModuleInfo**: 二进制模块（提供源码关联）
 * - **IdeaModuleInfo**: IDE 特定的模块信息（提供作用域）
 *
 * ## 实现注意事项
 *
 * 实现类应该确保：
 * 1. 返回的路径是绝对路径
 * 2. 路径指向实际存在的文件或目录
 * 3. 路径格式符合当前操作系统的规范
 *
 * @see ModuleInfo
 * @see IdeaModuleInfo
 * @see BinaryModuleInfo
 * @see LibraryInfo
 */
interface LibraryModuleInfo : ModuleInfo {

    /**
     * 获取库文件的根目录路径列表
     *
     * 返回此库模块的所有根目录的绝对路径。这些根目录通常包含：
     * - 编译后的 .cjo 文件
     * - 资源文件
     * - 元数据文件
     *
     * ## 路径格式
     *
     * - **绝对路径**: 返回的路径应该是绝对路径，不是相对路径
     * - **本地路径**: 对于 Jar 文件，应该返回解压后的本地路径或 Jar 文件路径
     * - **规范化**: 路径应该经过规范化处理，去除多余的 `.` 和 `..`
     *
     * ## 返回值说明
     *
     * 对于不同类型的库，返回值可能不同：
     *
     * ### JAR 库
     * ```
     * ["/path/to/mylib-1.0.jar"]
     * ```
     *
     * ### 目录库
     * ```
     * ["/path/to/lib/classes"]
     * ```
     *
     * ### 多根目录库
     * ```
     * [
     *   "/path/to/lib/classes",
     *   "/path/to/lib/resources"
     * ]
     * ```
     *
     * ### 标准库
     * ```
     * ["/path/to/cangjie-sdk/lib/std.cjo"]
     * ```
     *
     * ## 使用场景
     *
     * ### 类路径构建
     * ```kotlin
     * fun buildClassPath(libraries: List<LibraryModuleInfo>): String {
     *     return libraries
     *         .flatMap { it.getLibraryRoots() }
     *         .joinToString(File.pathSeparator)
     * }
     * ```
     *
     * ### 文件搜索
     * ```kotlin
     * fun findClassFile(
     *     library: LibraryModuleInfo,
     *     className: String
     * ): File? {
     *     val fileName = className.replace('.', '/') + ".cjo"
     *
     *     for (rootPath in library.getLibraryRoots()) {
     *         val file = File(rootPath, fileName)
     *         if (file.exists()) return file
     *     }
     *
     *     return null
     * }
     * ```
     *
     * ### 库大小统计
     * ```kotlin
     * fun calculateLibrarySize(library: LibraryModuleInfo): Long {
     *     return library.getLibraryRoots()
     *         .map { File(it) }
     *         .filter { it.exists() }
     *         .sumOf { it.length() }
     * }
     * ```
     *
     * ## 实现示例
     *
     * ```kotlin
     * class MyLibraryInfo(private val library: Library) : LibraryModuleInfo {
     *     override fun getLibraryRoots(): Collection<String> {
     *         return library.getFiles(OrderRootType.CLASSES)
     *             .mapNotNull { PathUtil.getLocalPath(it) }
     *     }
     * }
     * ```
     *
     * @return 库根目录的绝对路径集合
     */
    fun getLibraryRoots(): Collection<String>
}
