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
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Disposer.isDisposed
import com.intellij.serviceContainer.AlreadyDisposedException

/**
 * 模块信息验证工具类
 *
 * 该文件包含用于验证模块、库等对象有效性的扩展函数。
 * 这些函数确保在对模块或库执行操作前，它们仍然处于有效状态（未被释放）。
 *
 * ## 为什么需要验证？
 *
 * 在 IntelliJ 平台中，模块和库可能在以下情况下被释放（disposed）：
 * - 模块从项目中删除
 * - 项目关闭
 * - IDE 重新加载项目
 * - 库配置发生重大变更
 *
 * 在已释放的对象上执行操作会导致异常或未定义的行为，因此需要验证。
 *
 * ## 使用场景
 *
 * ### 在缓存中验证模块
 * ```kotlin
 * fun getCachedModuleInfo(module: Module): ModuleInfo? {
 *     try {
 *         module.checkValidity()
 *         return cache[module]
 *     } catch (e: AlreadyDisposedException) {
 *         // 模块已释放，清理缓存
 *         cache.remove(module)
 *         return null
 *     }
 * }
 * ```
 *
 * ### 在异步操作中验证库
 * ```kotlin
 * suspend fun analyzeLibrary(library: Library) {
 *     withContext(Dispatchers.IO) {
 *         library.checkValidity() // 确保库仍然有效
 *         // 执行耗时的分析操作
 *         performAnalysis(library)
 *     }
 * }
 * ```
 */

/**
 * 检查模块的有效性
 *
 * 验证 IntelliJ 模块是否仍然有效（未被释放）。
 * 如果模块已释放，抛出 [AlreadyDisposedException]。
 *
 * ## 使用场景
 *
 * - 在对模块执行操作前验证其有效性
 * - 在异步操作中确保模块仍然存在
 * - 在缓存失效时清理已释放的模块
 *
 * ## 示例
 *
 * ```kotlin
 * fun processModule(module: Module) {
 *     module.checkValidity()
 *     // 模块有效，继续处理
 *     val roots = ModuleRootManager.getInstance(module).contentRoots
 *     // ...
 * }
 * ```
 *
 * ## 为什么模块会被释放？
 *
 * - 模块被从项目中删除
 * - 项目被关闭
 * - IDE 重新加载项目
 * - 模块配置发生重大变更
 *
 * @receiver IntelliJ 模块
 * @throws AlreadyDisposedException 如果模块已被释放
 */
fun Module.checkValidity() {
    if (isDisposed) {
        throw AlreadyDisposedException("Module '${name}' is already disposed")
    }
}

/**
 * 检查库的有效性
 *
 * 验证库对象是否仍然有效（未被释放）。
 * 如果库是 [LibraryEx] 类型且已被释放，抛出 [AlreadyDisposedException]。
 *
 * ## 使用场景
 *
 * - 在对库执行操作前验证其有效性
 * - 在缓存中检查库的状态
 * - 在异步操作中确保库仍然存在
 *
 * ## 示例
 *
 * ```kotlin
 * fun analyzeLibrary(library: Library) {
 *     library.checkValidity()
 *     // 库有效，继续分析
 *     val classes = library.getFiles(OrderRootType.CLASSES)
 *     // ...
 * }
 * ```
 *
 * ## 为什么库会被释放？
 *
 * - 库从项目中删除
 * - 项目关闭
 * - 库配置发生变更
 * - 依赖管理工具更新了库
 *
 * @receiver 要检查的库对象
 * @throws AlreadyDisposedException 如果库已被释放
 */
fun Library.checkValidity() {
    if (this is LibraryEx && isDisposed) {
        throw AlreadyDisposedException("Library '${name}' is already disposed")
    }
}

/**
 * 获取库的所有根目录 URL（按类型分组）
 *
 * 返回库的所有类型根目录的 URL 映射，用于库的去重和比较。
 *
 * ## 使用场景
 *
 * - 比较两个库是否具有相同的内容
 * - 库的去重逻辑
 * - 生成库的指纹信息
 *
 * ## 示例
 *
 * ```kotlin
 * fun areSameLibraries(lib1: LibraryEx, lib2: LibraryEx): Boolean {
 *     val urls1 = lib1.urlsByType()
 *     val urls2 = lib2.urlsByType()
 *     return urls1.rootEquals(lib2)
 * }
 * ```
 *
 * @receiver 库对象（LibraryEx）
 * @return 映射表，键为根目录类型，值为该类型的所有 URL 数组
 * @see OrderRootType
 */
internal fun LibraryEx.urlsByType(): Map<OrderRootType, Array<String>> = buildMap {
    for (orderRootType in OrderRootType.getAllTypes()) {
        put(orderRootType, getUrls(orderRootType))
    }
}

/**
 * 检查根目录是否与另一个库相等
 *
 * 通过比较所有类型的根目录 URL 来判断两个库是否具有相同的内容。
 * 这用于库的去重逻辑，确保相同内容的库不会被重复缓存。
 *
 * ## 工作原理
 *
 * 对于每个根目录类型（CLASSES, SOURCES, DOCUMENTATION 等），
 * 比较两个库的 URL 数组是否完全相同（包括顺序）。
 *
 * ## 使用场景
 *
 * - 在库缓存中去重相同内容的库
 * - 判断两个库实例是否指向同一组文件
 * - 库版本升级检测
 *
 * ## 示例
 *
 * ```kotlin
 * fun deduplicateLibrary(newLibrary: LibraryEx, cache: Map<String, LibraryEx>): LibraryEx {
 *     val urls = newLibrary.urlsByType()
 *     for (cachedLibrary in cache.values) {
 *         if (urls.rootEquals(cachedLibrary)) {
 *             // 找到内容相同的库，返回缓存的版本
 *             return cachedLibrary
 *         }
 *     }
 *     // 没有找到重复的库，使用新库
 *     return newLibrary
 * }
 * ```
 *
 * @receiver 根目录 URL 映射表（由 [urlsByType] 生成）
 * @param another 要比较的另一个库对象
 * @return 如果所有类型的根目录都相等则返回 true
 * @see urlsByType
 */
internal fun Map<OrderRootType, Array<String>>.rootEquals(another: LibraryEx): Boolean = all { (k, v) ->
    v.contentEquals(another.getUrls(k))
}
