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
import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices


/**
 * 模块信息接口
 *
 * 该接口定义了分析器所需的模块元数据，类似于 Kotlin 的 ModuleInfo。
 *
 * ## 注意事项
 *
 * **该接口已被标记为过时**，正在逐步迁移到 [AnalysisContext]。
 * 新代码应该使用 [AnalysisContext] 而非 ModuleInfo。
 *
 * ## 与 AnalysisContext 的区别
 *
 * - **ModuleInfo**: Kotlin 风格的接口，包含平台相关的分析服务
 * - **AnalysisContext**: 更简洁的接口，专注于分析上下文的基本信息
 *
 * ## 核心职责
 *
 * 1. **模块标识**: 提供模块名称和显示名称
 * 2. **依赖管理**: 定义模块之间的依赖关系
 * 3. **可见性控制**: 管理 internal 声明的可见性
 * 4. **平台支持**: 提供平台特定的分析服务
 * 5. **能力扩展**: 通过 capabilities 机制扩展功能
 *
 * ## 使用场景
 *
 * - **多平台项目**: 通过 expectedBy 支持 expect/actual 机制
 * - **模块依赖**: 管理编译时和运行时的模块依赖关系
 * - **内部可见性**: 控制 internal 声明在模块间的可见性
 * - **平台分析**: 提供平台特定的类型检查和解析逻辑
 *
 * ## 迁移路径
 *
 * 如果你正在使用 ModuleInfo，建议迁移到 AnalysisContext：
 *
 * ```kotlin
 * // 旧代码（ModuleInfo）
 * val moduleInfo: ModuleInfo = ...
 * val dependencies = moduleInfo.dependencies()
 *
 * // 新代码（AnalysisContext）
 * val context: AnalysisContext = ...
 * val dependencies = context.dependencies
 * ```
 *
 * @see org.cangnova.cangjie.descriptors.AnalysisContext
 */
interface ModuleInfo {
    /**
     * 模块名称
     *
     * 用于唯一标识模块的内部名称。
     *
     * @return 模块的 Name 对象
     */
    val name: Name


    /**
     * 显示名称
     *
     * 用于在 UI 中显示的模块名称。默认使用 [name] 的字符串形式。
     *
     * ## 使用场景
     *
     * - **错误消息**: "模块 'my-app' 中找不到符号"
     * - **调试信息**: 显示当前正在分析的模块
     * - **UI 展示**: 在项目结构视图中显示
     *
     * @return 用于显示的模块名称
     */
    val displayedName: String get() = name.asString()

    /**
     * 模块依赖列表
     *
     * 返回此模块直接依赖的所有模块。
     *
     * ## 依赖顺序
     *
     * 列表顺序很重要：
     * 1. **符号解析**: 按顺序查找依赖模块中的符号
     * 2. **内置库位置**: 内置库通常在 SDK 之后、其他依赖之前
     * 3. **重载解析**: 依赖顺序可能影响重载函数的选择
     *
     * ## 示例
     *
     * ```kotlin
     * class MyModuleInfo : ModuleInfo {
     *     override fun dependencies(): List<ModuleInfo> {
     *         return listOf(
     *             this,           // 自依赖
     *             sdkModule,      // SDK
     *             builtInsModule, // 内置库（在 SDK 之后）
     *             stdlib,         // 标准库
     *             dep1, dep2      // 其他依赖
     *         )
     *     }
     * }
     * ```
     *
     * @return 依赖的模块列表，通常包含自身作为第一个元素
     */
      val dependencies: List<ModuleInfo>


    /**
     * 平台相关的分析服务
     *
     * 提供平台特定的类型检查、解析和代码生成逻辑。
     *
     * ## 平台差异
     *
     * 不同平台可能有不同的：
     * - **类型系统**: 如泛型的实现方式
     * - **调用约定**: 函数调用的底层实现
     * - **内置类型**: 平台特有的类型定义
     *
     * @return 平台分析服务实例
     */
    val analyzerServices: PlatformDependentAnalyzerServices

    /**
     * 可以访问此模块 internal 声明的模块列表
     *
     * 返回所有可以看到此模块内部声明的模块。
     *
     * ## 使用场景
     *
     * - **友元模块**: 测试模块可以访问主模块的 internal 声明
     * - **模块分组**: 同一组件的多个模块互相可见
     *
     * ## 示例
     *
     * ```kotlin
     * // 主模块
     * internal class InternalApi
     *
     * // 测试模块的 ModuleInfo
     * override fun modulesWhoseInternalsAreVisible() = listOf(mainModule)
     * // 这样测试模块就能访问 InternalApi
     * ```
     *
     * @return 可以访问 internal 声明的模块集合
     */
    fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> = listOf()

    /**
     * 模块能力映射
     *
     * 提供扩展机制，允许附加额外的模块信息而不修改接口。
     *
     * ## 工作原理
     *
     * ```kotlin
     * // 定义能力
     * val MyCapability = ModuleCapability<MyData>("MyCapability")
     *
     * // 提供能力
     * override val capabilities = mapOf(MyCapability to myData)
     *
     * // 使用能力
     * val data = moduleInfo.capabilities[MyCapability]
     * ```
     *
     * ## 默认能力
     *
     * 默认包含 `Capability` 能力，指向 ModuleInfo 自身。
     *
     * @return 能力键到值的映射
     * @see ModuleCapability
     */
    val capabilities: Map<ModuleCapability<*>, Any?>
        get() = mapOf(Capability to this)

