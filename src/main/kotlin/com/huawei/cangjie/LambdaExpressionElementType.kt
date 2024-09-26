//package com.huawei.cangjie
//
//import com.huawei.cangjie.ElementTypeUtils.getCangJieBlockImbalanceCount
//import com.huawei.cangjie.lang.CangJieLanguage
//import com.huawei.cangjie.lexer.CjTokens.DOUBLE_ARROW
//import com.huawei.cangjie.parsing.CangJieParser
//import com.huawei.cangjie.psi.CjFunctionLiteral
//import com.huawei.cangjie.psi.CjLambdaExpression
//import com.intellij.lang.ASTNode
//import com.intellij.lang.Language
//import com.intellij.lang.PsiBuilderFactory
//import com.intellij.openapi.project.Project
//import com.intellij.psi.PsiElement
//import com.intellij.psi.tree.IErrorCounterReparseableElementType
//import com.intellij.psi.util.PsiTreeUtil
//
//class LambdaExpressionElementType: IErrorCounterReparseableElementType("LAMBDA_EXPRESSION",CangJieLanguage) {
//    override fun getErrorsCount(seq: CharSequence, fileLanguage: Language?, project: Project?): Int {
//        return getCangJieBlockImbalanceCount(seq)
//
//    }
//
//
//    companion object{
//        private fun findLambdaExpression(parent: ASTNode?):CjLambdaExpression? {
//            if (parent == null) return null
//
//            val parentPsi = parent.psi
//            val lambdaExpressions: Array<CjLambdaExpression> = PsiTreeUtil.getChildrenOfType(
//                parentPsi,
//             CjLambdaExpression::class.java
//            )
//            if (lambdaExpressions == null || lambdaExpressions.size != 1) return null
//
//            // Now works only when actual node can be spotted ambiguously. Need change in API.
//            return lambdaExpressions[0]
//        }
//
//    }
//
//    override fun parseContents(chameleon: ASTNode): ASTNode {
//        val project = chameleon.psi.project
//        val builder = PsiBuilderFactory.getInstance().createBuilder(
//            project, chameleon, null,CangJieLanguage, chameleon.chars
//        )
//        return  CangJieParser.parseLambdaExpression(builder).getFirstChildNode()
//    }
//
//    override fun createNode(text: CharSequence?): ASTNode {
//        return CjLambdaExpression(text)
//    }
//    private fun wasArrowMovedOrDeleted(parent: ASTNode?, buffer: CharSequence): Boolean {
//        val lambdaArgument: CjLambdaExpression =
//          LambdaExpressionElementType.findLambdaExpression(parent)
//                ?: return false
//
//        val literal: CjFunctionLiteral = lambdaArgument.getFunctionLiteral()
//        val arrow: PsiElement = literal.getArrow() ?: return false
//
//        // No arrow in original node
//
//        val arrowOffset: Int = arrow.startOffsetInParent + literal.getStartOffsetInParent()
//
//        return hasTokenMoved(
//            lambdaArgument.getText(),
//            buffer,
//            arrowOffset,
//            DOUBLE_ARROW
//        )
//    }
//    override fun isParsable(parent: ASTNode?, buffer: CharSequence, fileLanguage: Language, project: Project): Boolean {
//                return super.isParsable(parent, buffer, fileLanguage!!, project!!) &&
//                !  wasArrowMovedOrDeleted(
//                    parent,
//                    buffer
//                ) && ! wasParameterCommaMovedOrDeleted(parent, buffer)
//    }
//
//
//}
