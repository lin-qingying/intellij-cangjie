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

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.addModifier
import org.cangnova.cangjie.psi.stubs.CangJieConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

// 主构造函数
class CjPrimaryConstructor : CjConstructor<CjPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstructorStub<CjPrimaryConstructor>) : super(stub, CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingTypeStatement() = parent?.parent as CjTypeStatement

    private fun getOrCreateConstructorKeyword(): PsiElement {
        return getInitKeyword() ?: addBefore(CjPsiFactory(project).createConstructorKeyword(), valueParameterList!!)
    }

    private fun removeRedundantConstructorKeywordAndSpace() {
        getInitKeyword()?.delete()
        if (prevSibling is PsiWhiteSpace) {
            prevSibling.delete()
        }
    }

    override fun getInitKeyword(): PsiElement? {
        return identifier
    }
    override fun addModifier(modifier: CjKeywordToken) {
        val modifierList = modifierList
        if (modifierList != null) {
            addModifier(modifierList, modifier)
            if (this.modifierList == null) {
                getInitKeyword()?.delete()
            }
        } else {
            if (modifier == CjTokens.PUBLIC_KEYWORD) return
            val newModifierList = CjPsiFactory(project).createModifierList(modifier)
            addBefore(newModifierList, getOrCreateConstructorKeyword())
        }
    }

    override fun getIdentifyingElement(): PsiElement? {
        return identifier
    }

    val identifier: PsiElement?
        get() {
            // 优先从 Stub 获取，避免访问 AST
            val stubIdentifier = greenStub?.name
            if (stubIdentifier != null) {
                // Stub 中有标识符名称，但我们需要返回 PsiElement
                // 如果只是为了获取名称，不需要 PsiElement，直接用 getName()
                // 这里仍然需要找到实际的 PsiElement
                if (containingFile.isValid) {
                    return findChildByType(CjTokens.IDENTIFIER)
                }
                return null
            }
            return findChildByType(CjTokens.IDENTIFIER)
        }

    override fun getName(): String? {
        // 优先从 Stub 获取，避免访问 AST 导致 PsiInvalidElementAccessException
        val stub = greenStub
        if (stub != null) {
            val identifierName = stub.name
            if (identifierName != null) {
                return identifierName
            }
        }
        // 如果 Stub 不可用，再访问 AST
        return try {
            if (containingFile.isValid) {
                identifier?.text
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    override fun removeModifier(modifier: CjKeywordToken) {
        super.removeModifier(modifier)
        if (modifierList == null) {
            removeRedundantConstructorKeywordAndSpace()
        }
    }
    override fun toString(): String {
        return node.elementType.toString()
    }
}
