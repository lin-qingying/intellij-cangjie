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

import com.intellij.psi.search.GlobalSearchScope


/**
 * 二进制模块对应的源码模块信息接口
 *
 * 该接口表示为编译后的二进制模块（如 .cjo 文件）提供源码的模块。
 * 它允许 IDE 在导航到二进制模块的声明时，显示对应的源代码而非反编译的代码。
 *
 * ## 设计目的
 *
 * 当用户跳转到库的声明时，IDE 需要决定显示什么：
 * 1. **有源码**: 显示原始源代码（可读性好，有注释）
 * 2. **无源码**: 显示反编译代码（可读性差，无注释）
 *
 * [SourceForBinaryModuleInfo] 提供了"有源码"的情况，将二进制模块与其源码模块关联起来。
 *
 * ## 核心职责
 *
 * 1. **关联源码**: 将二进制模块与对应的源码模块关联
 * 2. **双向查找**: 支持从二进制查找源码，或从源码查找二进制
 * 3. **导航优化**: 在 IDE 导航时优先使用源码而非反编译
 *
 * ## 继承层次
 *
 * ```
 * ModuleInfo
 *   ↓
 * IdeaModuleInfo
 *   ↓
 * SourceForBinaryModuleInfo (表示源码模块)
 *   ↓
 * LibrarySourceInfo (具体实现)
 * ```
 *
 * ## 设计特性
 *
 * ### 空内容作用域
 * 源码模块的 [contentScope] 返回空作用域，因为：
 * - 源码文件不独立分析，而是依赖对应的二进制模块
 * - 每个源文件单独分析，与二进制模块建立关联
 * - 避免重复分析（二进制已经包含完整的语义信息）
 *
 * ### 依赖传递
 * 源码模块的依赖直接继承自对应的二进制模块：
 * - `dependencies()` = [自身] + 二进制模块的依赖
 * - `dependenciesWithoutSelf()` = 二进制模块的依赖
 *
 * ## 使用场景
 *
 * ### IDE 导航
 * ```kotlin
 * fun navigateToDeclaration(binaryDeclaration: CjDeclaration): NavigationResult {
 *     val binaryModule = binaryDeclaration.moduleInfo
 *
 *     // 查找是否有对应的源码模块
 *     val sourceModule = findSourceForBinaryModule(binaryModule)
 *
 *     if (sourceModule != null) {
 *         // 在源码中查找声明
 *         val sourceDeclaration = findInSource(binaryDeclaration, sourceModule)
 *         return NavigateToSource(sourceDeclaration)
 *     } else {
 *         // 显示反编译代码
 *         return NavigateToDecompiled(binaryDeclaration)
 *     }
 * }
 * ```
 *
 * ### 源码附加
 * ```kotlin
 * // 为库附加源码
 * fun attachSources(library: Library, sourcesJar: File) {
 *     val binaryModule = library.moduleInfo as BinaryModuleInfo
 *
 *     // 创建源码模块
 *     val sourceModule = LibrarySourceInfo(library, sourcesJar)
 *
 *     // 建立关联
 *     sourceModule.binariesModuleInfo == binaryModule
 * }
 * ```
 *
 * ## 与其他接口的关系
 *
 * - **BinaryModuleInfo**: 表示编译后的二进制模块（.cjo）
 * - **SourceForBinaryModuleInfo**: 表示二进制模块的源码（.cj）
 * - 一个二进制模块可能有 0 个或 1 个对应的源码模块
 * - 一个源码模块对应 1 个二进制模块
 *
 * ## 示例
 *
 * ### Maven 库的源码附加
 * ```
 * 依赖库结构：
 * - mylib-1.0.jar (二进制，BinaryModuleInfo)
 * - mylib-1.0-sources.jar (源码，SourceForBinaryModuleInfo)
 *
 * 关系：
 * sourceModuleInfo.binariesModuleInfo = binaryModuleInfo
 * ```
 *
 * ### 标准库的源码
 * ```
 * 仓颉 SDK 结构：
 * - sdk/lib/std.cjo (二进制，BinaryModuleInfo)
 * - sdk/src/std/ (源码，SourceForBinaryModuleInfo)
 *
 * 关系：
 * stdSourceInfo.binariesModuleInfo = stdBinaryInfo
 * ```
 *
 * @see BinaryModuleInfo
 * @see LibrarySourceInfo
 * @see IdeaModuleInfo
 */
interface SourceForBinaryModuleInfo : IdeaModuleInfo {
    /**
     * 关联的二进制模块信息
     *
     * 返回此源码模块对应的二进制模块。二进制模块包含编译后的代码，
     * 而源码模块提供原始源代码用于 IDE 导航和显示。
     *
     * ## 用途
     *
     * - **依赖传递**: 源码模块的依赖继承自二进制模块
     * - **符号解析**: 实际的符号信息来自二进制模块
     * - **双向导航**: 支持从源码跳转到二进制，或从二进制跳转到源码
     *
     * ## 示例
     *
     * ```kotlin
     * val sourceModule: SourceForBinaryModuleInfo = getSourceModule()
     * val binaryModule: BinaryModuleInfo = sourceModule.binariesModuleInfo
     *
     * // 获取二进制模块的依赖
     * val dependencies = binaryModule.dependencies()
     *
     * // 在二进制模块中查找符号
     * val symbol = binaryModule.findSymbol(name)
     * ```
     *
     * @return 对应的二进制模块信息
     * @see BinaryModuleInfo
     */
    val binariesModuleInfo: BinaryModuleInfo

