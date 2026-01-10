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

package org.cangnova.cangjie.decompiler.stub.file

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.cangnova.cangjie.cjo.CjoPackageService
import org.cangnova.cangjie.decompiler.psi.compiled.ClsStubBuilder
import org.cangnova.cangjie.decompiler.psi.compiled.impl.ClassFileStubBuilder
import org.cangnova.cangjie.decompiler.stub.*
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.serialization.deserialization.BLACK_LIST
import org.cangnova.cangjie.serialization.deserialization.ClassDataFinder
import org.cangnova.cangjie.serialization.deserialization.FlatBuffersBasedClassDataFinder
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import java.io.IOException
import kotlin.contracts.contract

/**
 * 仓颉元数据 Stub 构建器抽象基类
 *
 * ## 架构概述
 *
 * CangJieMetadataStubBuilder 是反编译系统的核心 Stub 构建抽象类，负责从二进制元数据文件
 * （.cjo/.cjb）构建轻量级的 PSI Stub 树，用于 IDE 的快速索引、符号解析和代码导航。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * ClassFileStubBuilder (IDE 入口)
 *   ↓
 * ClassFileDecompilers.find() (选择反编译器)
 *   ↓
 * CangJieMetadataDecompiler (反编译器实例)
 *   ↓
 * CangJieMetadataStubBuilder (本类 - Stub 构建器)
 *   ├─ CangJieBuiltInMetadataStubBuilder (.cjb 内置库)
 *   └─ (未来可扩展的其他格式)
 *   ↓
 * FileWithMetadata (元数据表示)
 *   ├─ Compatible → buildCompatibleFileStub()
 *   └─ Incompatible → createIncompatibleAbiVersionFileStub()
 *   ↓
 * ClsStubBuilderComponents (Stub 构建组件)
 *   ↓
 * 各种 ClsStubBuilder (ClassClsStubBuilder, FunctionClsStubBuilder, etc.)
 *   ↓
 * CangJieFileStubImpl (Stub 树)
 * ```
 *
 * ## 设计模式
 *
 * ### 1. 模板方法模式 (Template Method)
 *
 * 定义 Stub 构建的骨架流程，延迟部分步骤给子类实现：
 *
 * **固定流程** (final 方法):
 * ```kotlin
 * final override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *     // 1. 验证文件类型 (委托给子类的 supportedFileType)
 *     requireWithAttachment(isSupported(virtualFile), { "Unexpected file type" })
 *
 *     // 2. 读取元数据 (委托给子类的 readFile)
 *     val file = readFileSafely(virtualFile, fileContent.content) ?: return null
 *
 *     // 3. 根据兼容性分支处理
 *     return when (file) {
 *         is Incompatible -> createIncompatibleAbiVersionFileStub()
 *         is Compatible -> buildCompatibleFileStub(file, virtualFile, project)
 *     }
 * }
 * ```
 *
 * **可变步骤** (抽象成员):
 * - `supportedFileType`: 子类定义支持的文件类型 (.cjb, .cjo, etc.)
 * - `expectedBinaryVersion`: 子类定义期望的二进制版本
 * - `readFile()`: 子类实现元数据读取逻辑
 *
 * ### 2. 策略模式 (Strategy)
 *
 * 通过 [FileWithMetadata] 密封类封装不同的处理策略：
 * - **Compatible 策略**: 完整 Stub 构建
 * - **Incompatible 策略**: 错误占位符 Stub
 *
 * ### 3. 工厂模式 (Factory)
 *
 * 通过 `createFileStub()`, `createClassStub()` 等工厂方法创建不同类型的 Stub。
 *
 * ### 4. 组件模式 (Component)
 *
 * [ClsStubBuilderComponents] 聚合所有 Stub 构建所需的组件：
 * - ClassDataFinder
 * - DeclTable / TypeTable
 * - CjoPackageService
 * - PackageWrapper
 *
 * ## 核心工作流程
 *
 * ### 完整 Stub 构建流程
 *
 * ```
 * IDE 索引请求
 *   ↓
 * ClassFileStubBuilder.buildStubTree(ArrayList.cjo)
 *   ↓
 * 1. 调用 buildFileStub(fileContent)
 *   ↓
 * 2. 验证文件类型
 *    isSupported(ArrayList.cjo)
 *      ├─ 检查文件扩展名 == "cjo"
 *      └─ 或检查文件类型 == CangJieBuiltInFileType
 *   ↓
 * 3. 读取元数据
 *    readFile(virtualFile, content)
 *      ├─ 读取 Flatbuffers 二进制数据
 *      ├─ 解析 PackageWrapper
 *      ├─ 检查二进制版本兼容性
 *      └─ 返回 FileWithMetadata.Compatible
 *   ↓
 * 4. 兼容性分支判断
 *    when (file) {
 *      Incompatible → createIncompatibleAbiVersionFileStub()
 *      Compatible → buildCompatibleFileStub()
 *    }
 *   ↓
 * 5. buildCompatibleFileStub() 详细流程
 *   ├─ 5.1 初始化 CjoPackageService (跨包引用解析)
 *   │   packageService = CjoPackageService.getInstance(project)
 *   │
 *   ├─ 5.2 创建 ClsStubBuilderComponents
 *   │   components = ClsStubBuilderComponents(
 *   │     classDataFinder = FlatBuffersBasedClassDataFinder(packageWrapper, version),
 *   │     declTable = packageWrapper.declTable,
 *   │     typeTable = packageWrapper.typeTable,
 *   │     packageService = packageService
 *   │   )
 *   │
 *   ├─ 5.3 创建 Stub 上下文
 *   │   context = components.createContext(packageFqName, typeTable)
 *   │
 *   ├─ 5.4 创建文件 Stub 根节点
 *   │   fileStub = createFileStub(packageFqName)
 *   │
 *   ├─ 5.5 构建顶层声明 Stubs
 *   │   createPackageDeclarationsStubs(
 *   │     fileStub,
 *   │     functions = packageWrapper.functions,
 *   │     variables = packageWrapper.variables
 *   │   )
 *   │
 *   ├─ 5.6 构建类声明 Stubs
 *   │   for (classDecl in classesToDecompile) {
 *   │     createClassStub(fileStub, classDecl, context)
 *   │       ↓
 *   │     ClassClsStubBuilder.build()
 *   │       ├─ 创建 CangJieClassStubImpl
 *   │       ├─ 构建成员函数 Stubs
 *   │       ├─ 构建成员变量 Stubs
 *   │       └─ 构建嵌套类 Stubs
 *   │   }
 *   │
 *   ├─ 5.7 构建扩展声明 Stubs
 *   │   for (extend in packageWrapper.extends) {
 *   │     ExtendClsStubBuilder(fileStub, context, extend).build()
 *   │   }
 *   │
 *   └─ 5.8 构建类型别名 Stubs
 *       for (typeAlias in packageWrapper.typeAliass) {
 *         TypeAliasClsStubBuilder(fileStub, context, typeAlias).build()
 *       }
 *   ↓
 * 6. 返回完整的 Stub 树
 *   CangJieFileStubImpl
 *     ├─ CangJieClassStubImpl (ArrayList)
 *     │   ├─ CangJieFunctionStubImpl (add)
 *     │   ├─ CangJieFunctionStubImpl (remove)
 *     │   └─ CangJieVariableStubImpl (size)
 *     ├─ CangJieExtendStubImpl
 *     └─ CangJieTypeAliasStubImpl
 * ```
 *
 * ## FileWithMetadata 密封类设计
 *
 * ### 设计目的
 *
 * 使用密封类表示元数据的两种互斥状态：
 * - **类型安全**: 编译期保证穷尽性检查 (when 表达式)
 * - **明确语义**: 区分"不兼容"和"兼容"两种完全不同的处理路径
 * - **避免 null**: 用类型表示状态而不是可空值
 *
 * ### 类型层次结构
 *
 * ```kotlin
 * sealed class FileWithMetadata {
 *     class Incompatible(val version: BinaryVersion) : FileWithMetadata()
 *
 *     open class Compatible(
 *         val package: PackageWrapper,
 *         val version: BinaryVersion,
 *         val serializerProtocol: SerializerExtensionFlatbuffers
 *     ) : FileWithMetadata() {
 *         val packageFqName: FqName
 *         open val classesToDecompile: List<ClassDeclWrapper>
 *     }
 * }
 * ```
 *
 * ### Incompatible - 不兼容版本
 *
 * **何时创建**:
 * - 元数据二进制版本高于 IDE 支持的最高版本
 * - 元数据格式发生不兼容变更
 *
 * **包含信息**:
 * - `version`: 实际的二进制版本号（用于错误提示）
 *
 * **处理方式**:
 * ```kotlin
 * is Incompatible -> createIncompatibleAbiVersionFileStub()
 *   ↓
 * 创建特殊的 Stub，反编译文本显示错误消息：
 * "// ERROR: Incompatible ABI version. Expected: X.Y, Actual: A.B"
 * ```
 *
 * ### Compatible - 兼容版本
 *
 * **何时创建**:
 * - 元数据二进制版本在支持范围内
 * - 元数据格式正确，可以解析
 *
 * **包含信息**:
 * - `package`: 完整的 PackageWrapper（包含所有声明）
 * - `version`: 二进制版本号
 * - `serializerProtocol`: Flatbuffers 序列化协议
 * - `packageFqName`: 包的完全限定名（从 package.packageName 派生）
 * - `classesToDecompile`: 过滤后的类列表
 *
 * **classesToDecompile 过滤逻辑**:
 * ```kotlin
 * package.allClassDecls.filter { decl ->
 *     !decl.classId.isNestedClass &&  // 排除嵌套类 (Outer$Inner)
 *     decl.classId !in BLACK_LIST     // 排除黑名单类 (编译器内部)
 * }
 * ```
 *
 * **为什么过滤**:
 * - **嵌套类**: 由父类 Stub 负责构建，避免重复
 * - **黑名单**: 编译器内部类，不应暴露给用户
 *
 * ## 跨包引用解析机制
 *
 * ### 问题背景
 *
 * 元数据文件中的类型引用使用 `FullId` 表示跨包类型：
 * ```
 * ArrayList.cjo 元数据:
 *   class ArrayList<T> {
 *     func add(element: T) { ... }
 *     func toArray(): std.array.Array<T> { ... }  // 跨包引用!
 *   }
 * ```
 *
 * `std.array.Array` 定义在另一个包中，需要解析其完整类型信息。
 *
 * ### 解析流程
 *
 * ```
 * 构建 ArrayList.toArray() 的 Stub
 *   ↓
 * TypeClsStubBuilder 遇到返回类型: FullId("std.array.Array")
 *   ↓
 * 调用 CjoPackageService.resolveFullId("std.array.Array", "std.array")
 *   ↓
 * CjoFullIdResolver 查找 std.array 包的元数据文件
 *   ├─ 检查项目依赖
 *   ├─ 查找 std-array.cjo 或 std-array.cjb
 *   └─ 读取元数据获取 Array 类型定义
 *   ↓
 * 返回 ClassDeclWrapper(Array<T>)
 *   ↓
 * 使用完整类型信息构建 Stub
 * ```
 *
 * ### 依赖 CjoPackageService
 *
 * [CjoPackageService] 是项目级服务，依赖 Workspace Model 同步完成：
 * - **问题**: IDE 启动早期，Workspace Model 可能未就绪
 * - **影响**: 跨包引用解析可能失败，导致 Stub 不完整
 * - **解决**: ClassFileStubBuilder 实现延迟构建机制（详见其文档）
 *
 * ### 降级策略
 *
 * 如果跨包引用解析失败：
 * - 使用简化的类型表示（仅保留类名）
 * - Stub 构建继续，不阻塞索引
 * - 后续重新索引时修正
 *
 * ## 错误处理
 *
 * ### readSafely 扩展函数
 *
 * 处理虚拟文件读取的各种异常情况：
 * ```kotlin
 * inline fun <T> VirtualFile.readSafely(action: () -> T): T? = try {
 *     if (isValid) {
 *         action()
 *     } else {
 *         null
 *     }
 * } catch (_: IOException) {
 *     null
 * }
 * ```
 *
 * **异常场景**:
 * 1. **文件无效** (`!isValid`): 文件已被删除或移动
 * 2. **IOException**:
 *    - 文件不存在（VirtualFile 指向不存在的 .jar 条目）
 *    - 磁盘 I/O 错误
 *    - 文件权限问题
 *
 * **为什么不调用 refresh()**:
 * - `refresh()` 在某些上下文不允许调用（ReadAction 中）
 * - 性能开销大（需要同步文件系统）
 * - 捕获异常是更轻量的方式
 *
 * ### requireWithAttachment
 *
 * 带附加信息的断言检查：
 * ```kotlin
 * requireWithAttachment(isSupported(virtualFile), { "Unexpected file type" }) {
 *     withVirtualFileEntry("file", virtualFile)
 * }
 * ```
 *
 * 失败时抛出 IllegalArgumentException，附带：
 * - 错误消息: "Unexpected file type"
 * - 附加信息: 文件路径、文件类型、文件系统
 *
 * ### Stub 构建失败处理
 *
 * | 失败场景 | 返回值 | IDE 行为 |
 * |---------|-------|----------|
 * | 文件类型不支持 | 抛异常 | 记录错误日志 |
 * | 文件读取失败 (IOException) | null | 不创建 Stub，跳过索引 |
 * | 元数据版本不兼容 | Incompatible Stub | 显示错误消息 |
 * | 元数据解析失败 | null | 不创建 Stub |
 *
 * ## 性能优化
 *
 * ### 延迟解析策略
 *
 * **Stub vs PSI**:
 * - **Stub**: 轻量级结构（仅保留索引需要的信息）
 * - **PSI**: 完整语法树（包含所有细节）
 *
 * **构建策略**:
 * - Stub 在索引阶段构建（一次性，缓存到磁盘）
 * - PSI 在需要时从 Stub 或源码构建（按需）
 *
 * ### 嵌套类过滤
 *
 * ```kotlin
 * classesToDecompile = package.allClassDecls.filter { decl ->
 *     !decl.classId.isNestedClass  // 排除 Outer$Inner
 * }
 * ```
 *
 * **好处**:
 * - 避免重复构建（嵌套类由父类 Stub 负责）
 * - 减少顶层 Stub 数量
 * - 加速索引查询
 *
 * ### ClassDataFinder 缓存
 *
 * [FlatBuffersBasedClassDataFinder] 内部缓存类数据：
 * - 首次查询解析元数据
 * - 后续查询直接返回缓存
 * - 避免重复解析相同类
 *
 * ## 线程安全
 *
 * ### ReadAction 要求
 *
 * Stub 构建必须在 ReadAction 中执行（由 IDE 框架保证）：
 * - 访问 VirtualFile
 * - 访问 Project 服务
 * - 读取文件内容
 *
 * ### 无状态设计
 *
 * 本类是无状态的（抽象属性除外）：
 * - `buildFileStub()` 无副作用
 * - 所有数据通过参数传递
 * - 可安全并发调用
 *
 * ### CjoPackageService 线程安全
 *
 * [CjoPackageService] 内部使用线程安全的缓存：
 * - 并发读取安全
 * - 更新时使用锁保护
 *
 * ## 子类实现要求
 *
 * ### 最小实现
 *
 * ```kotlin
 * class MyMetadataStubBuilder : CangJieMetadataStubBuilder() {
 *     // 1. 定义支持的文件类型
 *     override val supportedFileType: FileType = MyFileType
 *
 *     // 2. 定义期望的二进制版本
 *     override val expectedBinaryVersion: BinaryVersion =
 *         BinaryVersion(major = 1, minor = 0, patch = 0)
 *
 *     // 3. 实现元数据读取逻辑
 *     override fun readFile(virtualFile: VirtualFile, content: ByteArray?): FileWithMetadata? {
 *         val bytes = content ?: virtualFile.contentsToByteArray()
 *
 *         // 解析 Flatbuffers 元数据
 *         val packageWrapper = parseMetadata(bytes)
 *         val version = packageWrapper.metadataVersion
 *
 *         // 检查版本兼容性
 *         return if (version.isCompatible(expectedBinaryVersion)) {
 *             FileWithMetadata.Compatible(
 *                 package = packageWrapper,
 *                 version = version,
 *                 serializerProtocol = MySerializerProtocol
 *             )
 *         } else {
 *             FileWithMetadata.Incompatible(version)
 *         }
 *     }
 * }
 * ```
 *
 * ### 可选覆盖
 *
 * ```kotlin
 * // 自定义元数据存在检查（优化性能）
 * override fun hasMetadata(virtualFile: VirtualFile): Boolean {
 *     // 快速检查文件签名，避免完整解析
 *     return virtualFile.inputStream.use { stream ->
 *         stream.read() == EXPECTED_MAGIC_NUMBER
 *     }
 * }
 *
 * // 自定义 ClassDataFinder（添加额外功能）
 * override fun classDataFinder(original: ClassDataFinder, file: VirtualFile): ClassDataFinder {
 *     return CachedClassDataFinder(original)
 * }
 * ```
 *
 * ## 使用示例
 *
 * ### 示例 1: 内置库 Stub 构建
 *
 * ```kotlin
 * // CangJieBuiltInMetadataStubBuilder 实现
 * object CangJieBuiltInMetadataStubBuilder : CangJieMetadataStubBuilder() {
 *     override val supportedFileType = CangJieBuiltInFileType
 *     override val expectedBinaryVersion = BinaryVersion(1, 0, 0)
 *
 *     override fun readFile(virtualFile: VirtualFile, content: ByteArray?): FileWithMetadata? {
 *         // 读取 .cjb 文件
 *         val bytes = content ?: virtualFile.contentsToByteArray()
 *         val packageWrapper = BuiltInMetadataParser.parse(bytes)
 *
 *         return FileWithMetadata.Compatible(
 *             package = packageWrapper,
 *             version = packageWrapper.metadataVersion,
 *             serializerProtocol = BuiltInSerializerProtocol
 *         )
 *     }
 * }
 * ```
 *
 * ### 示例 2: 版本检查逻辑
 *
 * ```kotlin
 * override fun readFile(virtualFile: VirtualFile, content: ByteArray?): FileWithMetadata? {
 *     val packageWrapper = parseMetadata(content ?: virtualFile.contentsToByteArray())
 *     val version = packageWrapper.metadataVersion
 *
 *     return when {
 *         version > expectedBinaryVersion -> {
 *             // 版本过高，不兼容
 *             FileWithMetadata.Incompatible(version)
 *         }
 *         version.isCompatible(expectedBinaryVersion) -> {
 *             // 版本兼容
 *             FileWithMetadata.Compatible(packageWrapper, version, serializerProtocol)
 *         }
 *         else -> {
 *             // 版本过低，也视为不兼容
 *             FileWithMetadata.Incompatible(version)
 *         }
 *     }
 * }
 * ```
 *
 * ## 已知限制
 *
 * 1. **时序依赖**: 依赖 CjoPackageService 初始化完成，早期调用可能导致跨包引用解析失败
 * 2. **单次构建**: Stub 构建失败后不自动重试，需要手动触发重新索引
 * 3. **黑名单维护**: BLACK_LIST 是硬编码的，新增内部类需要手动添加
 * 4. **嵌套类限制**: 仅过滤第一层嵌套类，多层嵌套需要递归过滤
 *
 * ## 调试技巧
 *
 * ### 查看 Stub 构建过程
 *
 * ```kotlin
 * override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *     LOG.debug("Building stub for: ${fileContent.fileName}")
 *     val result = super.buildFileStub(fileContent)
 *     LOG.debug("Stub tree size: ${result?.childrenStubs?.size}")
 *     return result
 * }
 * ```
 *
 * ### 验证跨包引用解析
 *
 * ```kotlin
 * private fun buildCompatibleFileStub(...): PsiFileStub<*> {
 *     val packageService = CjoPackageService.getInstance(project)
 *     LOG.debug("CjoPackageService initialized: ${packageService.isInitialized}")
 *     // ...
 * }
 * ```
 *
 * ### 检查元数据版本
 *
 * ```kotlin
 * override fun readFile(...): FileWithMetadata? {
 *     val version = packageWrapper.metadataVersion
 *     LOG.debug("Metadata version: $version, expected: $expectedBinaryVersion")
 *     // ...
 * }
 * ```
 *
 * @param supportedFileType 子类支持的文件类型
 * @param expectedBinaryVersion 子类期望的二进制版本
 *
 * @see CangJieBuiltInMetadataStubBuilder
 * @see FileWithMetadata
 * @see ClsStubBuilderComponents
 * @see CjoPackageService
 * @see ClassFileStubBuilder
 */
