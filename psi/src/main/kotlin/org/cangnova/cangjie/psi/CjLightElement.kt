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

package org.cangnova.cangjie.psi

import com.intellij.psi.PsiElement

/**
 * 仓颉语言中轻量级 PSI 元素的基础接口
 *
 * 轻量级元素是一种性能优化手段,用于在不完全解析源代码的情况下提供 PSI 元素的功能。
 * 这种机制主要用于:
 * - 快速构建索引
 * - 减少内存占用
 * - 加速大型项目的符号解析
 *
 * @param T 原始仓颉 PSI 元素类型,继承自 [CjElement]
 * @param D 委托的 PSI 元素类型,用于提供底层实现
 *
 * @see CjElement
 */
interface CjLightElement<out T : CjElement, out D : PsiElement> : PsiElement {
    /**
     * 获取该轻量级元素对应的原始仓颉 PSI 元素
     *
     * @return 原始 PSI 元素,如果不存在则返回 null
     */
    val cangjieOrigin: T?

    /**
     * 提供给定的注解列表(用于特殊场景)
     *
     * CjLightModifierList 默认从相关的 CjElement 或 clsDelegate 中检索注解。
     * 但对于基于描述符构建的 CjUltraLightAnnotationForDescriptor,这两者都不存在。
     * 在这种情况下,CjLightModifierList 会首先检查 givenAnnotations,如果不为 null 则使用它。
     *
     * 注意: 这可能不是最优雅的解决方案,但目前尚不清楚如何更好地实现。
     */
//    val givenAnnotations: List<CjLightAbstractAnnotation>? get() = null
}
