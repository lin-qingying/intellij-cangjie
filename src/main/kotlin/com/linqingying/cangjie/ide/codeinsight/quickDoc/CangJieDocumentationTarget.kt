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

//package com.linqingying.cangjie.ide.codeinsight.quickDoc
//
//import com.google.common.html.HtmlEscapers
//import com.linqingying.cangjie.analyzer.CangJieAnalysisSession
//import com.linqingying.cangjie.analyzer.analyze
//
//import com.linqingying.cangjie.doc.CDocTemplate
//import com.linqingying.cangjie.doc.insert
//import com.linqingying.cangjie.lexer.CjTokens
//import com.linqingying.cangjie.psi.*
//import com.linqingying.cangjie.psi.psiUtil.containingTypeStatement
//import com.linqingying.cangjie.psi.psiUtil.getChildrenOfType
//import com.linqingying.cangjie.psi.psiUtil.prevLeaf
//import com.linqingying.cangjie.psi.psiUtil.referenceExpression
//import com.linqingying.cangjie.references.mainReference
//import com.intellij.codeInsight.navigation.targetPresentation
//import com.intellij.model.Pointer
//import com.intellij.platform.backend.documentation.DocumentationResult
//import com.intellij.platform.backend.documentation.DocumentationTarget
//import com.intellij.platform.backend.presentation.TargetPresentation
//import com.intellij.pom.Navigatable
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiWhiteSpace
//import com.intellij.refactoring.suggested.createSmartPointer
//import org.jetbrains.annotations.Nls
//
//internal fun PsiElement?.isModifier() =
//    this != null && parent is CjModifierList && CjTokens.MODIFIER_KEYWORDS_ARRAY.firstOrNull { it.value == text } != null
//
//internal class CangJieDocumentationTarget(val element: PsiElement, private val originalElement: PsiElement?) :
//    DocumentationTarget {
//    override fun createPointer(): Pointer<out DocumentationTarget> {
//        val elementPtr = element.createSmartPointer()
//        val originalElementPtr = originalElement?.createSmartPointer()
//        return Pointer {
//            val element = elementPtr.dereference() ?: return@Pointer null
//            CangJieDocumentationTarget(element, originalElementPtr?.dereference())
//        }
//    }
//
//    override fun computePresentation(): TargetPresentation {
//        return targetPresentation(element)
//    }
//
//    override fun computeDocumentationHint(): String? {
//        return computeLocalDocumentation(element, originalElement, true)
//    }
//
//    override val navigatable: Navigatable?
//        get() = element as? Navigatable
//
//    override fun computeDocumentation(): DocumentationResult? {
//        @Suppress("HardCodedStringLiteral") val html =
//            computeLocalDocumentation(element, originalElement, false) ?: return null
//        return DocumentationResult.documentation(html)
//    }
//
//    companion object {
//        internal val RENDERING_OPTIONS = CjDeclarationRendererForSource.WITH_SHORT_NAMES.with {
////            returnTypeFilter = CjCallableReturnTypeFilter.ALWAYS
////            propertyAccessorsRenderer = CjPropertyAccessorsRenderer.NONE
////            bodyMemberScopeProvider = CjRendererBodyMemberScopeProvider.NONE
////            singleTypeParameterRenderer = CjSingleTypeParameterSymbolRenderer.WITH_COMMA_SEPARATED_BOUNDS
////            parameterDefaultValueRenderer = CjParameterDefaultValueRenderer.THREE_DOTS
//        }
//    }
//}
//
//context(CangJieAnalysisSession)
//private fun renderFunctionTypeParameter(parameter: CjParameter): String =
//    with(CangJieDocumentationTarget.RENDERING_OPTIONS) {
//        prettyPrint {
//            parameter.nameAsName?.let { name ->
//                withSuffix(": ") {
//                    nameRenderer.renderName(
//                        name,
//                        symbol = null,
//                        printer = this
//                    )
//                }
//            }
//            parameter.typeReference?.getCjType()?.let { type -> typeRenderer.renderType(type, printer = this) }
//        }
//    }
//
//private fun String.escape(): String = HtmlEscapers.htmlEscaper().escape(this)
//
//private fun @receiver:Nls StringBuilder.renderCangJieDeclaration(
//    declaration: CjDeclaration,
//    onlyDefinition: Boolean,
//    symbolFinder: CangJieAnalysisSession.(CjSymbol) -> CjSymbol? = { it },
//    preBuild: CDocTemplate.() -> Unit = {}
//) {
//    analyze(declaration) {
//        // it's not possible to create symbol for function type parameter, so we need to process this case separately
//        // see KTIJ-22404 and KTIJ-25653
//        if (declaration is CjParameter && declaration.isFunctionTypeParameter()) {
//            val definition = renderFunctionTypeParameter(declaration)
//
//            insert(CDocTemplate()) {
//                definition {
//                    append(definition.escape())
//                }
//            }
//            return
//        }
//
//        val symbol = symbolFinder(declaration.getSymbol())
//        if (symbol !is CjDeclarationSymbol) return
//
//        insert(CDocTemplate()) {
////            definition {
////                append(symbol.render(CangJieDocumentationTarget.RENDERING_OPTIONS).escape())
////            }
////
////            if (!onlyDefinition) {
////                description {
////                    renderCDoc(symbol, this)
////                }
////            }
////            getContainerInfo(declaration).toString().takeIf { it.isNotBlank() }?.let { info ->
////                containerInfo {
////                    append(info)
////                }
////            }
//            preBuild()
//        }
//    }
//}
//
//
//private fun findElementWithText(element: PsiElement?, text: String): PsiElement? {
//    return when {
//        element == null -> null
//        element.text == text -> element
//        element.prevLeaf()?.text == text -> element.prevLeaf()
//        else -> null
//    }
//}
//
//private fun computeLocalDocumentation(
//    element: PsiElement,
//    originalElement: PsiElement?,
//    quickNavigation: Boolean
//): String? {
//    when {
//        element is PsiWhiteSpace -> {
////            val itElement =
////                findElementWithText(originalElement, StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME.identifier)
////            val itReference = itElement?.getParentOfType<CjNameReferenceExpression>(false)
////            if (itReference != null) {
////                return buildString {
////                    renderCangJieDeclaration(
////                        itReference.mainReference.resolve() as CjFunctionLiteral,
////                        quickNavigation,
////                        symbolFinder = { (it as? CjFunctionLikeSymbol)?.valueParameters?.firstOrNull() })
////                }
////            }
//        }
//
//        element is CjTypeReference -> {
//            val declaration = element.parent
//            if (declaration is CjCallableDeclaration &&
//                declaration.receiverTypeReference == element &&
//                findElementWithText(originalElement, "this") != null
//            ) {
//                return computeLocalDocumentation(declaration, originalElement, quickNavigation)
//            }
//        }
//
//        element is CjClass && element.isEnum() -> {
//            return buildString {
////                renderEnumSpecialFunction(originalElement, element, quickNavigation)
//            }
//        }
//
//        element is CjEnumEntry && !quickNavigation -> {
//            val ordinal =
//                element.containingTypeStatement?.body?.run { getChildrenOfType<CjEnumEntry>().indexOf(element) }
//
//            val project = element.project
//            return buildString {
//                renderCangJieDeclaration(element, false) {
//                    definition {
//                        it.inherit()
//                        ordinal?.let {
//                            append("<br>")
////                            appendHighlighted("// ", project) { asInfo }
////                            appendHighlighted(
////                                CangJieBundle.message("quick.doc.text.enum.ordinal", ordinal),
////                                project
////                            ) { asInfo }
//                        }
//                    }
//                }
//            }
//        }
//
//        element is CjDeclaration -> {
//            return buildString {
//                renderCangJieDeclaration(element, quickNavigation)
//            }
//        }
//
//        element is CjValueArgumentList -> {
//            val referenceExpression = element.prevSibling as? CjSimpleNameExpression ?: return null
//            val calledElement = referenceExpression.mainReference.resolve()
//            if (calledElement is CjNamedFunction || calledElement is CjConstructor<*>) {
//                // In case of CangJie function or constructor
//                return computeLocalDocumentation(calledElement as CjExpression, element, quickNavigation)
//            }
//
//        }
//
//        element is CjCallExpression -> {
//            val calledElement = element.referenceExpression()?.mainReference?.resolve() ?: return null
//            return computeLocalDocumentation(calledElement, originalElement, quickNavigation)
//        }
//
//        element.isModifier() -> {
//            when (element.text) {
////                CjTokens.LATEINIT_KEYWORD.value -> return CangJieBundle.message("quick.doc.text.lateinit")
////                CjTokens.TAILREC_KEYWORD.value -> return CangJieBundle.message("quick.doc.text.tailrec")
//            }
//        }
//    }
//    return null
//}
