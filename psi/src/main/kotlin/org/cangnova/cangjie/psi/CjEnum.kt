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

import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.ENUM_BODY
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.lexer.CjTokens

class CjEnum : CjTypeStatement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieEnumStub) : super(stub, CjStubElementTypes.ENUM)

    override fun toString(): String = node.elementType.toString() + ": " + name

    override val typeName: String
        get() = "enum"

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitEnum(this, data)
    }

    override val body: CjEnumBody?
        get() {
            return getStubOrPsiChild(ENUM_BODY)
        }

    /**
     * 是否非穷枚举
     */
    val isNonExhaustive: Boolean
        get() {
            val stub = stub as CangJieEnumStub?
            if (stub != null) {
                return stub.isNonExhaustive()
            }
            return body?.isNonExhaustive == true
        }

    val constructor: List<CjEnumConstructor>
        get() {
            return body?.constructor ?: emptyList()
        }
}
