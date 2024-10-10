package com.huawei.cangjie.ide.completion.handlers

import com.huawei.cangjie.ide.completion.LambdaSignatureTemplates
import com.huawei.cangjie.ide.formatter.cangjieCustomSettings
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjDotQualifiedExpression
import com.huawei.cangjie.psi.CjTypeArgumentList
import com.huawei.cangjie.psi.psiUtil.endOffset
import com.huawei.cangjie.psi.psiUtil.getLastParentOfTypeInRow
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.CallType
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionInitializationContext.START_OFFSET
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.OffsetKey
import com.intellij.codeInsight.completion.OffsetMap
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.createSmartPointer

class GenerateLambdaInfo(val lambdaType: CangJieType, val explicitParameters: Boolean)


sealed class CangJieFunctionInsertHandler(callType: CallType<*>) : CangJieCallableInsertHandler(callType) {

    class Normal(
        callType: CallType<*>,
        val inputTypeArguments: Boolean,
        val inputValueArguments: Boolean,
        val argumentText: String = "",
        val lambdaInfo: GenerateLambdaInfo? = null,
        val argumentsOnly: Boolean = false
    ) : CangJieFunctionInsertHandler(callType) {
        init {
            if (lambdaInfo != null) {
                assert(argumentText == "")
            }
        }

        //TODO: add 'data' or special annotation when supported
        fun copy(
            callType: CallType<*> = this.callType,
            inputTypeArguments: Boolean = this.inputTypeArguments,
            inputValueArguments: Boolean = this.inputValueArguments,
            argumentText: String = this.argumentText,
            lambdaInfo: GenerateLambdaInfo? = this.lambdaInfo,
            argumentsOnly: Boolean = this.argumentsOnly
        ) = Normal(callType, inputTypeArguments, inputValueArguments, argumentText, lambdaInfo, argumentsOnly)

        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
            val document = context.document

            if (!argumentsOnly) {
                surroundWithBracesIfInStringTemplate(context)

                super.handleInsert(context, item)
            }

            psiDocumentManager.commitDocument(document)
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

            // One of the offsets could become invalidated on the previous steps of insertion
            // We try to increase our chances to find a valid offset for the element
            val offsetMap = context.offsetMap
            val elementOffset = offsetMap[START_OFFSET]
                ?: offsetMap[InsertionContext.TAIL_OFFSET]?.let { it - 1 }
                ?: throw IllegalStateException("No valid offsets found in context: $offsetMap")

            val element = context.file.findElementAt(elementOffset) ?: return

            addArguments(context, element)

            // hack for KT-31902
            if (callType == CallType.DEFAULT) {
                context.file
                    .findElementAt(elementOffset)
                    ?.parent?.getLastParentOfTypeInRow<CjDotQualifiedExpression>()
                    ?.createSmartPointer()?.let {
                        psiDocumentManager.commitDocument(document)
                        val dotQualifiedExpression = it.element ?: return@let
                        SHORTEN_REFERENCES.process(dotQualifiedExpression)
                    }
            }
        }

