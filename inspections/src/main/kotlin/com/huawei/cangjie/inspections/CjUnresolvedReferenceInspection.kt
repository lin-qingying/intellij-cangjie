package com.huawei.cangjie.inspections

import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjVisitorVoid
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import inspections.CangJieInspectionsBundle

class CjUnresolvedReferenceInspection: CangJieLocalInspectionTool() {

    override fun getDisplayName() = CangJieInspectionsBundle.message("inspection.message.unresolved.reference2")



    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {


        return object :CjVisitorVoid(){
            override fun visitExpression(expression: CjExpression) {
                super.visitExpression(expression)
            }

            override fun visitCallExpression(expression: CjCallExpression) {
                super.visitCallExpression(expression)
            }
        }
    }
}