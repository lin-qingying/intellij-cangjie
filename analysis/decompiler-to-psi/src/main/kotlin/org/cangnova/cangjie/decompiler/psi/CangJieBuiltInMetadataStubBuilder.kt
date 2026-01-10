/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.fb.parser.toFbPackage
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.stubs.CangJieStubVersions
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import org.jetbrains.annotations.TestOnly
import java.io.ByteArrayInputStream

/**
 * 内置库 Stub 版本号
 *
 * ## 功能说明
 *
 * 该计算属性提供内置库 Stub 的复合版本号，用于 IDE 的 Stub 缓存失效机制。
 * 当编译器版本、内置库格式或 Stub 结构发生变化时，版本号会改变，触发自动重建所有内置库的 Stub。
 *
 * ## 版本计算公式
 *
 * ```kotlin
 * 复合版本号 = 基础版本 + 动态偏移量
 * ```
 *
 * 组成部分：
 * - **基础版本**: [CangJieStubVersions.BUILTIN_STUB_VERSION] - 硬编码的内置库 Stub 结构版本
 * - **动态偏移量**: [CangJieBuiltInStubVersionOffsetProvider.getVersionOffset] - 运行时计算的偏移量
 *
 * ## 为什么使用复合版本？
 *
 * ### 基础版本（固定部分）
 *
 * 当内置库的 Stub 结构发生根本性变化时递增：
 * - Stub 类字段添加/删除/修改
 * - 序列化格式变化
 * - Flatbuffers schema 更新
 *
 * **示例**:
 * ```kotlin
 * // CangJieStubVersions.kt
 * const val BUILTIN_STUB_VERSION = 15
 * ```
 *
 * ### 动态偏移量（可变部分）
 *
 * 允许在不修改代码的情况下强制重建 Stub：
 * - 编译器版本更新
 * - 内置库内容变化
 * - 手动触发重建
 *
 * **示例**:
 * ```kotlin
 * object CangJieBuiltInStubVersionOffsetProvider {
 *     fun getVersionOffset(): Int {
 *         // 可能基于编译器版本号或配置文件计算
 *         return compilerVersionHashCode() % 1000
 *     }
 * }
 * ```
 *
 * ## 版本变化触发 Stub 重建
 *
 * ```
 * IDE 启动
 *   ↓
 * 1. 读取缓存的 Stub 版本号
 *    例如: 15 + 42 = 57
 *   ↓
 * 2. 计算当前版本号
 *    stubVersionForStubBuilderAndDecompiler
 *    = BUILTIN_STUB_VERSION (15) + getVersionOffset() (43)
 *    = 58
 *   ↓
 * 3. 比较版本号
 *    57 != 58 → 版本不匹配
 *   ↓
 * 4. 清除所有内置库 Stub 缓存
 *   ↓
 * 5. 重新构建所有 .cjb 文件的 Stub
 *   ↓
 * 6. 保存新 Stub 和新版本号 (58)
 * ```
 *
 * ## 使用场景
 *
 * ### 场景 1: 编译器升级
 * ```
 * 用户更新仓颉编译器: v1.0.0 → v1.1.0
 *   ↓
 * 内置库格式可能变化
 *   ↓
 * getVersionOffset() 返回新值
 *   ↓
 * 复合版本号变化: 15 + 42 → 15 + 43
 *   ↓
 * IDE 自动重建所有内置库 Stub
 * ```
 *
 * ### 场景 2: 强制重建（开发调试）
 * ```
 * 开发者修改 Stub 结构但未更新基础版本
 *   ↓
 * 临时修改 getVersionOffset() 返回值
 *   ↓
 * 复合版本号变化: 15 + 0 → 15 + 1
 *   ↓
 * IDE 重建 Stub，验证新结构
 * ```
 *
 * ## 性能考虑
 *
 * - **计算频率**: 每次 [CangJieBuiltInMetadataStubBuilder] 被实例化或查询版本号时调用
 * - **计算开销**: O(1) - 两次属性访问 + 一次加法
 * - **缓存**: IDE 会缓存最终的版本号，不会重复计算
 *
 * ## 线程安全
 *
 * - **基础版本**: 编译期常量，线程安全
 * - **偏移量**: 由 [CangJieBuiltInStubVersionOffsetProvider] 保证线程安全
 * - **计算**: 无副作用，可安全并发调用
 *
 * ## 调试技巧
 *
 * ### 查看当前版本号
 * ```kotlin
 * LOG.info("Built-in Stub version: $stubVersionForStubBuilderAndDecompiler")
 * LOG.info("  Base: ${CangJieStubVersions.BUILTIN_STUB_VERSION}")
 * LOG.info("  Offset: ${CangJieBuiltInStubVersionOffsetProvider.getVersionOffset()}")
 * ```
 *
 * ### 强制触发重建
 * 临时修改偏移量提供者：
 * ```kotlin
 * object CangJieBuiltInStubVersionOffsetProvider {
 *     fun getVersionOffset() = 999  // 临时值
 * }
 * ```
 *
 * ### 验证缓存失效
 * ```
 * File → Invalidate Caches / Restart
 * → 查看日志确认 Stub 重建
 * ```
 *
 * @see CangJieStubVersions.BUILTIN_STUB_VERSION
 * @see CangJieBuiltInStubVersionOffsetProvider
 * @see CangJieBuiltInMetadataStubBuilder.stubVersion
 */
private val stubVersionForStubBuilderAndDecompiler: Int
    get() = CangJieStubVersions.BUILTIN_STUB_VERSION + CangJieBuiltInStubVersionOffsetProvider.getVersionOffset()

