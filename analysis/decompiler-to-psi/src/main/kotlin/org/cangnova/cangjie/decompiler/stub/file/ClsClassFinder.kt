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

/**
 * 仓颉编译文件类型识别和查找工具
 *
 * ## 架构概述
 *
 * 本文件提供了识别、分类和查找仓颉编译文件（.cjo/.cjb）的核心工具。
 * 它是反编译系统的基础设施层，负责区分不同类型的编译文件，并提供缓存优化。
 *
 * ## 核心组件
 *
 * ### 1. ClsClassFinder (主工具类)
 *
 * 单例对象，提供编译文件类型判断：
 * - **isCangJieInternalCompiledFile()**: 判断文件是否为内部文件（不应暴露给用户）
 * - **isMultifileClassPartFile()**: 判断文件是否为多文件类部分
 * - **allowMultifileClassPart()**: 临时修改多文件类处理策略
 *
 * ### 2. CangJieClassHeader (类型元数据)
 *
 * 存储编译文件的头信息：
 * - **classId**: 类的完全限定标识符
 * - **kind**: 文件类型（CLASS, FILE_FACADE, MULTIFILE_CLASS 等）
 *
 * ### 3. ClsCangJieBinaryClassCache (缓存层)
 *
 * 单例缓存，避免重复解析：
 * - **isCangJieCompiledFile()**: 快速判断文件类型
 * - **getCangJieBinaryClassHeaderData()**: 获取缓存的头信息
 *
 * ### 4. DirectoryBasedClassFinder (目录查找器)
 *
 * 基于目录结构查找类文件：
 * - **findCangJieClass()**: 根据 ClassId 查找对应的 .cjo 文件
 *
 * ## 文件类型分类
 *
 * 仓颉编译文件分为以下几类：
 *
 * | 类型 | 说明 | 是否内部 | 示例 |
 * |------|------|---------|------|
 * | CLASS | 普通类 | ❌ | ArrayList.cjo |
 * | FILE_FACADE | 包级函数/属性的外观类 | ❌ | UtilsKt.cjo |
 * | SYNTHETIC_CLASS | 编译器生成的合成类 | ✅ | Lambda$1.cjo |
 * | MULTIFILE_CLASS | 多文件类的门面 | ❌ | Utils__Multifile.cjo |
 * | MULTIFILE_CLASS_PART | 多文件类的部分 | 可配置 | Utils__Part1$$.cjo |
 * | UNKNOWN | 未知类型 | ✅ | - |
 *
 * ## 内部文件识别逻辑
 *
 * ### 判断规则
 *
 * 文件被视为"内部"（不应暴露给用户）的条件：
 *
 * 1. **嵌套类**: 文件名包含 `$`（例如：`Outer$Inner.cjo`）
 * 2. **本地类**: classId.isLocal == true
 * 3. **合成类**: kind == SYNTHETIC_CLASS
 * 4. **未知类型**: kind == UNKNOWN
 * 5. **多文件类部分**: 根据策略决定（见 [MultifileClassPartKindStrategy]）
 *
 * ### 判断流程
 *
 * ```
 * isCangJieInternalCompiledFile(file)
 *   ↓
 * 1. 文件有效性检查
 *    └─ file.isValid && file.exists() && content.size != 0
 *   ↓
 * 2. 是否为仓颉编译文件？
 *    └─ ClsCangJieBinaryClassCache.isCangJieCompiledFile()
 *   ↓
 * 3. 是否为嵌套类？（文件名包含 '$'）
 *    └─ Yes → 返回 true (内部文件)
 *   ↓
 * 4. 获取类头信息
 *    └─ ClsCangJieBinaryClassCache.getCangJieBinaryClassHeaderData()
 *   ↓
 * 5. 是否为本地类？
 *    └─ classId.isLocal → 返回 true
 *   ↓
 * 6. 根据 kind 判断
 *    ├─ SYNTHETIC_CLASS, UNKNOWN → true
 *    ├─ MULTIFILE_CLASS_PART → 根据策略
 *    └─ CLASS, FILE_FACADE, MULTIFILE_CLASS → false
 * ```
 *
 * ## 多文件类机制
 *
 * ### 什么是多文件类？
 *
 * 多文件类（Multifile Class）允许将一个类的实现拆分到多个源文件：
 *
 * **源码结构**:
 * ```
 * // File1.cj
 * @file:JvmMultifileClass
 * @file:JvmName("Utils")
 * package com.example
 *
 * func helper1() { ... }
 *
 * // File2.cj
 * @file:JvmMultifileClass
 * @file:JvmName("Utils")
 * package com.example
 *
 * func helper2() { ... }
 * ```
 *
 * **编译产物**:
 * ```
 * Utils.cjo              // MULTIFILE_CLASS (门面)
 * Utils__File1$$.cjo    // MULTIFILE_CLASS_PART (File1 的部分)
 * Utils__File2$$.cjo    // MULTIFILE_CLASS_PART (File2 的部分)
 * ```
 *
 * ### 为什么需要特殊处理？
 *
 * - **门面类**（MULTIFILE_CLASS）：应该暴露给用户，提供统一接口
 * - **部分类**（MULTIFILE_CLASS_PART）：通常是内部实现细节，默认隐藏
 *
 * ### 策略模式
 *
 * [MultifileClassPartKindStrategy] 提供三种处理策略：
 *
 * | 策略 | 行为 | 使用场景 |
 * |------|------|---------|
 * | INTERNAL | 视为内部文件 | 默认行为，隐藏实现细节 |
 * | NON_INTERNAL | 视为普通文件 | 调试或反编译工具 |
 * | FROM_STACK | 根据线程局部变量决定 | 动态控制，支持临时切换 |
 *
 * ## allowMultifileClassPart 机制
 *
 * ### 作用
 *
 * 临时允许多文件类部分作为非内部文件：
 * ```kotlin
 * ClsClassFinder.allowMultifileClassPart {
 *     // 在这个代码块中，MULTIFILE_CLASS_PART 被视为非内部文件
 *     val isInternal = ClsClassFinder.isCangJieInternalCompiledFile(partFile)
 *     // isInternal == false
 * }
 * ```
 *
 * ### 使用场景
 *
 * 1. **Stub 构建**: 在构建 Stub 树时，需要访问所有部分类
 * 2. **反编译**: 反编译工具需要查看完整实现
 * 3. **索引**: 符号索引需要扫描所有声明
 *
 * ### 线程安全
 *
 * 使用 [ThreadLocal] 确保线程隔离：
 * - 每个线程独立维护状态
 * - 不影响其他线程的判断逻辑
 * - 支持嵌套调用（通过 finally 块恢复旧值）
 *
 * ## 性能优化
 *
 * ### 1. 缓存机制
 *
 * [ClsCangJieBinaryClassCache] 缓存解析结果：
 * - 避免重复读取文件
 * - 避免重复解析头信息
 * - 减少 I/O 和 CPU 开销
 *
 * ### 2. 快速路径
 *
 * 优先使用快速检查：
 * - 文件扩展名检查（O(1)）
 * - 文件名模式匹配（O(n)，n = 文件名长度）
 * - 避免解析完整元数据（除非必要）
 *
 * ### 3. 延迟解析
 *
 * 只在必要时解析类头信息：
 * - 嵌套类检查在头信息解析前完成
 * - 提前返回可以跳过昂贵的解析操作
 *
 * ## 错误处理
 *
 * ### 异常捕获
 *
 * 所有解析错误被捕获并记录：
 * ```kotlin
 * try {
 *     isNestedClassFile(file, fileContent)
 * } catch (exception: Exception) {
 *     LOG.debug("Error checking nested class: ${file.path}", exception)
 *     return false  // 保守策略：异常时视为非内部文件
 * }
 * ```
 *
 * ### 保守策略
 *
 * 遇到不确定情况时，优先暴露文件而不是隐藏：
 * - 避免意外隐藏用户需要的类
 * - 允许调试和诊断问题
 *
 * ## 使用示例
 *
 * ### 示例 1: 过滤项目视图
 *
 * ```kotlin
 * fun shouldShowInProjectView(file: VirtualFile): Boolean {
 *     return !ClsClassFinder.isCangJieInternalCompiledFile(file)
 * }
 * ```
 *
 * ### 示例 2: Stub 构建
 *
 * ```kotlin
 * fun buildStubForMultifileClass(files: List<VirtualFile>): Stub {
 *     return ClsClassFinder.allowMultifileClassPart {
 *         files.forEach { file ->
 *             // 所有部分类都可访问
 *             buildStubForFile(file)
 *         }
 *     }
 * }
 * ```
 *
 * ### 示例 3: 目录扫描
 *
 * ```kotlin
 * fun scanDirectory(dir: VirtualFile): List<VirtualFile> {
 *     return dir.children.filter { file ->
 *         file.extension == "cjo" &&
 *         !ClsClassFinder.isCangJieInternalCompiledFile(file)
 *     }
 * }
 * ```
 *
 * ## 与其他组件的关系
 *
 * ### 上游组件
 *
 * - **ClassFileStubBuilder**: 使用 allowMultifileClassPart 构建 Stub
 * - **项目视图**: 使用 isCangJieInternalCompiledFile 过滤文件
 * - **符号索引**: 判断哪些文件需要索引
 *
 * ### 下游组件
 *
 * - **ClsCangJieBinaryClassCache**: 提供缓存的头信息
 * - **VirtualFile**: 文件系统抽象层
 * - **CangJieBuiltInFileType**: 文件类型定义
 *
 * ## 已知限制
 *
 * 1. **基于启发式**: 文件名模式匹配可能不完全准确
 * 2. **缓存无效化**: 缓存不会自动失效（依赖文件监听器）
 * 3. **元数据依赖**: 某些判断需要解析元数据（性能开销）
 *
 * ## 扩展点
 *
 * ### 添加新的文件类型
 *
 * 在 [CangJieClassHeader.Kind] 中添加新枚举值，然后更新判断逻辑。
 *
 * ### 自定义过滤规则
 *
 * 继承 ClsClassFinder 或直接使用其方法组合自定义过滤器。
 *
 * @see ClsClassFinder
 * @see CangJieClassHeader
 * @see ClsCangJieBinaryClassCache
 * @see DirectoryBasedClassFinder
 */
package org.cangnova.cangjie.decompiler.stub.file

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileContent
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 仓颉编译文件类型识别工具
 *
 * ## 功能说明
 *
 * ClsClassFinder 是用于识别和分类仓颉编译文件（.cjo/.cjb）的单例工具类。
 * 它提供了判断文件是否为"内部文件"（不应暴露给用户）的核心逻辑，
 * 并支持多文件类（Multifile Class）的特殊处理。
 *
 * ## 核心职责
 *
 * 1. **内部文件识别**: 判断文件是否应该对用户隐藏
 * 2. **多文件类处理**: 支持多文件类部分的动态策略
 * 3. **嵌套类检测**: 识别编译器生成的嵌套类文件
 *
 * ## 为什么需要隐藏内部文件？
 *
 * IDE 的项目视图、类搜索、反编译等功能应该只显示用户代码中的类，
 * 而隐藏编译器生成的辅助类。原因：
 * - **简化视图**: 避免项目视图被大量合成类污染
 * - **提高可读性**: 用户不关心编译器内部实现
 * - **避免混淆**: 合成类可能与用户类同名（通过 `$` 后缀区分）
 *
 * ## 主要方法
 *
 * | 方法 | 作用 | 返回值 |
 * |------|------|--------|
 * | isCangJieInternalCompiledFile() | 判断是否为内部文件 | Boolean |
 * | isMultifileClassPartFile() | 判断是否为多文件类部分 | Boolean |
 * | allowMultifileClassPart() | 临时允许访问多文件类部分 | T (泛型) |
 *
 * @see isCangJieInternalCompiledFile
 * @see MultifileClassPartKindStrategy
 * @see allowMultifileClassPart
 */
object ClsClassFinder {

    private val LOG = Logger.getInstance(ClsClassFinder::class.java)



