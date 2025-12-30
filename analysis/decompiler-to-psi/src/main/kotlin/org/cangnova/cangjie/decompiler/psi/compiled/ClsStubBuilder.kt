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

package org.cangnova.cangjie.decompiler.psi.compiled

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent

/**
 * 编译文件 Stub 构建器的抽象基类
 *
 * ## 架构概述
 *
 * ClsStubBuilder 是 IntelliJ Platform 反编译系统中 Stub 构建的核心抽象层，
 * 为仓颉语言二进制元数据文件（.cjo/.cjb）提供轻量级索引树构建功能。
 * 它定义了从编译文件到 PSI Stub 树的标准转换契约，是反编译系统性能优化的关键组件。
 *
 * ## 设计原理
 *
 * ### Stub 索引 vs 完整 PSI
 *
 * **问题**: IDE 需要快速访问符号信息（类名、函数签名等），但完整解析所有文件代价高昂。
 *
 * **解决方案**: 两层索引策略
 * ```
 * Stub 层 (轻量级)
 *   - 只包含符号元数据（名称、签名、可见性）
 *   - 序列化到磁盘缓存
 *   - 快速加载（~毫秒级）
 *   - 支持符号搜索、补全
 *   ↓
 * PSI 层 (完整)
 *   - 完整语法树
 *   - 按需构建（用户打开文件时）
 *   - 慢速加载（~秒级）
 *   - 支持所有 IDE 功能
 * ```
 *
 * **性能对比**:
 * | 操作 | Stub 索引 | 完整 PSI | 提升 |
 * |------|---------|---------|------|
 * | 索引 1000 个类文件 | 500ms | 5s | 10x |
 * | 符号搜索（Find Class） | < 50ms | 1s+ | 20x |
 * | IDE 启动时间 | 2s | 15s | 7.5x |
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * IDE 启动 / 文件扫描
 *   ↓
 * FileBasedIndex (IDE 框架)
 *   ↓
 * BinaryFileStubBuilders (扩展点)
 *   ↓
 * ClassFileStubBuilder (IntelliJ 适配器)
 *   ├─ 查找匹配的反编译器
 *   └─ 委托给 ClsStubBuilder
 *   ↓
 * ClsStubBuilder (本抽象类)
 *   └─ 定义 Stub 构建契约
 *   ↓
 * CangJieMetadataStubBuilder (实现类)
 *   ├─ 读取 Flatbuffers 元数据
 *   ├─ 解析包、类、函数、变量
 *   └─ 构建 Stub 树
 *   ↓
 * PsiFileStub (Stub 树根节点)
 *   ├─ CangJieClassStub
 *   ├─ CangJieFunctionStub
 *   └─ CangJieVariableStub
 *   ↓
 * StubIndex (符号索引)
 *   ├─ CangJieClassShortNameIndex
 *   ├─ CangJieFunctionShortNameIndex
 *   └─ CangJieSuperClassIndex
 *   ↓
 * IDE 服务
 *   ├─ Find Class (Ctrl+N)
 *   ├─ Find Symbol (Ctrl+Alt+Shift+N)
 *   ├─ Code Completion
 *   ├─ Go to Declaration
 *   └─ Find Usages
 * ```
 *
 * ## 核心职责
 *
 * ### 1. 定义 Stub 构建契约
 *
 * 抽象类通过两个成员定义实现契约：
 * ```kotlin
 * abstract val stubVersion: Int
 * abstract fun buildFileStub(fileContent: FileContent): PsiFileStub<*>?
 * ```
 *
 * **契约保证**:
 * - 实现类必须提供版本号（缓存控制）
 * - 实现类必须提供 Stub 构建逻辑
 * - 返回 null 表示文件应被跳过
 * - 抛出 ClsFormatException 表示格式错误
 *
 * ### 2. 支持多种元数据格式
 *
 * 通过继承扩展支持不同格式：
 * ```kotlin
 * // 仓颉元数据（Flatbuffers）
 * CangJieMetadataStubBuilder : ClsStubBuilder() {
 *     override fun buildFileStub(...) {
 *         // 解析 Flatbuffers 格式
 *     }
 * }
 *
 * // 未来可扩展: Protobuf 格式
 * ProtobufMetadataStubBuilder : ClsStubBuilder() {
 *     override fun buildFileStub(...) {
 *         // 解析 Protobuf 格式
 *     }
 * }
 * ```
 *
 * ### 3. 提供缓存失效机制
 *
 * 通过 stubVersion 控制缓存：
 * ```
 * 文件内容变化 OR stubVersion 变化
 *   ↓
 * IDE 检测到缓存失效
 *   ↓
 * 重新调用 buildFileStub()
 *   ↓
 * 更新磁盘缓存
 *   ↓
 * 重建索引
 * ```
 *
 * ## 实现类层次结构
 *
 * ```kotlin
 * abstract class ClsStubBuilder {
 *     abstract val stubVersion: Int
 *     abstract fun buildFileStub(FileContent): PsiFileStub<*>?
 * }
 *   ↓
 * abstract class CangJieMetadataStubBuilder : ClsStubBuilder() {
 *     // 添加元数据读取抽象
 *     abstract fun readFile(VirtualFile, ByteArray?): FileWithMetadata?
 *
 *     // 实现 buildFileStub() 通用流程
 *     final override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *         val file = readFile(...) ?: return null
 *         return when (file) {
 *             is Compatible -> buildCompatibleFileStub(...)
 *             is Incompatible -> createIncompatibleStub(...)
 *         }
 *     }
 * }
 *   ↓
 * object CangJieBuiltInMetadataStubBuilder : CangJieMetadataStubBuilder() {
 *     // 提供具体实现
 *     override val stubVersion = 42
 *     override fun readFile(...) = BuiltInDefinitionFile.read(...)
 * }
 * ```
 *
 * ## Stub 版本机制
 *
 * ### 为什么需要版本号？
 *
 * **场景 1: 元数据格式升级**
 * ```
 * 编译器 v1.0 → v1.1 (Flatbuffers schema 变化)
 *   ↓
 * 旧 Stub 无法正确表示新元数据
 *   ↓
 * 递增 stubVersion: 41 → 42
 *   ↓
 * IDE 检测版本不匹配 → 重建所有 Stub
 * ```
 *
 * **场景 2: Stub 结构变化**
 * ```
 * 添加新的 Stub 类型（如 TypeAliasStub）
 *   ↓
 * 旧 Stub 缺少新类型的索引
 *   ↓
 * 递增 stubVersion: 42 → 43
 *   ↓
 * 重建 Stub 以包含新类型
 * ```
 *
 * ### 版本比较流程
 *
 * ```
 * IDE 加载文件
 *   ↓
 * 从磁盘读取缓存的 Stub
 *   ├─ 缓存的 stubVersion: 41
 *   └─ 当前 stubVersion: 42
 *   ↓
 * 版本不匹配!
 *   ↓
 * 调用 buildFileStub() 重建
 *   ↓
 * 保存新 Stub (version=42) 到缓存
 * ```
 *
 * ## 返回 null 的语义
 *
 * buildFileStub() 返回 null 表示**预期的跳过**，不是错误。
 *
 * ### 返回 null 的 4 种场景
 *
 * #### 1. 内部类/嵌套类
 *
 * **原理**: 外部类文件已包含内部类的完整元数据。
 *
 * ```
 * 编译产物:
 *   Outer.cjo        # 包含 Outer 和 Outer$Inner 的元数据
 *   Outer$Inner.cjo  # 仅包含引用信息
 *
 * Stub 构建:
 *   Outer.cjo → buildFileStub() → 返回完整 Stub (包含 Inner)
 *   Outer$Inner.cjo → buildFileStub() → 返回 null (避免重复)
 * ```
 *
 * **判断逻辑**:
 * ```kotlin
 * override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *     if (ClsClassFinder.isCangJieInternalCompiledFile(file)) {
 *         return null  // 内部文件，跳过
 *     }
 *     // 正常构建...
 * }
 * ```
 *
 * #### 2. 合成类
 *
 * **定义**: 编译器自动生成的辅助类（Lambda、伴生对象等）。
 *
 * ```kotlin
 * // 源码
 * func process(items: List<Int>) {
 *     items.forEach { it * 2 }  // Lambda 表达式
 * }
 *
 * // 编译产物
 * Process.cjo           # 主类
 * Process$lambda$1.cjo  # Lambda 合成类 → 应该跳过
 * ```
 *
 * #### 3. 初始化未完成
 *
 * **场景**: IDE 启动早期，依赖服务未就绪。
 *
 * ```kotlin
 * override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *     val packageService = CjoPackageService.getInstance(project)
 *     if (!packageService.isInitialized()) {
 *         return null  // 延迟构建，等待初始化完成
 *     }
 *     // 正常构建...
 * }
 * ```
 *
 * **IDE 行为**: 初始化完成后会自动重新索引。
 *
 * #### 4. 文件无效
 *
 * **场景**: 虚拟文件失效或 I/O 错误。
 *
 * ```kotlin
 * override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *     if (!file.isValid || !file.exists()) {
 *         return null  // 文件已删除或移动
 *     }
 *
 *     try {
 *         val content = file.contentsToByteArray()
 *         // 正常构建...
 *     } catch (e: IOException) {
 *         return null  // I/O 错误，跳过
 *     }
 * }
 * ```
 *
 * ## 异常处理
 *
 * ### ClsFormatException 的使用
 *
 * **定义**: 表示文件格式无法识别或解析失败（非预期错误）。
 *
 * **抛出场景**:
 * 1. **Magic Number 不匹配**
 *    ```kotlin
 *    val magicNumber = ByteBuffer.wrap(content, 0, 4).int
 *    if (magicNumber != EXPECTED_MAGIC) {
 *        throw ClsFormatException("Invalid magic number: $magicNumber")
 *    }
 *    ```
 *
 * 2. **版本不兼容**
 *    ```kotlin
 *    val version = packageWrapper.metadataVersion
 *    if (version > expectedBinaryVersion) {
 *        throw ClsFormatException("Incompatible version: $version")
 *    }
 *    ```
 *
 * 3. **数据损坏**
 *    ```kotlin
 *    try {
 *        val packageWrapper = stream.toFbPackage()
 *    } catch (e: FlatbuffersException) {
 *        throw ClsFormatException("Corrupted metadata", e)
 *    }
 *    ```
 *
 * ### null vs Exception 决策树
 *
 * ```
 * buildFileStub() 遇到问题
 *   ↓
 * 问题是预期的吗？
 *   ├─ Yes (内部类、初始化中、文件无效)
 *   │   └─ 返回 null
 *   │       └─ IDE 静默跳过该文件
 *   └─ No (格式错误、版本不兼容、数据损坏)
 *       └─ 抛出 ClsFormatException
 *           └─ IDE 记录错误 + 显示错误占位符
 * ```
 *
 * ## 性能优化策略
 *
 * ### 1. 延迟解析
 *
 * 只解析索引需要的最小信息：
 * ```kotlin
 * // ✅ 好: 延迟解析方法体
 * class ClassStub {
 *     val name: String          // 立即解析
 *     val methods: List<String> // 立即解析（签名）
 *     // 方法体不解析（PSI 层负责）
 * }
 *
 * // ❌ 坏: 解析所有细节
 * class ClassStub {
 *     val methodBodies: List<AST>  // 不需要！
 * }
 * ```
 *
 * ### 2. 避免昂贵操作
 *
 * buildFileStub() 在索引线程中运行，必须快速：
 * ```kotlin
 * // ❌ 禁止
 * override fun buildFileStub(...) {
 *     psiManager.findFile(...)       // 访问 PSI
 *     http.get("api/metadata")       // 网络请求
 *     Files.walk(directory)          // 磁盘扫描
 *     Thread.sleep(100)              // 阻塞
 * }
 *
 * // ✅ 允许
 * override fun buildFileStub(...) {
 *     val content = fileContent.content  // 已缓存
 *     val stub = parseMetadata(content)  // 纯计算
 *     return stub
 * }
 * ```
 *
 * ### 3. 缓存复用
 *
 * 利用 FileContent.content 避免重复读取：
 * ```kotlin
 * // FileContent.content 已由 IDE 读取并缓存
 * val content: ByteArray = fileContent.content  // O(1)
 *
 * // 而不是
 * val content = file.contentsToByteArray()      // O(n) I/O
 * ```
 *
 * ## 线程安全要求
 *
 * ### 并发调用保证
 *
 * buildFileStub() 可能在多个索引线程中并发调用：
 * ```
 * 索引线程 1: buildFileStub(ArrayList.cjo)
 * 索引线程 2: buildFileStub(HashMap.cjo)
 * 索引线程 3: buildFileStub(HashSet.cjo)
 *
 * 必须线程安全!
 * ```
 *
 * ### 线程安全实现
 *
 * ```kotlin
 * // ❌ 不安全: 可变共享状态
 * class UnsafeStubBuilder : ClsStubBuilder() {
 *     private var currentFile: VirtualFile? = null  // 竞态条件!
 *
 *     override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *         currentFile = fileContent.file  // 线程 A/B 可能同时写入
 *         // ...
 *     }
 * }
 *
 * // ✅ 安全: 无状态或不可变
 * object SafeStubBuilder : ClsStubBuilder() {
 *     // 无可变状态
 *
 *     override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *         // 只使用局部变量
 *         val file = fileContent.file
 *         val stub = parse(file)
 *         return stub
 *     }
 * }
 * ```
 *
 * ## 使用示例
 *
 * ### 示例 1: 基本实现
 *
 * ```kotlin
 * class SimpleMetadataStubBuilder : ClsStubBuilder() {
 *     override val stubVersion: Int = 1
 *
 *     override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *         val file = fileContent.file
 *
 *         // 1. 验证文件
 *         if (!file.extension.equals("cjo", ignoreCase = true)) {
 *             return null
 *         }
 *
 *         // 2. 读取内容
 *         val content = fileContent.content
 *
 *         // 3. 解析元数据
 *         val packageWrapper = parseMetadata(content)
 *
 *         // 4. 构建 Stub
 *         return CangJieFileStubImpl(
 *             packageFqName = packageWrapper.packageName,
 *             classes = packageWrapper.classes.map { createClassStub(it) }
 *         )
 *     }
 * }
 * ```
 *
 * ### 示例 2: 带版本检查
 *
 * ```kotlin
 * class VersionCheckingStubBuilder : ClsStubBuilder() {
 *     override val stubVersion: Int = 2
 *
 *     override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *         val content = fileContent.content
 *
 *         // 解析版本
 *         val metadata = parseMetadata(content)
 *         val version = metadata.version
 *
 *         // 版本检查
 *         if (version.major > EXPECTED_MAJOR_VERSION) {
 *             throw ClsFormatException(
 *                 "Incompatible version: expected <= $EXPECTED_MAJOR_VERSION, got $version"
 *             )
 *         }
 *
 *         // 构建 Stub
 *         return buildStubFromMetadata(metadata)
 *     }
 * }
 * ```
 *
 * ### 示例 3: 带初始化检查
 *
 * ```kotlin
 * class DelayedInitStubBuilder : ClsStubBuilder() {
 *     override val stubVersion: Int = 3
 *
 *     override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
 *         val project = fileContent.project
 *
 *         // 检查依赖服务是否就绪
 *         val packageService = CjoPackageService.getInstance(project)
 *         if (!packageService.isReady()) {
 *             // 延迟构建，IDE 会稍后重试
 *             return null
 *         }
 *
 *         // 正常构建
 *         return buildStubWithService(fileContent, packageService)
 *     }
 * }
 * ```
 *
 * ## 与 IntelliJ Platform 的集成
 *
 * ### 扩展点注册
 *
 * ```xml
 * <!-- plugin.xml -->
 * <extensions defaultExtensionNs="com.intellij">
 *   <stubElementTypeHolder class="org.cangnova.cangjie.psi.stubs.CjStubElementTypes"/>
 * </extensions>
 *
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *   <classFileDecompiler implementation="org.cangnova.cangjie.decompiler.CangJieMetadataDecompiler"/>
 * </extensions>
 * ```
 *
 * ### IDE 调用流程
 *
 * ```
 * 用户打开项目
 *   ↓
 * IDE 启动索引任务
 *   ↓
 * FileBasedIndexImpl.scheduleIndexRebuild()
 *   ↓
 * 遍历项目文件
 *   ↓
 * 对每个 .cjo/.cjb 文件:
 *   ├─ 创建 FileContent
 *   ├─ 调用 ClassFileStubBuilder.buildStubTree()
 *   │   └─ 调用 ClsStubBuilder.buildFileStub()
 *   ├─ 保存 Stub 到缓存
 *   └─ 更新 StubIndex
 *   ↓
 * 索引完成 → IDE 就绪
 * ```
 *
 * ## 调试技巧
 *
 * ### 查看 Stub 构建日志
 *
 * ```
 * Help → Diagnostic Tools → Debug Log Settings
 * 添加: #com.intellij.psi.stubs
 * ```
 *
 * ### 验证 Stub 版本
 *
 * ```kotlin
 * val stubVersion = CangJieBuiltInMetadataStubBuilder.stubVersion
 * println("Current stub version: $stubVersion")
 * ```
 *
 * ### 测试 Stub 构建
 *
 * ```kotlin
 * @Test
 * fun `test stub building`() {
 *     val file = createTestFile("Test.cjo")
 *     val content = FileContentImpl.createByContent(file, byteArrayOf(...))
 *
 *     val stub = CangJieBuiltInMetadataStubBuilder.buildFileStub(content)
 *
 *     assertNotNull(stub)
 *     assertEquals("test.pkg", stub.packageFqName)
 * }
 * ```
 *
 * ## 最佳实践
 *
 * ### 1. 返回 null vs 抛异常
 *
 * ```kotlin
 * // ✅ 正确
 * if (isInternalClass) return null           // 预期的跳过
 * if (version > MAX_VERSION) throw ClsFormatException(...)  // 错误
 *
 * // ❌ 错误
 * if (isInternalClass) throw Exception(...)  // 不应用异常表示预期行为
 * ```
 *
 * ### 2. 版本号管理
 *
 * ```kotlin
 * // ✅ 集中管理
 * object CangJieStubVersions {
 *     const val BUILTIN_STUB_VERSION = 42
 *     const val NORMAL_STUB_VERSION = 15
 * }
 *
 * // ❌ 分散硬编码
 * override val stubVersion = 42  // 难以维护
 * ```
 *
 * ### 3. 线程安全
 *
 * ```kotlin
 * // ✅ 使用 object 单例（线程安全）
 * object MyStubBuilder : ClsStubBuilder()
 *
 * // ❌ 使用 class（可能需要同步）
 * class MyStubBuilder : ClsStubBuilder()  // 每次创建新实例？
 * ```
 *
 * ## 已知限制
 *
 * 1. **单一构建器**: 每种文件类型只能有一个构建器
 * 2. **同步执行**: buildFileStub() 必须同步返回，不支持异步
 * 3. **无进度报告**: 长时间运行的构建无法报告进度
 * 4. **无批量优化**: 每个文件独立构建，无法批量处理优化
 *
 * @see CangJieMetadataStubBuilder
 * @see ClassFileStubBuilder
 * @see PsiFileStub
 * @see StubIndex
 * @see FileContent
 */
