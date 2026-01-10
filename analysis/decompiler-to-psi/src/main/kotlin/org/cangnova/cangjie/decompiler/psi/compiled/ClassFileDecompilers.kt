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

package org.cangnova.cangjie.decompiler.psi.compiled

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager

/**
 * 类文件反编译器扩展点 API
 *
 * ## 架构概述
 *
 * ClassFileDecompilers 是 IntelliJ Platform 的扩展点管理服务，提供可插拔的反编译器注册和查找机制。
 * 它允许插件注册自定义反编译器来处理非 Java 语言编译生成的二进制文件（如 .class、.cjo、.cjb）。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * IntelliJ Platform
 *   ↓
 * BinaryFileTypeDecompilers (框架层)
 *   ↓
 * ClassFileDecompilers (本服务 - 扩展点管理)
 *   ├─ EP_NAME 扩展点 (org.cangnova.cangjie.classFileDecompiler)
 *   └─ find() 方法 (查找匹配的反编译器)
 *   ↓
 * 注册的反编译器实现
 *   ├─ Light 反编译器 (轻量级扩展)
 *   │   └─ 增强默认反编译器输出
 *   └─ Full 反编译器 (完整实现)
 *       ├─ CangJieMetadataDecompiler (.cjo 文件)
 *       ├─ CangJieBuiltInDecompiler (.cjb 文件)
 *       └─ (其他语言反编译器)
 * ```
 *
 * ## 核心职责
 *
 * 1. **扩展点管理**: 提供 EP_NAME 扩展点，支持动态注册和卸载反编译器
 * 2. **反编译器查找**: find() 方法根据文件类型和反编译器类型查找合适的实现
 * 3. **变更通知**: 监听扩展点变化，通知 IDE 框架刷新反编译器集合
 * 4. **单例服务**: 作为应用级服务，全局唯一实例
 *
 * ## 反编译器分层设计
 *
 * ### Light vs Full 对比
 *
 * | 特性 | Light 反编译器 | Full 反编译器 |
 * |------|---------------|--------------|
 * | **作用** | 增强默认反编译器 | 完全自定义反编译 |
 * | **输出** | 修改 IDEA 生成的 Java 代码 | 生成原始语言代码 |
 * | **Stub 支持** | ❌ 不支持 | ✅ 完整支持 |
 * | **PSI 树** | ❌ 使用 Java PSI | ✅ 自定义语言 PSI |
 * | **复杂度** | 低（仅实现 getText()） | 高（需实现 Stub 和 ViewProvider） |
 * | **典型用途** | 添加注释、格式化 | Kotlin、Scala、CangJie 等 |
 * | **性能开销** | 小 | 中等 |
 * | **示例** | 添加元数据注释 | CangJieMetadataDecompiler |
 *
 * ### Light 反编译器特点
 *
 * **设计目的**: 轻量级增强，不改变文件结构
 *
 * **典型用法**:
 * ```kotlin
 * class MyLightDecompiler : ClassFileDecompilers.Light() {
 *     override fun accepts(file: VirtualFile): Boolean {
 *         return file.extension == "class" && hasSpecialMetadata(file)
 *     }
 *
 *     override fun getText(file: VirtualFile): CharSequence {
 *         val defaultText = getDefaultDecompiledText(file)
 *         return "// Special Metadata: ...\n" + defaultText
 *     }
 * }
 * ```
 *
 * **注册建议**:
 * - 接受所有文件（或大范围文件）
 * - 使用 `order="last"` 避免干扰其他反编译器
 * - 抛出 CannotDecompileException 回退到默认实现
 *
 * **适用场景**:
 * - 为编译代码添加额外注释
 * - 替换 `/* compiled code */` 为更有意义的说明
 * - 显示调试信息或元数据
 *
 * ### Full 反编译器特点
 *
 * **设计目的**: 完整语言支持，提供自定义 PSI 和 Stub
 *
 * **典型用法**:
 * ```kotlin
 * class MyFullDecompiler : ClassFileDecompilers.Full() {
 *     override fun accepts(file: VirtualFile): Boolean {
 *         return file.extension == "myext"
 *     }
 *
 *     override val stubBuilder: ClsStubBuilder
 *         get() = MyStubBuilder
 *
 *     override fun createFileViewProvider(
 *         file: VirtualFile,
 *         manager: PsiManager,
 *         physical: Boolean
 *     ): FileViewProvider {
 *         return MyFileViewProvider(manager, file, physical)
 *     }
 * }
 * ```
 *
 * **实现要求**:
 * 1. **stubBuilder**: 提供 Stub 构建器，用于快速索引
 * 2. **createFileViewProvider()**: 创建自定义 ViewProvider，返回正确的语言
 * 3. **accepts()**: 精确匹配文件类型
 *
 * **适用场景**:
 * - Kotlin (.class) → Kotlin 源码
 * - Scala (.class) → Scala 源码
 * - CangJie (.cjo/.cjb) → CangJie 源码
 *
 * ## 扩展点机制
 *
 * ### 注册流程
 *
 * ```
 * plugin.xml 中定义
 *   ↓
 * <classFileDecompiler implementation="com.example.MyDecompiler"/>
 *   ↓
 * IDE 加载插件
 *   ↓
 * EP_NAME.extensionList 添加实例
 *   ↓
 * changeListener 触发
 *   ↓
 * BinaryFileTypeDecompilers.notifyDecompilerSetChange()
 *   ↓
 * IDE 刷新反编译器缓存
 * ```
 *
 * ### plugin.xml 配置
 *
 * ```xml
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *   <!-- Light 反编译器示例 -->
 *   <classFileDecompiler
 *     implementation="com.example.MyLightDecompiler"
 *     order="last"/>
 *
 *   <!-- Full 反编译器示例 -->
 *   <classFileDecompiler
 *     implementation="org.cangnova.cangjie.decompiler.CangJieMetadataDecompiler"/>
 * </extensions>
 * ```
 *
 * ### 动态扩展支持
 *
 * 扩展点监听器确保动态加载/卸载插件时刷新反编译器：
 * ```kotlin
 * EP_NAME.addChangeListener({
 *     BinaryFileTypeDecompilers.getInstance().notifyDecompilerSetChange()
 * }, null)
 * ```
 *
 * ## 反编译器查找机制
 *
 * ### find() 方法工作流程
 *
 * ```
 * find(file, Full::class.java) 调用
 *   ↓
 * 1. EP_NAME.findFirstSafe { } (遍历所有注册的反编译器)
 *   ↓
 * 2. 对每个反编译器检查:
 *    ├─ decompilerClass.isInstance(d)? (类型匹配?)
 *    │   ├─ Light.class.isInstance() → 检查是否为 Light
 *    │   └─ Full.class.isInstance() → 检查是否为 Full
 *    └─ d.accepts(file)? (文件匹配?)
 *        ├─ CangJieMetadataDecompiler.accepts(ArrayList.cjo) → true
 *        └─ 其他反编译器.accepts() → false
 *   ↓
 * 3. 返回第一个同时满足类型和文件匹配的反编译器
 * ```
 *
 * ### 匹配优先级
 *
 * 反编译器按注册顺序查找，第一个匹配的被返回：
 * - **注册顺序**: 由 plugin.xml 中的顺序决定
 * - **order 属性**: 可通过 `order="first|last"` 控制顺序
 * - **短路求值**: 找到匹配后立即返回，不继续查找
 *
 * ### 典型查找场景
 *
 * **场景 1: 查找 Full 反编译器处理 .cjo 文件**
 * ```
 * ClassFileDecompilers.instance.find(ArrayList.cjo, Full::class.java)
 *   ↓
 * 遍历: CangJieMetadataDecompiler, CangJieBuiltInDecompiler, ...
 *   ↓
 * CangJieMetadataDecompiler.accepts(ArrayList.cjo)
 *   └─ file.extension == "cjo" → true ✓
 *   ↓
 * 返回: CangJieMetadataDecompiler 实例
 * ```
 *
 * **场景 2: 查找 Light 反编译器（无匹配）**
 * ```
 * ClassFileDecompilers.instance.find(file, Light::class.java)
 *   ↓
 * 遍历所有注册的反编译器
 *   ├─ CangJieMetadataDecompiler: Full 类型，跳过
 *   ├─ CangJieBuiltInDecompiler: Full 类型，跳过
 *   └─ (没有 Light 反编译器注册)
 *   ↓
 * 返回: null (通过 findFirstSafe 的空安全机制)
 * ```
 *
 * ## 使用场景
 *
 * ### 1. 为新语言添加反编译支持
 *
 * ```kotlin
 * // 1. 实现 Full 反编译器
 * class MyLanguageDecompiler : ClassFileDecompilers.Full() {
 *     override fun accepts(file: VirtualFile) = file.extension == "mylang"
 *     override val stubBuilder = MyStubBuilder()
 *     override fun createFileViewProvider(...) = MyFileViewProvider(...)
 * }
 *
 * // 2. 在 plugin.xml 中注册
 * <classFileDecompiler implementation="com.example.MyLanguageDecompiler"/>
 *
 * // 3. IDE 自动发现并使用
 * ```
 *
 * ### 2. 增强 Java 反编译器输出
 *
 * ```kotlin
 * // 1. 实现 Light 反编译器
 * class EnhancedJavaDecompiler : ClassFileDecompilers.Light() {
 *     override fun accepts(file: VirtualFile) = true  // 接受所有文件
 *
 *     override fun getText(file: VirtualFile): CharSequence {
 *         val defaultText = readDefaultDecompiledText(file)
 *         val metadata = extractMetadata(file)
 *         return buildString {
 *             appendLine("// Metadata: $metadata")
 *             append(defaultText)
 *         }
 *     }
 * }
 *
 * // 2. 在 plugin.xml 中注册为 last
 * <classFileDecompiler
 *   implementation="com.example.EnhancedJavaDecompiler"
 *   order="last"/>
 * ```
 *
 * ### 3. ClassFileStubBuilder 集成
 *
 * ```kotlin
 * // ClassFileStubBuilder 使用 Full 反编译器
 * override fun getSubBuilder(fileContent: FileContent): ClassFileDecompilers.Full? {
 *     return ClassFileDecompilers.instance.find(
 *         fileContent.file,
 *         ClassFileDecompilers.Full::class.java
 *     )
 * }
 * ```
 *
 * ## 错误处理
 *
 * ### CannotDecompileException (Light 反编译器)
 *
 * Light 反编译器可以通过抛出异常回退到默认实现：
 * ```kotlin
 * override fun getText(file: VirtualFile): CharSequence {
 *     if (!canHandle(file)) {
 *         throw Light.CannotDecompileException("Unsupported format", null)
 *     }
 *     // 正常处理...
 * }
 * ```
 *
 * **IDE 行为**:
 * - 捕获 CannotDecompileException
 * - 回退到内置 Java 反编译器
 * - 不显示错误消息（静默回退）
 *
 * ### Full 反编译器错误处理
 *
 * Full 反编译器没有内置的异常回退机制，需要自己处理：
 * ```kotlin
 * override fun createFileViewProvider(...): FileViewProvider {
 *     return try {
 *         MyFileViewProvider(manager, file, physical)
 *     } catch (e: Exception) {
 *         // 创建错误显示的 ViewProvider
 *         ErrorViewProvider(manager, file, physical, e.message)
 *     }
 * }
 * ```
 *
 * ## 性能考虑
 *
 * ### 扩展点查找优化
 *
 * - **findFirstSafe**: 找到第一个匹配后立即返回，避免遍历所有扩展
 * - **accepts() 应快速**: 避免在 accepts() 中执行昂贵操作（如解析文件）
 * - **类型检查优先**: isInstance() 检查在 accepts() 之前，减少无效调用
 *
 * ### 缓存策略
 *
 * IDE 框架会缓存反编译器查找结果：
 * - 每个文件类型只查找一次
 * - 扩展点变化时清除缓存（通过 notifyDecompilerSetChange()）
 *
 * ## 线程安全
 *
 * ### 应用级服务
 *
 * ClassFileDecompilers 是应用级服务（@Service），全局单例：
 * - getInstance() 线程安全（由 ApplicationManager 保证）
 * - EP_NAME.extensionList 线程安全（由 IntelliJ 平台保证）
 *
 * ### 并发查找
 *
 * find() 方法可以安全地并发调用：
 * - 只读操作，不修改共享状态
 * - findFirstSafe 内部线程安全
 *
 * ### 扩展点监听器
 *
 * changeListener 在扩展点变化时被调用：
 * - 监听器执行在 EDT（Event Dispatch Thread）
 * - notifyDecompilerSetChange() 内部线程安全
 *
 * ## 与 IntelliJ 平台的集成
 *
 * ### BinaryFileTypeDecompilers 集成
 *
 * ```
 * IntelliJ Platform 框架
 *   ↓
 * BinaryFileTypeDecompilers (框架服务)
 *   ├─ 管理所有二进制文件类型的反编译器
 *   ├─ 监听 ClassFileDecompilers 的变化
 *   └─ notifyDecompilerSetChange() → 刷新缓存
 * ```
 *
 * ### 文件打开流程集成
 *
 * ```
 * 用户打开 .cjo 文件
 *   ↓
 * IDE 识别为二进制文件类型
 *   ↓
 * BinaryFileTypeDecompilers.getInstance()
 *   ↓
 * ClassFileDecompilers.find(file, Full::class.java)
 *   ↓
 * CangJieMetadataDecompiler.createFileViewProvider()
 *   ↓
 * 显示反编译内容
 * ```
 *
 * ## 扩展性
 *
 * ### 添加新的反编译器类型
 *
 * 可以继承 Decompiler 接口创建新类型：
 * ```kotlin
 * abstract class Hybrid : Decompiler {
 *     // 结合 Light 和 Full 的特性
 * }
 * ```
 *
 * 但推荐使用现有的 Light 或 Full。
 *
 * ### 动态加载反编译器
 *
 * 插件可以在运行时动态注册：
 * ```kotlin
 * val extensionPoint = ClassFileDecompilers.instance.EP_NAME
 * extensionPoint.registerExtension(MyDecompiler(), pluginDisposable)
 * ```
 *
 * ## 调试技巧
 *
 * ### 查看注册的反编译器
 *
 * ```kotlin
 * val all = ClassFileDecompilers.instance.EP_NAME.extensionList
 * all.forEach { decompiler ->
 *     LOG.info("Registered: ${decompiler::class.simpleName}")
 * }
 * ```
 *
 * ### 测试反编译器匹配
 *
 * ```kotlin
 * val decompiler = ClassFileDecompilers.instance.find(testFile, Full::class.java)
 * if (decompiler == null) {
 *     LOG.warn("No decompiler found for: ${testFile.name}")
 * } else {
 *     LOG.info("Matched: ${decompiler::class.simpleName}")
 * }
 * ```
 *
 * ## 已知限制
 *
 * 1. **单一匹配**: find() 只返回第一个匹配的反编译器，无法组合多个
 * 2. **顺序依赖**: 注册顺序影响查找结果，需谨慎配置 order 属性
 * 3. **无回退机制**: Full 反编译器失败时不会自动回退到其他实现
 *
 * ## 最佳实践
 *
 * ### Light 反编译器
 *
 * 1. 使用 `order="last"` 避免干扰
 * 2. accepts() 尽量宽泛（或返回 true）
 * 3. 抛出 CannotDecompileException 回退
 * 4. 避免改变文件结构
 *
 * ### Full 反编译器
 *
 * 1. accepts() 精确匹配文件类型
 * 2. 提供完整的 stubBuilder 实现
 * 3. createFileViewProvider() 返回正确的语言
 * 4. 处理所有错误情况（不依赖回退）
 *
 * @see ClassFileDecompilers.Light
 * @see ClassFileDecompilers.Full
 * @see ClassFileDecompilers.Decompiler
 * @see BinaryFileTypeDecompilers
 * @see CangJieMetadataDecompiler
 */
@Service
class ClassFileDecompilers private constructor() {
    /**
     * 反编译器基接口
     *
     * 实际实现应该继承 [Light] 或 [Full] 类，不符合要求的实现会被静默忽略。
     */
    interface Decompiler {
        /**
         * 判断该反编译器是否接受处理指定的文件
         *
         * @param file 虚拟文件
         * @return 如果接受处理返回 true
         */
        fun accepts(file: VirtualFile): Boolean
    }

    /**
     * 轻量级反编译器
     *
     * "Light" 反编译器用于增强标准 IDEA 反编译器生成的文件内容，而不改变其结构。
     * 例如：
     * - 在注释中提供额外信息
     * - 用更有意义的内容替换标准的 "compiled code" 方法体注释
     *
     * ## 异常处理
     *
     * 如果插件因某种原因无法反编译文件，可以抛出 [CannotDecompileException]，
     * 这会使 IDEA 回退到内置的反编译器实现。
     *
     * ## 注册建议
     *
     * 注册此类型扩展的插件通常应该：
     * - 接受所有文件
     * - 使用 `order="last"` 属性以避免干扰其他反编译器
     */
    abstract class Light : Decompiler {
        /**
         * 无法反编译异常
         *
         * 当反编译器无法处理文件时抛出，触发回退到默认实现。
         */
        class CannotDecompileException(message: String?, cause: Throwable?) :
            RuntimeException(message, cause)

        /**
         * 获取反编译后的文本
         *
         * @param file 要反编译的虚拟文件
         * @return 反编译后的文本内容
         * @throws CannotDecompileException 如果无法反编译
         */
        @Throws(CannotDecompileException::class)
        abstract fun getText(file: VirtualFile): CharSequence
    }

    /**
     * 完整反编译器
     *
     * "Full" 反编译器设计用于为与 Java 显著不同的语言提供扩展支持。
     * 此类型的扩展需要负责：
     * - 构建文件 Stub
     * - 正确地索引 Stub
     *
     * 作为回报，它们能够以更贴近原始语言的方式表示反编译后的文件。
     *
     * ## 实现示例
     *
     * Kotlin、Scala、CangJie 等语言都通过实现此接口提供自定义反编译支持。
     */
    abstract class Full : Decompiler {
        /**
         * Stub 构建器
         *
         * 用于从编译后的文件构建轻量级的 Stub 索引结构。
         */
        abstract val stubBuilder: ClsStubBuilder

        /**
         * 创建文件视图提供器
         *
         * ## 实现者注意事项
         *
         * 1. **语言设置**: 从 [FileViewProvider.getBaseLanguage] 返回正确的语言
         *
         * 2. **调用场景**: 此方法会在两种情况下被调用：
         *    - 构建 PSI 文件
         *    - 获取文档文本（此时 PsiManager 基于默认项目，只会调用 [FileViewProvider.getContents]）
         *
         * 3. **辅助类文件处理**: 语言编译器可能会生成应作为父类一部分处理的辅助 .class 文件。
         *    标准做法是从 [FileViewProvider.getPsi] 返回 `null` 来隐藏这些文件。
         *
         * @param file 虚拟文件
         * @param manager PSI 管理器
         * @param physical 是否为物理文件
         * @return 文件视图提供器
         */
        abstract fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider
    }

    /**
     * 反编译器扩展点名称
     */
    val EP_NAME: ExtensionPointName<Decompiler> = ExtensionPointName("org.cangnova.cangjie.classFileDecompiler")

    init {
        // 监听扩展点变化，通知二进制文件类型反编译器集合已改变
        EP_NAME.addChangeListener({ BinaryFileTypeDecompilers.getInstance().notifyDecompilerSetChange() }, null)
    }

    /**
     * 查找能够处理指定文件的反编译器
     *
     * @param file 要处理的虚拟文件
     * @param decompilerClass 反编译器类型
     * @return 找到的反编译器实例
     */
    fun <D : Decompiler> find(file: VirtualFile, decompilerClass: Class<D>): D {
        return EP_NAME.findFirstSafe { d: Decompiler -> decompilerClass.isInstance(d) && d.accepts(file) } as D
    }

    companion object {
        /**
         * 获取类文件反编译器服务实例
         */
        val instance: ClassFileDecompilers
            get() = ApplicationManager.getApplication().getService(
                ClassFileDecompilers::class.java,
            )
    }
}