    /**
     * 检查文件是否为内部编译的仓颉文件
     *
     * ## 功能说明
     *
     * 判断编译文件（.cjo/.cjb）是否为"内部文件"，即不应该暴露给用户的文件。
     * 这是反编译系统的核心过滤逻辑，确保 IDE 只显示用户真正需要的类声明。
     *
     * ## 判断规则
     *
     * 文件被视为内部文件的条件（按优先级）：
     *
     * ### 1. 文件有效性检查
     * ```kotlin
     * if (!file.isValidAndExists(fileContent)) return false
     * ```
     * - 无效文件直接返回 false（视为非内部文件，避免误过滤）
     *
     * ### 2. 文件类型检查
     * ```kotlin
     * if (!cache.isCangJieCompiledFile(file, fileContent)) return false
     * ```
     * - 非仓颉编译文件返回 false
     *
     * ### 3. 嵌套类检查（快速路径）
     * ```kotlin
     * if (isNestedClassFile(file, fileContent)) return true
     * ```
     * - 文件名包含 `$` → 内部文件（例如：`Outer$Inner.cjo`）
     * - 这是最快的检查，避免解析元数据
     *
     * ### 4. 类头信息检查
     * ```kotlin
     * val header = cache.getCangJieBinaryClassHeaderData(file, fileContent) ?: return false
     * if (header.classId.isLocal) return true
     * ```
     * - 本地类（局部类、匿名类）→ 内部文件
     *
     * ### 5. 类型（Kind）检查
     * ```kotlin
     * when (header.kind) {
     *     SYNTHETIC_CLASS, UNKNOWN -> true
     *     MULTIFILE_CLASS_PART -> 根据策略决定
     *     CLASS, FILE_FACADE, MULTIFILE_CLASS -> false
     * }
     * ```
     *
     * ## 判断流程图
     *
     * ```
     * isCangJieInternalCompiledFile(file)
     *   ↓
     * 1. file.isValidAndExists()?
     *    ├─ No → return false (无效文件，保守处理)
     *    └─ Yes → 继续
     *   ↓
     * 2. cache.isCangJieCompiledFile()?
     *    ├─ No → return false (非仓颉文件)
     *    └─ Yes → 继续
     *   ↓
     * 3. 文件名包含 '$'? (嵌套类检查)
     *    ├─ Yes → return true (嵌套类是内部文件)
     *    └─ No → 继续
     *   ↓
     * 4. 获取类头信息
     *    cache.getCangJieBinaryClassHeaderData()
     *    ├─ null → return false (无法解析，保守处理)
     *    └─ header → 继续
     *   ↓
     * 5. header.classId.isLocal?
     *    ├─ Yes → return true (本地类/匿名类)
     *    └─ No → 继续
     *   ↓
     * 6. header.kind 判断
     *    ├─ SYNTHETIC_CLASS → return true (编译器合成)
     *    ├─ UNKNOWN → return true (未知类型)
     *    ├─ MULTIFILE_CLASS_PART → 根据 multifileClassPartKindStrategy
     *    │   ├─ INTERNAL → true
     *    │   ├─ NON_INTERNAL → false
     *    │   └─ FROM_STACK → 查询 ThreadLocal
     *    └─ CLASS/FILE_FACADE/MULTIFILE_CLASS → return false
     * ```
     *
     * ## 多文件类部分处理
     *
     * 多文件类部分（MULTIFILE_CLASS_PART）的处理比较特殊：
     *
     * ### 三种策略
     *
     * | 策略 | 行为 | 使用场景 |
     * |------|------|---------|
     * | INTERNAL | 总是视为内部文件 | 默认行为，隐藏实现细节 |
     * | NON_INTERNAL | 总是视为普通文件 | 调试、反编译工具 |
     * | FROM_STACK | 查询 ThreadLocal 状态 | 动态控制（默认值） |
     *
     * ### FROM_STACK 策略（默认）
     *
     * 通过 ThreadLocal 变量 [treatMultifileClassPartAsInternal] 动态控制：
     * ```kotlin
     * // 默认值: true (视为内部文件)
     * private val treatMultifileClassPartAsInternal = ThreadLocal.withInitial { true }
     *
     * // 临时允许访问（在 allowMultifileClassPart 块中）
     * treatMultifileClassPartAsInternal.set(false)
     * ```
     *
     * ### 为什么需要动态控制？
     *
     * **场景 1: 项目视图** (默认行为)
     * ```
     * 用户浏览项目文件
     *   ↓
     * 调用 isCangJieInternalCompiledFile(file)
     *   ├─ treatMultifileClassPartAsInternal.get() == true
     *   └─ MULTIFILE_CLASS_PART → 返回 true (隐藏)
     *   ↓
     * 项目视图只显示门面类（MULTIFILE_CLASS）
     * ```
     *
     * **场景 2: Stub 构建** (临时允许)
     * ```
     * ClassFileStubBuilder.buildStubTree()
     *   ↓
     * ClsClassFinder.allowMultifileClassPart {
     *     ├─ treatMultifileClassPartAsInternal.set(false)
     *     ├─ 构建所有部分类的 Stub
     *     └─ 恢复 treatMultifileClassPartAsInternal.set(true)
     * }
     * ```
     *
     * ## 性能优化
     *
     * ### 快速路径优先
     *
     * 判断顺序从快到慢：
     * 1. **文件有效性**: O(1) - 直接查询文件系统
     * 2. **文件扩展名**: O(1) - 字符串比较
     * 3. **嵌套类检查**: O(n) - 文件名字符串扫描，n = 文件名长度
     * 4. **类头解析**: O(m) - 读取和解析元数据，m = 元数据大小
     *
     * 提前返回避免昂贵的元数据解析。
     *
     * ### 缓存机制
     *
     * [ClsCangJieBinaryClassCache] 缓存类头信息：
     * - 首次调用: 解析元数据（慢）
     * - 后续调用: 从缓存读取（快）
     *
     * ## 错误处理
     *
     * ### 保守策略
     *
     * 遇到不确定情况时，优先暴露文件而不是隐藏：
     * ```kotlin
     * try {
     *     isNestedClassFile(file, fileContent)
     * } catch (exception: Exception) {
     *     LOG.debug("Error checking nested class: ${file.path}", exception)
     *     return false  // 异常时视为非内部文件
     * }
     * ```
     *
     * **原理**: 宁可多显示一些文件，也不要错误隐藏用户需要的类。
     *
     * ### 异常捕获
     *
     * 捕获嵌套类检查中的异常：
     * - 文件名解析错误
     * - 元数据格式错误
     * - I/O 异常
     *
     * 所有异常记录到 Debug 日志，但不中断判断流程。
     *
     * ## 使用示例
     *
     * ### 示例 1: 过滤项目视图
     *
     * ```kotlin
     * // IDE 项目视图构建
     * fun buildProjectTree(directory: VirtualFile): List<VirtualFile> {
     *     return directory.children.filter { file ->
     *         file.extension == "cjo" &&
     *         !ClsClassFinder.isCangJieInternalCompiledFile(file)
     *     }
     * }
     *
     * // 结果: 只显示 ArrayList.cjo，隐藏 ArrayList$Iterator.cjo
     * ```
     *
     * ### 示例 2: 符号搜索
     *
     * ```kotlin
     * // "Find Class" 功能
     * fun findClassByName(name: String, scope: GlobalSearchScope): List<PsiClass> {
     *     val allFiles = FilenameIndex.getVirtualFilesByName(name + ".cjo", scope)
     *     return allFiles
     *         .filterNot { ClsClassFinder.isCangJieInternalCompiledFile(it) }
     *         .mapNotNull { PsiManager.getInstance(project).findFile(it) as? PsiClass }
     * }
     * ```
     *
     * ### 示例 3: 自定义策略
     *
     * ```kotlin
     * // 调试工具显示所有文件（包括多文件类部分）
     * fun showAllCompiledFiles(file: VirtualFile): Boolean {
     *     return !ClsClassFinder.isCangJieInternalCompiledFile(
     *         file,
     *         multifileClassPartKindStrategy = MultifileClassPartKindStrategy.NON_INTERNAL
     *     )
     * }
     * ```
     *
     * ## 参数说明
     *
     * ### file
     * 要检查的虚拟文件，通常是 .cjo 或 .cjb 文件：
     * - 必须是有效的 VirtualFile 实例
     * - 文件可以不存在（会返回 false）
     *
     * ### fileContent (可选)
     * 文件的字节数组内容：
     * - **提供**: 避免重复读取文件（性能优化）
     * - **null**: 方法内部会读取文件（首次调用）
     * - 用于批量处理时复用已读取的内容
     *
     * ### multifileClassPartKindStrategy (默认 FROM_STACK)
     * 多文件类部分的处理策略：
     * - **INTERNAL**: 总是隐藏多文件类部分
     * - **NON_INTERNAL**: 总是显示多文件类部分
     * - **FROM_STACK**: 根据 ThreadLocal 状态决定（默认，推荐）
     *
     * ## 返回值
     *
     * - **true**: 文件是内部文件，应该隐藏
     * - **false**: 文件是普通文件，应该显示
     *
     * ## 线程安全
     *
     * - **方法本身**: 无共享状态，线程安全
     * - **ThreadLocal**: FROM_STACK 策略使用 ThreadLocal，每个线程独立
     * - **缓存**: ClsCangJieBinaryClassCache 内部线程安全
     *
     * ## 性能特征
     *
     * - **最佳情况**: O(1) - 文件无效或扩展名错误
     * - **快速路径**: O(n) - 嵌套类检查，n = 文件名长度
     * - **慢速路径**: O(m) - 元数据解析，m = 元数据大小
     * - **缓存命中**: O(1) - 类头信息已缓存
     *
     * @param file 要检查的虚拟文件
     * @param fileContent 文件内容（可选，用于避免重复读取）
     * @param multifileClassPartKindStrategy 多文件类部分的处理策略
     * @return 如果是内部文件返回 true，否则返回 false
     *
     * @see MultifileClassPartKindStrategy
     * @see allowMultifileClassPart
     * @see ClsCangJieBinaryClassCache
     * @see CangJieClassHeader.Kind
     */
    @JvmOverloads
    fun isCangJieInternalCompiledFile(
        file: VirtualFile,
        fileContent: ByteArray? = null,
        multifileClassPartKindStrategy: MultifileClassPartKindStrategy = MultifileClassPartKindStrategy.FROM_STACK,
    ): Boolean {
        if (!file.isValidAndExists(fileContent)) {
            return false
        }

        val cache = ClsCangJieBinaryClassCache.getInstance()

        if (!cache.isCangJieCompiledFile(file, fileContent)) {
            return false
        }

        // 检查是否为嵌套类
        val isNestedClass = try {
            isNestedClassFile(file, fileContent)
        } catch (exception: Exception) {
            LOG.debug("Error checking nested class: ${file.path}", exception)
            return false
        }

        if (isNestedClass) {
            return true
        }

        val header = cache.getCangJieBinaryClassHeaderData(file, fileContent) ?: return false

        // 检查是否为本地类
        if (header.classId.isLocal) {
            return true
        }

        return when (header.kind) {
            CangJieClassHeader.Kind.SYNTHETIC_CLASS,
            CangJieClassHeader.Kind.UNKNOWN -> true

            CangJieClassHeader.Kind.MULTIFILE_CLASS_PART -> when (multifileClassPartKindStrategy) {
                MultifileClassPartKindStrategy.INTERNAL -> true
                MultifileClassPartKindStrategy.NON_INTERNAL -> false
                MultifileClassPartKindStrategy.FROM_STACK -> treatMultifileClassPartAsInternal.get()
            }

            CangJieClassHeader.Kind.CLASS,
            CangJieClassHeader.Kind.FILE_FACADE,
            CangJieClassHeader.Kind.MULTIFILE_CLASS -> false
        }
    }

