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

package org.cangnova.cangjie.diagnostics


/**
 * 诊断信息接收器接口
 *
 * 这是编译器诊断系统的核心接口，负责接收和处理编译过程中产生的诊断信息（错误、警告等）。
 * 实现类可以决定如何处理这些诊断信息：记录日志、抛出异常、显示在 IDE 中等。
 *
 * ## 主要功能
 * - **报告诊断**：接收并处理编译器产生的诊断信息
 * - **回调机制**：支持设置回调函数，在诊断报告时触发
 * - **按需处理**：通过 [wantsDiagnostics] 方法控制是否需要处理诊断
 *
 * ## 内置实现
 * - [DO_NOTHING]：静默模式，忽略所有诊断信息
 * - [THROW_EXCEPTION]：严格模式，遇到错误立即抛出异常
 *
 * ## 使用场景
 * 1. **IDE 集成**：将诊断信息显示在编辑器中
 * 2. **命令行编译**：将错误输出到控制台
 * 3. **测试环境**：收集诊断信息用于断言
 * 4. **代码分析**：累积所有问题供后续批量处理
 *
 * @see Diagnostic 诊断信息接口
 * @see DiagnosticsCallback 诊断回调接口
 *
 * @sample
 * ```kotlin
 * // 示例：自定义诊断接收器
 * class ConsoleDiagnosticSink : DiagnosticSink {
 *     override fun report(diagnostic: Diagnostic) {
 *         println("[${diagnostic.severity}] ${diagnostic.factoryName}")
 *     }
 *
 *     override fun wantsDiagnostics() = true
 * }
 * ```
 */
interface DiagnosticSink {

    /**
     * 诊断回调函数接口
     *
     * 用于在诊断信息报告时执行自定义操作。
     * 回调函数会在 [report] 方法执行时被调用。
     */
    fun interface DiagnosticsCallback {
        /**
         * 回调方法，在诊断报告时触发
         *
         * @param diagnostic 报告的诊断信息
         */
        fun callback(diagnostic: Diagnostic)
    }

    /**
     * 报告一个诊断信息
     *
     * 这是接收器的核心方法，用于接收并处理编译器产生的诊断信息。
     * 实现类应该在此方法中决定如何处理诊断：记录、显示、过滤等。
     *
     * @param diagnostic 要报告的诊断信息，包含错误位置、严重程度、消息等
     */
    fun report(diagnostic: Diagnostic)

    /**
     * 设置诊断回调函数（已废弃）
     *
     * 此方法已废弃，请使用 [setCallbackIfNotSet] 替代。
     * 用于设置在诊断报告时执行的回调函数。
     *
     * @param callback 回调函数
     * @deprecated 使用 [setCallbackIfNotSet] 替代
     */
    @Deprecated(
        message = "Use setCallbackIfNotSet instead",
        replaceWith = ReplaceWith("setCallbackIfNotSet(callback)")
    )
    fun setCallback(callback: DiagnosticsCallback) {
        setCallbackIfNotSet(callback)
    }

    /**
     * 设置诊断回调函数（如果尚未设置）
     *
     * 尝试设置诊断回调函数。如果接收器已经有回调函数，则不会覆盖。
     * 默认实现不支持回调，总是返回 false。
     *
     * @param callback 要设置的回调函数
     * @return 如果成功设置返回 true，如果已有回调或不支持回调返回 false
     */
    fun setCallbackIfNotSet(callback: DiagnosticsCallback): Boolean = false

    /**
     * 重置回调函数
     *
     * 清除已设置的回调函数。
     * 默认实现为空操作，不支持回调的接收器可以忽略此方法。
     */
    fun resetCallback() {}

    /**
     * 检查此接收器是否需要接收诊断信息
     *
     * 编译器在报告诊断前会调用此方法检查接收器是否需要诊断信息。
     * 返回 false 可以优化性能，避免不必要的诊断信息创建。
     *
     * @return 如果需要接收诊断返回 true，否则返回 false
     */
    fun wantsDiagnostics(): Boolean


}