        private fun addArguments(context: InsertionContext, offsetElement: PsiElement) {
            val completionChar = context.completionChar
            if (completionChar == '(') { //TODO: more correct behavior related to braces type
                context.setAddCompletionChar(false)
            }

            var offset = context.tailOffset
            val document = context.document
            val editor = context.editor
            val project = context.project
            var chars = document.charsSequence

            val isSmartEnterCompletion = completionChar == Lookup.COMPLETE_STATEMENT_SELECT_CHAR
            val isReplaceCompletion = completionChar == Lookup.REPLACE_SELECT_CHAR
            val isNormalCompletion = completionChar == Lookup.NORMAL_SELECT_CHAR

            val insertLambda = lambdaInfo != null && completionChar != '(' && !(isReplaceCompletion && chars.isCharAt(offset, '('))

            val openingBracket = if (insertLambda) '{' else '('
            val closingBracket = if (insertLambda) '}' else ')'

            var insertTypeArguments = inputTypeArguments && (isNormalCompletion || isReplaceCompletion || isSmartEnterCompletion)

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            if (isReplaceCompletion) {
                val offset1 = chars.skipSpaces(offset)
                if (offset1 < chars.length) {
                    if (chars[offset1] == '<') {
                        psiDocumentManager.commitDocument(document)
                        val token = context.file.findElementAt(offset1)!!
                        if (token.node.elementType == CjTokens.LT) {
                            val parent = token.parent
                            /* if type argument list is on multiple lines this is more likely wrong parsing*/
                            if (parent is CjTypeArgumentList && parent.getText().indexOf('\n') < 0) {
                                offset = parent.endOffset
                                insertTypeArguments = false
                            }
                        }
                    }
                }
            }

            if (insertLambda && lambdaInfo!!.explicitParameters) {
                insertTypeArguments = false
            }

            if (insertTypeArguments) {
                document.insertString(offset, "<>")
                chars = document.charsSequence
                editor.caretModel.moveToOffset(offset + 1)
                offset += 2
            }

            var openingBracketOffset = chars.indexOfSkippingSpace(openingBracket, offset)
            var closeBracketOffset = openingBracketOffset?.let { chars.indexOfSkippingSpace(closingBracket, it + 1) }
            var inBracketsShift = 0

            if (insertLambda && lambdaInfo!!.explicitParameters && closeBracketOffset == null) {
                openingBracketOffset = null
            }

            if (openingBracketOffset == null) {
                if (insertLambda) {
                    if (completionChar == ' ' || completionChar == '{') {
                        context.setAddCompletionChar(false)
                    }

                    if (isInsertSpacesInOneLineFunctionEnabled(context.file)) {
                        document.insertString(offset, " {  }")
                        inBracketsShift = 1
                    } else {
                        document.insertString(offset, " {}")
                    }
                } else {
                    if (isSmartEnterCompletion) {
                        document.insertString(offset, "(")
                    } else {
                        document.insertString(offset, "()")
                    }
                }
                psiDocumentManager.commitDocument(document)

                openingBracketOffset = document.charsSequence.indexOfSkippingSpace(openingBracket, offset)!!
                closeBracketOffset = document.charsSequence.indexOfSkippingSpace(closingBracket, openingBracketOffset + 1)
            }

            if (insertLambda && lambdaInfo!!.explicitParameters) {
                insertLambdaSignatureTemplate(openingBracketOffset, closeBracketOffset!!, lambdaInfo.lambdaType, context, context)
                return
            }

            document.insertString(openingBracketOffset + 1, argumentText)
            if (closeBracketOffset != null) {
                closeBracketOffset += argumentText.length
            }

            if (!insertTypeArguments) {
                if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == null) {
                    editor.caretModel.moveToOffset(openingBracketOffset + 1 + inBracketsShift)
                    AutoPopupController.getInstance(project)?.autoPopupParameterInfo(editor, offsetElement)
                } else {
                    editor.caretModel.moveToOffset(closeBracketOffset + 1)
                }
            }
        }

        private fun shouldPlaceCaretInBrackets(completionChar: Char): Boolean {
            if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
            if (completionChar == '(') return true
            return inputValueArguments || lambdaInfo != null
        }

        private fun isInsertSpacesInOneLineFunctionEnabled(file: PsiFile) =
            file.cangjieCustomSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD
    }



    class OnlyName(callType: CallType<*>) : CangJieFunctionInsertHandler(callType)

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitDocument(context.document)
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)
    }
}
private operator fun OffsetMap.get(key: OffsetKey): Int? {
    return if (containsOffset(key)) getOffset(key) else null
}

private fun insertLambdaSignatureTemplate(
    openBraceOffset: Int,
    closeBraceOffset: Int,
    lambdaType: CangJieType,
    currentContext: InsertionContext,
    originalContext: InsertionContext,
) {
    val placeholderRange = TextRange(openBraceOffset, closeBraceOffset + 1)
    val explicitParameterTypes = LambdaSignatureTemplates.explicitParameterTypesRequired(
        currentContext,
        placeholderRange,
        lambdaType,
    )

    LambdaSignatureTemplates.insertTemplate(
        originalContext,
        placeholderRange,
        lambdaType,
        explicitParameterTypes,
        signatureOnly = false,
    )
}
