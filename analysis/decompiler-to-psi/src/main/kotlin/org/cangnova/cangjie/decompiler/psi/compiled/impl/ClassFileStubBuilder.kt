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

/**
 * 编译文件 Stub 构建器适配器
 *
 * ## 架构概述
 *
 * ClassFileStubBuilder 是连接 IntelliJ Platform 的 Stub 索引系统和仓颉反编译器的适配器层。
 * 它实现了 IDE 的 [BinaryFileStubBuilder.CompositeBinaryFileStubBuilder] 接口，
 * 将 IDE 的 Stub 构建请求委托给注册的仓颉反编译器。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * IDE Stub 索引系统
 *   ↓
 * ClassFileStubBuilder (本类 - 适配器层)
 *   ├─ 查找合适的反编译器
 *   ├─ 检查项目初始化状态
 *   └─ 委托给反编译器构建 Stub
 *   ↓
 * CangJieMetadataDecompiler (实际的反编译器)
 *   ↓
 * CangJieMetadataStubBuilder (Stub 构建逻辑)
 *   ↓
 * Stub 树
 * ```
 *
 * ## 复合构建器模式
 *
 * ### CompositeBinaryFileStubBuilder 接口
 *
 * IDE 的复合构建器允许多个子构建器处理不同类型的二进制文件：
 * - **getAllSubBuilders()**: 获取所有注册的反编译器
 * - **getSubBuilder()**: 为特定文件选择合适的反编译器
 * - **buildStubTree()**: 使用选中的反编译器构建 Stub
 *
 * ### 为什么使用复合构建器？
 *
 * 1. **扩展性**: 支持多个反编译器（.cjo、.cjb、未来可能的其他格式）
 * 2. **灵活性**: 可以动态注册/卸载反编译器
 * 3. **版本管理**: 每个反编译器有独立的版本号
 * 4. **性能**: 只加载处理当前文件类型的反编译器
 *
 * ## 关键设计
 *
 * ### 1. 延迟 Stub 构建
 *
 * **核心逻辑** (buildStubTree:72-82):
 * ```kotlin
 * val projectsService = project.getServiceIfCreated(CjProjectsService::class.java)
 * if (projectsService != null && !projectsService.initialized) {
 *     // 工作空间模型未同步完成，延迟 stub 构建
 *     return null
 * }
 * ```
 *
 * **为什么需要延迟？**
 * - **跨包引用解析**: Stub 构建需要解析跨包类型引用（通过 CjoPackageService）
 * - **依赖工作空间模型**: CjoPackageService 依赖项目的工作空间模型（Workspace Model）
 * - **避免解析失败**: 工作空间未初始化时，跨包引用会解析失败，导致 Stub 不完整
 *
 * **何时重试？**
 * - IDE 会在项目初始化完成后重新索引文件
 * - 延迟的 Stub 会在后续的索引扫描中被构建
 *
 * ### 2. 反编译器选择机制
 *
 * **通过扩展点动态查找** (getSubBuilder:53-59):
 * ```kotlin
 * ClassFileDecompilers.instance.find(
 *     fileContent.file,
 *     ClassFileDecompilers.Full::class.java
 * )
 * ```
 *
 * **查找流程**:
 * ```
 * 1. 遍历所有注册的 ClassFileDecompilers.Full 扩展
 * 2. 调用每个反编译器的 acceptsFile() 方法
 * 3. 返回第一个接受该文件的反编译器
 * 4. 如果没有匹配的反编译器，返回 null
 * ```
 *
 * **典型扩展**:
 * - **CangJieMetadataDecompiler**: 处理 .cjo 文件
 * - **CangJieBuiltInDecompiler**: 处理内置库 .cjb 文件
 *
 * ### 3. 版本管理
 *
 * **复合版本字符串** (getSubBuilderVersion:61-65):
 * ```kotlin
 * decompiler::class.java.name + ":" + version
 * // 例如: "org.cangnova.cangjie.decompiler.CangJieMetadataDecompiler:27"
 * ```
 *
 * **为什么包含类名？**
 * - 不同反编译器可能有不同的 Stub 结构
 * - 类名变化时强制重新索引
 * - 避免版本号冲突
 *
 * **全局 STUB_VERSION** (companion:111):
 * ```kotlin
 * const val STUB_VERSION: Int = 27
 * ```
 *
 * **版本递增时机**:
 * - Stub 结构变化（添加/删除/修改字段）
 * - Stub 序列化格式变化
 * - 元数据格式变化
 * - Stub 构建逻辑变化
 *
 * ### 4. 错误处理
 *
 * **捕获 ClsFormatException** (buildStubTree:88-96):
 * ```kotlin
 * try {
 *     decompiler.stubBuilder.buildFileStub(fileContent)
 * } catch (e: ClsFormatException) {
 *     if (LOG.isDebugEnabled) {
 *         LOG.debug(file.path, e)  // Debug 模式记录堆栈
 *     } else {
 *         LOG.info(file.path + ": " + e.message)  // 正常模式只记录消息
 *     }
 * }
 * ```
 *
 * **典型失败场景**:
 * - 元数据文件损坏
 * - 不兼容的二进制版本
 * - Flatbuffers 解析错误
 * - 跨包引用解析失败
 *
 * **失败处理**:
 * - 记录日志（避免静默失败）
 * - 返回 null（IDE 将该文件标记为无 Stub）
 * - 不阻塞其他文件的索引
 *
 * ## 工作流程
 *
 * ### IDE 启动索引扫描
 *
 * ```
 * IDE 启动
 *   ↓
 * 扫描项目文件
 *   ↓
 * 发现 ArrayList.cjo
 *   ↓
 * IDE 调用 ClassFileStubBuilder.buildStubTree()
 *   ↓
 * 1. getSubBuilder() → CangJieMetadataDecompiler
 * 2. 检查项目初始化状态
 *    ├─ 未初始化 → 返回 null (延迟)
 *    └─ 已初始化 → 继续
 * 3. 调用 CangJieMetadataDecompiler.stubBuilder.buildFileStub()
 *    ├─ 解析 Flatbuffers 元数据
 *    ├─ 构建 Stub 树
 *    └─ 返回 CangJieFileStubImpl
 * 4. IDE 缓存 Stub 树
 * ```
 *
 * ### 项目初始化后重新索引
 *
 * ```
 * 项目同步完成
 *   ↓
 * CjProjectsService.initialized = true
 *   ↓
 * IDE 触发重新索引
 *   ↓
 * 重新扫描之前延迟的文件
 *   ↓
 * buildStubTree() 再次被调用
 *   ├─ 项目已初始化 → 继续
 *   ├─ CjoPackageService 可用
 *   └─ 跨包引用正确解析
 *   ↓
 * 完整的 Stub 树被构建和缓存
 * ```
 *
 * ## 性能优化
 *
 * ### 1. computeWithPreloadedContentHint
 *
 * ```kotlin
 * file.computeWithPreloadedContentHint(fileContent.content) {
 *     // 构建逻辑
 * }
 * ```
 *
 * **作用**:
 * - 告诉 IDE 文件内容已经预加载到内存
 * - 避免在构建过程中重复读取文件
 * - 减少 I/O 操作
 *
 * ### 2. 延迟构建
 *
 * 避免在工作空间未就绪时构建不完整的 Stub：
 * - 减少后续的 Stub 重新构建
 * - 避免无效的跨包引用
 * - 提高最终 Stub 质量
 *
 * ### 3. 日志级别控制
 *
 * ```kotlin
 * if (LOG.isDebugEnabled) {
 *     LOG.debug(file.path, e)  // 详细日志
 * } else {
 *     LOG.info(file.path + ": " + e.message)  // 简洁日志
 * }
 * ```
 *
 * 生产环境减少日志开销。
 *
 * ## 与其他组件的关系
 *
 * ### 上游组件
 *
 * - **IDE Stub 索引系统**: 调用本类构建 Stub
 * - **文件类型系统**: 注册本类为特定文件类型的 Stub 构建器
 *
 * ### 下游组件
 *
 * - **ClassFileDecompilers**: 扩展点，注册反编译器
 * - **CangJieMetadataDecompiler**: 实际的反编译器实现
 * - **CjProjectsService**: 项目初始化状态检查
 *
 * ## 注册配置
 *
 * 在 `plugin.xml` 中注册：
 * ```xml
 * <stubElementTypeHolder class="..." externalIdPrefix="..."/>
 * <stubIndex implementation="..."/>
 * <fileType.stubBuilder
 *     filetype="org.cangnova.cangjie.lang.CangJieBuiltInFileType"
 *     implementationClass="...ClassFileStubBuilder"/>
 * ```
 *
 * ## 已知限制
 *
 * 1. **延迟构建窗口**: 项目初始化期间，Stub 不可用
 * 2. **单线程检查**: 项目初始化状态检查不是原子的
 * 3. **全局版本号**: 所有反编译器共享 STUB_VERSION
 *
 * ## 扩展点
 *
 * ### 添加新的反编译器
 *
 * ```kotlin
 * class MyCustomDecompiler : ClassFileDecompilers.Full {
 *     override fun acceptsFile(file: VirtualFile): Boolean {
 *         return file.extension == "custom"
 *     }
 *
 *     override val stubBuilder: ClsStubBuilder
 *         get() = MyCustomStubBuilder()
 * }
 * ```
 *
 * 在 plugin.xml 中注册：
 * ```xml
 * <classFileDecompiler implementation="...MyCustomDecompiler"/>
 * ```
 *
 * @see BinaryFileStubBuilder.CompositeBinaryFileStubBuilder
 * @see ClassFileDecompilers
 * @see CangJieMetadataDecompiler
 * @see CjProjectsService
 */
