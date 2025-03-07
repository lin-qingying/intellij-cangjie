package cn.cangnova.cangjie.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import cn.cangnova.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import cn.cangnova.cangjie.psi.CjDotQualifiedExpression
import cn.cangnova.cangjie.psi.visitDotQualifiedExpression
import cn.cangnova.cangjie.references.mainReference
import cn.cangnova.cangjie.resolve.caches.analyze

class CangJieDotQualifiedExpressionInspection : AbstractCangJieInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return visitDotQualifiedExpression(
            fun(expression: CjDotQualifiedExpression) {

                checkuseModleName(holder, expression)
            }
        )
    }

    private fun checkuseModleName(holder: ProblemsHolder, expression: CjDotQualifiedExpression) {

    }
}
