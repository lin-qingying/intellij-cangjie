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

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.astReplace
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded
import org.cangnova.cangjie.psi.stubs.CangJieFieldStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName

/**
 * 类成员字段 PSI 元素
 *
 * 表示类/结构体/接口中的 let/var/const 声明。
 * 与 CjPatternVariable 不同，CjFieldVariable 不支持模式匹配，只有简单的标识符名称。
 *
 * 示例:
 * ```cangjie
 * class Person {
 *     let name: String           // 不可变字段
 *     var age: Int64             // 可变字段
 *     const MAX_AGE: Int64 = 150 // 常量字段
 * }
 * ```
 *
 * @see CjVariable
 * @see CjPatternVariable
 */
class CjFieldVariable : CjVariable<CangJieFieldStub>  {

    constructor(stub: CangJieFieldStub) : super(stub, CjStubElementTypes.FIELD)
    constructor(node: ASTNode) : super(node)

    companion object {
        private val LOG = Logger.getInstance(CjFieldVariable::class.java)

        private val LET_VAR_CONST_TOKEN_SET = TokenSet.create(
            CjTokens.LET_KEYWORD,
            CjTokens.VAR_KEYWORD,
            CjTokens.CONST_KEYWORD
        )
    }

    override val valueParameterList: CjParameterList?
        get() = null

    override val valueParameters: List<CjParameter>
        get() = emptyList()

    val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)

    override fun toString(): String {
        return super.toString() + ": " + name
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitFieldVariable(this, data)
    }
    //需要实现textOffset
    override fun getTextOffset(): Int {
        val identifier = nameIdentifier
        return identifier?.textRange?.startOffset ?: textRange.startOffset
    }


    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasReturnTypeRef()) {
                    return null
                } else {
                    val typeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                    if (typeReferences.isEmpty()) {
                        LOG.error(
                            """
                            Invalid stub structure built for field:
                            $text
                            """.trimIndent(),
                        )
                        return null
                    }
                    return typeReferences[0]
                }
            }
            return getTypeReference(this)
        }

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    override val isVar: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isVar()
            }
            return node.findChildByType(CjTokens.VAR_KEYWORD) != null
        }

    val isConst: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isConst()
            }
            return node.findChildByType(CjTokens.CONST_KEYWORD) != null
        }

    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)

    override val letOrVarKeyword: PsiElement
        get() {
            val element = checkNotNull(findChildByType(LET_VAR_CONST_TOKEN_SET)) {
                "Let, var, or const should always exist for field: " + this.text
            }
            return element
        }

    override val initializer: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasInitializer()) {
                    return null
                }
                if (containingCjFile.isCompiled) {
                    return null
                }
            }
            return PsiTreeUtil.getNextSiblingOfType(
                findChildByType(CjTokens.EQ),
                CjExpression::class.java,
            )
        }

    override fun hasInitializer(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasInitializer()
        }
        return initializer != null
    }

    /**
     * 获取所属的类型声明（class/struct/interface/enum）
     */
    val containingTypeStatement: CjTypeStatement?
        get() = getStrictParentOfType<CjTypeStatement>()

    // ========== CjNamedDeclaration 实现 ==========

    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }
        val identifier = nameIdentifier
        if (identifier != null) {
            val text = identifier.text
            return if (text != null) CjPsiUtil.unquoteIdentifier(text) else null
        }
        return null
    }

    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(CjTokens.IDENTIFIER)
    }

    override fun setName(name: @NlsSafe String): PsiElement? {
        val identifier = nameIdentifier ?: return null
        val newIdentifier = CjPsiFactory(project).createNameIdentifierIfPossible(name.quoteIfNeeded())
        if (newIdentifier != null) {
            identifier.astReplace(newIdentifier)
        } else {
            identifier.delete()
        }
        return this
    }

    override val nameAsName: Name?
        get() = this.name?.asOperatorName()

    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    override val fqName: FqName?
        get() {
            val stub = getStub()
            if (stub != null) {
                return stub.getFqName()
            }
            return CjNamedDeclarationUtil.getFQName(this)
        }

    // ========== CjTypeParameterListOwner 实现 ==========
    // 字段不支持类型参数，返回 null/空列表

    override val typeParameterList: CjTypeParameterList?
        get() = null

    override val typeConstraintList: CjTypeConstraintList?
        get() = null

    override val typeConstraints: List<CjTypeConstraint>
        get() = emptyList()

    override val typeParameters: List<CjTypeParameter>
        get() = emptyList()
}
