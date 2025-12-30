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

package org.cangnova.cangjie.resolve.qualified

import com.intellij.openapi.util.Key
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * 调试模式下抑制诊断的标记键
 *
 * 用于在调试器上下文中禁用某些错误检查，避免干扰调试体验。
 *
 * ## 使用场景
 * - 在调试器的表达式求值器中，用户可能输入不完整的表达式
 * - 在代码片段（Code Fragment）中，可能缺少某些上下文信息
 * - 在 REPL 环境中，允许更灵活的语法
 *
 * ## 实现方式
 * 通过 IntelliJ 平台的 `UserData` 机制存储在 PSI 文件中。
 */
val SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE = Key.create<Boolean>("SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE")

/**
 * 扩展属性：是否在调试模式下抑制诊断
 *
 * ## 行为说明
 * - **代码片段（CjCodeFragment）**：始终返回 true，因为代码片段通常在调试器中使用
 * - **普通文件**：根据 UserData 中的标记决定
 *
 * ## 使用示例
 * ```kotlin
 * if (!file.suppressDiagnosticsInDebugMode) {
 *     trace.report(ERROR.on(expression))
 * }
 * ```
 */
var CjFile.suppressDiagnosticsInDebugMode: Boolean
    get() = when (this) {
        // 代码片段总是抑制诊断
        is CjCodeFragment -> true
        // 普通文件根据 UserData 决定
        else -> getUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE) == true
    }
    set(skip) {
        putUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE, skip)
    }




/**
 * 包片段的自定义源包装类
 *
 * ## 用途
 * 在可见性检查时传递正确的源文件信息，确保模块间可见性判断正确。
 *
 * ## 背景
 * `PackageFragmentDescriptor` 通常没有关联具体的源文件（source = NO_SOURCE），
 * 这会导致跨模块的友好关系检查失败。通过这个包装类，我们可以临时关联一个源文件，
 * 使得 `ModuleVisibilityHelperImpl.isInFriendModule` 能够正确判断可见性。
 *
 * ## 委托模式
 * 使用 Kotlin 的 `by` 委托，只覆盖 `original` 和 `source` 属性，
 * 其他所有方法和属性都委托给原始的包片段描述符。
 *
 * @param original 原始的包片段描述符
 * @param source 自定义的源元素（通常是 CangJieSourceElement）
 */
internal class PackageFragmentWithCustomSource(
    override val original: PackageFragmentDescriptor,
    override val source: SourceElement
) : PackageFragmentDescriptor by original
