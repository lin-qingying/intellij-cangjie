/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve.check

import cn.cangnova.cangjie.config.LanguageFeature
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.diagnostics.DiagnosticSink
import cn.cangnova.cangjie.diagnostics.Errors
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.descriptors.impl.FunctionExpressionDescriptor
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.resolve.caches.DeclarationChecker
import cn.cangnova.cangjie.resolve.caches.DeclarationCheckerContext
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
