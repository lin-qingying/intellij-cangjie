package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.descriptors.PsiDiagnosticUtils
import com.huawei.cangjie.descriptors.PsiDiagnosticUtils.Companion.offsetToLineAndColumn
import com.huawei.cangjie.resolve.DescriptorToSourceUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile




object DiagnosticUtils {
    fun getLineAndColumnInPsiFile(
        file: PsiFile,
        range: TextRange
    ): PsiDiagnosticUtils.LineAndColumn {
        val document = file.viewProvider.document
        return offsetToLineAndColumn(document, range.startOffset)
    }

    fun throwIfRunningOnServer(e: Throwable?) {
        // This is needed for the Web Demo server to log the exceptions coming from the analyzer instead of showing them in the editor.
        if (System.getProperty(
                "cangjie.running.in.server.mode",
                "false"
            ) == "true" || ApplicationManager.getApplication().isUnitTestMode
        ) {
            if (e is RuntimeException) {
                throw (e as RuntimeException?)!!
            }
            if (e is Error) {
                throw (e as Error?)!!
            }
            throw RuntimeException(e)
        }
    }

}

inline fun reportOnDeclarationOrFail(trace: BindingTrace, descriptor: DeclarationDescriptor, what: (PsiElement) -> Diagnostic) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        trace.report(what(psiElement))
    } ?: throw AssertionError("No declaration for $descriptor")
}

fun BindingTrace.reportDiagnosticOnce(diagnostic: Diagnostic) {
    if (bindingContext.diagnostics.noSuppression().forElement(diagnostic.psiElement).any { it.factory == diagnostic.factory }) return

    report(diagnostic)
}
