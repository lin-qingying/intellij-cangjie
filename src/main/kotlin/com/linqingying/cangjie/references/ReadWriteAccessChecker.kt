/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.references

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjBinaryExpression
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjParenthesizedExpression
import com.linqingying.cangjie.psi.CjUnaryExpression
import com.linqingying.cangjie.psi.psiUtil.getAssignmentByLHS
import com.linqingying.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

import com.linqingying.cangjie.utils.constant

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
                is CjParenthesizedExpression  -> expression = parent
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
//    override fun readWriteAccessWithFullExpressionByResolve(assignment: CjBinaryExpression): Pair<ReferenceAccess, CjExpression>? {
//        val resolvedCall = assignment.resolveToCall() ?: return null
//        if (!resolvedCall.isReallySuccess()) return null
//        return if (resolvedCall.resultingDescriptor.name in OperatorConventions.ASSIGNMENT_OPERATIONS.values)
//            ReferenceAccess.READ to assignment
//        else
//            null
//    }
//}
