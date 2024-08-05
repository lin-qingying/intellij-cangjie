
package com.huawei.cangjie.ide.codeinsight.impl

import com.huawei.cangjie.ide.formatter.util.TrailingCommaContext
import com.huawei.cangjie.ide.formatter.util.TrailingCommaState
import com.huawei.cangjie.ide.formatter.util.canAddTrailingComma
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFunctionLiteral
import com.huawei.cangjie.psi.CjTreeVisitorVoid
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement


abstract class TrailingCommaVisitor : CjTreeVisitorVoid() {
    override fun visitCjElement(element: CjElement) {
        super.visitCjElement(element)
        // because CjFunctionLiteral contains CjParameterList
        if (element !is CjFunctionLiteral && element.canAddTrailingComma()) {
            runProcessIfApplicable(element)
        }
    }

    override fun visitElement(element: PsiElement) {
        ProgressIndicatorProvider.checkCanceled()

        if (recursively) super.visitElement(element)
    }

    private fun runProcessIfApplicable(element: CjElement) {
        val context = TrailingCommaContext.create(element)
        if (context.state != TrailingCommaState.NOT_APPLICABLE) {
            process(context)
        }
    }


    protected abstract fun process(trailingCommaContext: TrailingCommaContext)

    protected open val recursively: Boolean = true
}
