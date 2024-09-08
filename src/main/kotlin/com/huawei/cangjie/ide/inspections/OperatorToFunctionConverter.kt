package com.huawei.cangjie.ide.inspections

import com.huawei.cangjie.psi.*

object OperatorToFunctionConverter {



    /**
     * Converts operator call to an explicit function call.
     *
     * N.B. Has to use some resolve inside, so resorts to [allowAnalysisOnEdt].
     */
//    fun convert(element: CjExpression): Pair<CjExpression, CjSimpleNameExpression> {
//        var elementToBeReplaced = element
//        if (element is CjArrayAccessExpression && isAssignmentLeftSide(element)) {
//            elementToBeReplaced = element.parent as CjExpression
//        }
//
//        val commentSaver = CommentSaver(elementToBeReplaced, saveLineBreaks = true)
//
//        val result = when (element) {
//            is CjUnaryExpression -> convertUnary(element)
//            is CjBinaryExpression -> convertBinary(element)
//            is CjArrayAccessExpression -> convertArrayAccess(element)
//            is CjCallExpression -> convertCall(element)
//            else -> throw IllegalArgumentException(element.toString())
//        }
//
//        commentSaver.restore(result)
//
//        val callName = findCallName(result) ?: error("No call name found in ${result.text}")
//        return result to callName
//    }

}
