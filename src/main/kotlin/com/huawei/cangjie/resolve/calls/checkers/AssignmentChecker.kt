package com.huawei.cangjie.resolve.calls.checkers

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.psi.CjBinaryExpression
@DefaultImplementation(AssignmentChecker.Default::class)
interface AssignmentChecker {

    fun check(assignmentExpression: CjBinaryExpression, context: CallCheckerContext)

    object Default : AssignmentChecker {
        override fun check(assignmentExpression: CjBinaryExpression, context: CallCheckerContext)
        {

        }

    }
}