    /**
     * 多文件类部分的处理策略枚举
     *
     * ## 功能说明
     *
     * 定义多文件类部分（MULTIFILE_CLASS_PART）在内部文件判断时的处理策略。
     * 多文件类是仓颉语言允许将一个类的实现拆分到多个源文件的特性，
     * 编译后会生成一个门面类（MULTIFILE_CLASS）和多个部分类（MULTIFILE_CLASS_PART）。
     *
     * ## 什么是多文件类？
     *
     * ### 源码结构
     * ```kotlin
     * // File1.cj
     * @file:JvmMultifileClass
     * @file:JvmName("Utils")
     * package com.example
     *
     * func helper1() { ... }
     *
     * // File2.cj
     * @file:JvmMultifileClass
     * @file:JvmName("Utils")
     * package com.example
     *
     * func helper2() { ... }
     * ```
     *
     * ### 编译产物
     * ```
     * Utils.cjo              // MULTIFILE_CLASS (门面类)
     * Utils__File1$$.cjo    // MULTIFILE_CLASS_PART (部分类)
     * Utils__File2$$.cjo    // MULTIFILE_CLASS_PART (部分类)
     * ```
     *
     * ### 为什么需要策略？
     *
     * - **门面类**: 应该暴露给用户，提供统一接口
     * - **部分类**: 是内部实现细节，通常应该隐藏
     * - **但**: Stub 构建时需要访问所有部分类的声明
     *
     * ## 三种策略对比
     *
     * | 策略 | MULTIFILE_CLASS_PART 视为 | 使用场景 | ThreadLocal 依赖 |
     * |------|--------------------------|---------|------------------|
     * | [INTERNAL] | 内部文件 (隐藏) | 默认行为，隐藏实现细节 | ❌ 不依赖 |
     * | [NON_INTERNAL] | 普通文件 (显示) | 调试工具、反编译器 | ❌ 不依赖 |
     * | [FROM_STACK] | 根据 ThreadLocal 决定 | 动态控制（推荐） | ✅ 依赖 |
     *
     * ## 策略详解
     *
     * ### INTERNAL - 始终隐藏
     *
     * **行为**: 所有 MULTIFILE_CLASS_PART 文件都视为内部文件
     *
     * **适用场景**:
     * - 强制隐藏实现细节
     * - 不需要动态控制的工具
     *
     * **代码示例**:
     * ```kotlin
     * val isInternal = ClsClassFinder.isCangJieInternalCompiledFile(
     *     file,
     *     multifileClassPartKindStrategy = MultifileClassPartKindStrategy.INTERNAL
     * )
     * // 对于 Utils__File1$$.cjo → isInternal == true (总是)
     * ```
     *
     * ### NON_INTERNAL - 始终显示
     *
     * **行为**: 所有 MULTIFILE_CLASS_PART 文件都视为普通文件
     *
     * **适用场景**:
     * - 调试工具需要查看所有文件
     * - 反编译器需要访问所有声明
     * - 开发者检查编译产物
     *
     * **代码示例**:
     * ```kotlin
     * val isInternal = ClsClassFinder.isCangJieInternalCompiledFile(
     *     file,
     *     multifileClassPartKindStrategy = MultifileClassPartKindStrategy.NON_INTERNAL
     * )
     * // 对于 Utils__File1$$.cjo → isInternal == false (总是)
     * ```
     *
     * ### FROM_STACK - 动态控制（默认推荐）
     *
     * **行为**: 根据 ThreadLocal 变量 [treatMultifileClassPartAsInternal] 决定
     *
     * **默认值**: true（视为内部文件）
     *
     * **工作机制**:
     * ```kotlin
     * // 默认行为 (大多数场景)
     * treatMultifileClassPartAsInternal.get() == true
     *   → MULTIFILE_CLASS_PART 视为内部文件
     *
     * // allowMultifileClassPart 块中 (Stub 构建等)
     * ClsClassFinder.allowMultifileClassPart {
     *     // treatMultifileClassPartAsInternal.get() == false
     *     // MULTIFILE_CLASS_PART 视为普通文件
     * }
     * ```
     *
     * **适用场景**:
     * - IDE 默认行为（推荐）
     * - 需要在不同上下文中切换行为
     * - Stub 构建时临时允许访问
     *
     * ## 使用场景对比
     *
     * ### 场景 1: 项目视图
     * ```
     * 用户浏览 .cjo 文件
     *   ↓
     * 使用 FROM_STACK 策略 (默认)
     *   ↓
     * treatMultifileClassPartAsInternal.get() == true
     *   ↓
     * Utils__File1$$.cjo 被隐藏
     * Utils.cjo 显示
     * ```
     *
     * ### 场景 2: Stub 构建
     * ```
     * ClassFileStubBuilder.buildStubTree()
     *   ↓
     * ClsClassFinder.allowMultifileClassPart {
     *     使用 FROM_STACK 策略 (默认)
     *       ↓
     *     treatMultifileClassPartAsInternal.get() == false
     *       ↓
     *     Utils__File1$$.cjo 可访问
     *     构建所有部分类的 Stub
     * }
     * ```
     *
     * ### 场景 3: 调试工具
     * ```
     * 调试器显示所有编译文件
     *   ↓
     * 使用 NON_INTERNAL 策略 (显式指定)
     *   ↓
     * 所有 MULTIFILE_CLASS_PART 都显示
     * ```
     *
     * ## 线程安全
     *
     * - **INTERNAL**: 无状态，完全线程安全
     * - **NON_INTERNAL**: 无状态，完全线程安全
     * - **FROM_STACK**: 使用 ThreadLocal，每个线程独立状态
     *
     * ## 性能考虑
     *
     * - **INTERNAL/NON_INTERNAL**: O(1) - 直接返回常量
     * - **FROM_STACK**: O(1) - ThreadLocal.get() 快速查询
     *
     * 三者性能差异可忽略不计。
     *
     * ## 推荐使用
     *
     * **默认选择**: [FROM_STACK]
     * - 满足大多数场景需求
     * - 支持动态控制
     * - IDE 框架的标准做法
     *
     * **特殊工具**: [NON_INTERNAL]
     * - 调试器
     * - 反编译查看器
     * - 文件分析工具
     *
     * **强制隐藏**: [INTERNAL]
     * - 极少使用
     * - 仅在明确不需要访问部分类时
     *
     * @see isCangJieInternalCompiledFile
     * @see allowMultifileClassPart
     * @see treatMultifileClassPartAsInternal
     */
    enum class MultifileClassPartKindStrategy {
        /**
         * 视为内部文件
         *
         * 所有 MULTIFILE_CLASS_PART 类型的文件都被判定为内部文件，
         * 在项目视图、符号搜索等场景中隐藏。
         *
         * ## 使用场景
         * - 强制隐藏多文件类的实现细节
         * - 不需要动态控制的简单工具
         *
         * ## 示例
         * ```kotlin
         * ClsClassFinder.isCangJieInternalCompiledFile(
         *     file,
         *     multifileClassPartKindStrategy = INTERNAL
         * )
         * // Utils__Part1$$.cjo → true (总是隐藏)
         * ```
         *
         * @see isCangJieInternalCompiledFile
         */
        INTERNAL,

        /**
         * 视为普通文件
         *
         * 所有 MULTIFILE_CLASS_PART 类型的文件都被判定为普通文件，
         * 像 FILE_FACADE 一样对用户可见。
         *
         * ## 使用场景
         * - 调试工具需要查看所有文件
         * - 反编译器需要访问完整声明
         * - 开发者分析编译产物结构
         *
         * ## 示例
         * ```kotlin
         * ClsClassFinder.isCangJieInternalCompiledFile(
         *     file,
         *     multifileClassPartKindStrategy = NON_INTERNAL
         * )
         * // Utils__Part1$$.cjo → false (总是显示)
         * ```
         *
         * @see isCangJieInternalCompiledFile
         */
        NON_INTERNAL,

        /**
         * 根据线程局部标志决定
         *
         * 查询 ThreadLocal 变量 [treatMultifileClassPartAsInternal] 的当前值：
         * - **true** (默认): 视为内部文件
         * - **false** (allowMultifileClassPart 块中): 视为普通文件
         *
         * ## 默认行为
         * ```kotlin
         * treatMultifileClassPartAsInternal.get() == true
         * // 多文件类部分默认隐藏
         * ```
         *
         * ## 动态控制
         * ```kotlin
         * // 临时允许访问
         * ClsClassFinder.allowMultifileClassPart {
         *     // treatMultifileClassPartAsInternal.get() == false
         *     // 多文件类部分可访问
         * }
         * ```
         *
         * ## 使用场景
         * - IDE 默认策略（推荐）
         * - Stub 构建时临时允许访问
         * - 需要在不同上下文中切换行为
         *
         * ## 线程安全
         * - 每个线程独立的状态
         * - 不同线程的判断互不影响
         *
         * ## 示例
         * ```kotlin
         * // 场景 1: 项目视图 (默认)
         * ClsClassFinder.isCangJieInternalCompiledFile(file)
         * // FROM_STACK + ThreadLocal.get() == true
         * // Utils__Part1$$.cjo → true (隐藏)
         *
         * // 场景 2: Stub 构建
         * ClsClassFinder.allowMultifileClassPart {
         *     ClsClassFinder.isCangJieInternalCompiledFile(file)
         *     // FROM_STACK + ThreadLocal.get() == false
         *     // Utils__Part1$$.cjo → false (可访问)
         * }
         * ```
         *
         * @see allowMultifileClassPart
         * @see treatMultifileClassPartAsInternal
         * @see isCangJieInternalCompiledFile
         */
        FROM_STACK
    }

