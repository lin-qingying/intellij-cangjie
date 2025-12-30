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

package org.cangnova.cangjie.diagnostics

import org.cangnova.cangjie.diagnostics.rendering.DiagnosticRendererRegistry

/**
 * 诊断接收器的常用实现
 *
 * 提供 [DiagnosticSink] 接口的常用实现，方便直接使用。
 *
 * ## 可用实现
 * - [DO_NOTHING]：静默模式，忽略所有诊断
 * - [THROW_EXCEPTION]：严格模式，遇到错误抛出异常
 *
 * @see DiagnosticSink
 */
object DiagnosticSinks {
    /**
     * 空操作诊断接收器
     *
     * 这是一个静默实现，忽略所有诊断信息，不执行任何操作。
     * 适用于不关心诊断结果的场景，如性能测试、快速原型验证等。
     *
     * 特点：
     * - [report] 方法为空实现
     * - [wantsDiagnostics] 返回 false，告知编译器不需要诊断
     *
     * @sample
     * ```kotlin
     * // 禁用诊断收集以提升性能
     * val sink = DiagnosticSinks.DO_NOTHING
     * compiler.compile(source, sink)
     * ```
     */
    
    val DO_NOTHING: DiagnosticSink = object : DiagnosticSink {
        override fun report(diagnostic: Diagnostic) {
            // 不执行任何操作
        }

        override fun wantsDiagnostics(): Boolean = false
    }

    /**
     * 抛出异常的诊断接收器
     *
     * 这是一个严格模式的实现，遇到错误级别的诊断时立即抛出 [IllegalStateException]。
     * 适用于测试环境或需要零容忍错误的场景。
     *
     * 特点：
     * - 只处理 [Severity.ERROR] 级别的诊断
     * - 忽略警告和信息级别的诊断
     * - 异常消息包含：错误工厂名称、渲染的错误消息、文件位置信息
     *
     * 异常格式：
     * ```
     * {FactoryName}: {ErrorMessage} at {File}:{Line}:{Column}
     * ```
     *
     * @throws IllegalStateException 当报告错误级别的诊断时
     *
     * @sample
     * ```kotlin
     * // 测试中确保没有编译错误
     * val sink = DiagnosticSinks.THROW_EXCEPTION
     * try {
     *     compiler.compile(source, sink)
     * } catch (e: IllegalStateException) {
     *     fail("编译失败: ${e.message}")
     * }
     * ```
     */
    
    val THROW_EXCEPTION: DiagnosticSink = object : DiagnosticSink {
        override fun report(diagnostic: Diagnostic) {
            if (diagnostic.severity == Severity.ERROR) {
                val psiFile = diagnostic.psiFile
                val textRanges = diagnostic.textRanges
                val diagnosticText = DiagnosticRendererRegistry.render(diagnostic)
                val location = PsiDiagnosticUtils.atLocation(psiFile, textRanges[0])

                throw IllegalStateException(
                    "${diagnostic.factory.name}: $diagnosticText $location"
                )
            }
        }

        override fun wantsDiagnostics(): Boolean = true
    }
}