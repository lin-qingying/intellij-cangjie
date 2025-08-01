/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.references

import cn.cangnova.cangjie.psi.CjNameReferenceExpression
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

abstract class SyntheticPropertyAccessorReference(
    expression: CjNameReferenceExpression,
    val getter: Boolean
) : CjSimpleReference<CjNameReferenceExpression>(expression)
{

    protected fun isAccessorName(name: String): Boolean {
        if (getter) {
            return name.startsWith("get") || name.startsWith("is")
        }
        return name.startsWith("set")
    }

    override fun canBeReferenceTo(candidateTarget: PsiElement): Boolean {
//        if (candidateTarget !is PsiMethod || !isAccessorName(candidateTarget.name)) return false
//        if (getter && !candidateTarget.canHaveSyntheticGetter || !getter && !candidateTarget.canHaveSyntheticSetter) return false
//        if (!getter && expression.readWriteAccess(true) == ReferenceAccess.READ) return false
        return true
    }

    override fun getRangeInElement() = TextRange(0, expression.textLength)

    override fun canRename() = true

    override val resolvesByNames: Collection<Name>
        get() = listOf(element.referencedNameAsName)
}
