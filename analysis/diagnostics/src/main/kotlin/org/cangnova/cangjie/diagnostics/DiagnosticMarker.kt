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

import com.intellij.psi.PsiElement

/**
 * 诊断标记接口
 *
 * 最简化的诊断标识接口，仅包含最基本的两个属性：
 * - PSI 元素：诊断关联的代码元素
 * - 工厂名称：诊断类型的唯一标识
 *
 * 设计用途：
 * - 作为诊断接口继承层次的根接口
 * - 提供最小化的诊断识别能力
 * - 支持类型安全的参数化诊断标记
 */
interface DiagnosticMarker {
    /** 诊断关联的 PSI 元素 */
    val psiElement: PsiElement

    /** 诊断工厂名称，用于识别诊断类型 */
    val factoryName: String
}

/**
 * 带 1 个参数的诊断标记接口
 *
 * 扩展自 [DiagnosticMarker]，添加一个泛型参数 A。
 * 用于标记携带单个参数的诊断信息。
 *
 * 使用示例：
 * - "Unresolved reference: {0}" - 参数 A 是未解析的引用名称
 * - "Deprecated API: {0}" - 参数 A 是废弃的 API 名称
 *
 * @param A 参数类型
 */
interface DiagnosticWithParameters1Marker<A> : DiagnosticMarker {
    /** 第一个参数 */
    val a: A
}

/**
 * 带 2 个参数的诊断标记接口
 *
 * 扩展自 [DiagnosticMarker]，添加两个泛型参数 A 和 B。
 * 用于标记携带两个参数的诊断信息。
 *
 * 使用示例：
 * - "Type mismatch: expected {0}, found {1}" - A 是期望类型，B 是实际类型
 * - "Duplicate declaration: {0} at line {1}" - A 是声明名称，B 是行号
 *
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 */
interface DiagnosticWithParameters2Marker<A, B> : DiagnosticMarker {
    /** 第一个参数 */
    val a: A

    /** 第二个参数 */
    val b: B
}

/**
 * 带 3 个参数的诊断标记接口
 *
 * 扩展自 [DiagnosticMarker]，添加三个泛型参数 A、B 和 C。
 * 用于标记携带三个参数的诊断信息。
 *
 * 使用示例：
 * - "Cannot convert {0} to {1}: {2}" - A 是源类型，B 是目标类型，C 是原因
 * - "Function {0} expects {1} arguments, got {2}" - A 是函数名，B 是期望参数数量，C 是实际参数数量
 *
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 */
interface DiagnosticWithParameters3Marker<A, B, C> : DiagnosticMarker {
    /** 第一个参数 */
    val a: A

    /** 第二个参数 */
    val b: B

    /** 第三个参数 */
    val c: C
}

/**
 * 带 4 个参数的诊断标记接口
 *
 * 扩展自 [DiagnosticMarker]，添加四个泛型参数 A、B、C 和 D。
 * 用于标记携带四个参数的诊断信息。
 *
 * 使用示例：
 * - "Overload resolution failed: {0} at {1}:{2}, reason: {3}"
 *   - A 是函数名，B 是文件名，C 是行号，D 是失败原因
 *
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 * @param D 第四个参数类型
 */
interface DiagnosticWithParameters4Marker<A, B, C, D> : DiagnosticMarker {
    /** 第一个参数 */
    val a: A

    /** 第二个参数 */
    val b: B

    /** 第三个参数 */
    val c: C

    /** 第四个参数 */
    val d: D
}
