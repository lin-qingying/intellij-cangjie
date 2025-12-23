/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.inspections.suppress

import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.cangnova.cangjie.codeinsight.CangJieCodeInsightBundle
import org.cangnova.cangjie.diagnostics.Severity
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDestructuringDeclaration
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjStruct
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVisitor

class CangJieSuppressableWarningProblemGroup(private val factoryName: String) : SuppressableProblemGroup {
    override fun getProblemName(): String = factoryName

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction> {
        return if (element != null) {
            createSuppressWarningActions(element, Severity.WARNING, factoryName).toTypedArray()
        } else {
            SuppressIntentionAction.EMPTY_ARRAY
        }
    }
}


fun createSuppressWarningActions(
    element: PsiElement,
    severity: Severity,
    suppressionKey: String
): List<SuppressIntentionAction> {
    if (severity != Severity.WARNING) {
        return emptyList()
    }

    val actions = arrayListOf<SuppressIntentionAction>()
    var current: PsiElement? = element
    var suppressAtStatementAllowed = true
    while (current != null) {
        when {
            current is CjDeclaration && current !is CjDestructuringDeclaration -> {
                val declaration = current
                val kind = DeclarationKindDetector.detect(declaration)
                if (kind != null) {
                    actions.add(CangJieSuppressIntentionAction(declaration, suppressionKey, kind))
                }
                suppressAtStatementAllowed = false
            }

            current is CjExpression && suppressAtStatementAllowed -> {
                // Add suppress action at first statement
                if (current.parent is CjBlockExpression || current.parent is CjDestructuringDeclaration) {
                    val kind = if (current.parent is CjBlockExpression)
                        CangJieCodeInsightBundle.message("declaration.kind.statement")
                    else
                        CangJieCodeInsightBundle.message("declaration.kind.initializer")

                    val hostKind = AnnotationHostKind(kind, null, true)
                    actions.add(CangJieSuppressIntentionAction(current, suppressionKey, hostKind))
                    suppressAtStatementAllowed = false
                }
            }

            current is PsiWhiteSpace && current.prevSibling is CjClassLikeDeclaration -> {
                current = current.prevSibling
                continue
            }

            current is CjFile -> {
                val hostKind =
                    AnnotationHostKind(CangJieCodeInsightBundle.message("declaration.kind.file"), current.name, true)
                actions.add(CangJieSuppressIntentionAction(current, suppressionKey, hostKind))
                break
            }
        }

        current = current.parent
    }

    return actions
}

private object DeclarationKindDetector : CjVisitor<AnnotationHostKind?, Unit>() {
    @Suppress("UNCHECKED_CAST")
    fun detect(declaration: CjDeclaration) = declaration.accept(this as CjVisitor<AnnotationHostKind?, Unit?>, null as Unit?)

    override fun visitDeclaration(declaration: CjDeclaration, data: Unit) = null

    private fun getDeclarationName(declaration: CjDeclaration): @NlsSafe String {
        return declaration.name ?: CangJieCodeInsightBundle.message("declaration.name.anonymous")
    }

    override fun visitClass(declaration: CjClass, data: Unit): AnnotationHostKind? {


        return visitTypeStatement(declaration, data)
    }

    override fun visitInterface(cinterface: CjInterface, data: Unit): AnnotationHostKind? {
        return visitTypeStatement(cinterface, data)

    }

    override fun visitEnum(cenum: CjEnum, data: Unit): AnnotationHostKind? {
        return visitTypeStatement(cenum, data)

    }

    override fun visitStruct(cstruct: CjStruct, data: Unit): AnnotationHostKind? {
        return visitTypeStatement(cstruct, data)

    }

    override fun visitTypeStatement(typeStatement: CjTypeStatement, data: Unit): AnnotationHostKind? {

        val kind = when (typeStatement) {
            is CjClass -> CangJieCodeInsightBundle.message("declaration.kind.class")
            is CjInterface -> CangJieCodeInsightBundle.message("declaration.kind.interface")
            is CjStruct -> CangJieCodeInsightBundle.message("declaration.kind.struct")
            is CjEnum -> CangJieCodeInsightBundle.message("declaration.kind.enum")
            else -> ""
        }
        return AnnotationHostKind(kind, getDeclarationName(typeStatement), newLineNeeded = true)
    }

    override fun visitNamedFunction(declaration: CjNamedFunction, data: Unit): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.fun")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
    }

    override fun visitProperty(declaration: CjProperty, data: Unit): AnnotationHostKind {
        val kind = when {
            declaration.isVar -> CangJieCodeInsightBundle.message("declaration.kind.var")
            else -> CangJieCodeInsightBundle.message("declaration.kind.val")
        }
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
    }

    override fun visitTypeParameter(declaration: CjTypeParameter, data: Unit): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.type.parameter")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = false)
    }

//    override fun visitEnumEntry(declaration: CjEnumConstructor, data: Unit?): AnnotationHostKind {
//        val kind = CangJieCodeInsightBundle.message("declaration.kind.enum.entry")
//        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
//    }

    override fun visitParameter(declaration: CjParameter, data: Unit): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.parameter")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = false)
    }

    override fun visitSecondaryConstructor(declaration: CjSecondaryConstructor, data: Unit): AnnotationHostKind {
        val kind = CangJieCodeInsightBundle.message("declaration.kind.secondary.constructor.of")
        return AnnotationHostKind(kind, getDeclarationName(declaration), newLineNeeded = true)
    }


}