    /**
     * 稳定名称
     *
     * 用于序列化和缓存的稳定标识符。
     *
     * ## 为什么需要稳定名称？
     *
     * - **持久化缓存**: 缓存键需要跨 IDE 重启保持稳定
     * - **增量编译**: 识别模块是否发生变化
     * - **序列化**: 保存/加载模块状态
     *
     * ## 示例
     *
     * ```kotlin
     * // 使用唯一的稳定标识符
     * override val stableName: Name = Name.identifier("my-app-v1.0")
     * ```
     *
     * @return 稳定名称，如果为 null 则使用 [name]
     */
    val stableName: Name?
        get() = null

    /**
     * 内置库依赖策略
     *
     * 定义此模块如何依赖内置库（built-ins）。
     *
     * ## 重要说明
     *
     * 对于 common 模块，内置库应该添加在依赖列表的开始位置，在 SDK 之后。
     * 这是因为如果 JVM 模块依赖 common 模块，我们应该对两个模块都使用 JVM 内置库进行解析。
     *
     * ## 依赖顺序示例
     *
     * ```
     * JVM 模块依赖 common 模块：
     *
     * JVM 模块的依赖顺序：
     * 1. JVM 模块自身
     * 2. JVM SDK
     * 3. JVM built-ins     ← 在这里！
     * 4. common 模块
     * 5. 其他依赖
     *
     * 为什么？
     * - common 模块通常依赖 kotlin-stdlib-common
     * - kotlin-stdlib-common 可能有自己的（common，非 JVM）内置库
     * - 但如果存在 JVM 内置库，它们应该优先
     * - 因为 JVM 内置库包含依赖于 JDK 的额外成员
     * ```
     *
     * ## 策略类型
     *
     * - **DependencyOnBuiltIns.AFTER_SDK**: 内置库在 SDK 之后
     * - **DependencyOnBuiltIns.LAST**: 内置库在依赖列表末尾
     * - **DependencyOnBuiltIns.NONE**: 不依赖内置库
     *
     * @return 内置库依赖策略
     * @see DependencyOnBuiltIns
     */
    fun dependencyOnBuiltIns(): DependencyOnBuiltIns = analyzerServices.dependencyOnBuiltIns()


    companion object {
        /**
         * ModuleInfo 能力键
         *
         * 用于在 capabilities 映射中存储 ModuleInfo 自身的引用。
         *
         * ## 使用示例
         *
         * ```kotlin
         * val moduleInfo = capabilities[ModuleInfo.Capability] as? ModuleInfo
         * ```
         */
        val Capability = ModuleCapability<ModuleInfo>("ModuleInfo")
    }
}


/**
 * 模块来源类型枚举
 *
 * 用于标识模块信息的来源,区分不同类型的代码模块。
 * 这有助于在代码分析、依赖解析和符号查找时采用不同的处理策略。
 *
 * ## 枚举值说明
 *
 * ### MODULE
 * **项目模块**
 *
 * 表示当前项目中的模块,包含项目自己的源代码。
 * 这些模块:
 * - 可以被编辑和修改
 * - 拥有完整的源代码和 PSI 树
 * - 可以进行实时的语法和语义分析
 * - 支持重构和代码生成
 *
 * 示例: 项目中的 `main` 模块、`test` 模块
 *
 * ### LIBRARY
 * **库模块**
 *
 * 表示外部库或依赖项,通常以编译后的形式存在。
 * 这些模块:
 * - 只读,不可编辑
 * - 可能只有二进制代码(如 .cjo文件)
 * - 可能提供反编译视图或文档
 * - 用于类型检查和符号解析
 *
 * 示例: 标准库、第三方库
 *
 * ### OTHER
 * **其他类型**
 *
 * 表示特殊用途的模块,如:
 * - 内置类型和函数的虚拟模块
 * - 代码片段或临时代码
 * - 脚本文件
 * - SDK 提供的特殊模块
 *
 * ## 使用示例
 * ```kotlin
 * fun analyzeModule(moduleInfo: ModuleInfo) {
 *     when (moduleInfo.moduleOrigin) {
 *         ModuleOrigin.MODULE -> {
 *             // 分析项目源代码
 *             performFullAnalysis(moduleInfo)
 *         }
 *         ModuleOrigin.LIBRARY -> {
 *             // 从库的元数据中提取信息
 *             extractLibraryMetadata(moduleInfo)
 *         }
 *         ModuleOrigin.OTHER -> {
 *             // 特殊处理
 *             handleSpecialModule(moduleInfo)
 *         }
 *     }
 * }
 * ```
 *
 * @see AnalysisContext 分析上下文接口
 */
enum class ModuleOrigin {
    /** 项目模块,包含项目自己的源代码 */
    MODULE,

    /** 库模块,来自外部依赖或标准库 */
    LIBRARY,

    /** 其他类型的模块,如内置模块或特殊用途模块 */
    OTHER
}

/**
 * 内置库依赖策略
 *
 * 定义模块如何依赖内置库（built-ins）的策略。
 *
 * ## 枚举值说明
 *
 * ### NONE
 * 不依赖内置库。
 * 适用场景:
 * - 内置库本身
 * - 完全独立的模块
 *
 * ### AFTER_SDK
 * 在 SDK 之后依赖内置库。
 * 适用场景:
 * - 常规模块
 * - 用户代码
 *
 * ### LAST
 * 最后依赖内置库。
 * 适用场景:
 * - 需要覆盖标准库行为的特殊模块
 * - 测试模块
 */
enum class DependencyOnBuiltIns {
    /** 不依赖内置库 */
    NONE,

    /** 在 SDK 之后依赖内置库 */
    AFTER_SDK,

    /** 最后依赖内置库 */
    LAST
}