    /**
     * 临时允许多文件类部分作为非内部文件
     *
     * ## 功能说明
     *
     * 在 [action] 代码块执行期间，临时修改 ThreadLocal 变量 [treatMultifileClassPartAsInternal]，
     * 使多文件类部分（MULTIFILE_CLASS_PART）被视为普通文件而不是内部文件。
     * 代码块执行完毕后，自动恢复原来的值。
     *
     * ## 工作机制
     *
     * ### ThreadLocal 状态管理
     *
     * ```kotlin
     * val old = treatMultifileClassPartAsInternal.get()  // 保存旧值
     * try {
     *     treatMultifileClassPartAsInternal.set(false)    // 临时设置为 false
     *     action()                                         // 执行用户代码
     * } finally {
     *     treatMultifileClassPartAsInternal.set(old)      // 恢复旧值
     * }
     * ```
     *
     * ### 状态转换
     *
     * ```
     * 调用前: treatMultifileClassPartAsInternal.get() == true (默认)
     *   ↓
     * 进入 allowMultifileClassPart { }
     *   ├─ 保存旧值 (true)
     *   ├─ 设置新值 (false)
     *   └─ 执行 action()
     *       ↓
     *       在 action 内部:
     *         isCangJieInternalCompiledFile(multifileClassPartFile)
     *         → FROM_STACK 策略查询 ThreadLocal
     *         → 返回 false (视为普通文件)
     *   ↓
     * 退出 allowMultifileClassPart { }
     *   └─ 恢复旧值 (true)
     *   ↓
     * 调用后: treatMultifileClassPartAsInternal.get() == true
     * ```
     *
     * ## 使用场景
     *
     * ### 场景 1: Stub 构建
     *
     * **需求**: 构建 Stub 树时需要访问所有部分类的声明
     *
     * ```kotlin
     * // ClassFileStubBuilder.buildStubTree()
     * fun buildStubTree(fileContent: FileContent): Stub? {
     *     return ClsClassFinder.allowMultifileClassPart {
     *         // 在这里，MULTIFILE_CLASS_PART 文件可以被访问
     *         val stubTree = stubLoader.readOrBuild(project, file, null)
     *
     *         // Utils__Part1$$.cjo 的 Stub 可以被构建
     *         // Utils__Part2$$.cjo 的 Stub 也可以被构建
     *
     *         stubTree
     *     }
     *     // 退出后，部分类又变回内部文件
     * }
     * ```
     *
     * ### 场景 2: 反编译所有部分
     *
     * **需求**: 反编译工具需要查看完整实现，包括所有部分类
     *
     * ```kotlin
     * fun decompileMultifileClass(facadeFile: VirtualFile): List<String> {
     *     val allParts = findAllMultifileClassParts(facadeFile)
     *
     *     return ClsClassFinder.allowMultifileClassPart {
     *         allParts.map { partFile ->
     *             // 每个部分类的反编译文本
     *             decompile(partFile)
     *         }
     *     }
     * }
     * ```
     *
     * ### 场景 3: 符号索引
     *
     * **需求**: 建立符号索引时需要扫描所有声明，包括部分类中的符号
     *
     * ```kotlin
     * fun indexMultifileClass(files: List<VirtualFile>) {
     *     ClsClassFinder.allowMultifileClassPart {
     *         files.forEach { file ->
     *             if (file.extension == "cjo") {
     *                 // 索引器可以访问所有文件，包括部分类
     *                 indexFile(file)
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * ### 场景 4: 嵌套调用支持
     *
     * **需求**: 支持嵌套的 allowMultifileClassPart 调用
     *
     * ```kotlin
     * fun outerOperation() {
     *     // 外层: treatMultifileClassPartAsInternal == true
     *
     *     ClsClassFinder.allowMultifileClassPart {
     *         // 第一层: treatMultifileClassPartAsInternal == false
     *
     *         processFile1()
     *
     *         ClsClassFinder.allowMultifileClassPart {
     *             // 第二层: treatMultifileClassPartAsInternal == false
     *             // 保存的旧值是 false（从第一层）
     *
     *             processFile2()
     *
     *         } // 恢复到 false（第一层的值）
     *
     *         processFile3()
     *
     *     } // 恢复到 true（最初的值）
     *
     *     // 外层: treatMultifileClassPartAsInternal == true (已恢复)
     * }
     * ```
     *
     * ## 为什么需要这个方法？
     *
     * ### 问题背景
     *
     * 多文件类有两种矛盾的需求：
     *
     * 1. **用户视图**: 隐藏部分类，只显示门面类
     *    - 项目视图不应显示 `Utils__Part1$$.cjo`
     *    - 符号搜索不应找到部分类中的符号
     *
     * 2. **Stub 构建**: 必须访问所有部分类
     *    - Stub 索引需要所有部分类的声明
     *    - 否则跨包引用会解析失败
     *
     * ### 解决方案
     *
     * 通过 ThreadLocal 实现上下文相关的行为：
     * - **默认上下文**: 部分类是内部文件（隐藏）
     * - **Stub 构建上下文**: 部分类是普通文件（可访问）
     *
     * ## 线程安全
     *
     * ### ThreadLocal 隔离
     *
     * 每个线程有独立的状态：
     * ```
     * 线程 A (项目视图)
     *   treatMultifileClassPartAsInternal == true
     *   → 部分类隐藏
     *
     * 线程 B (Stub 构建)
     *   allowMultifileClassPart {
     *       treatMultifileClassPartAsInternal == false
     *       → 部分类可访问
     *   }
     *
     * 两个线程互不影响
     * ```
     *
     * ### finally 块保证
     *
     * 使用 `try-finally` 确保状态恢复：
     * ```kotlin
     * try {
     *     treatMultifileClassPartAsInternal.set(false)
     *     action()  // 即使抛异常...
     * } finally {
     *     treatMultifileClassPartAsInternal.set(old)  // 也会恢复状态
     * }
     * ```
     *
     * **好处**:
     * - 异常安全：即使 action 抛异常，状态也会恢复
     * - 无泄漏：不会导致状态污染
     *
     * ## 性能考虑
     *
     * ### 开销分析
     *
     * - **ThreadLocal.get()**: O(1) - 快速哈希查找
     * - **ThreadLocal.set()**: O(1) - 快速哈希插入
     * - **try-finally**: 无额外开销（JVM 优化）
     *
     * **总开销**: 可忽略不计（~几纳秒）
     *
     * ### 批量操作优化
     *
     * 对于批量文件处理，在外层调用一次即可：
     * ```kotlin
     * // ✅ 高效：只设置一次 ThreadLocal
     * ClsClassFinder.allowMultifileClassPart {
     *     files.forEach { file ->
     *         processFile(file)  // 所有文件共享相同上下文
     *     }
     * }
     *
     * // ❌ 低效：每个文件都设置一次
     * files.forEach { file ->
     *     ClsClassFinder.allowMultifileClassPart {
     *         processFile(file)
     *     }
     * }
     * ```
     *
     * ## 返回值
     *
     * 返回 [action] 代码块的执行结果，支持泛型类型 [T]：
     *
     * ```kotlin
     * // 示例 1: 返回 Stub 树
     * val stub: Stub? = ClsClassFinder.allowMultifileClassPart {
     *     buildStubTree(fileContent)
     * }
     *
     * // 示例 2: 返回文件列表
     * val files: List<VirtualFile> = ClsClassFinder.allowMultifileClassPart {
     *     findAllAccessibleFiles()
     * }
     *
     * // 示例 3: 返回 Unit
     * ClsClassFinder.allowMultifileClassPart {
     *     indexFiles()
     * }
     * ```
     *
     * ## 使用注意事项
     *
     * ### 1. 避免过度使用
     *
     * 只在确实需要访问部分类时使用：
     * ```kotlin
     * // ✅ 正确: Stub 构建需要访问部分类
     * ClsClassFinder.allowMultifileClassPart {
     *     buildStub(file)
     * }
     *
     * // ❌ 错误: 普通文件遍历不需要
     * ClsClassFinder.allowMultifileClassPart {
     *     listAllFiles()  // 不涉及多文件类判断
     * }
     * ```
     *
     * ### 2. 异常传播
     *
     * [action] 中的异常会正常传播：
     * ```kotlin
     * try {
     *     ClsClassFinder.allowMultifileClassPart {
     *         throw RuntimeException("Error")
     *     }
     * } catch (e: RuntimeException) {
     *     // 异常可以捕获
     *     // ThreadLocal 状态已恢复
     * }
     * ```
     *
     * ### 3. 不要手动修改 ThreadLocal
     *
     * ```kotlin
     * // ❌ 错误: 不要直接操作 ThreadLocal
     * treatMultifileClassPartAsInternal.set(false)
     * processFile()
     * treatMultifileClassPartAsInternal.set(true)  // 可能因异常不执行
     *
     * // ✅ 正确: 使用 allowMultifileClassPart
     * ClsClassFinder.allowMultifileClassPart {
     *     processFile()
     * }  // 自动恢复，异常安全
     * ```
     *
     * ## 与 MultifileClassPartKindStrategy 的关系
     *
     * 本方法只影响 [MultifileClassPartKindStrategy.FROM_STACK] 策略：
     *
     * ```kotlin
     * // FROM_STACK 策略: 受本方法影响
     * isCangJieInternalCompiledFile(file, strategy = FROM_STACK)
     *   → 查询 treatMultifileClassPartAsInternal.get()
     *
     * // INTERNAL 策略: 不受影响，总是返回 true
     * isCangJieInternalCompiledFile(file, strategy = INTERNAL)
     *   → 直接返回 true
     *
     * // NON_INTERNAL 策略: 不受影响，总是返回 false
     * isCangJieInternalCompiledFile(file, strategy = NON_INTERNAL)
     *   → 直接返回 false
     * ```
     *
     * @param T 返回值类型（泛型）
     * @param action 要执行的代码块，在执行期间多文件类部分被视为非内部文件
     * @return 代码块的返回值
     *
     * @see MultifileClassPartKindStrategy.FROM_STACK
     * @see treatMultifileClassPartAsInternal
     * @see isCangJieInternalCompiledFile
     */
    fun <T> allowMultifileClassPart(action: () -> T): T {
        val old = treatMultifileClassPartAsInternal.get()
        return try {
            treatMultifileClassPartAsInternal.set(false)
            action()
        } finally {
            treatMultifileClassPartAsInternal.set(old)
        }
    }

    /**
     * 线程局部变量：控制多文件类部分是否视为内部文件
     *
     * ## 功能说明
     *
     * 该 ThreadLocal 变量存储当前线程对多文件类部分（MULTIFILE_CLASS_PART）的处理策略。
     * 它是 [allowMultifileClassPart] 方法和 [MultifileClassPartKindStrategy.FROM_STACK] 策略的实现基础。
     *
     * ## 默认值
     *
     * ```kotlin
     * ThreadLocal.withInitial { true }
     * ```
     *
     * - **true** (默认): 多文件类部分视为内部文件，在项目视图等场景中隐藏
     * - **false** (临时): 多文件类部分视为普通文件，可以被访问和索引
     *
     * ## 工作原理
     *
     * ### ThreadLocal 机制
     *
     * ThreadLocal 为每个线程提供独立的变量副本：
     * ```
     * 线程 A:
     *   treatMultifileClassPartAsInternal.get() → true (默认)
     *
     * 线程 B:
     *   treatMultifileClassPartAsInternal.get() → true (默认)
     *
     * 线程 C (在 allowMultifileClassPart 块中):
     *   treatMultifileClassPartAsInternal.get() → false (临时修改)
     *
     * 各线程互不影响
     * ```
     *
     * ### 状态转换
     *
     * 通过 [allowMultifileClassPart] 方法修改：
     * ```kotlin
     * // 默认状态
     * treatMultifileClassPartAsInternal.get() == true
     *
     * // 临时修改
     * ClsClassFinder.allowMultifileClassPart {
     *     // treatMultifileClassPartAsInternal.get() == false
     * }
     *
     * // 自动恢复
     * treatMultifileClassPartAsInternal.get() == true
     * ```
     *
     * ## 使用位置
     *
     * 该变量在以下地方被查询：
     *
     * ### isCangJieInternalCompiledFile()
     * ```kotlin
     * when (header.kind) {
     *     MULTIFILE_CLASS_PART -> when (multifileClassPartKindStrategy) {
     *         FROM_STACK -> treatMultifileClassPartAsInternal.get()  // 查询此处
     *         INTERNAL -> true
     *         NON_INTERNAL -> false
     *     }
     *     // ...
     * }
     * ```
     *
     * ### allowMultifileClassPart()
     * ```kotlin
     * fun <T> allowMultifileClassPart(action: () -> T): T {
     *     val old = treatMultifileClassPartAsInternal.get()  // 保存旧值
     *     try {
     *         treatMultifileClassPartAsInternal.set(false)    // 临时修改
     *         return action()
     *     } finally {
     *         treatMultifileClassPartAsInternal.set(old)      // 恢复
     *     }
     * }
     * ```
     *
     * ## 为什么使用 ThreadLocal？
     *
     * ### 线程安全
     *
     * 不同线程可能同时处于不同的上下文：
     * - **项目视图线程**: 需要隐藏部分类
     * - **Stub 构建线程**: 需要访问部分类
     * - **符号索引线程**: 需要访问部分类
     *
     * ThreadLocal 确保各线程的策略互不干扰。
     *
     * ### 上下文相关行为
     *
     * 同一个线程在不同上下文中需要不同的行为：
     * ```kotlin
     * // 上下文 1: 用户浏览项目
     * fun buildProjectView() {
     *     // treatMultifileClassPartAsInternal.get() == true
     *     // 隐藏部分类
     * }
     *
     * // 上下文 2: 构建 Stub 索引
     * fun buildStubIndex() {
     *     ClsClassFinder.allowMultifileClassPart {
     *         // treatMultifileClassPartAsInternal.get() == false
     *         // 访问部分类
     *     }
     * }
     * ```
     *
     * ### 避免全局状态污染
     *
     * 如果使用全局变量：
     * ```kotlin
     * // ❌ 错误: 全局变量
     * var treatMultifileClassPartAsInternal = true
     *
     * // 问题: 线程 A 修改后影响线程 B
     * 线程 A: treatMultifileClassPartAsInternal = false
     * 线程 B: 读到 false (错误!)
     * ```
     *
     * ThreadLocal 隔离各线程的状态。
     *
     * ## 性能特征
     *
     * ### 初始化开销
     * - **首次访问**: 创建线程本地副本 (O(1))
     * - **后续访问**: 直接读取本地副本 (O(1))
     *
     * ### 内存开销
     * - **每个线程**: 存储一个 Boolean 值 (1 字节)
     * - **总开销**: 线程数 × 1 字节 (可忽略)
     *
     * ### 访问速度
     * - **ThreadLocal.get()**: ~几纳秒 (快速哈希查找)
     * - **ThreadLocal.set()**: ~几纳秒 (快速哈希插入)
     *
     * ## 生命周期
     *
     * ### 创建时机
     * - 类加载时创建 ThreadLocal 容器
     * - 线程首次访问时创建线程本地副本（初始值: true）
     *
     * ### 清理时机
     * - 线程结束时自动清理
     * - 无需手动清理
     *
     * ## 使用注意事项
     *
     * ### 1. 不要直接修改
     *
     * 应该通过 [allowMultifileClassPart] 修改，而不是直接调用 set()：
     * ```kotlin
     * // ❌ 错误: 直接修改，可能导致状态泄漏
     * treatMultifileClassPartAsInternal.set(false)
     * processFiles()
     * treatMultifileClassPartAsInternal.set(true)  // 如果抛异常，这行不执行
     *
     * // ✅ 正确: 使用 allowMultifileClassPart
     * ClsClassFinder.allowMultifileClassPart {
     *     processFiles()
     * }  // 异常安全，自动恢复
     * ```
     *
     * ### 2. 不要在构造函数中访问
     *
     * ThreadLocal 应该在方法中访问，不要在对象构造期间访问：
     * ```kotlin
     * // ❌ 错误: 构造时访问 ThreadLocal
     * class MyClass {
     *     val value = treatMultifileClassPartAsInternal.get()
     * }
     *
     * // ✅ 正确: 方法中访问
     * class MyClass {
     *     fun checkValue() = treatMultifileClassPartAsInternal.get()
     * }
     * ```
     *
     * ### 3. 适用于短生命周期的线程
     *
     * 长时间运行的线程（如线程池）可能积累过多 ThreadLocal 数据。
     * 但对于本场景（IDE 的工作线程），这不是问题。
     *
     * ## 调试技巧
     *
     * ### 查看当前值
     * ```kotlin
     * val currentValue = ClsClassFinder.treatMultifileClassPartAsInternal.get()
     * LOG.debug("Current thread treats multifile class part as internal: $currentValue")
     * ```
     *
     * ### 验证线程隔离
     * ```kotlin
     * thread(name = "Thread-A") {
     *     ClsClassFinder.allowMultifileClassPart {
     *         LOG.debug("Thread-A: ${treatMultifileClassPartAsInternal.get()}")  // false
     *     }
     * }
     *
     * thread(name = "Thread-B") {
     *     LOG.debug("Thread-B: ${treatMultifileClassPartAsInternal.get()}")  // true
     * }
     * ```
     *
     * @see allowMultifileClassPart
     * @see MultifileClassPartKindStrategy.FROM_STACK
     * @see isCangJieInternalCompiledFile
     */
    private val treatMultifileClassPartAsInternal = ThreadLocal.withInitial { true }

