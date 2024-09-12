package com.huawei.cangjie.resolve.check

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.diagnostics.DiagnosticSink
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.descriptors.impl.FunctionExpressionDescriptor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.caches.DeclarationChecker
import com.huawei.cangjie.resolve.caches.DeclarationCheckerContext
import com.intellij.psi.PsiElement


object UnderscoreChecker : DeclarationChecker {
    @JvmOverloads
    fun checkIdentifier(
        identifier: PsiElement?,
        diagnosticHolder: DiagnosticSink,
        languageVersionSettings: LanguageVersionSettings,
        allowSingleUnderscore: Boolean = false
    ) {
        if (identifier == null || identifier.text.isEmpty()) return
        val isValidSingleUnderscore = allowSingleUnderscore && identifier.text == "_"
        if (!isValidSingleUnderscore && identifier.text.all { it == '_' }) {
            diagnosticHolder.report(Errors.UNDERSCORE_IS_RESERVED.on(identifier))
        } else if (isValidSingleUnderscore && !languageVersionSettings.supportsFeature(LanguageFeature.SingleUnderscoreForParameterName)) {
            diagnosticHolder.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    identifier,
                    LanguageFeature.SingleUnderscoreForParameterName to languageVersionSettings
                )
            )
        }
    }

    @JvmOverloads
    fun checkNamed(
        declaration: CjNamedDeclaration,
        diagnosticHolder: DiagnosticSink,
        languageVersionSettings: LanguageVersionSettings,
        allowSingleUnderscore: Boolean = false
    ) {
        checkIdentifier(declaration.nameIdentifier, diagnosticHolder, languageVersionSettings, allowSingleUnderscore)
    }

    override fun check(declaration: CjDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration is CjProperty && descriptor !is VariableDescriptor) return
        if (declaration is CjCallableDeclaration) {
            for (parameter in declaration.valueParameters) {
                checkNamed(
                    parameter, context.trace, context.languageVersionSettings,
                    allowSingleUnderscore = descriptor is FunctionExpressionDescriptor
                )
            }
        }
        if (declaration is CjTypeParameterListOwner) {
            for (typeParameter in declaration.typeParameters) {
                checkNamed(typeParameter, context.trace, context.languageVersionSettings)
            }
        }
        if (declaration !is CjNamedDeclaration) return
        checkNamed(declaration, context.trace, context.languageVersionSettings)
    }
}
