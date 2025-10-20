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

package org.cangnova.cangjie.ide.refactoring

import org.cangnova.cangjie.ide.refactoring.introduce.CangJieIntroduceVariableService
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.getParentOfTypeAndBranch
import org.cangnova.cangjie.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.cangnova.cangjie.utils.isUnitTestMode
import com.intellij.codeInsight.navigation.PsiTargetNavigator
import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable

/**
 * @param failOnEmptySuggestion if true, then callback is invoked in the same thread, otherwise in pooled thread
 */
fun selectElement(
    editor: Editor,
    file: CjFile,
    failOnEmptySuggestion: Boolean,
    elementKinds: Collection<ElementKind>,
    callback: (PsiElement?) -> Unit
) {
    if (editor.selectionModel.hasSelection()) {
        val selectionStart = editor.selectionModel.selectionStart
        val selectionEnd = editor.selectionModel.selectionEnd
        val task = Callable {
            findElementAtRange(file, selectionStart, selectionEnd, elementKinds, failOnEmptySuggestion)
        }
        if (failOnEmptySuggestion) {
            callback(task.call())
        } else {
            ReadAction.nonBlocking(task)
                .expireWhen { editor.isDisposed }
                .finishOnUiThread(ModalityState.nonModal()) { element ->
                    callback(element)
                }.submit(AppExecutorUtil.getAppExecutorService())
        }
    } else {
        val offset = editor.caretModel.offset
        smartSelectElement(editor, file, offset, failOnEmptySuggestion, elementKinds, callback)
    }
}

class IntroduceRefactoringException(message: String) : RuntimeException(message)

fun getSmartSelectSuggestions(
    file: PsiFile,
    offset: Int,
    elementKind: ElementKind,
    isOriginalOffset: Boolean = true,
): List<CjElement> {
    if (offset < 0) return emptyList()

    var element: PsiElement? = file.findElementAt(offset) ?: return emptyList()

    if (element is PsiWhiteSpace
        || isOriginalOffset && element is LeafPsiElement && element.elementType == CjTokens.RPAR
        || element is PsiComment
        || element is LeafPsiElement && (element.elementType == CjTokens.DOT || element.elementType == CjTokens.COMMA)
        || element?.getParentOfType<CDoc>(strict = true, CjDeclaration::class.java) != null
    ) return getSmartSelectSuggestions(file, offset - 1, elementKind, isOriginalOffset = false)

    val elements = ArrayList<CjElement>()
    while (element != null && !(element is CjBlockExpression && element.parent !is CjFunctionLiteral) &&
        element !is CjNamedFunction
        && element !is CjClassBody
    ) {
        var addElement = false
        var keepPrevious = true

        if (element is CjTypeElement) {
            addElement =
                elementKind == ElementKind.TYPE_ELEMENT
                        && element.getParentOfTypeAndBranch<CjUserType>(true) { qualifier } == null
            if (!addElement) {
                keepPrevious = false
            }
        } else if (element is CjExpression && element !is CjStatementExpression) {
            addElement = elementKind == ElementKind.EXPRESSION

            if (addElement) {
                if (element is CjParenthesizedExpression) {
                    addElement = false
                } else if (element.parent is CjQualifiedExpression) {
                    val qualifiedExpression = element.parent as CjQualifiedExpression
                    if (qualifiedExpression.receiverExpression !== element) {
                        addElement = false
                    }
                } else if (element.parent is CjCallElement
                    || element.parent is CjThisExpression
                    || PsiTreeUtil.getParentOfType(element, CjSuperExpression::class.java) != null
                ) {
                    addElement = false
                } else if (element.parent is CjOperationExpression) {
                    val operationExpression = element.parent as CjOperationExpression
                    if (operationExpression.operationReference === element) {
                        addElement = false
                    }
                }
                if (addElement) {
                    val service = element.project.service<CangJieIntroduceVariableService>()
                    if (service.hasUnitType(element)) {
                        addElement = false
                    }
                }
            }
        }

        if (addElement) {
            elements.add(element as CjElement)
        }

        if (!keepPrevious) {
            elements.clear()
        }

        element = element.parent
    }
    return elements
}

private fun smartSelectElement(
    editor: Editor,
    file: PsiFile,
    offset: Int,
    failOnEmptySuggestion: Boolean,
    elementKinds: Collection<ElementKind>,
    callback: (PsiElement?) -> Unit
) {
    val elements = elementKinds.flatMap { getSmartSelectSuggestions(file, offset, it) }
    if (elements.isEmpty()) {
        if (failOnEmptySuggestion) throw IntroduceRefactoringException(
            CangJieBundle.message("cannot.refactor.not.expression")
        )
        callback(null)
        return
    }

    if (elements.size == 1 || isUnitTestMode) {
        callback(elements.first())
        return
    }

    val highlighter = ScopeHighlighter(editor)
    val title = if (elementKinds.size == 1) {
        when (elementKinds.iterator().next()) {
            ElementKind.EXPRESSION -> CangJieBundle.message("popup.title.expressions")
            ElementKind.TYPE_ELEMENT, ElementKind.TYPE_CONSTRUCTOR -> CangJieBundle.message("popup.title.types")
        }
    } else {
        CangJieBundle.message("popup.title.elements")
    }

    PsiTargetNavigator(elements)
        .presentationProvider { element -> TargetPresentation.builder(getExpressionShortText(element)).presentation() }
        .builderConsumer { builder ->
            builder
                .setMovable(false)
                .setResizable(false)
                .setRequestFocus(true)
                .setItemChosenCallback { presentation -> callback((presentation.item as SmartPsiElementPointer<*>).element) }
                .setItemSelectedCallback { presentation ->
                    highlighter.dropHighlight()
                    val psiElement =
                        (presentation?.item as? SmartPsiElementPointer<*>)?.element ?: return@setItemSelectedCallback
                    highlighter.highlight(psiElement, listOf(psiElement))
                }
                .addListener(object : JBPopupListener {
                    override fun onClosed(event: LightweightWindowEvent) {
                        highlighter.dropHighlight()
                    }
                })
        }
        .createPopup(file.project, title)
        .showInBestPositionFor(editor)
}

fun findElementAtRange(
    file: CjFile,
    selectionStart: Int,
    selectionEnd: Int,
    elementKinds: Collection<ElementKind>,
    failOnEmptySuggestion: Boolean
): PsiElement? {
    var adjustedStart = selectionStart
    var adjustedEnd = selectionEnd
    var firstElement: PsiElement = file.findElementAt(adjustedStart)!!
    var lastElement: PsiElement = file.findElementAt(adjustedEnd - 1)!!

    if (PsiTreeUtil.getParentOfType(
            firstElement,
            CjLiteralStringTemplateEntry::class.java,
            CjEscapeStringTemplateEntry::class.java
        ) == null
        && PsiTreeUtil.getParentOfType(
            lastElement,
            CjLiteralStringTemplateEntry::class.java,
            CjEscapeStringTemplateEntry::class.java
        ) == null
    ) {
        firstElement = firstElement.getNextSiblingIgnoringWhitespaceAndComments(true) ?: firstElement
        lastElement = lastElement.getPrevSiblingIgnoringWhitespaceAndComments(true) ?: lastElement
        adjustedStart = firstElement.textRange.startOffset
        adjustedEnd = lastElement.textRange.endOffset
    }

    val service = file.project.service<CangJieIntroduceVariableService>()

    return elementKinds.asSequence()
        .mapNotNull { service.findElement(file, adjustedStart, adjustedEnd, failOnEmptySuggestion, it) }
        .firstOrNull()
}