package org.cangnova.cangjie.decompiler.psi.compiled.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.stubs.BinaryFileStubBuilder
import com.intellij.psi.stubs.Stub
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent
import org.cangnova.cangjie.decompiler.psi.compiled.ClassFileDecompilers
import org.cangnova.cangjie.project.service.CjProjectsService
import java.util.function.Supplier
import java.util.stream.Stream

class ClassFileStubBuilder : BinaryFileStubBuilder.CompositeBinaryFileStubBuilder<ClassFileDecompilers.Full> {
    /**
     * 获取文件过滤器
     *
     * ## 功能说明
     *
     * 返回用于过滤可接受文件的 [VirtualFileFilter]。该方法是 [BinaryFileStubBuilder] 接口的一部分，
     * 用于在 IDE 扫描文件系统时快速过滤出可能需要构建 Stub 的文件。
     *
     * ## 实现策略
     *
     * 返回 [VirtualFileFilter.ALL]，即接受所有文件类型：
     * - 原因：本构建器通过 plugin.xml 注册到特定文件类型（.cjo/.cjb）
     * - 文件类型过滤已由 IDE 框架在调用前完成
     * - 这里不需要重复过滤
     *
     * ## 工作流程
     *
     * ```
     * IDE 文件扫描
     *   ↓
     * 1. 检查文件扩展名（由 FileType 系统处理）
     *    ├─ .cjo → 通过
     *    ├─ .cjb → 通过
     *    └─ 其他 → 跳过
     *   ↓
     * 2. 调用 getFileFilter().accept(file)
     *    └─ VirtualFileFilter.ALL → 总是返回 true
     *   ↓
     * 3. 调用 acceptsFile(file)
     *    └─ 进一步验证（本类总是返回 true）
     *   ↓
     * 4. 继续 Stub 构建流程
     * ```
     *
     * ## 为什么返回 ALL？
     *
     * ### 注册配置（plugin.xml）
     *
     * ```xml
     * <fileType.stubBuilder
     *     filetype="org.cangnova.cangjie.lang.CangJieCompiledFileType"
     *     implementationClass="...ClassFileStubBuilder"/>
     * ```
     *
     * 由于已在配置中指定 filetype，IDE 只会对 .cjo/.cjb 文件调用本构建器：
     * - **高效**: 避免重复的文件类型检查
     * - **简洁**: 逻辑集中在配置中
     * - **灵活**: 便于添加新的文件类型支持
     *
     * ### 与 acceptsFile() 的区别
     *
     * | 方法 | 调用时机 | 用途 | 本类实现 |
     * |------|---------|------|---------|
     * | getFileFilter() | 文件扫描阶段 | 批量快速过滤 | 返回 ALL |
     * | acceptsFile() | Stub 构建前 | 精确验证单个文件 | 返回 true |
     *
     * ## 扩展场景
     *
     * 如果需要更细粒度的过滤（例如排除临时文件），可以返回自定义过滤器：
     * ```kotlin
     * override fun getFileFilter(): VirtualFileFilter {
     *     return VirtualFileFilter { file ->
     *         !file.name.endsWith(".tmp.cjo")
     *     }
     * }
     * ```
     *
     * ## 性能考虑
     *
     * - **调用频率**: 文件扫描时每个文件调用一次
     * - **开销**: VirtualFileFilter.ALL 是常量，无计算开销
     * - **推荐**: 保持简单，将复杂逻辑放在 [acceptsFile] 或 [buildStubTree] 中
     *
     * @return 始终返回 [VirtualFileFilter.ALL]，接受所有由 IDE 路由到本构建器的文件
     *
     * @see VirtualFileFilter
     * @see acceptsFile
     */
    override fun getFileFilter(): VirtualFileFilter {
        return VirtualFileFilter.ALL // any file of file type that this builder is registered for
    }

    /**
     * 检查是否接受指定文件
     *
     * ## 功能说明
     *
     * 验证单个文件是否应该由本构建器处理。该方法在 Stub 构建前调用，
     * 提供最后一次机会拒绝不合适的文件。
     *
     * ## 实现策略
     *
     * 始终返回 `true`，接受所有文件：
     * - 文件类型过滤已由 [getFileFilter] 和 IDE 框架完成
     * - 具体的反编译器选择由 [getSubBuilder] 负责
     * - 本方法保持简单，避免重复逻辑
     *
     * ## 调用时机
     *
     * ```
     * IDE Stub 构建流程
     *   ↓
     * 1. getFileFilter().accept(file)  // 批量过滤
     *   ↓
     * 2. acceptsFile(file)  // 单文件验证（本方法）
     *    └─ 返回 true → 继续
     *    └─ 返回 false → 跳过该文件
     *   ↓
     * 3. getSubBuilder(fileContent)  // 选择反编译器
     *   ↓
     * 4. buildStubTree(...)  // 构建 Stub
     * ```
     *
     * ## 为什么总是返回 true？
     *
     * ### 职责分离原则
     *
     * - **本方法**: 顶层接受/拒绝决策
     * - **getSubBuilder()**: 选择具体的反编译器
     * - **反编译器.acceptsFile()**: 反编译器级别的文件验证
     *
     * ### 实际验证位置
     *
     * 真正的文件验证发生在 [getSubBuilder] 方法中：
     * ```kotlin
     * ClassFileDecompilers.instance.find(file, ClassFileDecompilers.Full::class.java)
     * // 遍历所有反编译器，找到第一个 acceptsFile() 返回 true 的
     * ```
     *
     * 反编译器可以根据文件内容精确判断：
     * - **CangJieMetadataDecompiler**: 检查 .cjo 文件头部签名
     * - **CangJieBuiltInDecompiler**: 检查 .cjb 内置库标记
     *
     * ## 与其他方法的关系
     *
     * | 方法 | 检查级别 | 检查依据 | 拒绝后果 |
     * |------|---------|---------|---------|
     * | getFileFilter() | 批量过滤 | 文件类型/扩展名 | 跳过整批文件 |
     * | acceptsFile() | 单文件验证 | 文件属性 | 跳过该文件 |
     * | getSubBuilder() | 反编译器选择 | 文件内容/格式 | 返回 null |
     * | buildStubTree() | 构建执行 | 元数据解析 | 返回 null 或异常 |
     *
     * ## 扩展场景
     *
     * 如果需要在构建器层面拒绝某些文件，可以添加检查：
     * ```kotlin
     * override fun acceptsFile(file: VirtualFile): Boolean {
     *     // 拒绝临时文件
     *     if (file.name.contains(".tmp.")) return false
     *
     *     // 拒绝过大文件（避免内存问题）
     *     if (file.length > 100 * 1024 * 1024) return false
     *
     *     return true
     * }
     * ```
     *
     * ## 性能考虑
     *
     * - **调用频率**: 每个候选文件调用一次
     * - **执行时机**: 在 Stub 构建前（避免浪费构建开销）
     * - **建议**: 快速检查，避免 I/O 操作（如读取文件内容）
     *
     * @param file 要检查的虚拟文件
     * @return 始终返回 `true`，接受所有文件
     *
     * @see getFileFilter
     * @see getSubBuilder
     */
    override fun acceptsFile(file: VirtualFile): Boolean {
        return true
    }

