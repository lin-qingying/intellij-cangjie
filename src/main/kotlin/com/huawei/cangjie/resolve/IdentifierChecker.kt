package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.container.PlatformSpecificExtension
import com.huawei.cangjie.diagnostics.DiagnosticSink
import com.huawei.cangjie.psi.*
import com.intellij.psi.PsiElement


@DefaultImplementation(impl = IdentifierChecker.Default::class)
interface IdentifierChecker : PlatformSpecificExtension<IdentifierChecker> {
    fun checkIdentifier(simpleNameExpression:CjSimpleNameExpression, diagnosticHolder: DiagnosticSink)
    fun checkDeclaration(declaration:CjDeclaration, diagnosticHolder: DiagnosticSink)

    object Default : IdentifierChecker {
        override fun checkIdentifier(simpleNameExpression: CjSimpleNameExpression, diagnosticHolder: DiagnosticSink) {}
        override fun checkDeclaration(declaration: CjDeclaration, diagnosticHolder: DiagnosticSink) {}
    }
}
object CangJieSimpleNameBacktickChecker : IdentifierChecker {
    // See The Java Virtual Machine Specification, section 4.7.9.1 https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9.1
    val INVALID_CHARS = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

    // These characters can cause problems on Windows. '?*"|' are not allowed in file names, and % leads to unexpected env var expansion.
    private val DANGEROUS_CHARS = setOf('?', '*', '"', '|', '%')

    override fun checkIdentifier(simpleNameExpression: CjSimpleNameExpression, diagnosticHolder: DiagnosticSink) {
        reportIfNeeded(simpleNameExpression.getReferencedName(), { simpleNameExpression.getIdentifier() }, diagnosticHolder)
    }

    override fun checkDeclaration(declaration: CjDeclaration, diagnosticHolder: DiagnosticSink) {
        if (declaration is CjDestructuringDeclaration) {
            declaration.entries.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is CjCallableDeclaration) {
            declaration.valueParameters.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is CjTypeParameterListOwner) {
            declaration.typeParameters.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is CjNamedDeclaration) {
            checkNamed(declaration, diagnosticHolder)
        }
    }

    private fun checkNamed(declaration: CjNamedDeclaration, diagnosticHolder: DiagnosticSink) {
        val name = declaration.name ?: return

        reportIfNeeded(name, { declaration.nameIdentifier ?: declaration }, diagnosticHolder)
    }

    private fun reportIfNeeded(name: String, reportOn: () -> PsiElement?, diagnosticHolder: DiagnosticSink) {
        val text = CjPsiUtil.unquoteIdentifier(name)
//        when {
//            text.isEmpty() -> {
//                diagnosticHolder.report(Errors.INVALID_CHARACTERS.on(reportOn() ?: return, "should not be empty"))
//            }
//            text.any { it in INVALID_CHARS } -> {
//                diagnosticHolder.report(
//                    Errors.INVALID_CHARACTERS.on(
//                        reportOn() ?: return,
//                        "contains illegal characters: ${INVALID_CHARS.intersect(text.toSet()).joinToString("")}"
//                    )
//                )
//            }
//            text.any { it in DANGEROUS_CHARS } -> {
//                diagnosticHolder.report(
//                    ErrorsJvm.DANGEROUS_CHARACTERS.on(reportOn() ?: return, DANGEROUS_CHARS.intersect(text.toSet()).joinToString(""))
//                )
//            }
//        }
    }
}
