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

import org.cangnova.cangjie.psi.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

@Suppress("deprecation")
abstract class CjFunctionNotStubbed(node: ASTNode) :
    CjTypeParameterListOwnerNotStubbed(node),
    CjFunction {
    override val valueParameterList: CjParameterList?
        get() = findChildByType(CjNodeTypes.VALUE_PARAMETER_LIST)

    override val valueParameters: List<CjParameter>
        get() {
            val list = valueParameterList
            return list?.parameters ?: emptyList()
        }

    override val bodyExpression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java)
        }

    override fun hasDeclaredReturnType(): Boolean {
        return false
    }



    override val typeReference: CjTypeReference?
        get() = null

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        if (typeRef == null) return null
        throw IllegalStateException("Lambda expressions can't have type reference")
    }

    override val colon: PsiElement?
        get() = null

    override val isLocal: Boolean
        get() {
            val parent = parent
            return !(parent is CjFile || parent is CjAbstractClassBody)
        }
}