    /**
     * 获取所有注册的子构建器（反编译器）
     *
     * ## 功能说明
     *
     * 返回当前系统中所有注册的 [ClassFileDecompilers.Full] 反编译器实例。
     * 这是复合构建器模式的核心方法之一，用于发现和管理多个反编译器。
     *
     * ## 实现逻辑
     *
     * 从扩展点系统获取所有反编译器：
     * ```kotlin
     * ClassFileDecompilers.instance.EP_NAME.extensionList
     *     .stream()
     *     .filter { it is ClassFileDecompilers.Full }
     *     .map { it as ClassFileDecompilers.Full }
     * ```
     *
     * ## 工作流程
     *
     * ```
     * getAllSubBuilders() 调用
     *   ↓
     * 1. ClassFileDecompilers.instance.EP_NAME
     *    └─ 获取扩展点 (Extension Point)
     *   ↓
     * 2. extensionList
     *    └─ 获取所有已注册的扩展实现
     *   ↓
     * 3. filter { it is ClassFileDecompilers.Full }
     *    └─ 过滤出完整反编译器（非 Light 版本）
     *   ↓
     * 4. map { it as ClassFileDecompilers.Full }
     *    └─ 类型转换为 Full 接口
     *   ↓
     * 5. 返回 Stream<ClassFileDecompilers.Full>
     * ```
     *
     * ## 扩展点机制
     *
     * ### 扩展点定义
     *
     * 反编译器通过 IntelliJ 平台的扩展点机制注册：
     * ```xml
     * <extensionPoint qualifiedName="org.cangnova.cangjie.classFileDecompiler"
     *                 interface="...ClassFileDecompilers$Full"/>
     * ```
     *
     * ### 典型注册的反编译器
     *
     * | 反编译器 | 用途 | 支持文件 |
     * |---------|------|---------|
     * | [CangJieMetadataDecompiler] | 普通编译文件 | .cjo |
     * | [CangJieBuiltInDecompiler] | 内置库文件 | .cjb |
     * | (未来扩展) | 调试信息文件 | .cjd |
     *
     * ## 为什么使用 Stream？
     *
     * - **延迟处理**: 只有在实际需要时才迭代
     * - **链式操作**: 支持 filter/map 等操作
     * - **API 一致性**: 符合 Java 8+ 的现代 API 风格
     *
     * ## Full vs Light 反编译器
     *
     * IntelliJ 平台支持两种反编译器接口：
     *
     * | 接口 | 功能 | Stub 支持 | 使用场景 |
     * |------|------|----------|---------|
     * | ClassFileDecompilers.Full | 完整功能 | ✅ 支持 | 需要 Stub 索引的文件 |
     * | ClassFileDecompilers.Light | 轻量级 | ❌ 不支持 | 仅需文本显示 |
     *
     * 本方法只返回 Full 反编译器，因为 Stub 构建需要完整功能。
     *
     * ## 使用场景
     *
     * ### 1. IDE 初始化
     *
     * IDE 启动时，调用此方法发现所有可用的反编译器：
     * ```
     * IDE 启动
     *   ↓
     * getAllSubBuilders()
     *   ├─ CangJieMetadataDecompiler
     *   ├─ CangJieBuiltInDecompiler
     *   └─ (其他插件提供的反编译器)
     *   ↓
     * 构建反编译器列表
     * ```
     *
     * ### 2. 版本管理
     *
     * IDE 需要跟踪所有反编译器的版本号：
     * ```kotlin
     * getAllSubBuilders().forEach { decompiler ->
     *     val version = getSubBuilderVersion(decompiler)
     *     // 检查是否需要重新构建 Stub
     * }
     * ```
     *
     * ### 3. 调试和诊断
     *
     * 开发者可以查看当前系统中注册的所有反编译器：
     * ```
     * Help → Diagnostic Tools → Installed Extensions
     *   ↓
     * 显示: CangJieMetadataDecompiler (v27)
     *       CangJieBuiltInDecompiler (v27)
     * ```
     *
     * ## 扩展性
     *
     * ### 添加新的反编译器
     *
     * 第三方插件可以通过扩展点注册自己的反编译器：
     *
     * **1. 实现接口**:
     * ```kotlin
     * class MyCustomDecompiler : ClassFileDecompilers.Full {
     *     override fun acceptsFile(file: VirtualFile): Boolean {
     *         return file.extension == "myext"
     *     }
     *
     *     override val stubBuilder: ClsStubBuilder
     *         get() = MyCustomStubBuilder()
     * }
     * ```
     *
     * **2. 注册到 plugin.xml**:
     * ```xml
     * <classFileDecompiler implementation="com.example.MyCustomDecompiler"/>
     * ```
     *
     * **3. 自动发现**: getAllSubBuilders() 会自动包含新注册的反编译器
     *
     * ## 性能考虑
     *
     * - **调用频率**: IDE 启动时调用一次，后续缓存结果
     * - **开销**: 扩展点查找 + Stream 构建（约 < 1ms）
     * - **优化**: 如果反编译器数量很多，可以考虑缓存 Stream 结果
     *
     * ## 线程安全
     *
     * - **扩展点系统**: IntelliJ 平台保证 extensionList 的线程安全
     * - **Stream 创建**: 每次调用创建新的 Stream，无共享状态
     * - **并发访问**: 可以从多个线程安全调用
     *
     * ## 返回值
     *
     * 所有注册的完整反编译器的 Stream，按注册顺序排列。
     * 如果没有注册任何反编译器，返回空 Stream（不是 null）。
     *
     * @return 所有 [ClassFileDecompilers.Full] 反编译器的流
     *
     * @see ClassFileDecompilers
     * @see ClassFileDecompilers.Full
     * @see getSubBuilder
     */
    override fun getAllSubBuilders(): Stream<ClassFileDecompilers.Full?> {
        return ClassFileDecompilers.instance.EP_NAME.extensionList.stream().filter { d -> d is ClassFileDecompilers.Full }
            .map { d -> d as ClassFileDecompilers.Full }
    }

