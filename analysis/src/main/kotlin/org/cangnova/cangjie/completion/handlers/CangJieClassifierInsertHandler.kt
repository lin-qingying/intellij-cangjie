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

package org.cangnova.cangjie.completion.handlers

import org.cangnova.cangjie.utils.canAddRootPrefix
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.completion.DescriptorBasedDeclarationLookupObject
import org.cangnova.cangjie.completion.isAfterDot
import org.cangnova.cangjie.completion.isArtificialImportAliasedDescriptor
import org.cangnova.cangjie.completion.shortenReferences
import org.cangnova.cangjie.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import org.cangnova.cangjie.completion.smart.SmartCompletionItemPriority
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.parentOfType
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.allowResolveInDispatchThread
import org.cangnova.cangjie.resolve.caches.analyze
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.unwrapIfTypeAlias
import org.cangnova.cangjie.utils.safeAs
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.imports.ImportDescriptorResult
import org.cangnova.cangjie.imports.ImportInsertHelper
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolver.Companion.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT
import org.cangnova.cangjie.utils.isUnitTestMode


object CangJieClassifierInsertHandler : BaseDeclarationInsertHandler() {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        surroundWithBracesIfInStringTemplate(context)

        super.handleInsert(context, item)

        val file = context.file as? CjFile ?: return
        val lookupObject = item.`object` as DescriptorBasedDeclarationLookupObject
        // never need to insert import or use qualified name for import-aliased class
        val descriptor = lookupObject.descriptor
        if (descriptor?.isArtificialImportAliasedDescriptor == true) return