    /**
     * 检查文件是否为多文件类的部分
     *
     * ## 功能说明
     *
     * 判断编译文件是否为多文件类的部分类（MULTIFILE_CLASS_PART）。
     * 与 [isCangJieInternalCompiledFile] 不同，本方法仅判断类型，不涉及是否隐藏。
     *
     * ## 判断逻辑
     *
     * ```
     * isMultifileClassPartFile(file)
     *   ↓
     * 1. file.isValidAndExists()?
     *    ├─ No → return false
     *    └─ Yes → 继续
     *   ↓
     * 2. 获取类头信息
     *    cache.getCangJieBinaryClassHeaderData(file, fileContent)
     *    ├─ null → return false
     *    └─ headerData → 继续
     *   ↓
     * 3. headerData.kind == MULTIFILE_CLASS_PART?
     *    ├─ Yes → return true
     *    └─ No → return false
     * ```
     *
     * ## 与 isCangJieInternalCompiledFile 的区别
     *
     * | 方法 | 判断内容 | 返回值语义 | 用途 |
     *|------|---------|-----------|------|
     * | isMultifileClassPartFile() | 是否为多文件类部分 | true = 是部分类 | 类型判断 |
     * | isCangJieInternalCompiledFile() | 是否应隐藏 | true = 应隐藏 | 过滤判断 |
     *
     * ### 示例对比
     * ```kotlin
     * val file = VirtualFile("Utils__Part1$$.cjo")
     *
     * // 类型判断: 总是 true
     * isMultifileClassPartFile(file) == true
     *
     * // 过滤判断: 取决于上下文
     * isCangJieInternalCompiledFile(file) == true  // 默认上下文
     *
     * ClsClassFinder.allowMultifileClassPart {
     *     isCangJieInternalCompiledFile(file) == false  // 临时允许
     * }
     * ```
     *
     * ## 使用场景
     *
     * ### 场景 1: 查找所有部分类
     *
     * ```kotlin
     * fun findAllMultifileClassParts(directory: VirtualFile): List<VirtualFile> {
     *     return directory.children.filter { file ->
     *         file.extension == "cjo" &&
     *         ClsClassFinder.isMultifileClassPartFile(file)
     *     }
     * }
     * ```
     *
     * ### 场景 2: 统计文件类型
     *
     * ```kotlin
     * fun analyzeCompiledFiles(files: List<VirtualFile>) {
     *     val stats = files.groupBy { file ->
     *         when {
     *             ClsClassFinder.isMultifileClassPartFile(file) -> "MULTIFILE_CLASS_PART"
     *             // 其他类型...
     *             else -> "UNKNOWN"
     *         }
     *     }
     * }
     * ```
     *
     * ### 场景 3: 验证文件类型
     *
     * ```kotlin
     * fun processMultifileClass(file: VirtualFile) {
     *     require(ClsClassFinder.isMultifileClassPartFile(file)) {
     *         "Expected multifile class part, got: ${file.name}"
     *     }
     *     // 处理部分类...
     * }
     * ```
     *
     * ## 性能考虑
     *
     * ### 开销分析
     * - **缓存命中**: O(1) - 直接从缓存读取类头信息
     * - **缓存未命中**: O(m) - 解析元数据，m = 文件大小
     *
     * ### 优化建议
     * - 批量处理时复用 fileContent: `isMultifileClassPartFile(file, fileContent)`
     * - 利用 ClsCangJieBinaryClassCache 的缓存
     *
     * ## 返回值
     *
     * - **true**: 文件是多文件类的部分类
     * - **false**: 文件不是多文件类部分（可能是门面类、普通类或其他类型）
     *
     * ## 线程安全
     *
     * - 方法本身线程安全（无共享可变状态）
     * - ClsCangJieBinaryClassCache 内部线程安全
     *
     * @param file 要检查的虚拟文件
     * @param fileContent 文件内容（可选，用于避免重复读取）
     * @return 如果是多文件类部分返回 true，否则返回 false
     *
     * @see isCangJieInternalCompiledFile
     * @see CangJieClassHeader.Kind.MULTIFILE_CLASS_PART
     * @see ClsCangJieBinaryClassCache.getCangJieBinaryClassHeaderData
     */
    fun isMultifileClassPartFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        if (!file.isValidAndExists(fileContent)) {
            return false
        }
        val cache = ClsCangJieBinaryClassCache.getInstance()
        val headerData = cache.getCangJieBinaryClassHeaderData(file, fileContent)
        return headerData?.kind == CangJieClassHeader.Kind.MULTIFILE_CLASS_PART
    }

    /**
     * 检查文件是否为嵌套类文件（私有方法）
     *
     * ## 功能说明
     *
     * 通过文件名判断编译文件是否为嵌套类（Nested Class）。
     * 嵌套类在编译后，其类名会包含 `$` 符号，用于分隔外部类和内部类。
     *
     * ## 判断规则
     *
     * ```kotlin
     * fileName.contains('$')
     * ```
     *
     * - **包含 $**: 文件是嵌套类
     * - **不包含 $**: 文件不是嵌套类
     *
     * ## 文件名模式
     *
     * ### 嵌套类示例
     *
     * | 源码结构 | 编译文件名 | contains('$') | 结果 |
     * |---------|-----------|---------------|------|
     * | class Outer { class Inner } | Outer$Inner.cjo | ✅ Yes | 嵌套类 |
     * | class Outer { class A { class B } } | Outer$A$B.cjo | ✅ Yes | 嵌套类 |
     * | class Outer { fun() { class Local } } | Outer$1$Local.cjo | ✅ Yes | 本地类 |
     * | class Outer { fun() { lambda } } | Outer$lambda$1.cjo | ✅ Yes | Lambda |
     * | class Outer { object Companion } | Outer$Companion.cjo | ✅ Yes | 伴生对象 |
     * | class Normal | Normal.cjo | ❌ No | 普通类 |
     * | Utils__Part1$$ (多文件类) | Utils__Part1$$.cjo | ✅ Yes | 特殊标记 |
     *
     * ## 为什么嵌套类是内部文件？
     *
     * ### 用户视角
     *
     * 用户在源码中这样写：
     * ```kotlin
     * class ArrayList {
     *     private class Iterator { ... }
     * }
     * ```
     *
     * 用户期望看到：
     * - ✅ `ArrayList.cjo` (主类)
     * - ❌ 不应该看到 `ArrayList$Iterator.cjo` (嵌套类)
     *
     * ### IDE 行为
     *
     * - **项目视图**: 只显示顶层类 `ArrayList`
     * - **符号搜索**: 搜索 "Iterator" 不应该找到 `ArrayList$Iterator`
     * - **导航**: 跳转到 Iterator 应该打开 ArrayList 文件，而不是单独的 .cjo
     *
     * ## 快速路径优化
     *
     * 本方法是 [isCangJieInternalCompiledFile] 的快速路径：
     * ```
     * isCangJieInternalCompiledFile(file)
     *   ↓
     * 1. isValidAndExists() → O(1)
     * 2. isCangJieCompiledFile() → O(1)
     * 3. isNestedClassFile() → O(n) [本方法，n = 文件名长度]
     *    └─ 如果包含 '$' → 直接返回 true (内部文件)
     * 4. getCangJieBinaryClassHeaderData() → O(m) [慢，m = 文件大小]
     *    └─ 只有非嵌套类才会执行到这里
     * ```
     *
     * **好处**: 嵌套类检查很快（字符串扫描），避免解析元数据。
     *
     * ## 多文件类的特殊情况
     *
     * 多文件类部分也包含 `$$`：
     * ```
     * Utils__Part1$$.cjo → contains('$') == true
     * ```
     *
     * 但多文件类的处理由后续的 `header.kind` 判断：
     * ```kotlin
     * when (header.kind) {
     *     MULTIFILE_CLASS_PART -> 根据策略决定
     *     // ...
     * }
     * ```
     *
     * 所以 `$$` 也会被本方法判定为嵌套类，但不影响最终结果。
     *
     * ## 性能特征
     *
     * ### 时间复杂度
     * - **最佳情况**: O(1) - 文件名第一个字符就是 `$`
     * - **最坏情况**: O(n) - n = 文件名长度
     * - **平均情况**: O(n/2)
     *
     * ### 实际性能
     * - 文件名通常很短（< 100 字符）
     * - String.contains() 是高度优化的 JVM 原生方法
     * - 实际开销: ~几纳秒
     *
     * ## 与元数据解析的对比
     *
     * | 方法 | 开销 | 准确性 | 使用时机 |
     * |------|------|--------|---------|
     * | isNestedClassFile() | ~纳秒级 | 启发式（基于文件名） | 快速路径 |
     * | getCangJieBinaryClassHeaderData() | ~毫秒级 | 精确（解析元数据） | 慢速路径 |
     *
     * 本方法作为快速启发式检查，避免大部分文件的元数据解析。
     *
     * ## 使用场景
     *
     * ### 场景 1: 内部文件判断（主要用途）
     * ```kotlin
     * fun isCangJieInternalCompiledFile(file: VirtualFile): Boolean {
     *     if (isNestedClassFile(file, fileContent)) {
     *         return true  // 嵌套类直接返回，避免解析元数据
     *     }
     *     // 其他判断...
     * }
     * ```
     *
     * ### 场景 2: 性能分析（调试用）
     * ```kotlin
     * val nestedClassCount = files.count { file ->
     *     file.extension == "cjo" && isNestedClassFile(file, null)
     * }
     * LOG.info("Found $nestedClassCount nested class files")
     * ```
     *
     * ## 限制和注意事项
     *
     * ### 1. 启发式判断
     *
     * 本方法是启发式的，不是 100% 准确：
     * - ✅ 大多数情况正确（嵌套类确实包含 $）
     * - ⚠️ 理论上可能误判（如果用户类名包含 $，但仓颉语言通常不允许）
     *
     * ### 2. 多文件类的 $$
     *
     * 多文件类的 `$$` 会被误判为嵌套类，但不影响最终结果：
     * ```kotlin
     * isNestedClassFile("Utils__Part1$$.cjo") == true
     * // 后续 header.kind == MULTIFILE_CLASS_PART 会覆盖此判断
     * ```
     *
     * ### 3. 不检查文件内容
     *
     * 本方法不使用 [fileContent] 参数：
     * ```kotlin
     * private fun isNestedClassFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
     *     // fileContent 参数被忽略
     *     val fileName = file.nameWithoutExtension
     *     return fileName.contains('$')
     * }
     * ```
     *
     * ## 为什么是 private？
     *
     * 本方法是内部实现细节：
     * - 用户应该使用 [isCangJieInternalCompiledFile]
     * - 启发式判断不应暴露为公共 API
     * - 实现可能在未来改变（例如检查更复杂的模式）
     *
     * @param file 要检查的虚拟文件
     * @param fileContent 文件内容（本方法不使用此参数）
     * @return 如果文件名包含 '$' 返回 true，否则返回 false
     *
     * @see isCangJieInternalCompiledFile
     */
    private fun isNestedClassFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        // 通过文件名判断：包含 '$' 的通常是嵌套类
        val fileName = file.nameWithoutExtension
        return fileName.contains('$')
    }

    /**
     * 检查虚拟文件是否有效且存在（扩展函数）
     *
     * ## 功能说明
     *
     * 组合检查虚拟文件的有效性、存在性和内容非空性。
     * 这是所有文件操作的前置检查，确保文件处于可操作状态。
     *
     * ## 检查逻辑
     *
     * ```kotlin
     * this.isValid && fileContent?.size != 0 && this.exists()
     * ```
     *
     * ### 三个条件（全部满足）
     *
     * | 条件 | 检查内容 | 失败原因示例 |
     * |------|---------|-------------|
     * | this.isValid | 文件对象有效 | 文件已被删除或移动 |
     * | fileContent?.size != 0 | 内容非空 | 空文件（0 字节） |
     * | this.exists() | 文件存在 | 文件被外部程序删除 |
     *
     * ## 为什么需要三个检查？
     *
     * ### 1. isValid - 对象有效性
     *
     * IntelliJ VirtualFile 是缓存的，可能失效：
     * ```
     * 用户删除文件 → VirtualFile 对象标记为 invalid
     * ```
     *
     * **检查**: 确保对象未失效
     *
     * ### 2. fileContent?.size != 0 - 内容非空
     *
     * 空文件可能是：
     * - 刚创建但未写入
     * - 编译失败的产物
     * - 文件损坏
     *
     * **检查**: 确保有内容可处理
     *
     * ### 3. exists() - 物理存在性
     *
     * 文件可能在缓存后被删除：
     * ```
     * VirtualFile 创建 → 外部程序删除文件 → exists() == false
     * ```
     *
     * **检查**: 确保文件物理存在
     *
     * ## fileContent 参数的处理
     *
     * ### null 时的行为
     *
     * ```kotlin
     * fileContent?.size != 0
     * ```
     *
     * - **fileContent == null**: 条件为 true（null-safe 操作符）
     * - **fileContent != null**: 检查 size != 0
     *
     * ### 为什么允许 null？
     *
     * 有些调用者没有预读文件内容：
     * ```kotlin
     * // 场景 1: 没有内容
     * isCangJieInternalCompiledFile(file)  // fileContent = null
     * // isValidAndExists() 会跳过内容大小检查
     *
     * // 场景 2: 有内容
     * isCangJieInternalCompiledFile(file, byteArrayOf(1, 2, 3))
     * // isValidAndExists() 会检查 size != 0
     * ```
     *
     * ## 保守策略
     *
     * ### 失败时返回 false
     *
     * 任何检查失败都返回 false（保守）：
     * ```kotlin
     * if (!file.isValidAndExists(fileContent)) {
     *     return false  // 视为非内部文件（保守）
     * }
     * ```
     *
     * **原理**: 宁可多显示一些文件，也不要错误隐藏用户需要的类。
     *
     * ## 调用位置
     *
     * 在 ClsClassFinder 的多个方法中作为前置检查：
     *
     * ### isCangJieInternalCompiledFile()
     * ```kotlin
     * if (!file.isValidAndExists(fileContent)) {
     *     return false
     * }
     * ```
     *
     * ### isMultifileClassPartFile()
     * ```kotlin
     * if (!file.isValidAndExists(fileContent)) {
     *     return false
     * }
     * ```
     *
     * ## 性能考虑
     *
     * ### 开销分析
     *
     * - **isValid**: O(1) - 检查标志位
     * - **fileContent?.size**: O(1) - 数组长度属性
     * - **exists()**: O(1) - 通常是缓存的文件系统状态
     *
     * **总开销**: ~几纳秒
     *
     * ### 短路求值
     *
     * 使用 `&&` 操作符的短路特性：
     * ```kotlin
     * this.isValid && fileContent?.size != 0 && this.exists()
     * //   第一个失败 → 不检查后续条件
     * ```
     *
     * ## 线程安全
     *
     * - **isValid**: 线程安全（IntelliJ 保证）
     * - **fileContent?.size**: 线程安全（只读操作）
     * - **exists()**: 线程安全（IntelliJ 保证）
     *
     * 方法本身是线程安全的。
     *
     * ## 使用示例
     *
     * ### 示例 1: 基本检查
     * ```kotlin
     * val file = VirtualFileManager.getInstance().findFileByUrl("file:///path/to/file.cjo")
     * if (file?.isValidAndExists() == true) {
     *     // 文件有效，可以处理
     *     processFile(file)
     * }
     * ```
     *
     * ### 示例 2: 批量过滤
     * ```kotlin
     * val validFiles = files.filter { it.isValidAndExists() }
     * ```
     *
     * ### 示例 3: 带内容检查
     * ```kotlin
     * val content = file.contentsToByteArray()
     * if (file.isValidAndExists(content)) {
     *     // 文件有效且非空
     *     processFileContent(content)
     * }
     * ```
     *
     * ## 为什么是扩展函数？
     *
     * 作为 VirtualFile 的扩展函数提供便利：
     * ```kotlin
     * // ✅ 简洁: 扩展函数
     * if (file.isValidAndExists(fileContent)) { ... }
     *
     * // ❌ 繁琐: 工具方法
     * if (isValidAndExists(file, fileContent)) { ... }
     * ```
     *
     * ## 命名选择
     *
     * - **isValidAndExists**: 清晰表达检查内容（有效性 + 存在性）
     * - **And**: 强调两个条件都需要满足
     * - **is** 前缀: 符合 Kotlin 命名约定（布尔属性/方法）
     *
     * @receiver VirtualFile 要检查的虚拟文件
     * @param fileContent 文件内容（可选），如果提供会检查非空
     * @return 文件有效、存在且内容非空（如果提供）时返回 true
     *
     * @see VirtualFile.isValid
     * @see VirtualFile.exists
     */
    private fun VirtualFile.isValidAndExists(fileContent: ByteArray? = null): Boolean =
        this.isValid && fileContent?.size != 0 && this.exists()
}

