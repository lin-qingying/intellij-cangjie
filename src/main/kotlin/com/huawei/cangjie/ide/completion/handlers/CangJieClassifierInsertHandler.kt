package com.huawei.cangjie.ide.completion.handlers

import com.huawei.cangjie.analyzer.canAddRootPrefix
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.IdeDescriptorRenderers
import com.huawei.cangjie.ide.completion.DescriptorBasedDeclarationLookupObject
import com.huawei.cangjie.ide.completion.isAfterDot
import com.huawei.cangjie.ide.completion.isArtificialImportAliasedDescriptor
import com.huawei.cangjie.ide.completion.shortenReferences
import com.huawei.cangjie.ide.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import com.huawei.cangjie.ide.completion.smart.SmartCompletionItemPriority
import com.huawei.cangjie.ide.imports.ImportDescriptorResult
import com.huawei.cangjie.ide.imports.ImportInsertHelper
import com.huawei.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getParentOfType
import com.huawei.cangjie.psi.psiUtil.parentOfType
import com.huawei.cangjie.renderer.render
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.QualifiedExpressionResolver.Companion.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT
import com.huawei.cangjie.resolve.allowResolveInDispatchThread
import com.huawei.cangjie.resolve.caches.analyze
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.unwrapIfTypeAlias
import com.huawei.cangjie.utils.CallType
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.huawei.cangjie.utils.safeAs
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager


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
                    "$;val v:"  // if we have no reference in the current context we have a more complicated prefix to get one
                }
                val tempSuffix = ".xxx" // we add "xxx" after dot because of KT-9606
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

        if (isUnitTestMode() || Registry.`is`("cangjie.auto.completion.insert.constructor.parenthesis")) {
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