    /**
     * 为特定文件选择合适的反编译器
     *
     * ## 功能说明
     *
     * 根据文件内容和格式，从所有注册的反编译器中选择第一个能够处理该文件的反编译器。
     * 这是复合构建器模式的核心方法，实现了反编译器的自动选择和路由。
     *
     * ## 实现逻辑
     *
     * 使用文件内容预加载优化，然后查找合适的反编译器：
     * ```kotlin
     * fileContent.file.computeWithPreloadedContentHint(
     *     fileContent.content,
     *     Supplier {
     *         ClassFileDecompilers.instance.find(
     *             fileContent.file,
     *             ClassFileDecompilers.Full::class.java
     *         )
     *     }
     * )
     * ```
     *
     * ## 工作流程
     *
     * ```
     * getSubBuilder(fileContent) 调用
     *   ↓
     * 1. computeWithPreloadedContentHint()
     *    ├─ 缓存文件内容到内存
     *    └─ 避免多次读取磁盘
     *   ↓
     * 2. ClassFileDecompilers.instance.find()
     *    ├─ 遍历所有注册的反编译器
     *    │   ├─ CangJieMetadataDecompiler.acceptsFile()?
     *    │   ├─ CangJieBuiltInDecompiler.acceptsFile()?
     *    │   └─ ...其他反编译器
     *    └─ 返回第一个 acceptsFile() == true 的
     *   ↓
     * 3. 返回选中的反编译器（或 null）
     * ```
     *
     * ## computeWithPreloadedContentHint 机制
     *
     * ### 作用
     *
     * 该方法通知 IntelliJ 平台文件内容已经预加载到内存：
     * - **避免重复 I/O**: 如果反编译器需要检查文件内容，不会再次读取磁盘
     * - **性能优化**: 文件内容在 fileContent.content 中已可用
     * - **一致性**: 确保所有操作看到相同的文件内容
     *
     * ### 原理
     *
     * ```
     * VirtualFile 内部缓存机制:
     *   ↓
     * computeWithPreloadedContentHint() 调用
     *   ├─ 将 fileContent.content 关联到 virtualFile
     *   ├─ 临时标记"内容已预加载"
     *   └─ 执行 Supplier 代码块
     *   ↓
     * Supplier 内部的反编译器.acceptsFile()
     *   ├─ 如果需要读取文件内容
     *   └─ 直接从缓存返回（不读磁盘）
     *   ↓
     * Supplier 返回后清除"内容已预加载"标记
     * ```
     *
     * ## find() 方法的选择策略
     *
     * [ClassFileDecompilers.instance.find] 遍历所有反编译器：
     *
     * | 反编译器 | acceptsFile() 检查逻辑 | 返回条件 |
     * |---------|---------------------|---------|
     * | CangJieMetadataDecompiler | 检查文件扩展名 == .cjo | 第一个匹配 |
     * | CangJieBuiltInDecompiler | 检查文件扩展名 == .cjb | 第二个匹配 |
     * | (其他) | 自定义检查逻辑 | 继续查找 |
     *
     * **优先级**: 由反编译器在 plugin.xml 中的注册顺序决定。
     *
     * ## 典型匹配场景
     *
     * ### 场景 1: 普通编译文件
     *
     * ```
     * fileContent.file = ArrayList.cjo
     *   ↓
     * CangJieMetadataDecompiler.acceptsFile(ArrayList.cjo)
     *   └─ file.extension == "cjo" → true
     *   ↓
     * 返回: CangJieMetadataDecompiler
     * ```
     *
     * ### 场景 2: 内置库文件
     *
     * ```
     * fileContent.file = std-core.cjb
     *   ↓
     * CangJieMetadataDecompiler.acceptsFile(std-core.cjb)
     *   └─ file.extension != "cjo" → false
     *   ↓
     * CangJieBuiltInDecompiler.acceptsFile(std-core.cjb)
     *   └─ file.extension == "cjb" → true
     *   ↓
     * 返回: CangJieBuiltInDecompiler
     * ```
     *
     * ### 场景 3: 不支持的文件
     *
     * ```
     * fileContent.file = unknown.xyz
     *   ↓
     * 遍历所有反编译器
     *   └─ 所有 acceptsFile() 返回 false
     *   ↓
     * 返回: null
     * ```
     *
     * ## 返回 null 的处理
     *
     * 如果没有反编译器接受该文件：
     * - [buildStubTree] 方法会检查 decompiler 是否为 null
     * - 如果为 null，直接返回 null，不构建 Stub
     * - IDE 将该文件视为无 Stub 索引的二进制文件
     *
     * ```kotlin
     * override fun buildStubTree(fileContent: FileContent, decompiler: ClassFileDecompilers.Full?): Stub? {
     *     if (decompiler == null) return null  // 无合适的反编译器
     *     // 继续 Stub 构建...
     * }
     * ```
     *
     * ## 与其他方法的关系
     *
     * | 方法 | 调用顺序 | 作用 | 返回值 |
     * |------|---------|------|--------|
     * | getFileFilter() | 1 | 批量过滤文件类型 | VirtualFileFilter |
     * | acceptsFile() | 2 | 验证单个文件 | boolean |
     * | **getSubBuilder()** | **3** | **选择反编译器** | **Decompiler or null** |
     * | buildStubTree() | 4 | 构建 Stub 树 | Stub or null |
     *
     * ## 性能优化
     *
     * ### 1. 内容预加载
     *
     * 通过 computeWithPreloadedContentHint 避免重复读取：
     * - **无优化**: 读取文件 N 次（N = 反编译器数量）
     * - **有优化**: 读取文件 1 次，反编译器从缓存读取
     *
     * ### 2. 短路求值
     *
     * find() 方法找到第一个匹配后立即返回：
     * - 不会继续检查剩余的反编译器
     * - 平均只需检查 1-2 个反编译器
     *
     * ### 3. 扩展名快速检查
     *
     * 反编译器通常首先检查扩展名（O(1) 操作）：
     * ```kotlin
     * override fun acceptsFile(file: VirtualFile): Boolean {
     *     if (file.extension != "cjo") return false  // 快速返回
     *     // 其他更复杂的检查...
     * }
     * ```
     *
     * ## 扩展性
     *
     * ### 添加自定义反编译器
     *
     * 第三方插件可以注册自己的反编译器，本方法会自动包含：
     *
     * ```kotlin
     * class MyCustomDecompiler : ClassFileDecompilers.Full {
     *     override fun acceptsFile(file: VirtualFile): Boolean {
     *         // 检查文件签名或其他特征
     *         return file.extension == "myformat" &&
     *                file.inputStream.use { it.read() == 0xCAFE }
     *     }
     * }
     * ```
     *
     * 注册后，find() 会自动考虑该反编译器。
     *
     * ## 调试和诊断
     *
     * 如果文件无法反编译，可以通过日志查看选择结果：
     * ```kotlin
     * val decompiler = getSubBuilder(fileContent)
     * if (decompiler == null) {
     *     LOG.warn("No decompiler found for file: ${fileContent.fileName}")
     * } else {
     *     LOG.debug("Selected decompiler: ${decompiler::class.simpleName}")
     * }
     * ```
     *
     * ## 线程安全
     *
     * - **computeWithPreloadedContentHint**: IntelliJ 平台保证线程安全
     * - **ClassFileDecompilers.instance**: 单例，线程安全
     * - **find() 方法**: 只读操作，可并发调用
     *
     * @param fileContent 文件内容，包含虚拟文件和字节内容
     * @return 能够处理该文件的反编译器，如果没有合适的反编译器则返回 null
     *
     * @see ClassFileDecompilers.find
     * @see ClassFileDecompilers.Full
     * @see computeWithPreloadedContentHint
     * @see buildStubTree
     */
    override fun getSubBuilder(fileContent: FileContent): ClassFileDecompilers.Full? {
        // 当 FileContent 是从文本创建时（例如在 Stub 一致性检查中），
        // 无法访问 content 字节数组，此时直接查找反编译器而不预加载内容
        return runCatching {
            fileContent.file.computeWithPreloadedContentHint(
                fileContent.content,
                Supplier { ClassFileDecompilers.instance.find(fileContent.file, ClassFileDecompilers.Full::class.java) },
            )
        }.getOrElse {
            // 如果访问 content 失败（文本创建的 FileContent），直接查找反编译器
            ClassFileDecompilers.instance.find(fileContent.file, ClassFileDecompilers.Full::class.java)
        }
    }

