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

package org.cangnova.cangjie.resolve.source

import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjPureElement

class CangJieSourceElement(override val psi: CjElement) : PsiSourceElement


fun CjPureElement?.toSourceElement(): SourceElement {
    if (this == null) return SourceElement.NO_SOURCE
    val psi = getPsiOrParent()
    // 如果 PSI 来自宏展开的虚拟文件（已通过 MACRO_EXPRESSION_KEY 标记），
    // 自动创建 MacroExpandedSourceElement，使所有宏展开节点（类、构造函数、方法、属性等）
    // 都能正确导航回原始宏调用处
    val macroExpr = psi.containingFile?.getUserData(MacroExpandedSourceElement.MACRO_EXPRESSION_KEY)
    return if (macroExpr != null) {
        MacroExpandedSourceElement(macroExpr, psi)
    } else {
        CangJieSourceElement(psi)
    }
}
