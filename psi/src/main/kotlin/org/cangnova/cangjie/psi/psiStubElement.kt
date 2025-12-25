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

package org.cangnova.cangjie.psi

import com.intellij.psi.stubs.IStubElementType

/**
 * 从 stub 或 PSI 树中查找指定类型的子元素（支持多种元素类型）
 *
 * 这个方法用于处理子元素可能是多种类型之一的情况，例如：
 * - 变量声明的模式可能是 BINDING_PATTERN, TUPLE_PATTERN, ENUM_PATTERN, WILDCARD_PATTERN 中的任一种
 *
 * 工作原理：
 * 1. 如果当前元素有 stub，则从 stub 的子节点中查找匹配 [elementTypes] 中任一类型的第一个元素
 * 2. 如果没有 stub，则从 PSI 树中查找类型为 [T] 的第一个子元素
 *
 * @param T 目标 PSI 元素类型（通常是一个接口或抽象类）
 * @param elementTypes 可能的 stub 元素类型列表
 * @return 找到的第一个匹配元素，如果没有找到则返回 null
 */
inline fun <reified T : CjElement> CjElementImplStub<*>.getStubOrPsiChildByTypes(
    vararg elementTypes: IStubElementType<*, *>
): T? {
    elementTypes.forEach {
        if (getStubOrPsiChild(it) != null) {
            return getStubOrPsiChild(it) as T
        }
    }
    return null
}