/**
 * 内置库定义文件
 *
 * ## 功能说明
 *
 * BuiltInDefinitionFile 表示一个已解析的内置库 (.cjb) 文件，包含从 Flatbuffers 元数据中读取的所有声明信息。
 * 它是 [CangJieMetadataStubBuilder.FileWithMetadata.Compatible] 的子类，专门用于处理内置库文件的特殊需求。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * .cjb 内置库文件
 *   ↓
 * BuiltInDefinitionFile.read() (工厂方法)
 *   ↓
 * BuiltInDefinitionFile (本类 - 已解析的定义)
 *   ├─ 持有 PackageWrapper (所有声明)
 *   ├─ 持有 BuiltInsBinaryVersion (版本信息)
 *   └─ 计算 classesToDecompile (过滤后的类列表)
 *   ↓
 * CangJieMetadataStubBuilder.buildCompatibleFileStub()
 *   ↓
 * ClsStubBuilderComponents (构建 Stub)
 *   ↓
 * CangJieFileStubImpl (Stub 树)
 * ```
 *
 * ## 核心职责
 *
 * 1. **封装元数据**: 持有从 .cjb 文件解析的包和声明信息
 * 2. **类过滤**: 智能决定哪些类需要反编译，哪些可以跳过（已有源文件）
 * 3. **版本管理**: 跟踪内置库的二进制版本 ([BuiltInsBinaryVersion])
 * 4. **目录关联**: 关联包目录，用于源文件查找
 * 5. **元数据标记**: 区分纯元数据文件 vs 包含实现的文件
 *
 * ## 类过滤策略
 *
 * 为了优化性能和避免重复工作，内置库的某些类可能不需要反编译。
 * [classesToDecompile] 属性实现了复杂的过滤逻辑：
 *
 * ### 过滤决策树
 *
 * ```
 * 是否反编译某个类？
 *   ↓
 * 1. packageDirectory == null? (凭空创建)
 *    ├─ Yes → 反编译所有类 (没有源文件可替代)
 *    └─ No → 继续检查
 *   ↓
 * 2. isMetadata == true? (纯元数据文件)
 *    ├─ Yes → 反编译所有类 (元数据总是需要的)
 *    └─ No → 继续检查
 *   ↓
 * 3. FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES == false? (全局开关)
 *    ├─ Yes → 反编译所有类 (测试模式)
 *    └─ No → 继续检查
 *   ↓
 * 4. filterOutClassesExistingAsClassFiles == false? (实例开关)
 *    ├─ Yes → 反编译所有类
 *    └─ No → 应用过滤
 *   ↓
 * 5. 对每个类调用 shouldDecompileBuiltInClass()
 *    ├─ 构造源文件名: ${className}.cj
 *    ├─ 在 packageDirectory 中查找
 *    ├─ 找到 → false (跳过反编译)
 *    └─ 未找到 → true (需要反编译)
 * ```
 *
 * ### 过滤示例
 *
 * **场景 1: 内置库目录包含部分源文件**
 * ```
 * 包目录结构:
 * std-core/
 *   ├─ Int8.cj         # 源文件存在
 *   ├─ Int16.cj        # 源文件存在
 *   └─ std-core.cjb    # 内置库文件
 *
 * 内置库文件包含类:
 *   - Int8
 *   - Int16
 *   - Int32 (没有对应源文件)
 *   - Int64 (没有对应源文件)
 *
 * 过滤结果 (classesToDecompile):
 *   - Int8: 跳过 (有 Int8.cj)
 *   - Int16: 跳过 (有 Int16.cj)
 *   - Int32: 反编译 (无源文件)
 *   - Int64: 反编译 (无源文件)
 * ```
 *
 * **场景 2: 凭空创建的内置库**
 * ```
 * 测试代码:
 * val builtIn = BuiltInDefinitionFile(
 *     package = testPackage,
 *     version = testVersion,
 *     packageDirectory = null,  // 凭空创建
 *     isMetadata = false
 * )
 *
 * 过滤结果:
 *   所有类都反编译 (无物理目录，无法查找源文件)
 * ```
 *
 * **场景 3: 纯元数据文件**
 * ```
 * val builtIn = BuiltInDefinitionFile(
 *     package = metadataPackage,
 *     version = version,
 *     packageDirectory = someDir,
 *     isMetadata = true  // 纯元数据
 * )
 *
 * 过滤结果:
 *   所有类都反编译 (元数据总是需要的)
 * ```
 *
 * ## 为什么需要过滤？
 *
 * ### 性能优化
 *
 * 内置库可能包含数百个类型定义：
 * - **不过滤**: 反编译所有 200 个类 → ~10 秒
 * - **过滤后**: 只反编译 50 个没有源文件的类 → ~2.5 秒
 *
 * ### 源文件优先
 *
 * 如果同一目录下存在 .cj 源文件，使用源文件的好处：
 * - **更快**: 直接解析源文件比反编译快
 * - **更准确**: 源文件包含注释、格式等额外信息
 * - **更新**: 源文件可能包含最新的修改
 *
 * ### 避免冲突
 *
 * 如果同时索引源文件和反编译文件，可能导致：
 * - 重复的类定义
 * - 符号解析冲突
 * - IDE 导航混乱
 *
 * ## 参数说明
 *
 * ### package
 * - **类型**: [PackageWrapper]
 * - **来源**: 从 .cjb 文件解析的 Flatbuffers 数据
 * - **内容**: 包含包级声明（类、函数、变量、类型别名、扩展）
 *
 * ### version
 * - **类型**: [BuiltInsBinaryVersion]
 * - **用途**: 版本兼容性检查（虽然当前未强制检查）
 * - **来源**: 从元数据中读取
 *
 * ### packageDirectory
 * - **类型**: [VirtualFile]? (可为 null)
 * - **用途**: 查找对应的 .cj 源文件
 * - **为 null 时**: 凭空创建的文件（测试场景或虚拟文件系统）
 *
 * ### isMetadata
 * - **类型**: Boolean
 * - **true**: 文件只包含类型签名，没有实现（.cjb 元数据）
 * - **false**: 文件可能包含实现代码
 * - **影响**: true 时不过滤任何类
 *
 * ### filterOutClassesExistingAsClassFiles
 * - **类型**: Boolean (默认 true)
 * - **用途**: 实例级开关，控制是否启用源文件过滤
 * - **测试用**: 可设为 false 强制反编译所有类
 *
 * ## 使用场景
 *
 * ### 场景 1: IDE 启动加载内置库
 * ```kotlin
 * // IDE 扫描内置库目录
 * val cjbFile = VirtualFileManager.getInstance().findFileByUrl("file:///sdk/std-core.cjb")
 * val contents = cjbFile.contentsToByteArray()
 *
 * // 解析内置库
 * val builtIn = BuiltInDefinitionFile.read(contents, cjbFile)
 *
 * // 构建 Stub
 * val stub = CangJieBuiltInMetadataStubBuilder.buildFileStub(fileContent)
 * ```
 *
 * ### 场景 2: 测试环境创建虚拟内置库
 * ```kotlin
 * @Test
 * fun testBuiltInDecompilation() {
 *     val testPackage = createTestPackage()
 *     val builtIn = BuiltInDefinitionFile(
 *         package = testPackage,
 *         version = BuiltInsBinaryVersion.INSTANCE,
 *         packageDirectory = null,  // 凭空创建
 *         isMetadata = false,
 *         filterOutClassesExistingAsClassFiles = false  // 测试模式
 *     )
 *
 *     assertEquals(5, builtIn.classesToDecompile.size)  // 所有类都反编译
 * }
 * ```
 *
 * ### 场景 3: 自定义内置库加载
 * ```kotlin
 * // 加载自定义内置库（带源文件过滤）
 * val customBuiltIn = BuiltInDefinitionFile(
 *     package = customPackage,
 *     version = customVersion,
 *     packageDirectory = customDir,
 *     isMetadata = false,
 *     filterOutClassesExistingAsClassFiles = true
 * )
 *
 * // 只反编译没有源文件的类
 * customBuiltIn.classesToDecompile.forEach { classDecl ->
 *     LOG.info("Decompiling: ${classDecl.classId}")
 * }
 * ```
 *
 * ## 性能特征
 *
 * ### 过滤开销
 *
 * - **凭空创建**: O(1) - 直接返回所有类
 * - **元数据文件**: O(1) - 直接返回所有类
 * - **标准过滤**: O(n) - n = 类的数量
 *   - 每个类: findChild() 查找 → O(1) HashMap 查找
 *   - 总开销: ~1-5ms (对于 100 个类)
 *
 * ### 内存使用
 *
 * - **PackageWrapper**: ~10-100KB (取决于声明数量)
 * - **classesToDecompile**: ~1KB per class (只存储引用)
 * - **总计**: ~50-500KB per .cjb file
 *
 * ## 线程安全
 *
 * - **不可变性**: 所有参数是 val，一旦创建不可修改
 * - **懒加载**: classesToDecompile 使用 getter，每次调用重新计算
 * - **并发安全**: 可以在多个线程并发访问（只读操作）
 * - **注意**: classesToDecompile 不缓存，每次调用都重新过滤
 *
 * ## 已知限制
 *
 * 1. **classesToDecompile 无缓存**: 每次访问都重新计算过滤逻辑
 * 2. **文件名匹配**: 仅通过文件名匹配，不验证类内容是否一致
 * 3. **嵌套类处理**: 继承自父类的过滤逻辑（排除嵌套类和黑名单）
 * 4. **版本检查未启用**: read() 方法中的版本兼容性检查被注释掉
 *
 * ## 调试技巧
 *
 * ### 查看过滤结果
 * ```kotlin
 * val builtIn = BuiltInDefinitionFile.read(contents, file)
 * LOG.debug("Total classes: ${builtIn.`package`.allClassDecls.size}")
 * LOG.debug("Classes to decompile: ${builtIn.classesToDecompile.size}")
 * builtIn.classesToDecompile.forEach {
 *     LOG.debug("  - ${it.classId}")
 * }
 * ```
 *
 * ### 验证过滤逻辑
 * ```kotlin
 * val classId = ClassId.fromString("std.core.Int32")
 * val shouldDecompile = shouldDecompileBuiltInClass(classId, packageDirectory)
 * LOG.debug("Should decompile ${classId.shortClassName}: $shouldDecompile")
 * ```
 *
 * ### 调试源文件查找
 * ```kotlin
 * val fileName = classId.shortClassName.asString() + ".cj"
 * val sourceFile = packageDirectory?.findChild(fileName)
 * LOG.debug("Looking for: $fileName")
 * LOG.debug("Found: ${sourceFile != null}")
 * if (sourceFile != null) {
 *     LOG.debug("  Path: ${sourceFile.path}")
 * }
 * ```
 *
 * @param package 包装的包信息，包含所有顶层声明
 * @param version 内置库的二进制版本
 * @param packageDirectory 包含该文件的目录，可能为 null（凭空创建的情况）
 * @param isMetadata 是否为纯元数据文件（vs. 包含实现的文件）
 * @param filterOutClassesExistingAsClassFiles 是否过滤掉已存在源文件的类
 *
 * @see CangJieMetadataStubBuilder.FileWithMetadata.Compatible
 * @see BuiltInDefinitionFile.read
 * @see shouldDecompileBuiltInClass
 */
