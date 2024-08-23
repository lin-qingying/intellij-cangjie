package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType

import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.utils.parentsWithSelf

class PreliminaryDeclarationVisitor(
    val declaration: CjDeclaration,
    val languageVersionSettings: LanguageVersionSettings
) : AssignedVariablesSearcher() {


    companion object {

        fun createForExpression(
            expression: CjExpression,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings
        ) {
            expression.getStrictParentOfType<CjDeclaration>()
                ?.let { createForDeclaration(it, trace, languageVersionSettings) }
        }

        private fun topMostNonClassDeclaration(declaration: CjDeclaration) =
            declaration.parentsWithSelf.filterIsInstance<CjDeclaration>().findLast { it !is CjTypeStatement }
                ?: declaration

        fun createForDeclaration(
            declaration: CjDeclaration,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings
        ) {
            val visitorOwner = topMostNonClassDeclaration(declaration)
            if (trace.get(BindingContext.PRELIMINARY_VISITOR, visitorOwner) != null) return
            trace.record(
                BindingContext.PRELIMINARY_VISITOR, visitorOwner,
                PreliminaryDeclarationVisitor(visitorOwner, languageVersionSettings)
            )
        }

    }
}
