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

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.container.PlatformSpecificExtension
import com.linqingying.cangjie.diagnostics.DiagnosticSink
import com.linqingying.cangjie.psi.*
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
