package com.linqingying.cangjie.references

import com.linqingying.cangjie.psi.CjBinaryExpression
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.caches.resolveToCall
import com.linqingying.cangjie.resolve.calls.model.isReallySuccess


class ReadWriteAccessCheckerDescriptorsImpl : ReadWriteAccessChecker {
    override fun readWriteAccessWithFullExpressionByResolve(assignment: CjBinaryExpression): Pair<ReferenceAccess, CjExpression>? {
        val resolvedCall = assignment.resolveToCall() ?: return null
        if (!resolvedCall.isReallySuccess()) return null
//        return if (resolvedCall.resultingDescriptor.name in OperatorConventions.ASSIGNMENT_OPERATIONS.values)
//            ReferenceAccess.READ to assignment
//        else
//            null

        return null
    }
}
