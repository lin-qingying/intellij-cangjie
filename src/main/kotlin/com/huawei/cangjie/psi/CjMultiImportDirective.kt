package com.huawei.cangjie.psi

import com.huawei.cangjie.name.FqName
import com.intellij.lang.ASTNode

class CjMultiImportDirective(node: ASTNode) : CjExpressionImpl(node), CjStatementExpression {

    val fqName: FqName?
        get() {
//            val expr = this.findChildByClass(CjNameReferenceExpressionElementType::class.java) ?: this.findChildByClass(
//                DOT_QUALIFIED_EXPRESSION::class.java
//            )
//
//            if (expr != null) {
//                expr as CjExpression
//
//                return CjImportDirective.fqNameFromExpression(expr)
//            }
//            return null

           if(this.children.isEmpty()){
               return null
           }
            return CjImportDirective.fqNameFromExpression(this.children[0] as? CjExpression)
        }
//
//    @IfNotParsed
//    fun getImportedReference(): CjExpression? {
//        val references: Array<CjExpression> =
//            getStubOrPsiChildren<CjExpression>(CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.ARRAY_FACTORY)
//        if (references.size > 0) {
//            return references[0]
//        }
//        return null
//    }
}

class CjMultiImportDirective1(node: ASTNode) : CjExpressionImpl(node), CjStatementExpression
