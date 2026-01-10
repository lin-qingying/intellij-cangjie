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
 * 泛型诊断集合的简单实现
 *
 * 提供了 [GenericDiagnostics] 接口的基础实现，用于存储任意类型的未绑定诊断信息。
 *
 * 特性：
 * - 防御性复制：构造时复制输入集合，防止外部修改影响内部状态
 * - 不可变语义：一旦创建，诊断集合内容不可更改
 * - 泛型支持：T 可以是任何 UnboundDiagnostic 的子类型
 *
 * 使用场景：
 * - 作为更具体诊断集合类的基类（如 SimpleDiagnostics）
 * - 临时存储和传递一组诊断信息
 * - 需要类型安全的诊断集合时
 *
 * @param T 诊断类型，必须继承自 UnboundDiagnostic
 * @param diagnostics 初始诊断集合，将被复制以防止外部修改
 */
open class SimpleGenericDiagnostics<T : UnboundDiagnostic>(diagnostics: Collection<T>) : GenericDiagnostics<T> {
    // 复制集合以防止外部修改
    private val diagnostics = ArrayList(diagnostics)

    /** 返回所有诊断信息的不可变视图 */
    override fun all() = diagnostics
}