package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver

fun isSuperOrDelegatingConstructorCall(call: Call): Boolean =
    call.calleeExpression.let { it is CjConstructorCalleeExpression || it is CjConstructorDelegationReferenceExpression }

fun isInvokeCallOnVariable(call: Call): Boolean {
    if (call.callType !== Call.CallType.INVOKE) return false
    val dispatchReceiver = call.dispatchReceiver
    //calleeExpressionAsDispatchReceiver for invoke is always ExpressionReceiver, see CallForImplicitInvoke
    val expression = (dispatchReceiver as ExpressionReceiver).expression
    return expression is CjSimpleNameExpression
}
fun isBinaryRemOperator(call: Call): Boolean {
    val callElement = call.callElement as? CjBinaryExpression ?: return false
    val operator = callElement.operationToken
    if (operator !is CjToken) return false

    //TODO: check if this is correct
    return true
//    val name = OperatorConventions.getNameForOperationSymbol(operator, true, true) ?: return false
//    return name in OperatorConventions.REM_TO_MOD_OPERATION_NAMES.keys
}
fun isInfixCall(call: Call): Boolean {
    val operationRefExpression = call.calleeExpression as? CjOperationReferenceExpression ?: return false
    val binaryExpression = operationRefExpression.parent as? CjBinaryExpression ?: return false
    return binaryExpression.operationReference === operationRefExpression && operationRefExpression.operationSignTokenType == null
}
