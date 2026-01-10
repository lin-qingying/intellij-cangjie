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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 变量声明的抽象基类
 *
 * 所有变量类型的公共基类，包括：
 * - [CjPatternVariable]: 模式匹配变量（顶层或局部）
 * - [CjFieldVariable]: 类成员字段
 *
 * @see CjPatternVariable
 * @see CjFieldVariable
 */
abstract class CjVariable<S : StubElement<*>> : CjDeclarationStub<S>, CjVariableDeclaration {
    constructor(stub: S, type: IStubElementType<S, *>) : super(stub, type)
    constructor(node: ASTNode) : super(node)
    val isLocal: Boolean
        get() = !isTopLevel && this !is CjFieldVariable
    open val isTopLevel: Boolean get() = false
}

/**
 * 变量声明 PSI 元素
 *
 * 变量声明是模式匹配的声明方式，用于顶层变量和局部变量。
 * 变量本身没有名称，名称信息来自模式匹配中的绑定模式。
 * 一个变量声明可能包含多个绑定（如元组解构），因此不提供单一的 name 属性。
 *
 * 示例:
 * ```cangjie
 * let a = 1              // BINDING 模式 - 单个绑定
 * let (x, y) = tuple     // TUPLE 模式 - 多个绑定
 * let Some(v) = optional // ENUM 模式
 * let _ = ignored        // WILDCARD 模式 - 无绑定
 * ```
 *
 * 要获取绑定的变量名，请使用 [pattern] 属性遍历子模式。
 */
class CjPatternVariable : CjVariable<CangJieVariableStub> {
    constructor(stub: CangJieVariableStub) : super(stub, CjStubElementTypes.VARIABLE)
    constructor(node: ASTNode) : super(node)

    override val valueParameterList: CjParameterList?
        get() = null

    override val valueParameters: List<CjParameter>
        get() = emptyList()



    // Variables don't have type parameters
    override val typeParameterList: CjTypeParameterList?
        get() = null

    override val typeConstraintList: CjTypeConstraintList?
        get() = null

    override val typeConstraints: List<CjTypeConstraint>
        get() = emptyList()

    override val typeParameters: List<CjTypeParameter>
        get() = emptyList()

    /**
     * 获取变量声明中的模式
     *
     * 模式可能是：
     * - [CjBindingPattern]: 简单绑定，如 `let a = 1`
     * - [CjTuplePattern]: 元组解构，如 `let (x, y) = tuple`
     * - [CjEnumPattern]: 枚举解构，如 `let Some(v) = optional`
     * - [CjWildcardPattern]: 通配符，如 `let _ = ignored`
     *
     * 使用 [getAllBindings] 扩展函数获取所有绑定的变量名。
     */
    val pattern: CjCasePatternElement?
        get() = getStubOrPsiChildByTypes(
            CjStubElementTypes.BINDING_PATTERN,
            CjStubElementTypes.TUPLE_PATTERN,
            CjStubElementTypes.ENUM_PATTERN,
            CjStubElementTypes.WILDCARD_PATTERN
        )

    /**
     * 获取模式类型
     */
    val patternKind: PatternKind
        get() {
            val stub = stub
            if (stub != null) {
                return stub.getPatternKind()
            }
            return when (pattern) {
                null -> PatternKind.BINDING
                is CjBindingPattern -> PatternKind.BINDING
                is CjTuplePattern -> PatternKind.TUPLE
                is CjEnumPattern -> PatternKind.ENUM
                is CjWildcardPattern -> PatternKind.WILDCARD
                else -> PatternKind.BINDING
            }
        }

    val equalsToken: PsiElement? get() = findChildByType(CjTokens.EQ)

    override fun toString(): String {
        return "${super.toString()} [${patternKind}]"
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternVariable(this, data)

    }

    override val isTopLevel: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isTopLevel()
            }

            return parent is CjFile
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
                                Invalid stub structure built for variable:
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
        return setTypeReference(this, pattern, typeRef)
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

    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)

    override val letOrVarKeyword: PsiElement
        get() {
            val element =
                checkNotNull(findChildByType(LET_VAR_TOKEN_SET)) { "Let or var should always exist for variable: " + this.text }
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
// CjVariable 是模式匹配声明，可能包含多个绑定。请从 CjBindingPattern 获取 fqName。
    override val nameAsSafeName: Name
        get() = throw UnsupportedOperationException(
            "CjVariable 是模式匹配声明，可能包含多个绑定。请使用 pattern.getAllBindings() 获取所有变量名。"
        )

    override val fqName: FqName?
        get() = null

    override fun getNameIdentifier(): PsiElement? {
      return  null
    }

    override fun setName(name: @NlsSafe String): PsiElement? {
       return null
    }

    override val nameAsName: Name?
        get() = null
    companion object {
        private val LOG = Logger.getInstance(
            CjVariable::class.java,
        )

        private val LET_VAR_TOKEN_SET =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.CONST_KEYWORD, CjTokens.VAR_KEYWORD)
    }
}
