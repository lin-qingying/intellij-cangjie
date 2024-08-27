package com.huawei.cangjie.references

import com.huawei.cangjie.psi.CjBinaryExpression
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.resolve.caches.resolveToCall
import com.huawei.cangjie.resolve.calls.model.isReallySuccess


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