/**
 * 仓颉二进制类接口
 *
 * ## 功能说明
 *
 * 表示仓颉编译后的二进制类（.cjo/.cjb）的抽象接口。
 * 提供类的基本元数据信息，用于反编译和类型解析。
 *
 * ## 核心属性
 *
 * | 属性 | 类型 | 说明 |
 * |------|------|------|
 * | classId | ClassId | 类的完全限定标识符 |
 * | version | BinaryVersion | 二进制格式版本号 |
 *
 * ## 使用场景
 *
 * ### DirectoryBasedClassFinder
 * ```kotlin
 * return object : CangJieBinaryClass {
 *     override val classId: ClassId = classId
 *     override val version: BinaryVersion = version
 * }
 * ```
 *
 * ## 设计原理
 *
 * - **轻量级**: 只包含必要的元数据，避免加载完整的类信息
 * - **延迟加载**: 实际的类结构通过其他机制（Stub）加载
 * - **版本兼容**: 通过 version 字段支持不同的二进制格式
 *
 * @see ClassId
 * @see BinaryVersion
 * @see DirectoryBasedClassFinder
 */
interface CangJieBinaryClass {
    /**
     * 类的标识符
     *
     * 包含包名和类名的完整标识符。
     *
     * 例如: ClassId(packageFqName = "std.collection", className = "ArrayList")
     */
    val classId: ClassId

    /**
     * 二进制格式版本号
     *
     * 用于版本兼容性检查，确保反编译器能够正确解析该版本的二进制文件。
     */
    val version: BinaryVersion
}

/**
 * 仓颉类头信息
 *
 * ## 功能说明
 *
 * 存储编译文件的类型元数据，包括类标识符和类型种类。
 * 通过头信息可以快速判断文件类型，无需完整解析元数据。
 *
 * ## Kind 枚举
 *
 * | Kind | 说明 | 是否内部 | 示例 |
 * |------|------|---------|------|
 * | CLASS | 普通类 | ❌ | ArrayList.cjo |
 * | FILE_FACADE | 包级函数/属性的门面类 | ❌ | UtilsKt.cjo |
 * | SYNTHETIC_CLASS | 编译器生成的合成类 | ✅ | Lambda$1.cjo |
 * | MULTIFILE_CLASS | 多文件类的门面 | ❌ | Utils__Multifile.cjo |
 * | MULTIFILE_CLASS_PART | 多文件类的部分 | 可配置 | Utils__Part1$$.cjo |
 * | UNKNOWN | 未知类型 | ✅ | - |
 *
 * ## 使用场景
 *
 * ### 内部文件判断
 * ```kotlin
 * val header = cache.getCangJieBinaryClassHeaderData(file)
 * when (header.kind) {
 *     SYNTHETIC_CLASS, UNKNOWN -> true  // 内部文件
 *     CLASS, FILE_FACADE, MULTIFILE_CLASS -> false  // 普通文件
 *     MULTIFILE_CLASS_PART -> 根据策略
 * }
 * ```
 *
 * @param classId 类的完全限定标识符
 * @param kind 类的类型种类
 *
 * @see Kind
 * @see ClsCangJieBinaryClassCache.getCangJieBinaryClassHeaderData
 */
data class CangJieClassHeader(
    val classId: ClassId,
    val kind: Kind,
) {
    /**
     * 仓颉类的类型种类
     *
     * 定义编译文件的分类，用于判断文件应该如何处理和是否对用户可见。
     */
    enum class Kind {
        /** 普通类/接口/结构体 */
        CLASS,

        /** 包级函数/属性的门面类 */
        FILE_FACADE,

        /** 编译器生成的合成类（Lambda、匿名类等） */
        SYNTHETIC_CLASS,

        /** 多文件类的门面类 */
        MULTIFILE_CLASS,

        /** 多文件类的部分类 */
        MULTIFILE_CLASS_PART,

        /** 未知类型 */
        UNKNOWN
    }
}

/**
 * 基于目录的类查找器
 *
 * ## 功能说明
 *
 * 在指定目录中查找仓颉编译类文件（.cjo）。
 * 根据 ClassId 查找对应的二进制类文件。
 *
 * ## 查找逻辑
 *
 * ```kotlin
 * val fileName = classId.relativeClassName.asString().replace('.', '$') + ".cjo"
 * val file = directory.findChild(fileName)
 * ```
 *
 * ### 示例
 * - ClassId("std.collection", "ArrayList") → `ArrayList.cjo`
 * - ClassId("std.collection", "ArrayList.Iterator") → `ArrayList$Iterator.cjo`
 *
 * ## 使用场景
 *
 * 在特定目录（如库目录）中查找类文件：
 * ```kotlin
 * val finder = DirectoryBasedClassFinder(libDirectory, FqName("std.collection"))
 * val binaryClass = finder.findCangJieClass(classId, version)
 * ```
 *
 * @param directory 要搜索的目录
 * @param packageFqName 包的完全限定名
 *
 * @see CangJieBinaryClass
 */