private inline fun <T> VirtualFile.readSafely(action: () -> T): T? = try {
    if (isValid) {
        action()
    } else {
        null
    }
} catch (_: IOException) {
    // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
    // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
    // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
    // it's not always allowed and also is likely to degrade performance
    null
}


abstract class CangJieMetadataStubBuilder : ClsStubBuilder() {
    protected abstract val supportedFileType: FileType
    protected abstract val expectedBinaryVersion: BinaryVersion
    protected abstract fun readFile(virtualFile: VirtualFile, content: ByteArray?): FileWithMetadata?

    /**
     * Whether [readFile] is expected to have a not null result
     */
    protected open fun hasMetadata(virtualFile: VirtualFile): Boolean = readFile(virtualFile, null) != null

    /**
     * Whether the [file] is supported, so it might have a stub
     */
    fun isSupported(file: VirtualFile): Boolean {
        val supportedType = supportedFileType
        return file.extension == supportedType.defaultExtension || file.fileType == supportedType
    }

    /**
     * Whether the [file] would have a stub as the result of [buildFileStub]
     */
    fun hasStub(file: VirtualFile): Boolean = isSupported(file) && file.readSafely { hasMetadata(file) } == true

    fun readFileSafely(file: VirtualFile, content: ByteArray? = null): FileWithMetadata? = file.readSafely {
        readFile(file, content)
    }

