package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjAnnotated
import com.huawei.cangjie.psi.CjCallElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjLambdaExpression

class AnnotationChecker {

    fun checkExpression(expression: CjExpression, trace: BindingTrace) {
//        checkEntries(
//            expression.getAnnotationEntries(),
//            getActualTargetList(expression, null, trace.bindingContext),
//            trace,
//            expression.parent as? CjAnnotated
//        )
//        if (expression is CjCallElement  ) {
//            val typeArguments = expression.typeArguments.mapNotNull { it.typeReference }
//            for (typeArgument in typeArguments) {
//                checkEntries(typeArgument.annotationEntries, getActualTargetList(typeArgument, null, trace.bindingContext), trace)
//            }
//        }
//        if (expression is CjLambdaExpression) {
//            for (parameter in expression.valueParameters) {
//                parameter.typeReference?.let { check(it, trace) }
//            }
//        }
    }
}