package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.doc.psi.CDoc
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


abstract class CjCasePattern(node: ASTNode) : CjElementImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCasePattern(this, data)
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
    override val colon: PsiElement? = null
    override val nameAsSafeName: Name
        get() = TODO("Not yet implemented")
    override val fqName: FqName? = null
    override val docComment: CDoc? = null
    override val expression: CjExpression? = null

    override val modifierList: CjModifierList? = null
    override fun hasModifier(modifier: CjModifierKeywordToken): Boolean = false

    override fun addModifier(modifier: CjModifierKeywordToken) {

    }

    override fun removeModifier(modifier: CjModifierKeywordToken) {

    }

    override val modifierVisibility: DescriptorVisibility? = null
    override val annotations: List<CjAnnotation> = emptyList()
    override val annotationEntries: List<CjAnnotationEntry> = emptyList()

    override fun setName(name: String): PsiElement {

        return this
    }

    override fun getNameIdentifier(): PsiElement? = null

    override val nameAsName: Name? = null
    override val typeParameterList: CjTypeParameterList? = null
    override val typeConstraintList: CjTypeConstraintList? = null
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    override val typeParameters: List<CjTypeParameter> = emptyList()
    override val initializer: CjExpression?
        get() = TODO("Not yet implemented")

    override fun hasInitializer(): Boolean {
        TODO("Not yet implemented")
    }

    override val letOrVarKeyword: PsiElement? = null
}

class CjBindingPattern(node: ASTNode) : PatternVariableDeclaration(node) ,CjReferenceExpression{
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPatternByBinding(this, data)
    }
}

class CjTypePattern(node: ASTNode) : PatternVariableDeclaration(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPatternByType(this, data)
    }
    val identifier: PsiElement? get() = findChildByType(CjTokens.IDENTIFIER)
    override fun getName(): String? {
        return identifier?.text
    }
    override val typeReference   get() = findChildByType<CjTypeReference>(CjNodeTypes.TYPE_REFERENCE)
}

class CjTuplePattern(node: ASTNode) : CjCasePattern(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPatternByTuple(this, data)
    }
}

class CjEnumPattern(node: ASTNode) : CjCasePattern(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPatternByEnum(this, data)
    }
}

class CjWildcardPattern(node: ASTNode) : CjCasePattern(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPatternByWildcard(this, data)
    }
}

class CjConstantPattern(node: ASTNode) : CjCasePattern(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPatternByConstant(this, data)
    }


    val expression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java)
        }

}
