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

package org.cangnova.cangjie.descriptors

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