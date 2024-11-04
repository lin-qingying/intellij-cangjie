package com.linqingying.cangjie.references

import com.linqingying.cangjie.psi.CjBinaryExpression
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjNamedFunction

class ReadWriteAccessCheckerDescriptorsFirImpl : ReadWriteAccessChecker {
    override fun readWriteAccessWithFullExpressionByResolve(assignment: CjBinaryExpression): Pair<ReferenceAccess, CjExpression>? {
        val function = assignment.operationReference.mainReference.resolve() as? CjNamedFunction ?: return null
        val name = function.name ?: return null
//        return if (Name.identifier(name) in OperatorConventions.ASSIGNMENT_OPERATIONS.values)
//            ReferenceAccess.READ to assignment
//        else
//            null
    return null
    }
}
