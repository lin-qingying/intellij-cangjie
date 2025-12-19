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
 * 二进制模块信息接口
 *
 * 该接口表示编译后的二进制模块，通常对应 `.cjo` 文件（仓颉编译产物）。
 * 二进制模块包含编译后的代码和元数据，用于：
 * - 作为其他模块的依赖
 * - 提供库功能
 * - 反编译导航
 *
 * ## 设计目的
 *
 * [BinaryModuleInfo] 区分了项目的源码模块和依赖的库模块：
 * - **源码模块** ([ModuleSourceInfo]): 项目自己的可编辑代码
 * - **二进制模块** ([BinaryModuleInfo]): 依赖的编译后的库
 *
 * ## 核心特性
 *
 * 1. **只读性**: 二进制模块是编译产物，不可直接编辑
 * 2. **元数据加载**: 从编译产物中加载符号和类型信息
 * 3. **反编译支持**: 可以将二进制代码反编译为可读的代码
 * 4. **源码关联**: 可能关联到对应的源码模块 ([SourceForBinaryModuleInfo])
 *
 * ## 继承层次
 *
 * ```
 * ModuleInfo
 *   ↓
 * IdeaModuleInfo
 *   ↓
 * BinaryModuleInfo (二进制模块)
 *   ↓
 * ├── LibraryModuleInfo (库模块接口)
 * │   └── LibraryInfo (具体库实现)
 * ```
 *
 * ## 使用场景
 *
 * ### 作为依赖
 * ```kotlin
 * fun resolveSymbolInDependencies(
 *     name: String,
 *     module: IdeaModuleInfo
 * ): DeclarationDescriptor? {
 *     for (dependency in module.dependencies()) {
 *         if (dependency is BinaryModuleInfo) {
 *             // 从二进制模块的元数据中查找符号
 *             val symbol = findSymbolInModule(dependency, name)
 *             if (symbol != null) return symbol
 *         }
 *     }
 *     return null
 * }
 * ```
 *
 * ### 源码附加检测
 * ```kotlin
 * fun hasAttachedSources(binaryModule: BinaryModuleInfo): Boolean {
 *     // 查找是否有关联的源码模块
 *     val sourceModule = binaryModule.sourcesModuleInfo
 *     return sourceModule != null
 * }
 * ```
 *
 * ### IDE 导航优化
 * ```kotlin
 * fun navigateToDeclaration(binaryModule: BinaryModuleInfo, descriptor: DeclarationDescriptor) {
 *     // 优先尝试导航到源码
 *     val sourcesModule = binaryModule.sourcesModuleInfo
 *     if (sourcesModule != null) {
 *         val sourceDeclaration = findInSourceModule(sourcesModule, descriptor)
 *         if (sourceDeclaration != null) {
 *             navigateToElement(sourceDeclaration)
 *             return
 *         }
 *     }
 *
 *     // 没有源码，使用反编译
 *     val decompiledDeclaration = decompileDeclaration(binaryModule, descriptor)
 *     navigateToElement(decompiledDeclaration)
 * }
 * ```
 *
 * ## 与其他接口的关系
 *
 * - **ModuleSourceInfo**: 源码模块（项目代码）
 * - **BinaryModuleInfo**: 二进制模块（依赖库）
 * - **SourceForBinaryModuleInfo**: 二进制模块对应的源码（库的源码）
 *
 * ## 示例
 *
 * ### 标准库
 * ```
 * 仓颉标准库：
 * - sdk/lib/std.cjo (BinaryModuleInfo)
 * - sdk/src/std/ (可选的 SourceForBinaryModuleInfo)
 *
 * 当用户跳转到标准库函数时：
 * 1. 如果有源码：显示 sdk/src/std/ 中的源代码
 * 2. 如果无源码：反编译 std.cjo 并显示
 * ```
 *
 * ### 第三方库
 * ```
 * Maven 依赖：
 * - ~/.cjpm/cache/mylib-1.0.cjo (BinaryModuleInfo)
 * - ~/.cjpm/cache/mylib-1.0-sources/ (可选的 SourceForBinaryModuleInfo)
 *
 * 依赖关系：
 * projectModule.dependencies() 包含 mylib-1.0.cjo
 * mylib-1.0.cjo.sourcesModuleInfo = mylib-1.0-sources/
 * ```
 *
 * @see IdeaModuleInfo
 * @see LibraryModuleInfo
 * @see SourceForBinaryModuleInfo
 * @see ModuleSourceInfo
 */
interface BinaryModuleInfo : IdeaModuleInfo {
    /**
     * 关联的源码模块信息
     *
     * 返回此二进制模块对应的源码模块（如果有的话）。
     * 源码模块提供原始源代码，用于 IDE 导航时显示可读的代码而非反编译结果。
     *
     * ## 使用场景
     *
     * 1. **IDE 导航优化**: 跳转到声明时，优先显示源码而非反编译代码
     * 2. **代码阅读**: 提供有注释、有原始格式的源代码
     * 3. **调试支持**: 在调试时显示原始源码位置
     *
     * ## 何时返回 null？
     *
     * - 库没有附加源码
     * - 源码文件丢失或损坏
     * - 只提供编译产物的闭源库
     *
     * ## 何时返回非 null？
     *
     * - Maven/Gradle 依赖附加了 sources.jar
     * - 标准库提供了源码
     * - 本地库手动附加了源码目录
     *
     * ## 示例
     *
     * ```kotlin
     * fun navigateToBinaryDeclaration(
     *     binaryModule: BinaryModuleInfo,
     *     descriptor: DeclarationDescriptor
     * ) {
     *     // 检查是否有源码
     *     val sourcesModule = binaryModule.sourcesModuleInfo
     *
     *     if (sourcesModule != null) {
     *         // 在源码中查找声明
     *         val sourceScope = sourcesModule.sourceScope()
     *         val sourceDeclaration = findDeclaration(descriptor, sourceScope)
     *
     *         if (sourceDeclaration != null) {
     *             // 导航到源码
     *             navigateToElement(sourceDeclaration)
     *             return
     *         }
     *     }
     *
     *     // 没有源码或源码中找不到，使用反编译
     *     val decompiledDeclaration = decompileDeclaration(binaryModule, descriptor)
     *     navigateToElement(decompiledDeclaration)
     * }
     * ```
     *
     * ## 实现说明
     *
     * 实现类（如 [LibraryInfo]）通常懒加载源码模块：
     *
     * ```kotlin
     * override val sourcesModuleInfo: SourceForBinaryModuleInfo by lazy {
     *     val sourcesRoots = library.getFiles(OrderRootType.SOURCES)
     *     if (sourcesRoots.isNotEmpty()) {
     *         LibrarySourceInfo(project, library, this, ...)
     *     } else {
     *         null
     *     }
     * }
     * ```
     *
     * @return 对应的源码模块，如果没有附加源码则返回 null
     * @see SourceForBinaryModuleInfo
     * @see LibrarySourceInfo
     */
    val sourcesModuleInfo: SourceForBinaryModuleInfo?
}