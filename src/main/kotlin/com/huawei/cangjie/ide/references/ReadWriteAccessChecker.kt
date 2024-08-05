package com.huawei.cangjie.ide.references

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjBinaryExpression
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjParenthesizedExpression
import com.huawei.cangjie.psi.CjUnaryExpression
import com.huawei.cangjie.psi.psiUtil.getAssignmentByLHS
import com.huawei.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

import com.huawei.cangjie.utils.constant

import com.intellij.openapi.project.Project


interface ReadWriteAccessChecker {
    fun readWriteAccessWithFullExpressionByResolve(assignment: CjBinaryExpression): Pair<ReferenceAccess, CjExpression>?

    fun readWriteAccessWithFullExpression(
        targetExpression: CjExpression,
        useResolveForReadWrite: Boolean
    ): Pair<ReferenceAccess, CjExpression> {
        var expression = targetExpression.getQualifiedExpressionForSelectorOrThis()
        loop@ while (true) {
            when (val parent = expression.parent) {
                is CjParenthesizedExpression  -> expression = parent as CjExpression
                else -> break@loop
            }
        }

        val assignment = expression.getAssignmentByLHS()
        if (assignment != null) {
            return when (assignment.operationToken) {
                CjTokens.EQ -> ReferenceAccess.WRITE to assignment

                else -> {
                    (if (useResolveForReadWrite) readWriteAccessWithFullExpressionByResolve(assignment) else null)
                        ?: (ReferenceAccess.READ_WRITE to assignment)
                }
            }
        }

        val unaryExpression = expression.parent as? CjUnaryExpression
        return if (unaryExpression != null && unaryExpression.operationToken in constant { setOf(CjTokens.PLUSPLUS, CjTokens.MINUSMINUS) })
            ReferenceAccess.READ_WRITE to unaryExpression
        else
            ReferenceAccess.READ to expression
    }

    companion object {
        fun getInstance(project: Project): ReadWriteAccessChecker = project.getService(ReadWriteAccessChecker::class.java)
    }
}

// Used in IDE
@Suppress("unused")
fun CjExpression.readWriteAccessWithFullExpression(useResolveForReadWrite: Boolean): Pair<ReferenceAccess, CjExpression> =
    ReadWriteAccessChecker.getInstance(project).readWriteAccessWithFullExpression(this, useResolveForReadWrite)

fun CjExpression.readWriteAccess(useResolveForReadWrite: Boolean): ReferenceAccess {
    return ReadWriteAccessChecker.getInstance(project).readWriteAccessWithFullExpression(this, useResolveForReadWrite).first
}

//class ReadWriteAccessCheckerDescriptorsImpl : ReadWriteAccessChecker {
//    override fun readWriteAccessWithFullExpressionByResolve(assignment: CjBinaryExpression): Pair<ReferenceAccess, KtExpression>? {
//        val resolvedCall = assignment.resolveToCall() ?: return null
//        if (!resolvedCall.isReallySuccess()) return null
//        return if (resolvedCall.resultingDescriptor.name in OperatorConventions.ASSIGNMENT_OPERATIONS.values)
//            ReferenceAccess.READ to assignment
//        else
//            null
//    }
//}