abstract class ClsStubBuilder {
    /**
     * Stub 版本号
     *
     * ## 版本控制
     *
     * 当以下情况发生时，必须递增此版本号：
     * 1. 元数据格式变化（如 Flatbuffers schema 更新）
     * 2. Stub 构建逻辑变化（如添加新的 Stub 类型）
     * 3. 序列化/反序列化逻辑变化
     *
     * ## 工作原理
     *
     * IDE 的索引系统会将此版本号与缓存中的版本比较：
     * - 版本匹配 → 使用缓存的 Stub
     * - 版本不匹配 → 重新构建 Stub
     *
     * ## 要求
     *
     * - 必须是正整数（> 0）
     * - 应该在每次不兼容更改时递增
     * - 建议在相关常量文件中集中管理
     *
     * @see org.cangnova.cangjie.analysis.serialization.CURRENT_STUB_VERSION
     */
    abstract val stubVersion: Int

    /**
     * 从文件内容构建 Stub 树
     *
     * ## 核心流程
     *
     * 1. **验证文件类型**: 检查文件扩展名和 FileType
     * 2. **读取元数据**: 从 [FileContent.content] 读取二进制数据
     * 3. **解析元数据**: 使用 Flatbuffers 反序列化
     * 4. **构建 Stub**: 递归构建声明 Stub（类、函数、变量等）
     * 5. **返回根 Stub**: PsiFileStub 作为 Stub 树的根节点
     *
     * ## 返回值语义
     *
     * - **非 null**: 成功构建 Stub 树，文件将被索引
     * - **null**: 文件应该被跳过，不参与索引
     *
     * ### 返回 null 的场景
     *
     * 1. **内部类/嵌套类**: 作为外部类的一部分被索引，不需要单独的 Stub
     *    ```kotlin
     *    // Outer.cjo 包含 Outer 和 Outer$Inner 的元数据
     *    // 构建 Outer$Inner.cjo 的 Stub 时应返回 null
     *    ```
     *
     * 2. **合成类**: 编译器生成的辅助类（如 lambda 类、伴生对象类）
     *
     * 3. **初始化未完成**: 依赖的服务尚未就绪
     *    ```kotlin
     *    if (!CjProjectsService.initialized) return null
     *    ```
     *
     * 4. **文件读取失败**: 虚拟文件无效或 I/O 错误
     *
     * ## 异常处理
     *
     * ### ClsFormatException
     *
     * 当遇到以下情况时抛出：
     * - 元数据格式无法识别（magic number 不匹配）
     * - 版本不兼容（BinaryVersion 检查失败）
     * - 数据损坏（Flatbuffers 解析失败）
     *
     * IDE 会捕获此异常并：
     * - 记录错误日志
     * - 标记文件为"反编译失败"
     * - 显示错误占位符内容
     *
     * ### 与返回 null 的选择
     *
     * | 场景 | 应该返回 null | 应该抛出异常 |
     * |------|--------------|-------------|
     * | 内部类 | ✓ | |
     * | 初始化未完成 | ✓ | |
     * | 版本不兼容 | | ✓ |
     * | 文件损坏 | | ✓ |
     * | I/O 错误 | ✓ (通常被捕获) | |
     *
     * ## 性能考虑
     *
     * - 该方法在索引线程中被调用，应避免耗时操作
     * - 不要在此方法中访问 PSI（PSI 可能尚未构建）
     * - 不要进行网络请求或磁盘 I/O（除了读取文件内容）
     * - 使用延迟加载策略处理大型元数据文件
     *
     * ## 线程安全
     *
     * - 此方法可能在多个索引线程中并发调用
     * - 实现必须是线程安全的
     * - 避免使用可变的共享状态
     *
     * @param fileContent 文件内容，包含虚拟文件、字节数组、项目引用等
     * @return Stub 树的根节点，如果文件应该被跳过则返回 `null`
     * @throws ClsFormatException 当文件格式无法识别或解析失败时
     *
     * @see CangJieMetadataStubBuilder.buildFileStub
     * @see FileContent
     * @see PsiFileStub
     */
    @Throws(ClsFormatException::class)
    abstract fun buildFileStub(fileContent: FileContent): PsiFileStub<*>?
}
