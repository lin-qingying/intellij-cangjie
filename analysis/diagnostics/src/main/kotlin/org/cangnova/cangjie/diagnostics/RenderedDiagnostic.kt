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


import org.cangnova.cangjie.diagnostics.rendering.DiagnosticRenderer

/**
 * 已渲染的诊断信息
 *
 * 封装一个诊断对象及其对应的渲染器，提供渲染后的文本消息。
 * 用于在 IDE 中显示诊断信息时，将诊断数据转换为用户可读的文本格式。
 *
 * @param D 诊断类型，必须是 [org.cangnova.cangjie.diagnostics.Diagnostic] 的子类型
 * @property diagnostic 原始诊断对象，包含诊断的详细信息
 * @property renderer 用于将诊断转换为文本的渲染器
 */
class RenderedDiagnostic<D : Diagnostic>(
    val diagnostic: D,
    val renderer: DiagnosticRenderer<D>
) {
    /**
     * 渲染后的诊断文本消息
     *
     * 在构造时通过渲染器生成，包含格式化的错误/警告信息。
     */
    val text: String = renderer.render(diagnostic)

    /**
     * 获取诊断工厂
     *
     * 返回创建此诊断的工厂实例，可用于诊断类型识别和比较。
     */
    val factory: DiagnosticFactory<*> get() = diagnostic.factory

    /**
     * 返回渲染后的文本表示
     */
    override fun toString(): String = text
}