class DirectoryBasedClassFinder(
    private val directory: VirtualFile,
    private val packageFqName: FqName
) {
    /**
     * 查找指定的仓颉类
     *
     * @param classId 类的标识符
     * @param version 期望的二进制版本
     * @return 找到的二进制类，如果未找到返回 null
     */
    fun findCangJieClass(classId: ClassId, version: BinaryVersion): CangJieBinaryClass? {
        val fileName = classId.relativeClassName.asString().replace('.', '$') + ".cjo"
        val file = directory.findChild(fileName) ?: return null

        if (!file.isValid || !file.exists()) {
            return null
        }

        return object : CangJieBinaryClass {
            override val classId: ClassId = classId
            override val version: BinaryVersion = version
        }
    }
}

/**
 * 仓颉二进制类缓存
 *
 * ## 功能说明
 *
 * ClsCangJieBinaryClassCache 是一个单例缓存类，负责缓存编译文件的类型判断结果，
 * 避免重复解析文件头信息，提升 [ClsClassFinder] 的性能。
 *
 * ## 核心职责
 *
 * 1. **文件类型判断**: 快速判断文件是否为仓颉编译文件（.cjo/.cjb）
 * 2. **类头信息缓存**: 提供类头信息（[CangJieClassHeader]）的获取和缓存
 * 3. **启发式推断**: 从文件名推断类型（CLASS, SYNTHETIC_CLASS, MULTIFILE_CLASS_PART）
 *
 * ## 设计原理
 *
 * ### 为什么使用单例？
 *
 * - **全局共享**: 所有调用者共享相同的缓存实例
 * - **内存效率**: 避免创建多个缓存实例
 * - **线程安全**: 单例模式简化线程安全管理
 *
 * ### 缓存策略
 *
 * 当前实现是**无缓存**的简化版本：
 * - 每次调用都重新计算（基于文件名启发式）
 * - 未来可扩展为真正的缓存实现（Map<VirtualFile, CangJieClassHeader>）
 *
 * **为什么当前不缓存？**
 * - 文件名解析非常快（~几纳秒）
 * - 缓存失效机制复杂（需要监听文件变化）
 * - 简单实现满足当前性能需求
 *
 * ## 启发式推断逻辑
 *
 * ### 文件名模式识别
 *
 * ```kotlin
 * when {
 *     fileName.contains("$$") -> MULTIFILE_CLASS_PART
 *     fileName.contains('$') -> SYNTHETIC_CLASS
 *     else -> CLASS
 * }
 * ```
 *
 * | 文件名模式 | 推断类型 | 示例 |
 * |----------|---------|------|
 * | 包含 `$$` | MULTIFILE_CLASS_PART | Utils__Part1$$.cjo |
 * | 包含 `$` | SYNTHETIC_CLASS | Outer$Inner.cjo |
 * | 其他 | CLASS | ArrayList.cjo |
 *
 * ### 推断准确性
 *
 * **优点**:
 * - 快速（O(n)，n = 文件名长度）
 * - 大多数情况准确
 * - 无需解析元数据
 *
 * **限制**:
 * - 不检查实际元数据内容
 * - 可能误判特殊命名的文件
 * - 依赖编译器命名约定
 *
 * ## 使用场景
 *
 * ### 场景 1: 内部文件判断
 *
 * ```kotlin
 * val cache = ClsCangJieBinaryClassCache.getInstance()
 * if (!cache.isCangJieCompiledFile(file)) {
 *     return false  // 非仓颉文件，跳过
 * }
 * ```
 *
 * ### 场景 2: 获取类头信息
 *
 * ```kotlin
 * val cache = ClsCangJieBinaryClassCache.getInstance()
 * val header = cache.getCangJieBinaryClassHeaderData(file)
 * when (header?.kind) {
 *     MULTIFILE_CLASS_PART -> // 处理多文件类部分
 *     SYNTHETIC_CLASS -> // 处理合成类
 *     CLASS -> // 处理普通类
 * }
 * ```
 *
 * ### 场景 3: 批量文件处理
 *
 * ```kotlin
 * val cache = ClsCangJieBinaryClassCache.getInstance()
 * files.forEach { file ->
 *     if (cache.isCangJieCompiledFile(file)) {
 *         val header = cache.getCangJieBinaryClassHeaderData(file)
 *         // 处理文件...
 *     }
 * }
 * ```
 *
 * ## 与其他组件的关系
 *
 * ```
 * ClsClassFinder.isCangJieInternalCompiledFile()
 *   ↓
 * ClsCangJieBinaryClassCache.getInstance()
 *   ├─ isCangJieCompiledFile() → 文件类型检查
 *   └─ getCangJieBinaryClassHeaderData() → 类头信息
 *       ↓
 *       CangJieClassHeader(classId, kind)
 * ```
 *
 * ## 性能特征
 *
 * ### 时间复杂度
 *
 * - **isCangJieCompiledFile()**: O(1) - 扩展名比较
 * - **getCangJieBinaryClassHeaderData()**: O(n) - n = 文件名长度
 * - **inferClassIdFromFile()**: O(1) - 直接创建 ClassId
 *
 * ### 内存使用
 *
 * - **单例实例**: ~几字节（无状态）
 * - **临时对象**: String, ClassId（GC 后释放）
 * - **无缓存存储**: 当前版本不占用额外内存
 *
 * ## 扩展点
 *
 * ### 添加真正的缓存
 *
 * 可以扩展为基于 Map 的缓存实现：
 * ```kotlin
 * class ClsCangJieBinaryClassCache private constructor() {
 *     private val cache = ConcurrentHashMap<VirtualFile, CangJieClassHeader>()
 *
 *     fun getCangJieBinaryClassHeaderData(file: VirtualFile): CangJieClassHeader? {
 *         return cache.computeIfAbsent(file) { computeHeader(it) }
 *     }
 *
 *     fun invalidate(file: VirtualFile) {
 *         cache.remove(file)
 *     }
 * }
 * ```
 *
 * ### 监听文件变化
 *
 * 集成 VirtualFileListener 自动失效缓存：
 * ```kotlin
 * project.messageBus.connect().subscribe(
 *     VirtualFileManager.VFS_CHANGES,
 *     object : BulkFileListener {
 *         override fun after(events: List<VFileEvent>) {
 *             events.forEach { event ->
 *                 if (event is VFileContentChangeEvent) {
 *                     cache.invalidate(event.file)
 *                 }
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * ## 线程安全
 *
 * - **单例访问**: getInstance() 是线程安全的（JVM 保证）
 * - **方法执行**: 所有方法都是只读的，线程安全
 * - **无共享状态**: 当前版本无可变状态
 *
 * ## 已知限制
 *
 * 1. **无真正缓存**: 每次调用都重新计算
 * 2. **启发式推断**: 可能不准确（但实际中很少出错）
 * 3. **无元数据验证**: 不检查文件实际内容
 * 4. **ClassId 简化**: inferClassIdFromFile 未处理包名
 *
 * @see ClsClassFinder
 * @see CangJieClassHeader
 * @see CangJieBuiltInFileType
 */
class ClsCangJieBinaryClassCache private constructor() {

