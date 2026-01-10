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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * 诊断信息接口
 *
 * 表示已绑定到具体 PSI 元素的诊断信息（错误、警告、信息等）。
 * 继承自 [UnboundDiagnostic] 和 [DiagnosticMarker]，是诊断系统的核心接口。
 *
 * 继承关系：
 * - [UnboundDiagnostic]：提供基础属性（factory, severity, textRanges, isValid）
 * - [DiagnosticMarker]：标记接口，提供 psiElement 和 factoryName
 *
 * 与 UnboundDiagnostic 的区别：
 * - UnboundDiagnostic：未绑定到具体元素，只有文本范围
 * - Diagnostic：绑定到具体 PSI 元素，可获取所在文件
 *
 * 实现类：
 * - [org.cangnova.cangjie.diagnostics.SimpleDiagnostic]：无参数诊断
 * - [org.cangnova.cangjie.diagnostics.DiagnosticWithParameters1]：带 1 个参数的诊断
 * - [org.cangnova.cangjie.diagnostics.DiagnosticWithParameters2]：带 2 个参数的诊断
 * - [org.cangnova.cangjie.diagnostics.DiagnosticWithParameters3]：带 3 个参数的诊断
 * - [org.cangnova.cangjie.diagnostics.DiagnosticWithParameters4]：带 4 个参数的诊断
 *
 * 使用场景：
 * - 编译器/分析器报告语义错误
 * - IDE 显示代码检查结果
 * - 通过 DiagnosticSink 收集诊断
 */
interface Diagnostic : UnboundDiagnostic, DiagnosticMarker {
    /** 诊断关联的 PSI 元素 */
    override val psiElement: PsiElement

    /** 诊断所在的 PSI 文件 */
    val psiFile: PsiFile

    /** 诊断工厂名称，用于识别诊断类型 */
    override val factoryName: String
        get() = factory.name
}