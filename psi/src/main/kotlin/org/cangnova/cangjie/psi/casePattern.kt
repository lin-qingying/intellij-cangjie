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

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

abstract class CjCasePattern(node: ASTNode) : CjElementImpl(node), ValueArgument, CjExpression {

    val destructuringDeclaration: CjDestructuringDeclaration?
        get() {

            return findChildByType(CjNodeTypes.DESTRUCTURING_DECLARATION)
        }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitCasePattern(this, data)
    }

    override fun getArgumentName(): ValueArgumentName? {
        return null
    }

    override fun isNamed(): Boolean {
        return false
    }

    override fun asElement(): CjElement {
        return this
    }

    override fun getSpreadElement(): LeafPsiElement? {
        return null
    }

    override fun isExternal(): Boolean {
        return false
    }

    override fun getArgumentExpression(): CjExpression? {
        return this
    }
}

class CjMatchConditionWithExpression(node: ASTNode) : CjCasePattern(node) {
    @get:IfNotParsed
    val expression
        get() = findChildByClass<CjExpression>(CjExpression::class.java)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitMatchConditionWithExpression(this, data)
    }
}

abstract class PatternVariableDeclaration(node: ASTNode) : CjCasePattern(node), CjVariableDeclaration {
    override val isVar: Boolean
        get() = false
    override val valueParameterList: CjParameterList? = null
    override val valueParameters: List<CjParameter> = emptyList()
    override val receiverTypeReference: CjTypeReference? = null
    override val typeReference: CjTypeReference? = null

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? = null
    override fun getName(): String? {
        return text
    }
    private fun getParentVariable(): CjVariable? {
        var parent = parent

//       如果parent是 CjCasePattern，则继续向上寻找
        while (parent is CjCasePattern) {
            parent = parent.parent
        }
        return parent as? CjVariable
    }

    //    绑定模式隶属的变量声明
    val variable: CjVariable?
        get() {
            return getParentVariable()
        }
    override val colon: PsiElement? = null
    override val nameAsSafeName: Name
        get() = Name.identifier("")
    override val fqName: FqName? = null
    override val docComment: CDoc? = null
    override val expression: CjExpression? = null

    override val modifierList: CjModifierList? = null
    override fun hasModifier(modifier: CjKeywordToken): Boolean = false

    override fun addModifier(modifier: CjKeywordToken) {
    }

    override fun removeModifier(modifier: CjKeywordToken) {
    }

    override val annotations: CjAnnotations? = null
    override val annotationEntries: List<CjAnnotation> = emptyList()

    override fun setName(name: String): PsiElement {
        return this
    }

    override fun getNameIdentifier(): PsiElement? = null

    override val nameAsName: Name? = nameAsSafeName
    override val typeParameterList: CjTypeParameterList? = null
    override val typeConstraintList: CjTypeConstraintList? = null
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    override val typeParameters: List<CjTypeParameter> = emptyList()
    override val initializer: CjExpression? = null

    override fun hasInitializer(): Boolean {
        TODO("Not yet implemented")
    }

    override val letOrVarKeyword: PsiElement? = null
}

class CjBindingPattern(node: ASTNode) : PatternVariableDeclaration(node), CjSimpleNameExpression {
    override val referencedName: String get() {
        return expression?.name ?: ""
    }

    override val referencedNameAsName: Name get() {
        return Name.identifier(referencedName)
    }

    val isLocal: Boolean
        get() = !isTopLevel
    val isTopLevel: Boolean
        get() {

            return parent is CjFile
        }

    override val referencedNameElement: PsiElement get() {
        return expression ?: this
    }

    override val identifier: PsiElement? get() {
        return findChildByType(CjTokens.IDENTIFIER)
    }

    override val referencedNameElementType: IElementType get() {
        return CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
    }

    override val expression: CjSimpleNameExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitPatternByBinding(this, data)
    }

    override val nameAsSafeName: Name
        get() = Name.identifier(name ?: "")
}

class CjTypePattern(node: ASTNode) : PatternVariableDeclaration(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitPatternByType(this, data)
    }

    val identifier: PsiElement? get() = findChildByType(CjTokens.IDENTIFIER)
    override fun getName(): String? {
        return reference?.text
    }

    val reference get() = findChildByType<CjSimpleNameExpression>(CjNodeTypes.REFERENCE_EXPRESSION)

    override val typeReference get() = findChildByType<CjTypeReference>(CjNodeTypes.TYPE_REFERENCE)
}

class CjTuplePattern(node: ASTNode) : CjCasePattern(node), CjEnumAndTuplePattern {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitPatternByTuple(this, data)
    }

    override val patterns get() = findChildrenByClass(CjCasePattern::class.java).toList()
}

interface CjEnumAndTuplePattern {
    val patterns: List<CjCasePattern>
}

class CjEnumPattern(node: ASTNode) : CjCasePattern(node), CjEnumAndTuplePattern {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitPatternByEnum(this, data)
    }

    val type: CjTypeReference?
        get() = findChildByType(CjNodeTypes.TYPE_REFERENCE)
    val expression: CjExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION) ?: findChildByType(
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
        )

    override val patterns get() = findChildrenByClass(CjCasePattern::class.java).toList()
}

class CjWildcardPattern(node: ASTNode) : CjCasePattern(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitPatternByWildcard(this, data)
    }
}

class CjConstantPattern(node: ASTNode) : CjCasePattern(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitPatternByConstant(this, data)
    }

    val expression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java)
        }
}
