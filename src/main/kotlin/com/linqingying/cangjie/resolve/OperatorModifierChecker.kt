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

package com.linqingying.cangjie.resolve

import com.intellij.psi.PsiElement
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.diagnostics.DiagnosticSink
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.utils.CheckResult
import com.linqingying.cangjie.utils.OperatorChecks


object OperatorModifierChecker {
    fun check(
        declaration: CjDeclaration,
        descriptor: DeclarationDescriptor,
        diagnosticHolder: DiagnosticSink,
        languageVersionSettings: LanguageVersionSettings
    ) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (!functionDescriptor.isOperator) return
        val modifier = declaration.modifierList?.getModifier(CjTokens.OPERATOR_KEYWORD) ?: return

        val checkResult = OperatorChecks.check(functionDescriptor)
        if (checkResult.isSuccess) {
//            when {
//                functionDescriptor.name in REM_TO_MOD_OPERATION_NAMES.keys ->
//                    checkSupportsFeature(LanguageFeature.OperatorRem, languageVersionSettings, diagnosticHolder, modifier)
//
//                functionDescriptor.name == OperatorNameConventions.PROVIDE_DELEGATE ->
//                    checkSupportsFeature(LanguageFeature.OperatorProvideDelegate, languageVersionSettings, diagnosticHolder, modifier)
//
//                functionDescriptor.isTypedEqualsInValueClass() ->
//                    checkSupportsFeature(LanguageFeature.CustomEqualsInValueClasses, languageVersionSettings, diagnosticHolder, modifier)
//            }
//
//            if (functionDescriptor.name in REM_TO_MOD_OPERATION_NAMES.values &&
//                languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
//            ) {
//                val diagnosticFactory = if (!CangJieBuiltIns.isUnderCangJiePackage(descriptor) &&
//                    languageVersionSettings.supportsFeature(LanguageFeature.ProhibitOperatorMod)
//                )
//                    Errors.FORBIDDEN_BINARY_MOD
//                else
//                    Errors.DEPRECATED_BINARY_MOD
//
//                val newNameConvention = REM_TO_MOD_OPERATION_NAMES.inverse()[functionDescriptor.name]
//                diagnosticHolder.report(diagnosticFactory.on(modifier, functionDescriptor, newNameConvention!!.asString()))
//            }

            return
        }

        val errorDescription = (checkResult as? CheckResult.IllegalSignature)?.error ?: "illegal function name"

        diagnosticHolder.report(Errors.INAPPLICABLE_OPERATOR_MODIFIER.on(modifier, errorDescription))
    }

    private fun checkSupportsFeature(
        feature: LanguageFeature,
        languageVersionSettings: LanguageVersionSettings,
        diagnosticHolder: DiagnosticSink,
        modifier: PsiElement
    ) {
        if (!languageVersionSettings.supportsFeature(feature)) {
            diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(modifier, feature to languageVersionSettings))
        }
    }
}
