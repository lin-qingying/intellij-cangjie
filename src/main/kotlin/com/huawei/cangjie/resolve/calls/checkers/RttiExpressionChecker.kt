package com.huawei.cangjie.resolve.calls.checkers

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.types.CangJieType
import com.intellij.psi.PsiElement


@DefaultImplementation(RttiExpressionChecker.Default::class)
interface RttiExpressionChecker {
    fun check(rttiInformation: RttiExpressionInformation, reportOn: PsiElement, trace: BindingTrace)

    object Default : RttiExpressionChecker {
        override fun check(rttiInformation: RttiExpressionInformation, reportOn: PsiElement, trace: BindingTrace) {

        }
    }
}

enum class RttiOperation {
    IS,

    AS

}

class RttiExpressionInformation(
    val subject: CjElement,
    val sourceType: CangJieType?,
    val targetType: CangJieType?,
    val operation: RttiOperation
)