    /**
     * 获取源码文件的搜索作用域
     *
     * 返回包含此源码模块所有源文件的搜索范围，用于：
     * - 在源码中查找文件
     * - 导航到源码声明
     * - 显示源码内容
     *
     * ## 与 contentScope 的区别
     *
     * - **sourceScope()**: 返回实际的源码文件范围（用于查找源文件）
     * - **contentScope**: 返回空范围（源码不独立分析）
     *
     * ## 使用场景
     *
     * ```kotlin
     * // 在源码中查找声明
     * fun findSourceDeclaration(
     *     name: String,
     *     sourceModule: SourceForBinaryModuleInfo
     * ): CjDeclaration? {
     *     val sourceScope = sourceModule.sourceScope()
     *     return CangJieClassShortNameIndex.getInstance()
     *         .get(name, sourceModule.project, sourceScope)
     *         .firstOrNull()
     * }
     * ```
     *
     * @return 源码文件的全局搜索作用域
     * @see GlobalSearchScope
     * @see contentScope
     */
    fun sourceScope(): GlobalSearchScope

    /**
     * 模块内容的搜索作用域（空）
     *
     * **重要**: 源码模块的 contentScope 总是返回空范围。
     *
     * ## 设计原因
     *
     * 源码模块不具有独立的"内容"，原因如下：
     *
     * 1. **不独立分析**: 源码文件不作为独立的分析单元
     * 2. **依赖二进制**: 每个源文件的分析依赖对应的二进制模块
     * 3. **避免重复**: 二进制模块已包含完整的语义信息，无需重新分析源码
     * 4. **单独处理**: 每个源文件单独分析，不作为一个整体
     *
     * ## 实际使用
     *
     * 如需访问源码文件，应使用 [sourceScope] 而非 [contentScope]：
     *
     * ```kotlin
     * // 错误：contentScope 是空的
     * val files1 = FilenameIndex.getAllFiles(sourceModule.contentScope) // 返回空
     *
     * // 正确：使用 sourceScope
     * val files2 = FilenameIndex.getAllFiles(sourceModule.sourceScope()) // 返回源文件
     * ```
     *
     * ## 分析机制
     *
     * 源码文件的分析机制：
     * 1. 用户打开源码文件
     * 2. IDE 为该文件创建临时分析上下文
     * 3. 分析上下文依赖对应的二进制模块
     * 4. 使用二进制模块的类型信息进行语法高亮和补全
     *
     * @return 总是返回 [GlobalSearchScope.EMPTY_SCOPE]
     * @see sourceScope
     */
    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.EMPTY_SCOPE

    /**
     * 模块依赖列表
     *
     * 返回源码模块的依赖，包括自身和对应二进制模块的所有依赖。
     *
     * ## 依赖组成
     *
     * ```
     * dependencies() = [自身] + 二进制模块的依赖
     * ```
     *
     * ## 为什么包含二进制模块的依赖？
     *
     * 源码模块需要访问二进制模块的依赖，因为：
     * - 源码可能引用依赖库的类型
     * - 符号解析需要在依赖库中查找
     * - 保持与二进制模块相同的依赖环境
     *
     * ## 示例
     *
     * ```
     * 假设：
     * - mylib-binary 依赖 [stdlib, utils]
     * - mylib-source 对应 mylib-binary
     *
     * 则：
     * mylib-source.dependencies() = [mylib-source, mylib-binary, stdlib, utils]
     * ```
     *
     * @return 包含自身和二进制模块依赖的列表
     * @see dependenciesWithoutSelf
     * @see binariesModuleInfo
     */
    override val dependencies get() = listOf(this) + binariesModuleInfo.dependencies

    /**
     * 获取不包含自身的依赖序列
     *
     * 直接返回对应二进制模块的依赖，不包含源码模块自身。
     *
     * ## 实现说明
     *
     * 与默认实现不同，此方法直接返回二进制模块的依赖，
     * 而非过滤 [dependencies] 的结果。这样更高效，避免了不必要的列表操作。
     *
     * ## 依赖关系
     *
     * ```
     * dependencies() = [自身] + 二进制模块的依赖
     * dependenciesWithoutSelf() = 二进制模块的依赖
     * ```
     *
     * ## 示例
     *
     * ```kotlin
     * val sourceModule: SourceForBinaryModuleInfo = getSourceModule()
     *
     * // 包含自身
     * val allDeps = sourceModule.dependencies()
     * // [sourceModule, binaryModule, stdlib, utils]
     *
     * // 不包含自身
     * val externalDeps = sourceModule.dependenciesWithoutSelf().toList()
     * // [binaryModule, stdlib, utils]
     * ```
     *
     * @return 二进制模块的依赖序列
     */
    override fun dependenciesWithoutSelf(): Sequence<IdeaModuleInfo> = binariesModuleInfo.dependencies.asSequence()

    /**
     * 模块来源类型
     *
     * 源码模块的来源类型总是 [ModuleOrigin.OTHER]，
     * 表示它不是标准的源码模块或库模块，而是一种特殊类型。
     *
     * ## 为什么是 OTHER？
     *
     * - **MODULE**: 用于项目的源码模块
     * - **LIBRARY**: 用于依赖的二进制库
     * - **OTHER**: 用于特殊类型，如：
     *   - 库的源码（SourceForBinaryModuleInfo）
     *   - SDK 源码
     *   - 临时文件
     *
     * ## 使用场景
     *
     * ```kotlin
     * fun shouldAnalyze(module: IdeaModuleInfo): Boolean {
     *     return when (module.moduleOrigin) {
     *         ModuleOrigin.MODULE -> true  // 分析项目源码
     *         ModuleOrigin.LIBRARY -> false  // 不分析库
     *         ModuleOrigin.OTHER -> module !is SourceForBinaryModuleInfo  // 特殊处理
     *     }
     * }
     * ```
     *
     * @return [ModuleOrigin.OTHER]
     * @see ModuleOrigin
     */
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER
}