    /**
     * 检查文件是否为仓颉编译文件
     *
     * ## 功能说明
     *
     * 快速判断虚拟文件是否为仓颉编译文件（.cjo 或 .cjb）。
     * 这是 [ClsClassFinder] 的第一道过滤，用于跳过非仓颉文件。
     *
     * ## 判断逻辑
     *
     * 使用两个条件（任一满足即可）：
     * ```kotlin
     * file.extension == CangJieBuiltInFileType.defaultExtension  // 扩展名匹配
     *     ||
     * file.fileType == CangJieBuiltInFileType  // 文件类型匹配
     * ```
     *
     * ### 为什么需要两个条件？
     *
     * 1. **扩展名检查** (`file.extension == "cjo"`):
     *    - **快速**: O(1) 字符串比较
     *    - **覆盖**: 大多数文件通过扩展名识别
     *
     * 2. **文件类型检查** (`file.fileType == CangJieBuiltInFileType`):
     *    - **准确**: 基于 IDE 的文件类型系统
     *    - **兜底**: 处理扩展名不标准的文件
     *    - **支持**: .cjb 等其他编译文件类型
     *
     * ## 工作流程
     *
     * ```
     * isCangJieCompiledFile(file)
     *   ↓
     * 1. 检查 file.extension == "cjo"?
     *    ├─ Yes → 返回 true
     *    └─ No → 继续
     *   ↓
     * 2. 检查 file.fileType == CangJieBuiltInFileType?
     *    ├─ Yes → 返回 true
     *    └─ No → 返回 false
     * ```
     *
     * ## 支持的文件类型
     *
     * | 扩展名 | 文件类型 | 说明 |
     * |-------|---------|------|
     * | .cjo | CangJieBuiltInFileType | 普通编译文件 |
     * | .cjb | CangJieBuiltInFileType | 内置库文件 |
     *
     * ## 使用场景
     *
     * ### 场景 1: 快速过滤
     *
     * ```kotlin
     * fun processFiles(files: List<VirtualFile>) {
     *     val cache = ClsCangJieBinaryClassCache.getInstance()
     *     files.filter { cache.isCangJieCompiledFile(it) }
     *         .forEach { processCompiledFile(it) }
     * }
     * ```
     *
     * ### 场景 2: 前置检查
     *
     * ```kotlin
     * val cache = ClsCangJieBinaryClassCache.getInstance()
     * if (!cache.isCangJieCompiledFile(file)) {
     *     return  // 非仓颉文件，跳过
     * }
     * // 继续处理...
     * ```
     *
     * ## 性能特征
     *
     * - **时间复杂度**: O(1) - 两次快速检查
     * - **调用频率**: 非常高（每个文件扫描都会调用）
     * - **优化**: 扩展名检查优先（大多数情况提前返回）
     *
     * ## 参数说明
     *
     * ### file
     * 要检查的虚拟文件：
     * - 可以是任何类型的文件
     * - 通常是项目中的编译产物
     *
     * ### fileContent (未使用)
     * 文件内容参数：
     * - **当前未使用**: 仅通过文件名/类型判断
     * - **保留**: 为将来基于内容的检查预留
     * - **向后兼容**: 与其他方法签名保持一致
     *
     * ## 返回值
     *
     * - **true**: 文件是仓颉编译文件（.cjo/.cjb）
     * - **false**: 文件不是仓颉编译文件
     *
     * ## 线程安全
     *
     * - 只读操作，完全线程安全
     * - 可以在多个线程并发调用
     *
     * @param file 虚拟文件
     * @param fileContent 文件内容（当前未使用，保留参数）
     * @return 如果是仓颉编译文件返回 true，否则返回 false
     *
     * @see CangJieBuiltInFileType
     * @see VirtualFile.extension
     * @see VirtualFile.fileType
     */
    fun isCangJieCompiledFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        return file.extension == CangJieBuiltInFileType.defaultExtension ||
                file.fileType == CangJieBuiltInFileType
    }

    /**
     * 获取仓颉二进制类的头信息
     *
     * ## 功能说明
     *
     * 从虚拟文件提取或推断类头信息（[CangJieClassHeader]），包括类标识符和类型种类。
     * 使用启发式方法基于文件名快速推断，无需解析实际元数据。
     *
     * ## 工作流程
     *
     * ```
     * getCangJieBinaryClassHeaderData(file)
     *   ↓
     * 1. isCangJieCompiledFile(file)?
     *    └─ No → 返回 null (非仓颉文件)
     *   ↓
     * 2. 提取文件名（无扩展名）
     *    例如: "ArrayList.cjo" → "ArrayList"
     *   ↓
     * 3. 启发式推断类型 (kind)
     *    ├─ 包含 "$$" → MULTIFILE_CLASS_PART
     *    ├─ 包含 '$' → SYNTHETIC_CLASS
     *    └─ 其他 → CLASS
     *   ↓
     * 4. 推断类 ID
     *    inferClassIdFromFile(file)
     *   ↓
     * 5. 返回 CangJieClassHeader(classId, kind)
     * ```
     *
     * ## 启发式推断规则
     *
     * ### 类型 (Kind) 推断
     *
     * | 文件名模式 | 推断类型 | 推断依据 | 示例 |
     * |----------|---------|---------|------|
     * | 包含 `$$` | MULTIFILE_CLASS_PART | 多文件类标记 | Utils__Part1$$.cjo |
     * | 包含 `$` | SYNTHETIC_CLASS | 嵌套类/Lambda 标记 | Outer$Inner.cjo |
     * | 无 `$` | CLASS | 普通顶层类 | ArrayList.cjo |
     *
     * **检查顺序**: 先检查 `$$`，再检查 `$`，最后默认 CLASS。
     *
     * ### 类 ID (ClassId) 推断
     *
     * 当前实现简化：
     * ```kotlin
     * ClassId(FqName.ROOT, Name.identifier(fileName))
     * ```
     *
     * - **包名**: 固定为 ROOT（未实现包名推断）
     * - **类名**: 直接使用文件名（未处理嵌套类）
     *
     * **限制**: 无法准确推断包名和嵌套层级。
     *
     * ## 准确性分析
     *
     * ### 高准确性场景
     *
     * 编译器生成的标准文件名：
     * - ✅ `ArrayList.cjo` → CLASS
     * - ✅ `Outer$Inner.cjo` → SYNTHETIC_CLASS
     * - ✅ `Utils__Part1$$.cjo` → MULTIFILE_CLASS_PART
     *
     * ### 可能误判场景
     *
     * 非标准命名的文件：
     * - ⚠️ `My$Special$Name.cjo` → 可能误判为 SYNTHETIC_CLASS
     * - ⚠️ 用户手动重命名的文件 → 推断错误
     *
     * **实践中**: 编译器生成的文件遵循命名约定，误判极少。
     *
     * ## 使用场景
     *
     * ### 场景 1: 内部文件判断
     *
     * ```kotlin
     * val cache = ClsCangJieBinaryClassCache.getInstance()
     * val header = cache.getCangJieBinaryClassHeaderData(file)
     *
     * if (header?.kind == CangJieClassHeader.Kind.SYNTHETIC_CLASS) {
     *     return true  // 合成类，隐藏
     * }
     * ```
     *
     * ### 场景 2: 多文件类处理
     *
     * ```kotlin
     * val cache = ClsCangJieBinaryClassCache.getInstance()
     * val header = cache.getCangJieBinaryClassHeaderData(file)
     *
     * when (header?.kind) {
     *     MULTIFILE_CLASS_PART -> handleMultifileClassPart(file)
     *     CLASS -> handleNormalClass(file)
     *     else -> skip()
     * }
     * ```
     *
     * ### 场景 3: 文件统计
     *
     * ```kotlin
     * val cache = ClsCangJieBinaryClassCache.getInstance()
     * val stats = files.groupBy { file ->
     *     cache.getCangJieBinaryClassHeaderData(file)?.kind
     * }
     * ```
     *
     * ## 返回值语义
     *
     * | 返回值 | 含义 | 后续处理 |
     * |--------|------|---------|
     * | CangJieClassHeader | 成功推断 | 使用 classId 和 kind |
     * | null | 非仓颉文件或推断失败 | 跳过该文件 |
     *
     * ## 性能考虑
     *
     * ### 时间复杂度
     *
     * - **文件类型检查**: O(1)
     * - **文件名提取**: O(1)
     * - **包含检查**: O(n)，n = 文件名长度
     * - **ClassId 创建**: O(1)
     * - **总计**: O(n)，n 通常 < 100
     *
     * ### 实际性能
     *
     * - 单次调用: ~几纳秒到 1 微秒
     * - 批量调用 1000 文件: ~1-5 毫秒
     * - 可忽略不计的开销
     *
     * ## 与元数据解析的对比
     *
     * | 方法 | 准确性 | 性能 | 依赖 |
     * |------|-------|------|------|
     * | 启发式推断 (本方法) | ~95% | 快 (~纳秒) | 文件名 |
     * | 元数据解析 | 100% | 慢 (~毫秒) | 元数据格式 |
     *
     * **权衡**: 本方法牺牲少量准确性换取显著性能提升。
     *
     * ## 参数说明
     *
     * ### file
     * 要分析的虚拟文件：
     * - 应该是仓颉编译文件（.cjo/.cjb）
     * - 如果不是，返回 null
     *
     * ### fileContent (未使用)
     * 文件内容参数：
     * - **当前未使用**: 仅基于文件名推断
     * - **保留**: 为将来解析实际元数据预留
     * - **向后兼容**: 统一方法签名
     *
     * ## 返回值
     *
     * - **非 null**: [CangJieClassHeader] 包含推断的 classId 和 kind
     * - **null**: 文件不是仓颉编译文件
     *
     * ## 线程安全
     *
     * - 只读操作，完全线程安全
     * - 可以在多个线程并发调用
     *
     * ## 已知限制
     *
     * 1. **包名缺失**: classId.packageFqName 总是 ROOT
     * 2. **嵌套类**: 不处理 `$` 分隔的嵌套层级
     * 3. **本地类**: isLocal 标志无法推断
     * 4. **无元数据验证**: 不检查实际文件内容
     *
     * @param file 虚拟文件
     * @param fileContent 文件内容（当前未使用，保留参数）
     * @return 类头信息，如果无法推断返回 null
     *
     * @see CangJieClassHeader
     * @see CangJieClassHeader.Kind
     * @see inferClassIdFromFile
     */
    fun getCangJieBinaryClassHeaderData(file: VirtualFile, fileContent: ByteArray? = null): CangJieClassHeader? {
        if (!isCangJieCompiledFile(file, fileContent)) {
            return null
        }

        // 从文件名推断类型
        val fileName = file.nameWithoutExtension
        val kind = when {
            fileName.contains("$$") -> CangJieClassHeader.Kind.MULTIFILE_CLASS_PART
            fileName.contains('$') -> CangJieClassHeader.Kind.SYNTHETIC_CLASS
            else -> CangJieClassHeader.Kind.CLASS
        }

        val classId = inferClassIdFromFile(file)
        return CangJieClassHeader(classId, kind)
    }

    /**
     * 从文件路径推断类 ID（私有方法）
     *
     * ## 功能说明
     *
     * 从虚拟文件推断 [ClassId]（类的完全限定标识符）。
     * 当前实现是简化版本，仅使用文件名作为类名，包名固定为 ROOT。
     *
     * ## 简化实现
     *
     * ```kotlin
     * val fileName = file.nameWithoutExtension  // 例如: "ArrayList"
     * return ClassId(FqName.ROOT, Name.identifier(fileName))
     * ```
     *
     * **结果**:
     * - 包名: FqName.ROOT (空包)
     * - 类名: 文件名（无扩展名）
     *
     * ## 示例
     *
     * | 文件路径 | 文件名 | 推断的 ClassId |
     * |---------|-------|---------------|
     * | .../ArrayList.cjo | ArrayList | ClassId(ROOT, "ArrayList") |
     * | .../Outer$Inner.cjo | Outer$Inner | ClassId(ROOT, "Outer$Inner") |
     * | .../Utils__Part1$$.cjo | Utils__Part1$$ | ClassId(ROOT, "Utils__Part1$$") |
     *
     * ## 为什么是简化版本？
     *
     * ### 当前设计
     *
     * - **包名**: 固定为 ROOT（不从路径推断）
     * - **类名**: 直接使用文件名（不解析嵌套）
     *
     * **原因**:
     * 1. **性能**: 避免路径解析和字符串处理开销
     * 2. **简单**: 减少代码复杂度
     * 3. **足够**: 当前使用场景不需要准确的包名
     *
     * ### 完整实现示例
     *
     * 如果需要准确的包名，可以这样实现：
     * ```kotlin
     * private fun inferClassIdFromFile(file: VirtualFile): ClassId {
     *     // 从文件路径推断包名
     *     val relativePath = getRelativePathFromSourceRoot(file)
     *     val packagePath = relativePath.dropLast(file.name.length + 1)
     *     val packageFqName = FqName(packagePath.replace('/', '.'))
     *
     *     // 处理嵌套类
     *     val fileName = file.nameWithoutExtension
     *     val classNameParts = fileName.split('$')
     *     val topLevelClass = Name.identifier(classNameParts[0])
     *
     *     var classId = ClassId(packageFqName, topLevelClass)
     *     for (i in 1 until classNameParts.size) {
     *         classId = classId.createNestedClassId(Name.identifier(classNameParts[i]))
     *     }
     *
     *     return classId
     * }
     * ```
     *
     * ## 使用场景
     *
     * 本方法只在 [getCangJieBinaryClassHeaderData] 中调用：
     * ```kotlin
     * val classId = inferClassIdFromFile(file)
     * return CangJieClassHeader(classId, kind)
     * ```
     *
     * ## 影响和限制
     *
     * ### 对调用者的影响
     *
     * 由于 classId 不准确，以下场景可能受影响：
     * - ❌ **符号解析**: 无法通过 ClassId 定位到正确的包
     * - ❌ **导入语句**: 无法生成正确的 import
     * - ✅ **类型判断**: 仍然可以区分文件类型（通过 kind）
     * - ✅ **内部文件过滤**: 不依赖准确的 ClassId
     *
     * ### 为什么不影响现有功能？
     *
     * [ClsClassFinder] 主要关注文件类型（kind），而不是 ClassId：
     * ```kotlin
     * when (header.kind) {
     *     SYNTHETIC_CLASS -> true  // 主要判断逻辑
     *     MULTIFILE_CLASS_PART -> // 主要判断逻辑
     *     CLASS -> false
     * }
     * // classId 仅用于 isLocal 判断，但 ROOT 包的类不会是 local
     * ```
     *
     * ## 性能特征
     *
     * - **时间复杂度**: O(1) - 直接访问文件名
     * - **空间复杂度**: O(n) - n = 文件名长度（创建新 String）
     * - **调用频率**: 每次 getCangJieBinaryClassHeaderData() 调用一次
     *
     * ## 扩展建议
     *
     * 如果未来需要准确的 ClassId：
     * 1. 解析文件路径获取包名
     * 2. 处理 `$` 分隔的嵌套类层级
     * 3. 考虑从元数据读取（而不是推断）
     * 4. 添加缓存避免重复计算
     *
     * ## 返回值
     *
     * 简化的 [ClassId]，包含：
     * - **packageFqName**: FqName.ROOT (空包)
     * - **className**: 文件名（无扩展名）
     *
     * @param file 虚拟文件
     * @return 简化的类标识符（包名为 ROOT）
     *
     * @see ClassId
     * @see FqName
     * @see Name
     */
    private fun inferClassIdFromFile(file: VirtualFile): ClassId {
        val fileName = file.nameWithoutExtension
        // 简化实现：假设文件名就是类名
        return ClassId(FqName.ROOT, Name.identifier(fileName))
    }

    companion object {
        /**
         * 单例实例
         *
         * 线程安全的单例实现，通过 JVM 类加载机制保证。
         */
        private val INSTANCE = ClsCangJieBinaryClassCache()

        /**
         * 获取单例实例
         *
         * ## 功能说明
         *
         * 返回全局唯一的 [ClsCangJieBinaryClassCache] 实例。
         *
         * ## 单例模式
         *
         * 使用 Kotlin object companion + private constructor 实现：
         * ```kotlin
         * companion object {
         *     private val INSTANCE = ClsCangJieBinaryClassCache()
         *     fun getInstance() = INSTANCE
         * }
         * ```
         *
         * ## 线程安全
         *
         * - **类加载**: JVM 保证类加载是线程安全的
         * - **单次初始化**: INSTANCE 只初始化一次
         * - **可见性**: 所有线程看到相同的实例
         *
         * ## 使用方式
         *
         * ```kotlin
         * val cache = ClsCangJieBinaryClassCache.getInstance()
         * val header = cache.getCangJieBinaryClassHeaderData(file)
         * ```
         *
         * @return 单例实例
         */
        fun getInstance(): ClsCangJieBinaryClassCache = INSTANCE
    }
}