        var position: CjElement
        if (!context.isAfterDot()) {
            val project = context.project
            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.commitDocument(context.document)

            position = file.findElementAt(context.startOffset)?.getParentOfType<CjElement>(strict = false) ?: file

            val startOffset = context.startOffset
            val document = context.document

            val qualifiedName = qualifiedName(lookupObject)

            val importAction: (() -> ImportDescriptorResult?)? =
                descriptor?.takeIf { DescriptorUtils.isTopLevelDeclaration(it) }
                    ?.let {
                        fun(): ImportDescriptorResult = ImportInsertHelper.getInstance(project).importDescriptor(position, it)
                    } ?: lookupObject.safeAs<PsiClassLookupObject>()
                    ?.let {
                        fun(): ImportDescriptorResult = ImportInsertHelper.getInstance(project).importPsiClass(position, it.psiClass)
                    }

            val importDescriptorResult = importAction?.invoke()
            if (importDescriptorResult == null || importDescriptorResult == ImportDescriptorResult.FAIL) {
                // first try to resolve short name for faster handling
                val token = file.findElementAt(startOffset)!!
                val nameRef = token.parent as? CjNameReferenceExpression
                if (nameRef != null) {
                    val bindingContext = allowResolveInDispatchThread { nameRef.analyze(BodyResolveMode.PARTIAL) }
                    val target = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, nameRef]
                        ?: bindingContext[BindingContext.REFERENCE_TARGET, nameRef] as? ClassDescriptor
                    if (target != null && IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(target) == qualifiedName) return
                }

                val tempPrefix = if (nameRef != null) {
                    val isAnnotation = CallTypeAndReceiver.detect(nameRef) is CallTypeAndReceiver.ANNOTATION
                    // we insert space so that any preceding spaces inserted by formatter on reference shortening are deleted
                    // (but not for annotations where spaces are not allowed after @)
                    if (isAnnotation) "" else " "
                } else {
                    "$;let v:"  // if we have no reference in the current context we have a more complicated prefix to get one
                }
                val tempSuffix = ".xxx"
                val qualifierNameWithRootPrefix = qualifiedName.let {
                    if (FqName(it).canAddRootPrefix())
                        ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT + it
                    else
                        it
                }
                document.replaceString(startOffset, context.tailOffset, tempPrefix + qualifierNameWithRootPrefix + tempSuffix)

                psiDocumentManager.commitDocument(document)

                val classNameStart = startOffset + tempPrefix.length
                val classNameEnd = classNameStart + qualifierNameWithRootPrefix.length
                val rangeMarker = document.createRangeMarker(classNameStart, classNameEnd)
                val wholeRangeMarker = document.createRangeMarker(startOffset, classNameEnd + tempSuffix.length)

                shortenReferences(context, classNameStart, classNameEnd)
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

                if (rangeMarker.isValid && wholeRangeMarker.isValid) {
                    document.deleteString(wholeRangeMarker.startOffset, rangeMarker.startOffset)
                    document.deleteString(rangeMarker.endOffset, wholeRangeMarker.endOffset)
                }
                // position is invalid due to short refs and tempSuffix manipulation, need to be recalculated
                position = file.findElementAt(context.startOffset + tempPrefix.length)?.getParentOfType<CjElement>(strict = false) ?: file
            } else {
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
            }
        } else {
            position = file.findElementAt(context.startOffset)?.getParentOfType<CjElement>(strict = false) ?: file
        }

        if (isUnitTestMode  || Registry.`is`("cangjie.auto.completion.insert.constructor.parenthesis")) {
            val expression = position as? CjSimpleNameExpression
            if (expression != null) {
                (expression.parent as? CjExpression)?.let {
                    // avoid incomplete expressions like `super<Type` or `this<Type`
                    if (it is CjBinaryExpression && (it.left is CjSuperExpression || it.left is CjThisExpression)) return
                }
                insertParentheses(item, expression, context)
            }
        }
    }

    private fun insertParentheses(
        item: LookupElement,
        expression: CjSimpleNameExpression,
        context: InsertionContext
    ) {
        val editor = context.editor
        if (!editor.settings.isInsertParenthesesAutomatically) return
        if (context.completionChar != '\n') return
        // do not insert any parenthesis when smart completion strategy is used
        if (context.elements.any {
                val smartCompletionItemPriority = it.getUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY)
                smartCompletionItemPriority != null && smartCompletionItemPriority != SmartCompletionItemPriority.DEFAULT
            }) return

        val callTypeAndReceiver = CallTypeAndReceiver.detect(expression)
        val callType = callTypeAndReceiver.callType
        if (callType != CallType.DEFAULT && callType != CallType.DOT &&
            // `class F: Foo<caret>` has callType == TYPE
            (expression.parentOfType<CjSuperTypeEntry>() == null ||
                    // do not add parenthesis when caret within generic like `class F: Foo<Str<caret>`
                    expression.parentOfType<CjTypeArgumentList>() != null)
        ) return

        val lookupObject = item.`object` as DescriptorBasedDeclarationLookupObject

        val parenthesisConfig: ParenthesisConfig =
            cangjieParenthesis(lookupObject) ?: return

        var parenthesesOffset: Int
        val parenthesesText = buildString {
            // add `<>` and put caret within then if ctor with some generics
            if (parenthesisConfig.needInsertDiamonds) {
                append("<>")
                parenthesesOffset = 1
            } else {
                // otherwise put a caret within `()` if there is at least one non-empty ctor
                // or, put a caret right after `()`
                parenthesesOffset =
                    if (parenthesisConfig.emptyParametersConstructor) {
                        2
                    } else {
                        1
                    }
            }
            append("()")
        }
        val offset = editor.caretModel.offset
        context.document.insertString(offset, parenthesesText)
        editor.caretModel.moveToOffset(offset + parenthesesOffset)
        PsiDocumentManager.getInstance(context.project).commitDocument(context.document)
    }

    private data class ParenthesisConfig(
        val needInsertDiamonds: Boolean,
        val emptyParametersConstructor: Boolean
    )

    private fun cangjieParenthesis(lookupObject: DescriptorBasedDeclarationLookupObject): ParenthesisConfig? {
        val descriptor: DeclarationDescriptor? = lookupObject.descriptor
        val classDescriptor = (descriptor?.unwrapIfTypeAlias() as? ClassDescriptor)
            ?.takeIf { it.kind == ClassKind.CLASS && it.modality != Modality.ABSTRACT } ?: return null
        val constructors = classDescriptor.constructors
        val publicVisibleConstructors = constructors.filter { it.visibility == DescriptorVisibilities.PUBLIC }
        if (publicVisibleConstructors.isEmpty() && constructors.isNotEmpty()) return null

        val constructorsWithMinValueParams =
            constructors.minByOrNull { it.valueParameters.size }

        var needInsertDiamonds = false
        var emptyParametersConstructor = true
        if (constructorsWithMinValueParams != null) {
            if (constructorsWithMinValueParams.typeParameters.isNotEmpty()) {
                needInsertDiamonds = true
            } else if (constructorsWithMinValueParams.valueParameters.isNotEmpty() ||
                constructors.any { it.valueParameters.isNotEmpty() }
            ) {
                emptyParametersConstructor = false
            }
        }

        return ParenthesisConfig(needInsertDiamonds, emptyParametersConstructor)
    }



    private fun qualifiedName(lookupObject: DescriptorBasedDeclarationLookupObject): String {
        return if (lookupObject.descriptor != null) {
            IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(lookupObject.descriptor as ClassifierDescriptor)
        } else {
            val qualifiedName = (lookupObject.psiElement as CjTypeStatement).fqName
            return qualifiedName?.render() ?: ""

        }
    }
}
