package com.linqingying.cangjie.resolve.calls.checkers

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.psi.CjBinaryExpression
@DefaultImplementation(AssignmentChecker.Default::class)
interface AssignmentChecker {

    fun check(assignmentExpression: CjBinaryExpression, context: CallCheckerContext)

    object Default : AssignmentChecker {
        override fun check(assignmentExpression: CjBinaryExpression, context: CallCheckerContext)
        {

        }

    }
}