    /**
     * 获取反编译器的版本字符串
     *
     * ## 功能说明
     *
     * 为指定的反编译器生成复合版本字符串，用于 IDE 的 Stub 缓存失效机制。
     * 当版本字符串变化时，IDE 会自动重新构建所有使用该反编译器的文件的 Stub。
     *
     * ## 复合版本字符串格式
     *
     * ```
     * 格式: "<反编译器全限定类名>:<版本号>"
     * 示例: "org.cangnova.cangjie.decompiler.CangJieMetadataDecompiler:27"
     * ```
     *
     * ## 实现逻辑
     *
     * ```kotlin
     * if (decompiler == null) return "default"
     *
     * val version = decompiler.stubBuilder.stubVersion
     * return "${decompiler::class.java.name}:$version"
     * ```
     *
     * ## 为什么包含类名？
     *
     * ### 1. 避免版本号冲突
     *
     * 不同反编译器可能恰好使用相同的版本号：
     * ```
     * CangJieMetadataDecompiler → stubVersion = 27
     * CangJieBuiltInDecompiler  → stubVersion = 27
     * ```
     *
     * 如果只用版本号，无法区分它们的变化：
     * - ❌ 仅版本号: "27" vs "27" → 无法区分
     * - ✅ 复合字符串: "...Metadata:27" vs "...BuiltIn:27" → 可区分
     *
     * ### 2. 强制重建当类名变化
     *
     * 如果重构代码，改变反编译器的类名：
     * ```kotlin
     * // 重构前
     * class CangJieMetadataDecompiler  // "...CangJieMetadataDecompiler:27"
     *
     * // 重构后
     * class CjMetadataDecompiler       // "...CjMetadataDecompiler:27"
     * ```
     *
     * 类名变化会导致版本字符串变化，触发 Stub 重建。
     *
     * ### 3. 调试和诊断
     *
     * 版本字符串清楚地显示使用的反编译器：
     * ```
     * IDE 日志:
     *   Stub version for ArrayList.cjo:
     *     "org.cangnova.cangjie.decompiler.CangJieMetadataDecompiler:27"
     * ```
     *
     * 开发者可以快速定位使用的反编译器和版本。
     *
     * ## 工作流程
     *
     * ```
     * IDE 启动/索引更新
     *   ↓
     * 1. 读取缓存的 Stub 版本字符串
     *    例如: "...CangJieMetadataDecompiler:26"
     *   ↓
     * 2. 调用 getSubBuilderVersion(decompiler)
     *    返回: "...CangJieMetadataDecompiler:27"
     *   ↓
     * 3. 比较版本字符串
     *    26 != 27 → 版本不匹配
     *   ↓
     * 4. 清除旧 Stub 缓存
     *   ↓
     * 5. 重新构建 Stub
     *   ↓
     * 6. 保存新 Stub 和新版本号
     * ```
     *
     * ## null 处理
     *
     * 如果 decompiler 参数为 null：
     * - 返回 "default" 字符串
     * - 表示没有特定的反编译器
     * - IDE 使用默认的 Stub 构建逻辑（通常是不构建 Stub）
     *
     * **何时为 null？**
     * - [getSubBuilder] 返回 null（没有反编译器接受该文件）
     * - IDE 调用 buildStubTree(fileContent, null)
     *
     * ## 版本号来源
     *
     * 版本号来自反编译器的 Stub 构建器：
     * ```kotlin
     * CangJieMetadataDecompiler
     *   ↓
     * .stubBuilder
     *   ↓
     * .stubVersion  // 通常是 STUB_VERSION 常量
     * ```
     *
     * ### 版本号递增时机
     *
     * 当以下任何情况发生时，反编译器应递增版本号：
     * - **Stub 结构变化**: 添加/删除/修改字段
     * - **元数据格式变化**: Flatbuffers schema 更新
     * - **序列化格式变化**: Stub 序列化方式改变
     * - **构建逻辑变化**: Stub 构建算法调整
     *
     * ## 与全局 STUB_VERSION 的关系
     *
     * 本类有全局 [STUB_VERSION] 常量（当前为 27）：
     * - 所有反编译器通常共享相同的版本号
     * - 当元数据格式全面更新时，统一递增
     * - 各反编译器也可以独立维护版本号
     *
     * | 版本管理策略 | 优点 | 缺点 |
     * |------------|------|------|
     * | 全局统一版本 | 管理简单，一致性强 | 一个变化导致全部重建 |
     * | 独立版本 | 精确控制重建范围 | 管理复杂，容易遗漏 |
     *
     * **当前策略**: 全局统一版本（使用 [STUB_VERSION]）。
     *
     * ## 示例
     *
     * ### 正常场景
     *
     * ```kotlin
     * val decompiler = CangJieMetadataDecompiler()
     * val version = getSubBuilderVersion(decompiler)
     * // 返回: "org.cangnova.cangjie.decompiler.CangJieMetadataDecompiler:27"
     * ```
     *
     * ### null 场景
     *
     * ```kotlin
     * val version = getSubBuilderVersion(null)
     * // 返回: "default"
     * ```
     *
     * ## 性能考虑
     *
     * - **调用频率**: 每个文件在索引时调用一次，然后缓存
     * - **开销**: 字符串拼接（< 1μs）
     * - **缓存**: IDE 缓存版本字符串，不重复计算
     *
     * ## 调试技巧
     *
     * 如果 Stub 未正确更新，检查版本字符串：
     * ```kotlin
     * LOG.debug("Stub version for ${file.name}: ${getSubBuilderVersion(decompiler)}")
     * ```
     *
     * 确保：
     * 1. 版本号确实递增了
     * 2. 类名没有意外变化
     * 3. 反编译器正确注册
     *
     * @param decompiler 反编译器实例，可以为 null
     * @return 复合版本字符串 "<类名>:<版本号>"，如果 decompiler 为 null 则返回 "default"
     *
     * @see STUB_VERSION
     * @see ClassFileDecompilers.Full.stubBuilder
     * @see ClsStubBuilder.stubVersion
     */
    override fun getSubBuilderVersion(decompiler: ClassFileDecompilers.Full?): String {
        if (decompiler == null) return "default"
        val version: Int = decompiler.stubBuilder.stubVersion
        return decompiler::class.java.name + ":" + version
    }