    final override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
        val virtualFile = fileContent.file
        requireWithAttachment(isSupported(virtualFile), { "Unexpected file type" }) {
            withVirtualFileEntry("file", virtualFile)
        }

        val file = readFileSafely(virtualFile, fileContent.content) ?: return null

        // 从 FileContent 获取 project
        val project = fileContent.project ?: ProjectManager.getInstance().defaultProject


        return when (file) {
            is FileWithMetadata.Incompatible -> {
                createIncompatibleAbiVersionFileStub()
            }

            is FileWithMetadata.Compatible -> {
                buildCompatibleFileStub(file, virtualFile, project)
            }
        }
    }


    /**
     * 构建兼容版本的文件 Stub
     *
     * ## 核心流程
     *
     * 1. **初始化 CjoPackageService**: 获取项目级包服务实例，用于跨包引用解析
     * 2. **创建 ClsStubBuilderComponents**: 组装 Stub 构建所需的所有组件
     *    - ClassDataFinder: 基于 Flatbuffers 的类数据查找器
     *    - DeclTable/TypeTable: 声明和类型表，来自元数据文件
     *    - CjoPackageService: 用于解析跨包 FullId 引用
     * 3. **创建 Stub 上下文**: 包含包名、类型表等上下文信息
     * 4. **构建声明 Stubs**: 按顺序构建包中的所有声明
     *
     * ## 跨包引用处理
     *
     * 当遇到跨包类型引用（如 `std.collection.ArrayList`）时：
     * - TypeClsStubBuilder 通过 FullId 解析类型
     * - 调用 CjoPackageService.resolveFullId()
     * - CjoFullIdResolver 查找导入包的元数据
     * - 返回完整的类型信息用于 Stub 构建
     *
     * ## 注意事项
     *
     * **时序依赖**: 该方法依赖 CjoPackageService 已完成初始索引构建。
     * 如果在 IDE 启动早期调用，可能遇到包服务未就绪的情况，
     * 导致跨包引用解析失败（详见 CjoFullIdResolverImpl 的降级处理）。
     *
     * @param file 兼容的元数据文件信息
     * @param virtualFile 元数据虚拟文件
     * @param project 当前项目实例
     * @return 构建完成的文件 Stub
     */
    private fun buildCompatibleFileStub(
        file: CangJieMetadataStubBuilder.FileWithMetadata.Compatible,
        virtualFile: VirtualFile,
        project: Project
    ): PsiFileStub<*> {
        val packageWrapper = file.`package`
        val packageFqName = file.packageFqName

        // 获取包服务
        val packageService = CjoPackageService.getInstance(project)


        // 创建组件
        val components = ClsStubBuilderComponents(
            project = project,
            classDataFinder = FlatBuffersBasedClassDataFinder(
                packageWrapper,
                file.version
            ),
            virtualFileForDebug = virtualFile,
            declTable = packageWrapper.declTable,
            typeTable = packageWrapper.typeTable,
            packageWrapper = packageWrapper,
            packageService = packageService
        )

        // 创建上下文
        val context = components.createContext(packageFqName, packageWrapper.typeTable)

        // 创建文件 Stub
        val fileStub = createFileStub(packageFqName)

        // 创建包容器
        val metadataContainer = MetadataContainer.Package(packageFqName, packageWrapper.typeTable)

        // 创建顶层函数和变量 Stubs
        createPackageDeclarationsStubs(
            fileStub,
            context,
            metadataContainer,
            packageWrapper.functions,
            packageWrapper.variables
        )

        // 创建类声明 Stubs
        for (classDecl in file.classesToDecompile) {
            createClassStub(fileStub, classDecl, context)
        }

        // 创建扩展声明 Stubs
        for (extend in packageWrapper.extends) {
            ExtendClsStubBuilder(fileStub, context, extend).build()
        }

        // 创建类型别名 Stubs
        for (typeAlias in packageWrapper.typeAliass) {
            TypeAliasClsStubBuilder(fileStub, context, typeAlias).build()
        }

        return fileStub
    }

    protected open fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement? = null
    protected open fun classDataFinder(original: ClassDataFinder, file: VirtualFile): ClassDataFinder = original

    /**
     * 用于保存从元数据文件读取的信息
     */
    sealed class FileWithMetadata {
        /**
         * 不兼容的元数据版本
         */
        class Incompatible(val version: BinaryVersion) : FileWithMetadata()

        /**
         * 兼容的元数据，包含包信息和版本
         */
        open class Compatible(
            val `package`: PackageWrapper,
            val version: BinaryVersion,
            val serializerProtocol: SerializerExtensionFlatbuffers
        ) : FileWithMetadata() {
            val packageFqName = `package`.packageName

            /**
             * 需要反编译的类列表，过滤掉嵌套类和黑名单中的类
             */
            open val classesToDecompile: List<ClassDeclWrapper> =
                `package`.allClassDecls.filter { decl ->
                    !decl.classId.isNestedClass && decl.classId !in BLACK_LIST
                }
        }
    }
}


fun ExceptionAttachmentBuilder.withVirtualFileEntry(name: String, virtualFile: VirtualFile?) {
    withEntry(name, virtualFile) { file ->
        "path: ${file.path}, filetype: ${file.fileType} ,filesystem,${file.fileSystem}"
    }
}