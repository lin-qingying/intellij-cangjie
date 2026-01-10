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

/**
 * 参数化诊断接口
 *
 * 这是一个泛型接口，用于表示与特定 PSI 元素类型关联的编译器诊断信息。
 * 与基础的 [org.cangnova.cangjie.diagnostics.Diagnostic] 接口相比，此接口通过泛型参数提供了类型安全的 PSI 元素访问。
 *
 * ## 主要特性
 * - **类型安全**：通过泛型参数 `E` 确保诊断信息与特定类型的 PSI 元素关联
 * - **自动工厂命名**：自动从关联的诊断工厂获取工厂名称
 * - **继承 Diagnostic**：保持与基础诊断接口的兼容性
 *
 * ## 使用场景
 * 1. 需要对特定类型的 PSI 元素进行诊断（如只针对函数声明、类声明等）
 * 2. 在类型系统中需要更强的类型约束
 * 3. 实现类型特定的快速修复（Quick Fix）或意图操作（Intention Action）
 *
 * @param E PSI 元素类型，必须继承自 [PsiElement]，指定此诊断适用的具体元素类型
 *
 * @see org.cangnova.cangjie.diagnostics.Diagnostic 基础诊断接口
 * @see DiagnosticFactory 诊断工厂，用于创建诊断实例
 *
 * @sample
 * ```kotlin
 * // 示例：针对函数声明的诊断
 * class FunctionDiagnostic(
 *     override val psiElement: CjFunctionDeclaration,
 *     override val factory: DiagnosticFactory<CjFunctionDeclaration>
 * ) : ParametrizedDiagnostic<CjFunctionDeclaration>
 * ```
 */
interface ParametrizedDiagnostic<E : PsiElement> : Diagnostic {
    /**
     * 与诊断关联的 PSI 元素
     *
     * 此属性覆盖了父接口 [org.cangnova.cangjie.diagnostics.Diagnostic.psiElement]，提供了更具体的类型信息。
     * 通过泛型参数 `E`，调用者可以直接获得正确类型的元素，无需类型转换。
     */
    override val psiElement: E

    /**
     * 诊断工厂的名称
     *
     * 自动从关联的诊断工厂对象中获取名称，用于标识诊断的类型和来源。
     * 这个名称通常用于：
     * - 日志记录和调试
     * - 诊断信息的分组和过滤
     * - 错误报告和统计分析
     */
    override val factoryName: String
        get() = factory.name
}