    /**
     * 构建文件的 Stub 树
     *
     * ## 功能说明
     *
     * 这是反编译系统的核心方法，负责将二进制元数据文件（.cjo/.cjb）转换为 IDE 可索引的 Stub 树。
     * 该方法集成了延迟构建、错误处理、性能优化等关键机制。
     *
     * ## 工作流程
     *
     * ```
     * buildStubTree(fileContent, decompiler) 调用
     *   ↓
     * 1. null 检查
     *    if (decompiler == null) return null
     *   ↓
     * 2. 项目初始化检查（延迟构建机制）
     *    ├─ 获取 CjProjectsService
     *    ├─ 检查 projectsService.initialized
     *    ├─ 未初始化 → 返回 null (延迟构建)
     *    └─ 已初始化 → 继续
     *   ↓
     * 3. computeWithPreloadedContentHint
     *    ├─ 缓存文件内容到内存
     *    └─ 执行反编译
     *   ↓
     * 4. 调用反编译器
     *    decompiler.stubBuilder.buildFileStub(fileContent)
     *      ↓
     *      CangJieMetadataStubBuilder.buildFileStub()
     *        ├─ 读取 Flatbuffers 元数据
     *        ├─ 解析包信息、类型声明
     *        ├─ 构建 Stub 树
     *        └─ 返回 CangJieFileStubImpl
     *   ↓
     * 5. 错误处理
     *    catch (ClsFormatException)
     *      ├─ Debug 模式 → 记录完整堆栈
     *      ├─ 普通模式 → 记录错误消息
     *      └─ 返回 null
     *   ↓
     * 6. 返回 Stub 树（或 null）
     * ```
     *
     * ## 延迟构建机制
     *
     * ### 为什么需要延迟？
     *
     * Stub 构建依赖跨包类型引用解析，而跨包引用解析依赖 [CjoPackageService]，
     * CjoPackageService 又依赖项目的 Workspace Model。如果 Workspace Model 未就绪，
     * 跨包引用将无法解析，导致 Stub 不完整。
     *
     * **依赖链**:
     * ```
     * Stub 构建
     *   ↓ 需要
     * 跨包类型引用解析
     *   ↓ 需要
     * CjoPackageService
     *   ↓ 需要
     * Workspace Model 同步完成
     * ```
     *
     * ### 延迟逻辑
     *
     * ```kotlin
     * val projectsService = project.getServiceIfCreated(CjProjectsService::class.java)
     * if (projectsService != null && !projectsService.initialized) {
     *     // 工作空间未初始化，延迟构建
     *     LOG.debug("Workspace not initialized, delaying stub build for: ${fileContent.fileName}")
     *     return null
     * }
     * ```
     *
     * **关键点**:
     * - 使用 `getServiceIfCreated` 而不是 `getService`，避免触发服务初始化
     * - 检查 `projectsService.initialized` 标志（由 CjProjectsService 维护）
     * - 返回 `null` 表示"暂时无法构建 Stub"
     *
     * ### 何时重试？
     *
     * IDE 会在以下时机重新尝试构建 Stub：
     * 1. **项目初始化完成**: CjProjectsService 设置 `initialized = true`
     * 2. **触发重新索引**: IDE 自动或手动触发文件索引更新
     * 3. **文件修改事件**: 虚拟文件变化时
     *
     * **重试流程**:
     * ```
     * 项目同步完成
     *   ↓
     * CjProjectsService.initialized = true
     *   ↓
     * IDE 触发重新索引
     *   ↓
     * 重新调用 buildStubTree() for 所有延迟的文件
     *   ├─ 项目已初始化 → 继续构建
     *   ├─ CjoPackageService 可用
     *   └─ 跨包引用正确解析
     *   ↓
     * 完整的 Stub 树被构建和缓存
     * ```
     *
     * ## computeWithPreloadedContentHint 优化
     *
     * 在构建 Stub 前，将文件内容预加载到内存：
     * ```kotlin
     * fileContent.file.computeWithPreloadedContentHint(
     *     fileContent.content,
     *     Supplier { /* 反编译逻辑 */ }
     * )
     * ```
     *
     * **作用**:
     * - **避免重复 I/O**: 反编译器可能多次读取文件（例如检查签名、解析元数据）
     * - **性能提升**: 从内存读取比从磁盘读取快 100-1000 倍
     * - **一致性**: 确保整个构建过程看到相同的文件内容
     *
     * ## 错误处理
     *
     * ### ClsFormatException 捕获
     *
     * 捕获反编译过程中的格式错误：
     * ```kotlin
     * try {
     *     return@Supplier decompiler.stubBuilder.buildFileStub(fileContent)
     * } catch (e: ClsFormatException) {
     *     if (LOG.isDebugEnabled) {
     *         LOG.debug(file.path, e)  // 完整堆栈
     *     } else {
     *         LOG.info(file.path + ": " + e.message)  // 仅消息
     *     }
     *     return null
     * }
     * ```
     *
     * ### 典型失败场景
     *
     * | 场景 | 异常原因 | 日志级别 | 处理方式 |
     * |------|---------|---------|---------|
     * | 元数据损坏 | 文件被截断或损坏 | INFO | 返回 null |
     * | 不兼容版本 | 元数据格式版本不匹配 | INFO | 返回 null |
     * | Flatbuffers 错误 | 元数据结构不合法 | INFO | 返回 null |
     * | 跨包引用失败 | CjoPackageService 未就绪 | DEBUG | 延迟构建 |
     *
     * ### 日志级别选择
     *
     * - **Debug 模式** (`LOG.isDebugEnabled`): 记录完整异常堆栈，用于开发调试
     * - **普通模式**: 仅记录文件路径和错误消息，避免日志膨胀
     *
     * **原理**: 元数据格式错误是可预期的（例如用户库损坏），不应该作为严重错误处理。
     *
     * ## 返回值语义
     *
     * | 返回值 | 含义 | IDE 行为 |
     * |--------|------|---------|
     * | Stub 树 | 成功构建 | 缓存 Stub，启用索引 |
     * | null | 无法构建或延迟 | 不缓存，稍后重试 |
     *
     * **注意**: 返回 null 不是错误，它表示"暂时无法构建"或"不支持此文件"。
     *
     * ## 与其他方法的交互
     *
     * ```
     * IDE 索引流程
     *   ↓
     * getFileFilter() → 批量过滤
     *   ↓
     * acceptsFile() → 单文件验证
     *   ↓
     * getSubBuilder() → 选择反编译器
     *   ↓
     * getSubBuilderVersion() → 获取版本号
     *   ↓
     * [buildStubTree()] ← 构建 Stub (本方法)
     *   ↓
     * getStubVersion() → 获取全局版本
     *   ↓
     * IDE 缓存 Stub
     * ```
     *
     * ## 性能特征
     *
     * ### 时间复杂度
     *
     * - **缓存命中**: O(1) - 直接从缓存返回
     * - **缓存未命中**: O(n) - n 为元数据文件大小
     *   - 读取 Flatbuffers: ~10-50ms
     *   - 构建 Stub 树: ~5-20ms
     *   - 写入缓存: ~1-5ms
     *   - **总计**: ~15-75ms per file
     *
     * ### 内存使用
     *
     * - **文件内容**: 文件大小（例如 100KB）
     * - **Stub 树**: 约为文件大小的 50-200%（例如 50-200KB）
     * - **临时对象**: ~1-10MB（GC 后释放）
     *
     * ### 优化建议
     *
     * 1. **批量索引**: IDE 并发处理多个文件，利用多核 CPU
     * 2. **延迟构建**: 减少无效构建，提高启动速度
     * 3. **缓存**: Stub 持久化到磁盘，避免重复构建
     *
     * ## 调试技巧
     *
     * ### 启用 Debug 日志
     *
     * 在 IDE 中启用 Debug 日志：
     * ```
     * Help → Diagnostic Tools → Debug Log Settings
     * 添加: #org.cangnova.cangjie.decompiler
     * ```
     *
     * ### 查看 Stub 构建状态
     *
     * ```kotlin
     * LOG.debug("Building stub for: ${fileContent.fileName}")
     * LOG.debug("Decompiler: ${decompiler::class.simpleName}")
     * LOG.debug("Project initialized: ${projectsService?.initialized}")
     * LOG.debug("Stub tree size: ${stubTree.childrenStubs.size}")
     * ```
     *
     * ### 常见问题诊断
     *
     * **问题 1**: Stub 未生成
     * - 检查: decompiler 是否为 null
     * - 检查: 项目是否初始化
     * - 检查: 是否有 ClsFormatException
     *
     * **问题 2**: 跨包引用失败
     * - 检查: CjProjectsService.initialized 是否为 true
     * - 检查: CjoPackageService 是否可用
     * - 检查: Workspace Model 是否同步完成
     *
     * **问题 3**: 性能问题
     * - 检查: 是否有大量文件延迟构建
     * - 检查: Stub 缓存是否正常工作
     * - 检查: 文件扫描范围是否过大
     *
     * ## 线程安全
     *
     * - **方法本身**: 无共享状态，线程安全
     * - **反编译器**: 由反编译器实现保证线程安全
     * - **文件内容**: fileContent 是不可变的，安全共享
     * - **项目服务**: CjProjectsService 内部线程安全
     *
     * ## 扩展点
     *
     * 如果需要自定义构建逻辑，可以：
     * 1. 继承本类并覆盖 buildStubTree 方法
     * 2. 添加自定义的延迟条件或错误处理
     * 3. 在 plugin.xml 中注册自定义构建器
     *
     * @param fileContent 文件内容，包含虚拟文件、字节数组、项目上下文
     * @param decompiler 选中的反编译器，可以为 null（表示没有合适的反编译器）
     * @return 构建的 Stub 树，如果无法构建则返回 null
     *
     * @see CjProjectsService
     * @see CjoPackageService
     * @see ClassFileDecompilers.Full
     * @see ClsStubBuilder.buildFileStub
     * @see ClsFormatException
     */
    override fun buildStubTree(fileContent: FileContent, decompiler: ClassFileDecompilers.Full?): Stub? {
        if (decompiler == null) return null

        // 延迟 stub 构建，直到工作空间模型同步完成
        // 这样可以确保 CjoPackageService 能够正确解析跨包引用
        val project = fileContent.project
        if (project != null) {
            val projectsService = project.getServiceIfCreated( CjProjectsService::class.java)
            if (projectsService != null && !projectsService.initialized) {
                // 工作空间模型未同步完成，延迟 stub 构建
                if (LOG.isDebugEnabled) {
                    LOG.debug("Workspace not initialized, delaying stub build for: ${fileContent.fileName}")
                }
                return null
            }
        }

        // 当 FileContent 是从文本创建时（例如在 Stub 一致性检查中），
        // 无法访问 content 字节数组，此时直接构建 Stub 而不预加载内容
        return runCatching {
            fileContent.file.computeWithPreloadedContentHint(
                fileContent.content,
                Supplier {
                    val file = fileContent.file
                    try {
                        return@Supplier decompiler.stubBuilder.buildFileStub(fileContent)
                    } catch (e: ClsFormatException) {
                        if (LOG.isDebugEnabled) {
                            LOG.debug(file.path, e)
                        } else {
                            LOG.info(file.path + ": " + e.message)
                        }
                    }
                    null
                },
            )
        }.getOrElse {
            // 如果访问 content 失败（文本创建的 FileContent），直接构建 Stub
            val file = fileContent.file
            try {
                decompiler.stubBuilder.buildFileStub(fileContent)
            } catch (e: ClsFormatException) {
                if (LOG.isDebugEnabled) {
                    LOG.debug(file.path, e)
                } else {
                    LOG.info(file.path + ": " + e.message)
                }
                null
            }
        }
    }

    /**
     * 获取全局 Stub 版本号
     *
     * ## 功能说明
     *
     * 返回本构建器的全局 Stub 版本号。该版本号用于 IDE 的 Stub 缓存失效机制，
     * 当版本号变化时，IDE 会自动清除旧缓存并重新构建所有文件的 Stub。
     *
     * ## 实现
     *
     * 直接返回 companion object 中定义的 [STUB_VERSION] 常量：
     * ```kotlin
     * override fun getStubVersion(): Int {
     *     return STUB_VERSION  // 当前为 27
     * }
     * ```
     *
     * ## 与 getSubBuilderVersion() 的区别
     *
     * | 方法 | 作用域 | 返回类型 | 用途 |
     * |------|-------|---------|------|
     * | getStubVersion() | 全局（本构建器） | Int | IDE 框架版本检查 |
     * | getSubBuilderVersion() | 单个反编译器 | String | 反编译器级别版本检查 |
     *
     * **协作方式**:
     * - getStubVersion() 提供基础版本号（27）
     * - getSubBuilderVersion() 在此基础上添加反编译器类名
     * - 最终版本字符串：`"<类名>:27"`
     *
     * ## 工作流程
     *
     * ```
     * IDE 启动/索引更新
     *   ↓
     * 1. 调用 getStubVersion()
     *    返回: 27
     *   ↓
     * 2. 读取缓存的版本号
     *    例如: 26
     *   ↓
     * 3. 比较版本号
     *    27 != 26 → 版本不匹配
     *   ↓
     * 4. 清除所有 Stub 缓存
     *   ↓
     * 5. 重新构建所有文件的 Stub
     *   ↓
     * 6. 保存新 Stub 和新版本号 (27)
     * ```
     *
     * ## 版本号递增时机
     *
     * 以下任何变化都应该递增 [STUB_VERSION]：
     *
     * ### 1. Stub 结构变化
     *
     * - 添加新的 Stub 字段
     * - 删除或修改现有字段
     * - 改变 Stub 树的层次结构
     *
     * ```kotlin
     * // 例如：在 CangJieClassStubImpl 中添加新字段
     * data class CangJieClassStubImpl(
     *     val name: String,
     *     val superTypes: List<String>,
     *     val isSealed: Boolean  // 新增字段 → 递增版本号!
     * )
     * ```
     *
     * ### 2. 序列化格式变化
     *
     * - 修改 Stub 的序列化/反序列化逻辑
     * - 改变存储布局或编码方式
     *
     * ### 3. 元数据格式变化
     *
     * - Flatbuffers schema 更新
     * - 元数据版本升级
     * - 二进制格式调整
     *
     * ### 4. 构建逻辑变化
     *
     * - Stub 构建算法调整
     * - 跨包引用解析逻辑改变
     * - 类型推导规则修改
     *
     * ## 为什么需要版本号？
     *
     * ### 缓存失效机制
     *
     * IDE 将 Stub 缓存到磁盘以加速启动。如果 Stub 结构变化，旧缓存会导致：
     * - **反序列化失败**: 字段不匹配
     * - **NPE 异常**: 缺少新增字段
     * - **数据损坏**: 类型不兼容
     *
     * 版本号变化会触发自动清除旧缓存，避免这些问题。
     *
     * ### 示例场景
     *
     * **场景 1: 插件更新**
     * ```
     * 用户更新插件: v1.0 → v1.1
     *   ├─ v1.0 使用 STUB_VERSION = 26
     *   └─ v1.1 使用 STUB_VERSION = 27
     *   ↓
     * IDE 检测到版本不匹配
     *   ↓
     * 清除旧缓存，重新构建 Stub
     * ```
     *
     * **场景 2: 开发调试**
     * ```
     * 开发者修改 Stub 结构
     *   ↓
     * 递增 STUB_VERSION: 27 → 28
     *   ↓
     * 重新运行 IDE
     *   ↓
     * IDE 自动清除旧 Stub，使用新结构
     * ```
     *
     * ## 性能影响
     *
     * ### 版本号不变
     *
     * - **首次启动**: 构建 Stub 并缓存（慢）
     * - **后续启动**: 直接读取缓存（快，< 1 秒）
     *
     * ### 版本号变化
     *
     * - **首次启动**: 清除旧缓存 + 重新构建（慢）
     * - **后续启动**: 读取新缓存（快）
     *
     * **建议**: 仅在必要时递增版本号，避免不必要的缓存重建。
     *
     * ## 与全局版本策略
     *
     * 当前使用**全局统一版本号**策略：
     * - 所有反编译器共享相同的版本号 ([STUB_VERSION])
     * - 任何反编译器的变化都递增全局版本号
     * - 简化版本管理，确保一致性
     *
     * **优点**:
     * - 管理简单，不会遗漏
     * - 避免版本号冲突
     * - 保证所有 Stub 同步更新
     *
     * **缺点**:
     * - 一个反编译器变化导致所有文件重建
     * - 可能引入不必要的缓存失效
     *
     * ## 调试技巧
     *
     * ### 检查当前版本号
     *
     * ```kotlin
     * LOG.info("Current Stub version: ${ClassFileStubBuilder.STUB_VERSION}")
     * ```
     *
     * ### 强制清除缓存
     *
     * 如果遇到缓存问题：
     * 1. 递增 STUB_VERSION
     * 2. 或手动删除缓存：`File → Invalidate Caches / Restart`
     *
     * ### 验证版本匹配
     *
     * ```kotlin
     * val cachedVersion = stubIndexCache.getVersion()
     * val currentVersion = getStubVersion()
     * if (cachedVersion != currentVersion) {
     *     LOG.info("Stub version mismatch: $cachedVersion != $currentVersion, rebuilding...")
     * }
     * ```
     *
     * ## 历史版本参考
     *
     * 版本号的变化历史（供参考）：
     * - v27: 当前版本
     * - v26: 之前版本（可能的变更原因：Stub 结构调整）
     * - ...（更早版本）
     *
     * **注意**: 版本号历史通常不需要详细记录，重要的是保持递增。
     *
     * @return 全局 Stub 版本号，当前为 27
     *
     * @see STUB_VERSION
     * @see getSubBuilderVersion
     */
    override fun getStubVersion(): Int {
        return STUB_VERSION
    }

    companion object {
        private val LOG = Logger.getInstance(
            ClassFileStubBuilder::class.java,
        )

        /**
         * 全局 Stub 版本号
         *
         * ## 功能说明
         *
         * 定义本反编译系统的 Stub 版本号。该常量是整个 Stub 缓存机制的核心，
         * 用于标识 Stub 结构的版本，确保缓存有效性。
         *
         * ## 当前版本
         *
         * ```kotlin
         * const val STUB_VERSION: Int = 27
         * ```
         *
         * ## 何时递增
         *
         * **必须递增的场景**:
         *
         * 1. **Stub 数据结构变化**
         *    - 添加/删除/修改 Stub 类的字段
         *    - 改变 Stub 类的继承关系
         *    - 调整 Stub 树的层次结构
         *
         * 2. **序列化格式变化**
         *    - 修改 Stub 序列化算法
         *    - 改变二进制存储布局
         *    - 调整压缩或编码方式
         *
         * 3. **元数据格式升级**
         *    - Flatbuffers schema 版本更新
         *    - 二进制元数据格式变化
         *    - 新增或移除元数据字段
         *
         * 4. **构建逻辑调整**
         *    - Stub 构建算法改变
         *    - 跨包引用解析规则修改
         *    - 类型推导逻辑更新
         *
         * ## 使用位置
         *
         * 该常量在多个地方被引用：
         *
         * ### 1. 本类的 getStubVersion()
         * ```kotlin
         * override fun getStubVersion(): Int = STUB_VERSION
         * ```
         *
         * ### 2. 反编译器的 stubVersion
         * ```kotlin
         * class CangJieMetadataDecompiler : ClassFileDecompilers.Full {
         *     override val stubBuilder = object : ClsStubBuilder {
         *         override val stubVersion = STUB_VERSION
         *     }
         * }
         * ```
         *
         * ### 3. getSubBuilderVersion() 的复合版本
         * ```kotlin
         * override fun getSubBuilderVersion(decompiler: ClassFileDecompilers.Full?): String {
         *     val version = decompiler.stubBuilder.stubVersion  // = STUB_VERSION
         *     return "${decompiler::class.java.name}:$version"
         * }
         * ```
         *
         * ## 版本管理策略
         *
         * ### 全局统一版本 (当前策略)
         *
         * 所有反编译器共享同一个 STUB_VERSION：
         * - **CangJieMetadataDecompiler**: 使用 STUB_VERSION (27)
         * - **CangJieBuiltInDecompiler**: 使用 STUB_VERSION (27)
         * - **未来的反编译器**: 也使用 STUB_VERSION
         *
         * **优点**:
         * - 管理简单，只需维护一个版本号
         * - 避免版本号冲突和不一致
         * - 确保所有 Stub 同步更新
         *
         * **缺点**:
         * - 一个反编译器变化导致所有文件重建
         * - 可能引入不必要的性能开销
         *
         * ### 独立版本策略 (可选)
         *
         * 每个反编译器维护独立版本号：
         * - **CangJieMetadataDecompiler**: stubVersion = 27
         * - **CangJieBuiltInDecompiler**: stubVersion = 15
         *
         * **优点**:
         * - 精确控制缓存失效范围
         * - 避免无关的 Stub 重建
         *
         * **缺点**:
         * - 管理复杂，容易遗漏
         * - 需要跟踪多个版本号
         *
         * **当前选择**: 全局统一版本，因为简单性优于性能微优化。
         *
         * ## 递增流程
         *
         * 当需要递增版本号时：
         *
         * ### 1. 修改常量
         * ```kotlin
         * const val STUB_VERSION: Int = 28  // 27 → 28
         * ```
         *
         * ### 2. 提交代码
         * ```bash
         * git commit -m "feat: update STUB_VERSION to 28 for new Stub structure"
         * ```
         *
         * ### 3. 测试缓存清除
         * - 运行 IDE
         * - 验证旧缓存被清除
         * - 验证新 Stub 正确构建
         *
         * ### 4. 文档更新
         * - 在版本历史中记录变更（如果需要）
         * - 更新相关文档说明
         *
         * ## 缓存失效机制
         *
         * IDE 如何使用版本号管理缓存：
         *
         * ```
         * IDE 读取 Stub 缓存
         *   ↓
         * 检查缓存版本 == STUB_VERSION?
         *   ├─ Yes → 使用缓存（快速启动）
         *   └─ No  → 清除缓存 + 重新构建
         *       ↓
         *       遍历所有 .cjo/.cjb 文件
         *       ↓
         *       调用 buildStubTree() 重建 Stub
         *       ↓
         *       保存新 Stub 和新版本号
         *       ↓
         *       下次启动直接使用新缓存
         * ```
         *
         * ## 性能考虑
         *
         * ### 版本号不变
         * - 首次启动：构建 Stub（可能 10-60 秒，取决于项目大小）
         * - 后续启动：读取缓存（< 1 秒）
         *
         * ### 版本号递增
         * - 首次启动：清除缓存 + 重新构建（慢）
         * - 后续启动：读取新缓存（快）
         *
         * **建议**: 只在必要时递增，避免频繁的缓存重建。
         *
         * ## 与 CangJieStubVersions 的关系
         *
         * 项目中可能还有其他版本号常量（如 CangJieStubVersions.kt）：
         * - 这些通常用于不同的缓存层级或序列化格式
         * - 与 STUB_VERSION 协同工作，但作用域不同
         *
         * **STUB_VERSION** 专注于本反编译系统的 Stub 缓存。
         *
         * ## 调试技巧
         *
         * ### 查看版本号
         * ```kotlin
         * LOG.info("STUB_VERSION: ${ClassFileStubBuilder.STUB_VERSION}")
         * ```
         *
         * ### 强制触发缓存重建
         * 临时修改版本号可以强制清除缓存：
         * ```kotlin
         * const val STUB_VERSION: Int = 999  // 临时版本号
         * ```
         *
         * 测试后恢复正常版本号。
         *
         * ### 验证缓存状态
         * ```
         * File → Invalidate Caches / Restart
         * → 查看 Stub 重建日志
         * ```
         *
         * ## 历史记录
         *
         * - **v27**: 当前版本
         * - **v26**: 前一个版本
         * - ...
         *
         * **注意**: 详细的版本历史通常不需要维护，重要的是保持递增和一致性。
         *
         * @see getStubVersion
         * @see getSubBuilderVersion
         */
        const val STUB_VERSION: Int = 27
    }
}
