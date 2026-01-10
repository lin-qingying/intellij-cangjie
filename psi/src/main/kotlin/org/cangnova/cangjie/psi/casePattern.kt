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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieBindingPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieConstantPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieEnumPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieMatchConditionStub
import org.cangnova.cangjie.psi.stubs.CangJieTuplePatternStub
import org.cangnova.cangjie.psi.stubs.CangJieTypePatternStub
import org.cangnova.cangjie.psi.stubs.CangJieWildcardPatternStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 模式匹配 PSI 元素的基础接口
 */
interface CjCasePatternElement : CjElement, ValueArgument, CjExpression

/**
 * 具有名称的模式元素接口
 *
 * 用于标识可以创建变量绑定的模式元素，如 [CjBindingPattern] 和 [CjTypePattern]。
 * 这些模式元素会创建变量描述符，因此需要同时支持 PSI 元素操作和名称访问。
 */
interface CjNamedPattern : CjCasePatternElement, CjNamed

/**
 * 基于 Stub 的模式匹配基类（所有模式都支持 Stub）
 */
abstract class CjCasePattern<T : StubElement<*>> : CjElementImplStub<T>, CjCasePatternElement {

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCasePattern(this, data)
    }

    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): CjElement = this
    override fun getSpreadElement(): LeafPsiElement? = null
    override fun isExternal(): Boolean = false
    override fun getArgumentExpression(): CjExpression? = this
}

/**
 * match 表达式条件模式（支持 Stub）
 */
class CjMatchConditionWithExpression : CjCasePattern<CangJieMatchConditionStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieMatchConditionStub) : super(stub, CjStubElementTypes.MATCH_CONDITION)

    @get:IfNotParsed
    val expression
        get() = findChildByClass<CjExpression>(CjExpression::class.java)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitMatchConditionWithExpression(this, data)
    }
}

/**
 * 绑定模式 PSI 元素（支持 Stub）
 *
 * 表示变量绑定模式，如 `let a = 1` 中的 `a`
 * 绑定模式是模式匹配的一部分，不是独立的变量声明
 */
class CjBindingPattern : CjCasePattern<CangJieBindingPatternStub>, CjSimpleNameExpression, PsiNameIdentifierOwner, CjNamedPattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieBindingPatternStub) : super(stub, CjStubElementTypes.BINDING_PATTERN)

    override val referencedName: String
        get() = name ?: ""

    override val referencedNameAsName: Name
        get() = Name.identifier(referencedName)

    val isLocal: Boolean
        get() = !isTopLevel

    val isTopLevel: Boolean
        get() = parent is CjFile

    override val referencedNameElement: PsiElement
        get() = expression ?: this

    override val identifier: PsiElement?
        get() = findChildByType(CjTokens.IDENTIFIER)

    override val referencedNameElementType: IElementType
        get() = CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)

    val expression: CjSimpleNameExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByBinding(this, data)
    }

    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }
        return expression?.referencedName
    }

    override fun getNameIdentifier(): PsiElement? {
        return expression?.identifier
    }

    val nameAsSafeName: Name
        get() = Name.identifier(name ?: "")

    override val nameAsName: Name?
        get() = nameAsSafeName

    override fun setName(name: String): PsiElement = this

    /**
     * 获取所属的变量声明
     */
    private fun getParentVariable(): CjPatternVariable? {
        var parent = parent
        while (parent is CjCasePatternElement) {
            parent = parent.parent
        }
        return parent as? CjPatternVariable
    }

    val variable: CjPatternVariable?
        get() = getParentVariable()
}

/**
 * 类型模式 PSI 元素（支持 Stub）
 *
 * 表示类型匹配模式，如 `case x: Int => ...` 中的 `x: Int`
 */
class CjTypePattern : CjCasePattern<CangJieTypePatternStub>, PsiNameIdentifierOwner, CjNamedPattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypePatternStub) : super(stub, CjStubElementTypes.TYPE_PATTERN)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByType(this, data)
    }

    val identifierElement: PsiElement?
        get() = findChildByType(CjTokens.IDENTIFIER)
    /**
     * 获取所属的变量声明
     */
    private fun getParentVariable(): CjPatternVariable? {
        var parent = parent
        while (parent is CjCasePatternElement) {
            parent = parent.parent
        }
        return parent as? CjPatternVariable
    }
    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }
        return reference?.text
    }
    val variable: CjPatternVariable?
        get() = getParentVariable()
    val reference: CjSimpleNameExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)

    val typeReference: CjTypeReference?
        get() = findChildByType(CjNodeTypes.TYPE_REFERENCE)

    override fun getNameIdentifier(): PsiElement? = identifierElement

    val nameAsSafeName: Name
        get() = Name.identifier(name ?: "")

    override val nameAsName: Name?
        get() = nameAsSafeName

    override fun setName(name: String): PsiElement = this
}

/**
 * 元组模式 PSI 元素（支持 Stub）
 *
 * 表示元组解构模式，如 `let (a, b) = tuple` 中的 `(a, b)`
 */
class CjTuplePattern : CjCasePattern<CangJieTuplePatternStub>, CjEnumAndTuplePattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTuplePatternStub) : super(stub, CjStubElementTypes.TUPLE_PATTERN)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByTuple(this, data)
    }

    override val patterns: List<CjCasePatternElement>
        get() = findChildrenByClass(CjCasePatternElement::class.java).toList()
}

interface CjEnumAndTuplePattern {
    val patterns: List<CjCasePatternElement>
}

/**
 * 枚举模式 PSI 元素（支持 Stub）
 *
 * 表示枚举解构模式，如 `let Some(x) = optional` 中的 `Some(x)`
 */
class CjEnumPattern : CjCasePattern<CangJieEnumPatternStub>, CjEnumAndTuplePattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieEnumPatternStub) : super(stub, CjStubElementTypes.ENUM_PATTERN)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByEnum(this, data)
    }

    val type: CjTypeReference?
        get() = findChildByType(CjNodeTypes.TYPE_REFERENCE)

    val expression: CjExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)
            ?: findChildByType(CjNodeTypes.DOT_QUALIFIED_EXPRESSION)

    override val patterns: List<CjCasePatternElement>
        get() = findChildrenByClass(CjCasePatternElement::class.java).toList()
}

/**
 * 通配符模式 PSI 元素（支持 Stub）
 *
 * 表示通配符模式，如 `let _ = ignored` 中的 `_`
 */
class CjWildcardPattern : CjCasePattern<CangJieWildcardPatternStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieWildcardPatternStub) : super(stub, CjStubElementTypes.WILDCARD_PATTERN)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByWildcard(this, data)
    }
}

/**
 * 常量模式 PSI 元素（支持 Stub）
 *
 * 表示常量匹配模式，如 `case 1 => ...` 中的 `1`
 */
class CjConstantPattern : CjCasePattern<CangJieConstantPatternStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstantPatternStub) : super(stub, CjStubElementTypes.CONSTANT_PATTERN)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByConstant(this, data)
    }

    val expression: CjExpression?
        get() = findChildByClass(CjExpression::class.java)
}
