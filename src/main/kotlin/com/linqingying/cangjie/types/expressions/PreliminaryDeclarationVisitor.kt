package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType

import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.psi.psiUtil.parentsWithSelf

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
