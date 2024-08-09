package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes.FUNCTION_LITERAL
import com.linqingying.cangjie.CjNodeTypes.LAMBDA_EXPRESSION
import com.linqingying.cangjie.lexer.CjTokens.LBRACE
import com.linqingying.cangjie.lexer.CjTokens.RBRACE
import com.linqingying.cangjie.psi.psiUtil.getContainingCjFile
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement

class CjLambdaExpression(text: CharSequence?) :
    LazyParseablePsiElement( LAMBDA_EXPRESSION, text),
   CjExpression {


    val functionLiteral: CjFunctionLiteral
        get() = findChildByType( FUNCTION_LITERAL)?.getPsi(CjFunctionLiteral::class.java)!!

    val valueParameters: MutableList<CjParameter>
        get() = functionLiteral.getValueParameters()

    val bodyExpression: CjBlockExpression?
        get() = functionLiteral.bodyExpression

    fun hasDeclaredReturnType(): Boolean {
        return functionLiteral.typeReference != null
    }

    fun asElement(): CjElement {
        return this
    }

    val leftCurlyBrace: ASTNode
        get() = functionLiteral.node.findChildByType( LBRACE)!!

    val rightCurlyBrace: ASTNode
        get() = functionLiteral.node.findChildByType( RBRACE)!!


    override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {
        CjPsiUtil.visitChildren<D>(this, visitor, data)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitLambdaExpression(this, data)

    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            accept(visitor, null)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun getPsiOrParent(): CjElement {
      return this
    }

    override fun getContainingCjFile(): CjFile {

        return (this as LazyParseablePsiElement).getContainingCjFile()

    }



    @Suppress("unused") //keep for compatibility with potential plugins
    fun shouldChangeModificationCount(place: PsiElement?): Boolean {
        return false
    }
}