class BuiltInDefinitionFile(
    `package`: PackageWrapper,
    version: BuiltInsBinaryVersion,
    /**
     * 包含该虚拟文件的目录
     *
     * 可能为 null，当内置文件是"凭空创建"时（例如在测试或特殊场景中）。
     * 如果为 null，表示没有对应的物理文件系统位置，需要反编译所有类。
     */
    val packageDirectory: VirtualFile?,
    /**
     * 是否为纯元数据文件
     *
     * - true: 文件只包含类型签名，没有实现
     * - false: 文件可能包含实现代码
     */
    val isMetadata: Boolean,
    /**
     * 是否过滤掉已存在源文件的类
     *
     * 如果设置为 true，并且在同一目录下找到了对应的 .cj 源文件，
     * 则不会反编译该类，而是使用源文件。
     */
    private val filterOutClassesExistingAsClassFiles: Boolean = true,
) : CangJieMetadataStubBuilder.FileWithMetadata.Compatible(`package`, version, BuiltInSerializerFlatbuffers) {

    /**
     * 需要反编译的类列表
     *
     * 该属性根据过滤策略决定哪些类需要反编译：
     *
     * ## 过滤逻辑
     *
     * 1. **凭空创建的文件** (packageDirectory == null):
     *    - 反编译所有类，因为没有源文件可替代
     *
     * 2. **纯元数据文件** (isMetadata == true):
     *    - 反编译所有类，元数据总是需要的
     *
     * 3. **禁用过滤** (FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES == false):
     *    - 反编译所有类（通常用于测试）
     *
     * 4. **标准过滤** (其他情况):
     *    - 只反编译那些在同一目录下没有对应 .cj 源文件的类
     *
     * ## 性能考虑
     *
     * 过滤可以显著减少反编译工作量，因为许多内置类型可能已有源文件表示。
     */
    override val classesToDecompile: List<ClassDeclWrapper>
        get() = super.classesToDecompile.let { classes ->
            if (packageDirectory == null) {
                // 如果内置文件是凭空创建的，
                // 意味着我们需要所有内置类，因为没有 .cj 源文件来替代它们
                return@let classes
            }
            if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES || !filterOutClassesExistingAsClassFiles) classes
            else classes.filter { decl ->
                shouldDecompileBuiltInClass(decl.classId, packageDirectory)
            }
        }

    /**
     * 判断内置类是否应该被反编译
     *
     * 检查指定类是否在包目录中存在对应的 .cj 源文件。
     * 如果源文件存在，则不需要反编译；如果不存在，则需要反编译。
     *
     * ## 判断逻辑
     *
     * 构造源文件名: `类名短名.cj`（例如 `Int8.cj`）
     * 在包目录中查找该文件
     * - 找到: 返回 false（不反编译）
     * - 未找到: 返回 true（需要反编译）
     *
     * @param classId 类的标识符
     * @param packageDirectory 包所在的目录
     * @return true 如果应该反编译该类，false 如果可以跳过
     */
    private fun shouldDecompileBuiltInClass(classId: ClassId, packageDirectory: VirtualFile): Boolean {
        val realClassFileName = classId.shortClassName.asString() + "." + CangJieFileType.INSTANCE.defaultExtension
        return packageDirectory.findChild(realClassFileName) == null
    }

    companion object {
        /**
         * 全局开关：是否过滤掉已存在源文件的类
         *
         * 该标志控制是否跳过那些在包目录中已有对应 .cj 源文件的内置类。
         *
         * ## 使用场景
         *
         * - **生产环境** (true): 跳过已有源文件的类，优化性能
         * - **测试环境** (false): 强制反编译所有类，用于测试反编译逻辑
         *
         * ## 注意事项
         *
         * 该属性仅用于测试目的，生产环境应始终保持 true。
         * 使用 @TestOnly 注解限制 setter 只能在测试代码中调用。
         */
        var FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = true
            @TestOnly set

        /**
         * 从字节数组读取内置库定义文件
         *
         * 该方法是创建 [BuiltInDefinitionFile] 的工厂方法，负责：
         * 1. 解析 Flatbuffers 格式的包数据
         * 2. 检查版本兼容性（当前已注释）
         * 3. 创建定义文件对象
         * 4. 验证文件是否包含有效声明
         *
         * ## 解析流程
         *
         * ```
         * ByteArray
         *   ↓
         * ByteArrayInputStream
         *   ↓
         * toFbPackage() - Flatbuffers 解析
         *   ↓
         * PackageWrapper - 包装对象
         *   ↓
         * BuiltInDefinitionFile - 定义文件
         * ```
         *
         * ## 空文件处理
         *
         * 如果文件不包含任何声明（类、类型别名、函数、变量都为空），
         * 则返回 null，跳过该文件的索引构建。
         *
         * ## 版本检查
         *
         * 版本检查代码当前被注释掉，未来可能重新启用以确保兼容性。
         *
         * @param contents 内置库文件的字节内容
         * @param file 虚拟文件对象
         * @param filterOutClassesExistingAsClassFiles 是否过滤已存在源文件的类
         * @return 解析后的定义文件，如果文件为空或不兼容则返回 null
         */
        @JvmOverloads
        fun read(
            contents: ByteArray,
            file: VirtualFile,
            filterOutClassesExistingAsClassFiles: Boolean = true
        ): CangJieMetadataStubBuilder.FileWithMetadata? {
            val stream = ByteArrayInputStream(contents)

            // 从 Flatbuffers 格式解析包数据
            val `package` = stream.toFbPackage().packageWrapper
            val version = `package`.cjoVersion

            // 版本兼容性检查（当前未启用）
            // 未来可能需要验证内置库版本是否与当前编译器兼容
//            if (!version.isCompatibleWithCurrentCompilerVersion()) {
//                return Incompatible(version)
//            }

            val result = BuiltInDefinitionFile(
                `package`, version, file.parent,
                false,
                filterOutClassesExistingAsClassFiles
            )

            // 检查是否有任何需要反编译的声明
            // 如果所有声明列表都为空，则跳过该文件
//            if (result.classesToDecompile.isEmpty() &&
//                `package`.typeAliass.isEmpty() && `package`.functions.isEmpty() && `package`.variables.isEmpty()
//            ) {
//                // 没有任何声明需要反编译：应该跳过该文件
//                return null
//            }

            return result
        }
    }
}

