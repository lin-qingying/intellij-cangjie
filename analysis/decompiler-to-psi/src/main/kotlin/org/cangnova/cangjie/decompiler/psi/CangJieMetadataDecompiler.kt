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

package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.decompiler.psi.compiled.ClassFileDecompilers
import org.cangnova.cangjie.decompiler.psi.compiled.ClsStubBuilder
import org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile
import org.cangnova.cangjie.decompiler.psi.text.DecompiledText
import org.cangnova.cangjie.decompiler.psi.text.buildDecompiledText
import org.cangnova.cangjie.decompiler.psi.text.createIncompatibleMetadataVersionDecompiledText
import org.cangnova.cangjie.decompiler.psi.text.defaultDecompilerRendererOptions
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.renderer.DescriptorRenderer
import java.io.IOException

/**
 * 仓颉语言元数据反编译器的抽象基类
 *
 * ## 架构概述
 *
 * CangJieMetadataDecompiler 是反编译系统的核心抽象类，定义了所有元数据反编译器的通用行为契约。
 * 它采用模板方法模式，将反编译流程中的公共逻辑固化在 final 方法中，将可变部分留给子类实现。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * ClassFileDecompilers Extension Point
 *   ↓
 * CangJieMetadataDecompiler (本类 - 抽象基类)
 *   ├─ CangJieBuiltInDecompiler (.cjb 内置库反编译器)
 *   ├─ CangJieNormalDecompiler (.cjo 普通编译文件反编译器)
 *   └─ (未来可扩展的反编译器)
 *   ↓
 * CangJieDecompiledFileViewProvider
 *   ↓
 * CjDecompiledFile
 *   ↓
 * IDE 服务 (编辑器、导航、分析等)
 * ```
 *
 * ## 架构演进历史
 *
 * ### 旧架构 (已废弃)
 *
 * ```kotlin
 * abstract class CangJieMetadataDecompiler<V: BinaryVersion>(
 *     val fileType: FileType,
 *     val serializerFlatbuffers: () -> SerializerExtensionFlatbuffers,
 *     val expectedBinaryVersion: V
 * ) : ClassFileDecompilers.Full() {
 *     protected abstract val metadataStubBuilder: CangJieMetadataStubBuilder
 *
 *     // 子类需要实现大量方法
 *     protected abstract fun readFileSafely(file: VirtualFile): FileWithMetadata?
 *     protected abstract fun doReadFile(...): FileWithMetadata?
 *     protected abstract fun buildDecompiledText(...): DecompiledText
 *     protected abstract fun createFile(...): CjDecompiledFile
 *     ...
 * }
 * ```
 *
 * **旧架构的问题**:
 * 1. **过度泛型化**: BinaryVersion 泛型参数增加复杂性，但实际用途有限
 * 2. **职责不清**: 反编译器既负责 Stub 构建，又负责文本生成
 * 3. **重复代码**: 多个子类都实现相似的文件读取和异常处理逻辑
 * 4. **难以扩展**: 添加新的文件类型需要实现大量方法
 * 5. **紧耦合**: 反编译器直接持有序列化器、文件类型等实现细节
 *
 * ### 新架构 (当前)
 *
 * ```kotlin
 * abstract class CangJieMetadataDecompiler : ClassFileDecompilers.Full() {
 *     // 仅 2 个抽象成员
 *     abstract override val stubBuilder : CangJieMetadataStubBuilder
 *     protected abstract fun createFile(viewProvider: CangJieDecompiledFileViewProvider): CjDecompiledFile
 *
 *     // 固化的 final 方法
 *     final override fun accepts(file: VirtualFile): Boolean
 *     final override fun createFileViewProvider(...): CangJieDecompiledFileViewProvider
 * }
 * ```
 *
 * **新架构的优势**:
 * 1. **职责单一**: 反编译器只负责"识别文件"和"创建 PSI 文件"
 * 2. **简化继承**: 子类只需实现 2 个成员，降低 80% 代码量
 * 3. **统一流程**: 所有文件类型共用同一套 ViewProvider 创建逻辑
 * 4. **易于扩展**: 新增文件类型只需提供 StubBuilder 和文件创建逻辑
 * 5. **松耦合**: 实现细节委托给 StubBuilder 和 ViewProvider
 *
 * ### 架构变更的关键洞察
 *
 * **核心洞察**: 反编译器的本质职责是"适配 IDE 的文件系统到 PSI 系统"，而不是"执行反编译逻辑"。
 *
 * - **文件识别**: 委托给 `stubBuilder.isSupported()`
 * - **Stub 构建**: 委托给 `stubBuilder.buildFileStub()`
 * - **文本生成**: 委托给 `buildDecompiledText()`
 * - **ViewProvider 创建**: 统一流程，使用 factory 模式
 *
 * 反编译器只是"胶水代码"，连接各个组件完成完整流程。
 *
 * ## 核心设计模式
 *
 * ### 1. 模板方法模式 (Template Method)
 *
 * **作用**: 定义反编译流程骨架，将可变部分延迟到子类实现。
 *
 * **模板流程**:
 * ```kotlin
 * // 固化的模板方法 (final)
 * fun accepts(file: VirtualFile): Boolean {
 *     return stubBuilder.isSupported(file)  // 调用子类提供的 stubBuilder
 * }
 *
 * fun createFileViewProvider(...): CangJieDecompiledFileViewProvider {
 *     return CangJieDecompiledFileViewProvider(...) { provider ->
 *         if (stubBuilder.hasStub(file)) {  // 调用子类提供的 stubBuilder
 *             createFile(provider)          // 调用子类实现的 createFile
 *         } else {
 *             null
 *         }
 *     }
 * }
 * ```
 *
 * **好处**:
 * - 避免子类重复实现相同逻辑
 * - 确保所有反编译器行为一致
 * - 简化维护和升级
 *
 * ### 2. 工厂方法模式 (Factory Method)
 *
 * **作用**: createFile() 是工厂方法，子类决定创建哪种 PSI 文件。
 *
 * ```kotlin
 * // 基类定义工厂方法签名
 * protected abstract fun createFile(viewProvider: CangJieDecompiledFileViewProvider): CjDecompiledFile
 *
 * // 子类 1: 创建内置库文件
 * class CangJieBuiltInDecompiler : CangJieMetadataDecompiler() {
 *     override fun createFile(viewProvider) = CangJieBuiltInDecompiledFile(viewProvider)
 * }
 *
 * // 子类 2: 创建普通编译文件
 * class CangJieNormalDecompiler : CangJieMetadataDecompiler() {
 *     override fun createFile(viewProvider) = CjDecompiledFile(viewProvider)
 * }
 * ```
 *
 * **好处**:
 * - 符合开闭原则 (对扩展开放，对修改关闭)
 * - 每种文件类型可以有独立的 PSI 文件实现
 *
 * ### 3. 策略模式 (Strategy)
 *
 * **作用**: stubBuilder 是策略对象，封装了 Stub 构建算法。
 *
 * ```kotlin
 * // 策略接口
 * abstract class CangJieMetadataStubBuilder : ClsStubBuilder {
 *     abstract fun isSupported(file: VirtualFile): Boolean
 *     abstract fun buildFileStub(fileContent: FileContent): CangJieFileStubImpl?
 *     abstract fun hasStub(file: VirtualFile): Boolean
 * }
 *
 * // 具体策略 1: 内置库 Stub 构建
 * object CangJieBuiltInMetadataStubBuilder : CangJieMetadataStubBuilder() {
 *     override fun isSupported(file: VirtualFile) = file.extension == "cjb"
 *     // ...
 * }
 *
 * // 具体策略 2: 普通文件 Stub 构建
 * object CangJieNormalMetadataStubBuilder : CangJieMetadataStubBuilder() {
 *     override fun isSupported(file: VirtualFile) = file.extension == "cjo"
 *     // ...
 * }
 * ```
 *
 * **好处**:
 * - 算法可互换
 * - 避免条件分支
 * - 易于测试
 *
 * ### 4. 延迟初始化模式 (Lazy Initialization via Factory)
 *
 * **作用**: createFileViewProvider() 使用 factory 函数延迟 PSI 文件创建。
 *
 * ```kotlin
 * CangJieDecompiledFileViewProvider(manager, file, physical) { provider ->
 *     // 这个 lambda 不会立即执行
 *     // 只有在 IDE 需要 PSI 文件时才调用
 *     if (stubBuilder.hasStub(provider.virtualFile)) {
 *         createFile(provider)
 *     } else {
 *         null
 *     }
 * }
 * ```
 *
 * **好处**:
 * - 避免创建无用的 PSI 文件
 * - 减少启动时间和内存占用
 * - 按需加载
 *
 * ## 完整工作流程
 *
 * ### 用户打开 .cjo 文件的完整流程
 *
 * ```
 * 1. IDE 文件系统层
 *   用户双击 ArrayList.cjo
 *     ↓
 *   VirtualFile 创建
 *     ↓
 *   PsiManager.findFile() 查找对应的 PSI 文件
 *
 * 2. 反编译器查找 (ClassFileDecompilers.find)
 *   遍历所有注册的 ClassFileDecompilers
 *     ↓
 *   对每个反编译器调用 accepts(file)
 *     ↓
 *   CangJieMetadataDecompiler.accepts(file)
 *     └─ stubBuilder.isSupported(file)
 *         ├─ CangJieBuiltInDecompiler: file.extension == "cjb" ? No
 *         └─ CangJieNormalDecompiler: file.extension == "cjo" ? Yes ✓
 *     ↓
 *   返回 CangJieNormalDecompiler
 *
 * 3. ViewProvider 创建
 *   IDE 调用 createFileViewProvider(file, manager, true)
 *     ↓
 *   CangJieMetadataDecompiler.createFileViewProvider()
 *     ├─ 创建 CangJieDecompiledFileViewProvider
 *     └─ 传入 factory lambda (延迟执行)
 *     ↓
 *   返回 ViewProvider
 *
 * 4. PSI 文件创建 (延迟触发)
 *   IDE 需要 PSI 文件 → 调用 viewProvider.getPsi(CangJieLanguage)
 *     ↓
 *   ViewProvider 调用 createFile(project, file, fileType)
 *     ↓
 *   执行 factory lambda
 *     ├─ stubBuilder.hasStub(file) ?
 *     │   └─ 检查文件是否有有效的元数据
 *     ├─ Yes → createFile(provider)
 *     │   └─ CangJieNormalDecompiler 返回 CjDecompiledFile(provider)
 *     └─ No → 返回 null
 *     ↓
 *   返回 CjDecompiledFile
 *
 * 5. Stub 树加载
 *   CjDecompiledFile.getStub()
 *     ↓
 *   CompiledStubBuilder.buildStubTree()
 *     ↓
 *   CompiledStubBuilder.readOrBuildCompiledStub()
 *     ↓
 *   ClsClassFinder.allowMultifileClassPart {
 *     StubTreeLoader.readOrBuild(project, file, null)
 *       ├─ 检查内存缓存
 *       ├─ 检查磁盘缓存
 *       └─ 缓存未命中 → stubBuilder.buildFileStub()
 *           ↓
 *           CangJieNormalMetadataStubBuilder.buildFileStub()
 *             ├─ 读取 Flatbuffers 元数据
 *             ├─ 解析包信息、类型声明
 *             └─ 构建 CangJieFileStubImpl
 *   }
 *     ↓
 *   返回 Stub 树
 *
 * 6. 反编译文本生成
 *   CjDecompiledFile.getText()
 *     ↓
 *   decompiledText.get() (LockedClearableLazyValue)
 *     ↓
 *   buildDecompiledText(stub)
 *     ├─ 遍历 Stub 树
 *     ├─ 使用 DescriptorRenderer 渲染
 *     └─ 生成格式化的仓颉源码
 *     ↓
 *   返回 DecompiledText
 *
 * 7. 编辑器显示
 *   IDE 编辑器组件获取 psiFile.text
 *     ↓
 *   显示反编译后的仓颉源代码
 * ```
 *
 * ## 子类实现要求
 *
 * ### 最小实现 (2 个成员)
 *
 * ```kotlin
 * class MinimalDecompiler : CangJieMetadataDecompiler() {
 *     // 1. 提供 StubBuilder (必需)
 *     override val stubBuilder = MyStubBuilder
 *
 *     // 2. 创建 PSI 文件 (必需)
 *     override fun createFile(viewProvider: CangJieDecompiledFileViewProvider) =
 *         MyCjDecompiledFile(viewProvider)
 * }
 * ```
 *
 * ### 完整实现示例
 *
 * ```kotlin
 * /**
 *  * 自定义格式的元数据反编译器
 *  */
 * class CustomFormatDecompiler : CangJieMetadataDecompiler() {
 *     /**
 *      * 提供自定义格式的 Stub 构建器
 *      */
 *     override val stubBuilder = object : CangJieMetadataStubBuilder() {
 *         override fun isSupported(file: VirtualFile): Boolean {
 *             return file.extension == "cjx" && // 自定义扩展名
 *                    file.inputStream.use { it.read() == 0xCAFE } // 验证文件签名
 *         }
 *
 *         override fun buildFileStub(fileContent: FileContent): CangJieFileStubImpl? {
 *             // 解析自定义格式的元数据
 *             val metadata = parseCustomFormat(fileContent.content)
 *             return buildStubFromMetadata(metadata)
 *         }
 *
 *         override fun hasStub(file: VirtualFile): Boolean {
 *             return isSupported(file) && file.length > 0
 *         }
 *
 *         override val stubVersion: Int = 42 // 自定义版本号
 *     }
 *
 *     /**
 *      * 创建自定义的 PSI 文件（如果需要特殊行为）
 *      */
 *     override fun createFile(viewProvider: CangJieDecompiledFileViewProvider) =
 *         CustomCjDecompiledFile(viewProvider) // 或直接使用 CjDecompiledFile(viewProvider)
 * }
 * ```
 *
 * ## 错误处理
 *
 * ### 文件类型不匹配
 *
 * ```kotlin
 * // accepts() 返回 false
 * override fun accepts(file: VirtualFile): Boolean {
 *     return stubBuilder.isSupported(file) // 如果不支持，返回 false
 * }
 * ```
 *
 * **结果**: IDE 会尝试下一个反编译器，如果都不匹配，将文件视为二进制文件。
 *
 * ### 文件没有 Stub
 *
 * ```kotlin
 * // factory lambda 返回 null
 * if (stubBuilder.hasStub(provider.virtualFile)) {
 *     createFile(provider)
 * } else {
 *     null // 没有 Stub，不创建 PSI 文件
 * }
 * ```
 *
 * **结果**: ViewProvider 创建成功，但没有 PSI 文件，IDE 不会显示反编译内容。
 *
 * ### 元数据损坏
 *
 * ```kotlin
 * // stubBuilder.buildFileStub() 返回 null 或抛异常
 * try {
 *     val stub = parseMetadata(file)
 *     buildStub(stub)
 * } catch (e: IOException) {
 *     LOG.error("Failed to parse metadata", e)
 *     return null // 或返回 forInvalid stub
 * }
 * ```
 *
 * **结果**: Stub 为 null 或包含错误消息，反编译文本显示"无法反编译"。
 *
 * ## 性能考虑
 *
 * ### 延迟加载策略
 *
 * 通过 factory 模式，PSI 文件只在真正需要时创建：
 * ```kotlin
 * // ViewProvider 创建 (快速)
 * val viewProvider = createFileViewProvider(file, manager, true)  // < 1ms
 *
 * // PSI 文件创建 (延迟)
 * val psiFile = viewProvider.getPsi(CangJieLanguage)  // 只在需要时调用
 * ```
 *
 * **好处**:
 * - 启动时不创建所有库文件的 PSI 文件
 * - 减少内存占用
 * - 提升 IDE 响应速度
 *
 * ### Stub 缓存复用
 *
 * stubBuilder 与 StubTreeLoader 集成：
 * ```kotlin
 * StubTreeLoader.readOrBuild(project, file, null)
 *   ├─ 缓存命中: 直接返回 (< 1ms)
 *   └─ 缓存未命中: 构建并缓存 (10-50ms)
 * ```
 *
 * **缓存层级**:
 * 1. **内存缓存**: IDE 运行期间有效
 * 2. **磁盘缓存**: IDE 重启后仍有效
 * 3. **跨项目共享**: 同一库在不同项目中只解析一次
 *
 * ## 线程安全
 *
 * ### final 方法保证
 *
 * accepts() 和 createFileViewProvider() 都是 final：
 * - 避免子类引入线程不安全的实现
 * - 确保行为一致性
 *
 * ### stubBuilder 要求
 *
 * stubBuilder 应该是无状态的单例对象：
 * ```kotlin
 * object MyStubBuilder : CangJieMetadataStubBuilder() {
 *     // 无可变状态，线程安全
 * }
 * ```
 *
 * ### ViewProvider 线程安全
 *
 * CangJieDecompiledFileViewProvider 内部使用 LockedClearableLazyValue：
 * - 延迟初始化是线程安全的
 * - 多线程并发访问不会重复创建
 *
 * ## 扩展性
 *
 * ### 添加新文件类型支持
 *
 * **步骤**:
 * 1. 创建 StubBuilder 实现
 * 2. 创建反编译器子类
 * 3. 在 plugin.xml 中注册
 *
 * **示例**:
 * ```kotlin
 * // 1. 创建 StubBuilder
 * object CjxStubBuilder : CangJieMetadataStubBuilder() {
 *     override fun isSupported(file: VirtualFile) = file.extension == "cjx"
 *     // ...
 * }
 *
 * // 2. 创建反编译器
 * class CjxDecompiler : CangJieMetadataDecompiler() {
 *     override val stubBuilder = CjxStubBuilder
 *     override fun createFile(viewProvider) = CjDecompiledFile(viewProvider)
 * }
 *
 * // 3. 注册到 plugin.xml
 * <classFileDecompiler implementation="...CjxDecompiler"/>
 * ```
 *
 * ### 自定义 PSI 文件行为
 *
 * 如果需要特殊的 PSI 文件行为，继承 CjDecompiledFile：
 * ```kotlin
 * class CustomDecompiledFile(provider: CangJieDecompiledFileViewProvider) : CjDecompiledFile(provider) {
 *     override fun getText(): String {
 *         val originalText = super.getText()
 *         return "// Custom header\n" + originalText
 *     }
 * }
 *
 * class CustomDecompiler : CangJieMetadataDecompiler() {
 *     override fun createFile(viewProvider) = CustomDecompiledFile(viewProvider)
 *     // ...
 * }
 * ```
 *
 * ## 调试技巧
 *
 * ### 检查反编译器是否被调用
 *
 * ```kotlin
 * override fun accepts(file: VirtualFile): Boolean {
 *     val result = super.accepts(file)
 *     println("CangJieMetadataDecompiler.accepts(${file.name}) = $result")
 *     return result
 * }
 * ```
 *
 * ### 查看 factory lambda 执行
 *
 * ```kotlin
 * override fun createFileViewProvider(...) = CangJieDecompiledFileViewProvider(...) { provider ->
 *     println("Factory lambda called for: ${provider.virtualFile.name}")
 *     val hasStub = stubBuilder.hasStub(provider.virtualFile)
 *     println("Has stub: $hasStub")
 *     if (hasStub) createFile(provider) else null
 * }
 * ```
 *
 * ### 验证 Stub 构建
 *
 * ```kotlin
 * val stub = stubBuilder.buildFileStub(fileContent)
 * println("Stub tree: ${stub?.treeToString()}")
 * ```
 *
 * ## 常见陷阱
 *
 * ### 陷阱 1: stubBuilder 不是单例
 *
 * ```kotlin
 * // ❌ 错误: 每次创建新实例
 * override val stubBuilder get() = CangJieMetadataStubBuilder()
 *
 * // ✅ 正确: 使用单例对象
 * override val stubBuilder = CangJieMetadataStubBuilder
 * ```
 *
 * **后果**: 每次调用都创建新对象，浪费内存和性能。
 *
 * ### 陷阱 2: createFile() 中执行昂贵操作
 *
 * ```kotlin
 * // ❌ 错误: 在文件创建时反编译
 * override fun createFile(viewProvider) = CjDecompiledFile(viewProvider).apply {
 *     parseAndDecompile() // 昂贵操作
 * }
 *
 * // ✅ 正确: 延迟到 getText() 时执行
 * override fun createFile(viewProvider) = CjDecompiledFile(viewProvider)
 * ```
 *
 * **后果**: IDE 启动变慢，因为创建所有 PSI 文件时都会反编译。
 *
 * ### 陷阱 3: 覆盖 final 方法
 *
 * ```kotlin
 * // ❌ 编译错误: 不能覆盖 final 方法
 * override fun accepts(file: VirtualFile): Boolean {
 *     return myCustomLogic(file)
 * }
 * ```
 *
 * **解决**: 将自定义逻辑放在 stubBuilder.isSupported() 中。
 *
 * ## 与其他组件的集成
 *
 * ### 与 ClassFileDecompilers 的关系
 *
 * ```
 * ClassFileDecompilers Extension Point (IDE 框架)
 *   ↓
 * ClassFileDecompilers.EP_NAME.extensions (所有注册的反编译器)
 *   ├─ CangJieBuiltInDecompiler (本类子类)
 *   ├─ CangJieNormalDecompiler (本类子类)
 *   └─ OtherLanguageDecompiler (其他语言)
 *   ↓
 * ClassFileDecompilers.find() 查找匹配的反编译器
 *   └─ 调用 accepts() 方法判断
 * ```
 *
 * ### 与 StubTreeLoader 的关系
 *
 * ```
 * CangJieMetadataDecompiler.createFileViewProvider()
 *   ↓
 * CangJieDecompiledFileViewProvider.createFile()
 *   ↓
 * CjDecompiledFile 创建
 *   ↓
 * CompiledStubBuilder.readOrBuildCompiledStub()
 *   ↓
 * StubTreeLoader.readOrBuild()
 *   ├─ 查询缓存
 *   └─ 缓存失败 → stubBuilder.buildFileStub()
 * ```
 *
 * ### 与 PSI 系统的关系
 *
 * ```
 * VirtualFile (文件系统层)
 *   ↓
 * PsiManager.findFile()
 *   ↓
 * FileViewProvider (桥接层)
 *   ↓
 * PsiFile (PSI 层)
 *   ↓
 * IDE 服务 (语法高亮、补全、导航等)
 * ```
 *
 * 本类负责创建 FileViewProvider 和 PsiFile。
 *
 * ## 最佳实践
 *
 * ### 1. stubBuilder 使用 object 单例
 *
 * ```kotlin
 * override val stubBuilder = MyStubBuilder // object
 * ```
 *
 * ### 2. createFile() 返回简单的 PSI 文件
 *
 * ```kotlin
 * override fun createFile(viewProvider) = CjDecompiledFile(viewProvider)
 * ```
 *
 * ### 3. 不要覆盖 final 方法
 *
 * 将自定义逻辑放在抽象成员的实现中。
 *
 * ### 4. 利用 hasStub() 提前过滤
 *
 * ```kotlin
 * override fun hasStub(file: VirtualFile): Boolean {
 *     // 快速检查，避免解析大文件
 *     return file.length > 0 && file.inputStream.use { /* 验证签名 */ }
 * }
 * ```
 *
 * @see ClassFileDecompilers.Full
 * @see CangJieMetadataStubBuilder
 * @see CangJieDecompiledFileViewProvider
 * @see CjDecompiledFile
 * @see ClsClassFinder.allowMultifileClassPart
 * @see StubTreeLoader
 */
abstract class CangJieMetadataDecompiler : ClassFileDecompilers.Full() {
    /**
     * 判断该反编译器是否接受指定的虚拟文件
     *
     * 委托给 [stubBuilder] 进行文件类型检查。这确保了文件类型判断逻辑
     * 与 Stub 构建逻辑保持一致。
     *
     * @param file 待检查的虚拟文件
     * @return true 如果文件类型被支持
     */
    final override fun accepts(file: VirtualFile): Boolean = stubBuilder.isSupported(file)

    /**
     * Stub 构建器实例
     *
     * 子类必须提供具体的 Stub 构建器实现。该构建器负责：
     * - 判断文件类型是否支持
     * - 读取并解析元数据文件
     * - 构建轻量级的 Stub 索引树
     *
     * @see CangJieBuiltInMetadataStubBuilder
     */
    abstract override val stubBuilder : CangJieMetadataStubBuilder

    /**
     * 创建反编译的 PSI 文件
     *
     * 子类必须实现此方法以创建对应类型的 CjDecompiledFile。
     * 该文件将负责：
     * - 延迟加载反编译文本
     * - 提供 getText() 方法给 IDE
     * - 管理 Stub 缓存
     *
     * @param viewProvider 文件视图提供者
     * @return 反编译的 PSI 文件实例
     */
    protected abstract fun createFile(viewProvider: CangJieDecompiledFileViewProvider): CjDecompiledFile

    /**
     * 创建文件视图提供者
     *
     * 该方法使用 factory 模式创建 [CangJieDecompiledFileViewProvider]。
     * factory 函数会检查文件是否有 Stub，只有存在 Stub 的文件才会创建 PSI 文件。
     *
     * ## 工作流程
     *
     * 1. 创建 CangJieDecompiledFileViewProvider，传入 factory 函数
     * 2. factory 函数在需要时被调用（通过 createFile(project, file, fileType)）
     * 3. factory 检查 stubBuilder.hasStub(file)
     * 4. 如果有 Stub，调用 createFile(provider) 创建实际的 PSI 文件
     * 5. 如果没有 Stub，返回 null
     *
     * @param file 元数据虚拟文件
     * @param manager PSI 管理器
     * @param physical 是否为物理文件
     * @return 文件视图提供者实例
     */
    final override fun createFileViewProvider(
        file: VirtualFile,
        manager: PsiManager,
        physical: Boolean,
    ): CangJieDecompiledFileViewProvider = CangJieDecompiledFileViewProvider(manager, file, physical) { provider ->
        if (stubBuilder.hasStub(provider.virtualFile)) {
            createFile(provider)
        } else {
            null
        }
    }
}
