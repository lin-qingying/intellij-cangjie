package com.linqingying.cangjie.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.linqingying.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import com.linqingying.cangjie.psi.CjDotQualifiedExpression
import com.linqingying.cangjie.psi.visitDotQualifiedExpression
import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.resolve.caches.analyze

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