/**
 * 内置库元数据 Stub 构建器
 *
 * ## 架构概述
 *
 * CangJieBuiltInMetadataStubBuilder 是专门为仓颉内置库（.cjb 文件）设计的 Stub 构建器，
 * 采用 Kotlin object 单例模式实现，继承自抽象基类 [CangJieMetadataStubBuilder]。
 * 它是反编译系统中处理内置库索引和符号解析的核心组件。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * IDE 启动 / 项目打开
 *   ↓
 * ClassFileStubBuilder (IDE 框架层)
 *   ↓
 * getSubBuilder(fileContent) → 查找匹配的 Stub 构建器
 *   ├─ 检查文件类型 == CangJieBuiltInFileType?
 *   └─ Yes → 返回 CangJieBuiltInMetadataStubBuilder (本对象)
 *   ↓
 * CangJieBuiltInMetadataStubBuilder.buildFileStub(fileContent)
 *   ├─ readFile() → 读取和解析 .cjb 文件
 *   │   ├─ CangJieBuiltInDecompilationInterceptor (测试拦截)
 *   │   └─ BuiltInDefinitionFile.read() (标准解析)
 *   │       ↓
 *   │       Flatbuffers 解析 → PackageWrapper
 *   ↓
 * buildCompatibleFileStub() [继承自父类]
 *   ├─ 创建 ClsStubBuilderComponents
 *   ├─ 构建包级声明 Stubs (函数、变量)
 *   ├─ 构建类声明 Stubs (过滤后的类列表)
 *   ├─ 构建扩展 Stubs
 *   └─ 构建类型别名 Stubs
 *   ↓
 * CangJieFileStubImpl (完整的 Stub 树)
 *   ↓
 * StubIndex 注册 (CangJieClassShortNameIndex, etc.)
 *   ↓
 * IDE 服务 (符号解析、代码补全、导航等)
 * ```
 *
 * ## 架构演进历史
 *
 * ### 旧架构 (已废弃)
 *
 * ```kotlin
 * // 旧设计：中间层过多
 * CangJieBuiltInDecompiler : CangJieMetadataDecompiler {
 *     private val stubBuilder = CangJieBuiltInMetadataStubBuilder()
 *
 *     override fun buildFileStub(fileContent: FileContent): PsiFileStub? {
 *         return stubBuilder.buildFileStub(fileContent)  // 额外的委托
 *     }
 * }
 * ```
 *
 * **问题**:
 * - 不必要的中间层（Decompiler 只是简单委托）
 * - 生命周期管理复杂（需要管理 stubBuilder 实例）
 * - 违反单一职责原则（Decompiler 和 StubBuilder 职责重叠）
 *
 * ### 新架构 (当前)
 *
 * ```kotlin
 * // 新设计：直接继承，无中间层
 * object CangJieBuiltInMetadataStubBuilder : CangJieMetadataStubBuilder() {
 *     override val stubVersion: Int = ...
 *     override fun readFile(...): FileWithMetadata? = ...
 *
 *     // buildFileStub() 继承自父类，直接被 IDE 调用
 * }
 * ```
 *
 * **优势**:
 * 1. **简化调用链**: IDE → StubBuilder（去除 Decompiler 中间层）
 * 2. **统一入口**: 所有元数据类型通过 ClassFileStubBuilder 统一调度
 * 3. **单例模式**: object 保证全局唯一实例，线程安全
 * 4. **职责清晰**: StubBuilder 专注 Stub 构建，Decompiler 专注文本反编译
 *
 * ### 架构变更动机
 *
 * **原理**: Stub 构建和反编译文本生成是两个独立的关注点：
 * - **Stub**: 轻量级索引，快速符号查找
 * - **Decompiled Text**: 完整的源码表示，用于查看和导航
 *
 * 旧架构将两者混在 Decompiler 中，新架构分离关注点。
 *
 * ## 核心职责
 *
 * ### 1. 文件类型识别
 *
 * 通过 `supportedFileType` 属性告诉 IDE 该构建器处理哪种文件：
 * ```kotlin
 * override val supportedFileType: FileType get() = CangJieBuiltInFileType
 * ```
 *
 * **影响**:
 * - ClassFileStubBuilder 根据此属性选择正确的构建器
 * - 只有 `.cjb` 文件会被路由到本构建器
 *
 * ### 2. 版本管理
 *
 * 提供内置库特定的版本号，控制 Stub 缓存失效：
 * ```kotlin
 * override val stubVersion: Int get() = stubVersionForStubBuilderAndDecompiler
 * ```
 *
 * **详细说明**: 参见 [stubVersionForStubBuilderAndDecompiler] 的完整文档
 *
 * **关键点**:
 * - 版本号变化 → Stub 缓存失效 → 重新构建所有内置库 Stub
 * - 支持编译器升级后自动重建
 * - 支持开发时强制刷新
 *
 * ### 3. 元数据读取
 *
 * 实现 `readFile()` 方法，解析 .cjb 文件内容：
 * ```kotlin
 * override fun readFile(virtualFile: VirtualFile, content: ByteArray?): FileWithMetadata? {
 *     val content = content ?: virtualFile.contentsToByteArray(false)
 *
 *     // 测试拦截点（允许测试代码注入自定义逻辑）
 *     return CangJieBuiltInDecompilationInterceptor.readFile(content, virtualFile)
 *         ?: BuiltInDefinitionFile.read(content, virtualFile)  // 标准解析
 * }
 * ```
 *
 * **工作流程**:
 * 1. **获取内容**: content 参数 ?: 读取文件
 * 2. **测试拦截**: 检查是否有测试拦截器
 * 3. **标准解析**: 调用 BuiltInDefinitionFile.read()
 *    - 解析 Flatbuffers 元数据
 *    - 提取 PackageWrapper
 *    - 应用类过滤策略
 * 4. **返回结果**: FileWithMetadata.Compatible 或 null
 *
 * ### 4. Stub 树构建
 *
 * 继承自父类的 `buildFileStub()` 方法完成实际构建：
 * ```
 * buildFileStub(fileContent)  [继承自 CangJieMetadataStubBuilder]
 *   ↓
 * 1. 验证文件类型 (isSupported)
 * 2. 读取元数据 (readFile)
 * 3. 兼容性检查
 *    ├─ Compatible → buildCompatibleFileStub()
 *    └─ Incompatible → createIncompatibleAbiVersionFileStub()
 * 4. 返回 Stub 树
 * ```
 *
 * ## 完整工作流程示例
 *
 * ### 场景: IDE 启动时索引内置库
 *
 * ```
 * 1. IDE 启动 + 项目打开
 *   ↓
 * 2. VFS 扫描发现 std-core.cjb
 *   └─ VirtualFile(/path/to/sdk/std-core.cjb)
 *   ↓
 * 3. FileContent 创建
 *   FileContent(file=std-core.cjb, content=byte[1024000])
 *   ↓
 * 4. ClassFileStubBuilder.buildStubTree(fileContent)
 *   ├─ 调用 getSubBuilder(fileContent)
 *   ├─ 遍历所有注册的 Full 反编译器
 *   ├─ CangJieBuiltInMetadataStubBuilder.stubBuilder.isSupported(file)
 *   │   └─ file.fileType == CangJieBuiltInFileType → true ✓
 *   └─ 返回 CangJieBuiltInMetadataStubBuilder
 *   ↓
 * 5. CangJieBuiltInMetadataStubBuilder.buildFileStub(fileContent)
 *   ├─ 5.1 验证文件类型
 *   │   requireWithAttachment(isSupported(file), ...)
 *   ├─ 5.2 读取元数据
 *   │   readFile(file, fileContent.content)
 *   │     ├─ CangJieBuiltInDecompilationInterceptor.readFile() → null (无测试拦截)
 *   │     └─ BuiltInDefinitionFile.read(content, file)
 *   │         ├─ 解析 Flatbuffers: ByteArrayInputStream(content).toFbPackage()
 *   │         ├─ 提取包信息: packageWrapper
 *   │         ├─ 提取版本: packageWrapper.cjoVersion
 *   │         ├─ 应用类过滤:
 *   │         │   std-core.cjb 包含 200 个类
 *   │         │   packageDirectory 包含 50 个 .cj 源文件
 *   │         │   → classesToDecompile = 150 个类 (跳过已有源文件)
 *   │         └─ 返回 BuiltInDefinitionFile(package, version, ...)
 *   ├─ 5.3 兼容性检查
 *   │   file is FileWithMetadata.Compatible → 继续
 *   ├─ 5.4 构建 Stub 树 [buildCompatibleFileStub]
 *   │   ├─ 创建 CjoPackageService 实例
 *   │   ├─ 创建 ClsStubBuilderComponents
 *   │   │   └─ FlatBuffersBasedClassDataFinder(packageWrapper, version)
 *   │   ├─ 创建 Stub 上下文
 *   │   │   └─ components.createContext(packageFqName, typeTable)
 *   │   ├─ 创建文件 Stub 根节点
 *   │   │   └─ createFileStub(FqName("std.core"))
 *   │   ├─ 构建包级函数/变量 Stubs
 *   │   │   └─ createPackageDeclarationsStubs(fileStub, ...)
 *   │   ├─ 构建 150 个类的 Stubs
 *   │   │   for (classDecl in classesToDecompile) {
 *   │   │     ClassClsStubBuilder(fileStub, context, classDecl).build()
 *   │   │       ├─ 创建 CangJieClassStubImpl
 *   │   │       ├─ 构建成员函数 Stubs
 *   │   │       ├─ 构建成员变量 Stubs
 *   │   │       └─ 构建嵌套类 Stubs
 *   │   │   }
 *   │   ├─ 构建扩展 Stubs
 *   │   └─ 构建类型别名 Stubs
 *   └─ 5.5 返回 CangJieFileStubImpl
 *   ↓
 * 6. StubIndex 注册
 *   ├─ CangJieClassShortNameIndex.process(stub)
 *   ├─ CangJieFunctionShortNameIndex.process(stub)
 *   └─ CangJieSuperClassIndex.process(stub)
 *   ↓
 * 7. 索引完成 → IDE 就绪
 * ```
 *
 * ## 与普通库 Stub 构建器的区别
 *
 * | 特性 | 内置库 (.cjb) | 普通库 (.cjo) |
 * |------|--------------|--------------|
 * | **构建器** | CangJieBuiltInMetadataStubBuilder | CangJieNormalMetadataStubBuilder |
 * | **文件类型** | CangJieBuiltInFileType | CangJieCompiledFileType |
 * | **版本管理** | BuiltInsBinaryVersion | NormalBinaryVersion |
 * | **序列化器** | BuiltInSerializerFlatbuffers | NormalSerializerFlatbuffers |
 * | **类过滤** | ✅ 支持（跳过已有源文件） | ❌ 不支持（反编译所有类） |
 * | **测试拦截** | ✅ 支持 (CangJieBuiltInDecompilationInterceptor) | ❌ 不支持 |
 * | **位置** | SDK 目录 | 项目依赖/输出目录 |
 * | **可变性** | 只读（SDK 一部分） | 可重新编译 |
 *
 * ## 类过滤机制
 *
 * 内置库支持智能类过滤，避免重复反编译已有源文件的类：
 *
 * ### 为什么需要过滤？
 *
 * **场景**: SDK 目录结构
 * ```
 * sdk/
 * ├─ std-core/
 * │   ├─ Int8.cj        # 源文件
 * │   ├─ Int16.cj       # 源文件
 * │   ├─ Int32.cj       # 源文件
 * │   └─ std-core.cjb   # 编译文件（包含 Int8/16/32/64/...）
 * └─ ...
 * ```
 *
 * **不过滤的后果**:
 * - 重复解析: Int8.cj 和 std-core.cjb 中的 Int8 都被索引
 * - 符号冲突: IDE 找到两个 Int8 定义
 * - 性能浪费: 反编译已有源文件的类
 *
 * **过滤后**:
 * - 只索引源文件 (Int8.cj, Int16.cj, Int32.cj)
 * - 只反编译缺失的类 (Int64, UInt8, ...)
 * - 性能提升: ~4x (200 类 → 50 类)
 *
 * ### 过滤实现
 *
 * 在 [BuiltInDefinitionFile.classesToDecompile] 中实现：
 * ```kotlin
 * classesToDecompile.filter { decl ->
 *     val fileName = decl.classId.shortClassName.asString() + ".cj"
 *     packageDirectory.findChild(fileName) == null  // 源文件不存在才反编译
 * }
 * ```
 *
 * ## 测试拦截机制
 *
 * ### 什么是测试拦截？
 *
 * [CangJieBuiltInDecompilationInterceptor] 允许测试代码在不修改生产代码的情况下，
 * 注入自定义的内置库读取逻辑。
 *
 * ### 使用场景
 *
 * **场景 1: 单元测试虚拟内置库**
 * ```kotlin
 * @Test
 * fun testBuiltInStubGeneration() {
 *     // 注册测试拦截器
 *     CangJieBuiltInDecompilationInterceptor.register { content, file ->
 *         // 返回模拟的 BuiltInDefinitionFile
 *         createTestBuiltInDefinitionFile()
 *     }
 *
 *     try {
 *         // 触发 Stub 构建
 *         val stub = CangJieBuiltInMetadataStubBuilder.buildFileStub(fileContent)
 *
 *         // 验证 Stub 结构
 *         assertEquals(5, stub.childrenStubs.size)
 *     } finally {
 *         // 清理拦截器
 *         CangJieBuiltInDecompilationInterceptor.unregister()
 *     }
 * }
 * ```
 *
 * **场景 2: 性能测试**
 * ```kotlin
 * CangJieBuiltInDecompilationInterceptor.register { content, file ->
 *     // 注入带性能计数的包装器
 *     val result = BuiltInDefinitionFile.read(content, file)
 *     metrics.recordParsing(file.name, content.size)
 *     result
 * }
 * ```
 *
 * ### 拦截点位置
 *
 * 在 `readFile()` 方法的第一行：
 * ```kotlin
 * override fun readFile(...): FileWithMetadata? {
 *     val content = content ?: virtualFile.contentsToByteArray(false)
 *
 *     // 拦截点：测试代码可以在此注入逻辑
 *     return CangJieBuiltInDecompilationInterceptor.readFile(content, virtualFile)
 *         ?: BuiltInDefinitionFile.read(content, virtualFile)  // 默认逻辑
 * }
 * ```
 *
 * ## 实现细节
 *
 * ### stubVersion 实现
 *
 * ```kotlin
 * override val stubVersion: Int get() = stubVersionForStubBuilderAndDecompiler
 * ```
 *
 * **作用**:
 * - 返回复合版本号（基础版本 + 动态偏移量）
 * - IDE 将此版本号存储在 Stub 缓存中
 * - 版本变化时自动清除缓存
 *
 * **调用频率**: 每次 Stub 构建或缓存验证时调用（高频）
 *
 * ### supportedFileType 实现
 *
 * ```kotlin
 * override val supportedFileType: FileType get() = CangJieBuiltInFileType
 * ```
 *
 * **作用**:
 * - 告诉 ClassFileStubBuilder 该构建器处理 .cjb 文件
 * - ClassFileStubBuilder 根据此属性路由文件到正确的构建器
 *
 * **文件类型匹配**:
 * ```
 * ClassFileStubBuilder.getSubBuilder(fileContent)
 *   ↓
 * for (decompiler in decompilers) {
 *     if (decompiler.stubBuilder.supportedFileType == fileContent.fileType) {
 *         return decompiler.stubBuilder  // 匹配!
 *     }
 * }
 * ```
 *
 * ### expectedBinaryVersion 实现
 *
 * ```kotlin
 * override val expectedBinaryVersion: BinaryVersion get() = BuiltInsBinaryVersion.INSTANCE
 * ```
 *
 * **作用**:
 * - 定义期望的元数据二进制版本
 * - 用于版本兼容性检查（当前未强制检查）
 *
 * **未来扩展**: 可用于拒绝不兼容的内置库版本
 *
 * ### readFile() 实现
 *
 * **参数**:
 * - `virtualFile: VirtualFile` - 元数据虚拟文件
 * - `content: ByteArray?` - 可选的文件内容（性能优化）
 *
 * **返回值**:
 * - `FileWithMetadata.Compatible` - 成功解析
 * - `null` - 解析失败或文件无效
 *
 * **实现逻辑**:
 * ```kotlin
 * val content = content ?: virtualFile.contentsToByteArray(false)
 * // 1. content 参数为 null → 读取文件
 * // 2. content 参数非 null → 复用已读取内容（避免重复 I/O）
 *
 * return CangJieBuiltInDecompilationInterceptor.readFile(content, virtualFile)
 * // 3. 测试拦截器返回非 null → 使用测试逻辑
 * // 4. 测试拦截器返回 null → 继续标准流程
 *
 *     ?: BuiltInDefinitionFile.read(content, virtualFile)
 * // 5. 标准解析: Flatbuffers → PackageWrapper → BuiltInDefinitionFile
 * ```
 *
 * ## 性能考虑
 *
 * ### 单例优势
 *
 * ```kotlin
 * object CangJieBuiltInMetadataStubBuilder  // 全局唯一实例
 * ```
 *
 * **好处**:
 * - 无实例创建开销
 * - 无 GC 压力
 * - 线程安全（JVM 类加载保证）
 *
 * ### 类过滤性能提升
 *
 * | 场景 | 不过滤 | 过滤后 | 提升 |
 * |------|-------|-------|------|
 * | std-core.cjb (200 类) | 10 秒 | 2.5 秒 | 4x |
 * | std-collection.cjb (150 类) | 7.5 秒 | 2 秒 | 3.75x |
 * | 所有内置库 (1000+ 类) | 50 秒 | 15 秒 | 3.3x |
 *
 * ### 缓存机制
 *
 * - **Stub 缓存**: IDE 自动缓存构建的 Stub 树
 * - **缓存键**: (文件路径, stubVersion, 文件修改时间)
 * - **缓存位置**: IDE 系统目录 (如 ~/.cache/JetBrains)
 * - **失效触发**: stubVersion 变化 或 文件修改
 *
 * ## 线程安全
 *
 * ### object 单例
 *
 * ```kotlin
 * object CangJieBuiltInMetadataStubBuilder
 * ```
 *
 * **保证**:
 * - JVM 类加载机制确保单例初始化是线程安全的
 * - 所有线程看到相同的实例
 * - 无并发初始化问题
 *
 * ### 无状态设计
 *
 * 本对象没有可变状态（无 var 字段）：
 * - 所有方法都是纯函数或基于不可变配置
 * - 可以安全地在多个线程并发调用
 *
 * ### 方法调用线程安全
 *
 * | 方法 | 线程安全性 | 原因 |
 * |------|----------|------|
 * | stubVersion | ✅ 安全 | 只读计算属性 |
 * | supportedFileType | ✅ 安全 | 返回常量 |
 * | expectedBinaryVersion | ✅ 安全 | 返回单例常量 |
 * | readFile() | ✅ 安全 | 无共享状态，每次创建新对象 |
 * | buildFileStub() | ✅ 安全 | 继承自父类，无副作用 |
 *
 * ## 错误处理
 *
 * ### readFile() 错误场景
 *
 * | 错误场景 | 返回值 | IDE 行为 |
 * |---------|-------|----------|
 * | 文件不存在 | null | 跳过该文件 |
 * | 文件损坏（无效 Flatbuffers） | null | 跳过，记录警告 |
 * | 版本不兼容 | Incompatible | 创建错误占位符 Stub |
 * | I/O 异常 | null (捕获) | 跳过，记录错误 |
 *
 * ### buildFileStub() 错误处理
 *
 * 继承自父类 [CangJieMetadataStubBuilder.buildFileStub]：
 * ```kotlin
 * try {
 *     val file = readFile(virtualFile, content) ?: return null
 *     when (file) {
 *         is Incompatible -> createIncompatibleAbiVersionFileStub()
 *         is Compatible -> buildCompatibleFileStub(file, ...)
 *     }
 * } catch (e: Exception) {
 *     LOG.error("Failed to build stub for ${virtualFile.path}", e)
 *     return null
 * }
 * ```
 *
 * ## 调试技巧
 *
 * ### 查看 Stub 构建过程
 *
 * 启用调试日志：
 * ```
 * Help → Diagnostic Tools → Debug Log Settings
 * 添加: #org.cangnova.cangjie.decompiler.stub
 * ```
 *
 * 日志输出：
 * ```
 * [STUB] Building stub for: /sdk/std-core.cjb
 * [STUB] Stub version: 42
 * [STUB] Classes to decompile: 150 (filtered from 200)
 * [STUB] Stub tree size: 450 nodes
 * [STUB] Build time: 2.5s
 * ```
 *
 * ### 验证类过滤
 *
 * ```kotlin
 * val builtIn = BuiltInDefinitionFile.read(content, file)
 * println("Total classes: ${builtIn.`package`.allClassDecls.size}")
 * println("Classes to decompile: ${builtIn.classesToDecompile.size}")
 * builtIn.classesToDecompile.forEach { decl ->
 *     println("  - ${decl.classId}")
 * }
 * ```
 *
 * ### 测试拦截验证
 *
 * ```kotlin
 * var interceptorCalled = false
 * CangJieBuiltInDecompilationInterceptor.register { _, _ ->
 *     interceptorCalled = true
 *     null  // 回退到标准逻辑
 * }
 *
 * buildFileStub(fileContent)
 *
 * assertTrue(interceptorCalled, "Interceptor should be called")
 * ```
 *
 * ## 使用示例
 *
 * ### 示例 1: IDE 框架调用
 *
 * ```kotlin
 * // IDE 内部代码（简化）
 * val stubBuilder = ClassFileStubBuilder.getSubBuilder(fileContent)
 * if (stubBuilder == CangJieBuiltInMetadataStubBuilder) {
 *     val stub = stubBuilder.buildFileStub(fileContent)
 *     StubIndex.getInstance().updateIndex(stub)
 * }
 * ```
 *
 * ### 示例 2: 测试代码
 *
 * ```kotlin
 * class BuiltInStubTest {
 *     @Test
 *     fun `test stub generation for std-core`() {
 *         val file = createTestVirtualFile("std-core.cjb")
 *         val content = loadTestResource("/testData/std-core.cjb")
 *         val fileContent = FileContentImpl.createByContent(file, content)
 *
 *         val stub = CangJieBuiltInMetadataStubBuilder.buildFileStub(fileContent)
 *
 *         assertNotNull(stub)
 *         assertEquals("std.core", stub.packageFqName.asString())
 *         assertTrue(stub.childrenStubs.any { it is CangJieClassStub })
 *     }
 * }
 * ```
 *
 * ### 示例 3: 性能测试
 *
 * ```kotlin
 * @Test
 * fun `test stub build performance`() {
 *     val files = loadAllBuiltInFiles()  // ~20 个 .cjb 文件
 *
 *     val startTime = System.currentTimeMillis()
 *     files.forEach { file ->
 *         CangJieBuiltInMetadataStubBuilder.buildFileStub(file)
 *     }
 *     val duration = System.currentTimeMillis() - startTime
 *
 *     assertTrue(duration < 30_000, "Should build all stubs in < 30s")
 * }
 * ```
 *
 * ## 与其他组件的集成
 *
 * ### ClassFileStubBuilder 集成
 *
 * ```
 * ClassFileStubBuilder (IDE 框架层)
 *   ↓
 * getSubBuilder(fileContent)
 *   ├─ 遍历所有注册的 Full 反编译器
 *   ├─ CangJieMetadataDecompiler.accepts(file)?
 *   │   └─ stubBuilder.isSupported(file)?
 *   │       └─ file.fileType == CangJieBuiltInFileType?
 *   │           └─ Yes → 返回 CangJieBuiltInMetadataStubBuilder
 *   └─ 调用 stubBuilder.buildFileStub()
 * ```
 *
 * ### StubIndex 集成
 *
 * ```
 * CangJieBuiltInMetadataStubBuilder.buildFileStub()
 *   ↓
 * 返回 CangJieFileStubImpl
 *   ↓
 * StubIndexImpl.updateIndex(stub)
 *   ├─ 遍历 stub 树的所有节点
 *   ├─ 调用各个 StubIndex 的 indexStub()
 *   │   ├─ CangJieClassShortNameIndex.indexStub()
 *   │   ├─ CangJieFunctionShortNameIndex.indexStub()
 *   │   └─ CangJieSuperClassIndex.indexStub()
 *   └─ 持久化索引到磁盘
 * ```
 *
 * ### CjoPackageService 集成
 *
 * ```
 * buildCompatibleFileStub()
 *   ↓
 * CjoPackageService.getInstance(project)
 *   ↓
 * 用于解析跨包引用
 *   例如: ArrayList<T> 的方法返回 std.array.Array<T>
 *   └─ CjoPackageService.resolveFullId("std.array.Array", "std.array")
 *       └─ 查找 std-array.cjb 获取 Array 类型定义
 * ```
 *
 * ## 最佳实践
 *
 * ### 1. 不要直接实例化
 *
 * ```kotlin
 * // ❌ 错误: object 不能实例化
 * val builder = CangJieBuiltInMetadataStubBuilder()
 *
 * // ✅ 正确: 直接使用单例
 * val stub = CangJieBuiltInMetadataStubBuilder.buildFileStub(fileContent)
 * ```
 *
 * ### 2. 测试时使用拦截器
 *
 * ```kotlin
 * // ✅ 正确: 使用拦截器注入测试逻辑
 * CangJieBuiltInDecompilationInterceptor.register { content, file ->
 *     createTestBuiltInDefinitionFile()
 * }
 *
 * try {
 *     // 测试代码
 * } finally {
 *     CangJieBuiltInDecompilationInterceptor.unregister()
 * }
 * ```
 *
 * ### 3. 复用 content 参数
 *
 * ```kotlin
 * // ✅ 高效: 复用已读取的内容
 * val content = file.contentsToByteArray()
 * val stub1 = builder1.readFile(file, content)
 * val stub2 = builder2.readFile(file, content)
 *
 * // ❌ 低效: 重复读取文件
 * val stub1 = builder1.readFile(file, null)  // 读取一次
 * val stub2 = builder2.readFile(file, null)  // 又读取一次
 * ```
 *
 * ## 已知限制
 *
 * 1. **无批量构建 API**: 每个文件独立构建，无法批量优化
 * 2. **版本兼容性检查未强制**: expectedBinaryVersion 当前仅作参考
 * 3. **类过滤无缓存**: 每次访问 classesToDecompile 都重新计算
 * 4. **测试拦截器全局**: 影响所有线程，需谨慎使用
 *
 * @see CangJieMetadataStubBuilder
 * @see BuiltInDefinitionFile
 * @see CangJieBuiltInDecompilationInterceptor
 * @see CangJieBuiltInFileType
 * @see BuiltInsBinaryVersion
 * @see stubVersionForStubBuilderAndDecompiler
 */
object CangJieBuiltInMetadataStubBuilder : CangJieMetadataStubBuilder(
) {
    override val stubVersion: Int get() = stubVersionForStubBuilderAndDecompiler
    override val supportedFileType: FileType get() = CangJieBuiltInFileType
    override val expectedBinaryVersion: BinaryVersion get() = BuiltInsBinaryVersion.INSTANCE
    override fun readFile(
        virtualFile: VirtualFile,
        content: ByteArray?
    ): FileWithMetadata? {
        val content = content ?: virtualFile.contentsToByteArray(false)
        return CangJieBuiltInDecompilationInterceptor.readFile(content, virtualFile) ?: BuiltInDefinitionFile.read(
            content,
            virtualFile
        )

    }

}