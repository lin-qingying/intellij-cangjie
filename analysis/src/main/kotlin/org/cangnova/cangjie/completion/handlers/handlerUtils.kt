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

import org.cangnova.cangjie.completion.LambdaSignatureTemplates
import org.cangnova.cangjie.completion.keywords.KeywordLookupObject
import org.cangnova.cangjie.formatter.adjustLineIndent
import org.cangnova.cangjie.formatter.cangjieCustomSettings
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBlockStringTemplateEntry
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.psiUtil.*
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.types.CangJieType
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionInitializationContext.IDENTIFIER_END_OFFSET
import com.intellij.codeInsight.completion.CompletionInitializationContext.START_OFFSET
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.createSmartPointer
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments

fun removeRedundantBracesInStringTemplate(context: InsertionContext) {
    val document = context.document
    val tailOffset = context.tailOffset
    if (document.charsSequence[tailOffset] == '}') {
        PsiDocumentManager.getInstance(context.project).commitDocument(document)

        val token = context.file.findElementAt(tailOffset)
        if (token != null && token.node.elementType == CjTokens.LONG_TEMPLATE_ENTRY_END) {
            val entry = token.parent as CjBlockStringTemplateEntry
            val nameExpression = entry.expression as? CjNameReferenceExpression ?: return
            if (canPlaceAfterSimpleNameEntry(entry.nextSibling)) {
                context.tailOffset++ // place after '}' otherwise it gets invalidated
                val name = nameExpression.referencedName
                val newEntry = CjPsiFactory(context.project).createSimpleNameStringTemplateEntry(name)
                entry.replace(newEntry)
            }
        }
    }
}
fun Document.isTextAt(offset: Int, text: String) =
    offset + text.length <= textLength && getText(TextRange(offset, offset + text.length)) == text
fun createNormalFunctionInsertHandler(
    editor: Editor,
    callType: CallType<*>,
    functionName: Name,
    inputTypeArguments: Boolean,
    inputValueArguments: Boolean,
    argumentText: String = "",
    lambdaInfo: GenerateLambdaInfo? = null,
    argumentsOnly: Boolean = false
): InsertHandler<LookupElement> {
    if (lambdaInfo != null) {
        assert(argumentText == "")
    }

    val lazyHandlers = mutableMapOf<String, Lazy<DeclarativeInsertHandler>>()

    // \n - NormalCompletion
    lazyHandlers[Lookup.NORMAL_SELECT_CHAR.toString()] = DeclarativeInsertHandler.LazyBuilder(holdReadLock = true) { builder ->
        val argumentsStringToInsert = StringBuilder()

        val chars = editor.document.charsSequence
        val offset = editor.caretModel.offset
        val insertLambda = lambdaInfo != null
        val openingBracket = if (insertLambda) '{' else '('
        val closingBracket = if (insertLambda) '}' else ')'

        val insertTypeArguments = inputTypeArguments && !(insertLambda && lambdaInfo!!.explicitParameters)
        if (insertTypeArguments) {
            argumentsStringToInsert.append("<>")
            builder.offsetToPutCaret += 1
        }

        var absoluteOpeningBracketOffset = chars.indexOfSkippingSpace(openingBracket, offset)
        var absoluteCloseBracketOffset = absoluteOpeningBracketOffset?.let { chars.indexOfSkippingSpace(closingBracket, it + 1) }
        if (insertLambda && lambdaInfo!!.explicitParameters && absoluteCloseBracketOffset == null) {
            absoluteOpeningBracketOffset = null
        }

        if (absoluteOpeningBracketOffset == null) {
            var lambdaCaseInsideBracketOffset = 0
            var noLambdaCaseInsideBracketOffset = 0
            if (insertLambda) {
                val file = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)!!
                if (file.cangjieCustomSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD) {
                    argumentsStringToInsert.append(" {  }")
                    lambdaCaseInsideBracketOffset = 3
                } else {
                    argumentsStringToInsert.append(" {}")
                    lambdaCaseInsideBracketOffset = 2
                }
            } else {
                argumentsStringToInsert.append("($argumentText)")
                noLambdaCaseInsideBracketOffset = 1
            }
            val shouldPlaceCaretInBrackets = inputValueArguments || lambdaInfo != null
            if (!inputTypeArguments) {
                // no need to insert typeParams, may move cursor around valueParams
                if (shouldPlaceCaretInBrackets) {
                    builder.offsetToPutCaret += noLambdaCaseInsideBracketOffset + lambdaCaseInsideBracketOffset
                    builder.withPopupOptions(DeclarativeInsertHandler.PopupOptions.ParameterInfo)
                } else {
                    builder.offsetToPutCaret += argumentsStringToInsert.toString().length
                }
            } else {
                // we would love to put caret inside value params, but we can't, cause we have to stay on typeParams first
                // so do nothing here.
            }
        } else if (!(insertLambda && lambdaInfo!!.explicitParameters)) {
            builder.addOperation(absoluteOpeningBracketOffset + 1 - offset, argumentText)
            if (absoluteCloseBracketOffset != null) {
                absoluteCloseBracketOffset += argumentText.length
            }

            if (!insertTypeArguments) {
                builder.offsetToPutCaret = absoluteOpeningBracketOffset + 1 - offset
                val shouldPlaceCaretInBrackets = inputValueArguments || lambdaInfo != null
                if (shouldPlaceCaretInBrackets) {
                    builder.withPopupOptions(DeclarativeInsertHandler.PopupOptions.ParameterInfo)
                }
            }
        }

        var prefixModificationOperation: DeclarativeInsertHandler.RelativeTextEdit? = null
        var alreadyHasBacCjickInTheEnd = false
        if (!argumentsOnly) {
            val specialSymbols = charArrayOf('_', '`', '~')
            val typedFuzzyName = editor.document.text.subSequence(0, offset)
                .reversed()
                .takeWhile { it.isLetterOrDigit() || specialSymbols.contains(it) }
                .toString()
            val functionStartOffset = offset - typedFuzzyName.length

            /*
                Essentially `normalizedBeforeFunctionOffset' can be reduced to just
                val normalizedBeforeFunctionOffset = 0 - functionName.asString().length

                Though it is not obvious why. Operation offsets are relative to cursor position before insertion. In this
                case it will be offset of lookup element text end - which is functionStartOffset + `functionName.asString().length`.
                NB! asString() and not render(), we rely on knowledge that bacCjicks are not elevated into LookupElement.

                Example:
                we have a function "fun fooBar(i: Int) {}" which we want to call from a string template.
                In editor we have: "$fb<caret>"
                                    |  \
                                    \   \-offset
                                    \- functionStartOffset


                Insert handler will be applied at stage: "$fooBar<caret>", (result of handler application should be "${fooBar(<caret>)}")
                And this new caret position is calculated here as `lookupElementEndPosition`
                So all the offsets should be relative to the new caret position.

                "$fooBar<caret>"
                  \- normalizedBeforeFunctionOffset
             */
            val lookupElementEndPosition =
                functionStartOffset + functionName.asString().length // NB! It's `asString()` on purpose, do not change to `render()`

            val normalizedBeforeFunctionOffset = functionStartOffset - lookupElementEndPosition

            // surroundWithBracesIfInStringTemplate
            run {
                if (functionStartOffset > 0) {
                    if (chars[functionStartOffset - 1] == '$') {
                        // add paranoia check
                        val dollarIsEscaped = (functionStartOffset - 2).let { predollarOffset ->
                            if (predollarOffset >= 0) chars[predollarOffset] == '\\'
                            else false
                        }

                        if (!dollarIsEscaped) {
                            argumentsStringToInsert.append('}')

                            prefixModificationOperation = DeclarativeInsertHandler.RelativeTextEdit(
                                normalizedBeforeFunctionOffset,
                                normalizedBeforeFunctionOffset,
                                "{"
                            )
                        }
                    }
                }
            }

            // enclosing with bacCjicks
            run {
                if (!functionName.isSpecial) {
                    val renderedName = functionName.render()
                    // it's possible, that nothing typedFuzzyName is empty, and cursor is located after the last symbol in the document,
                    // which means: `functionStartOffset` and `offset` are outside `chars` bounds.
                    val alreadyHasTickAtFront = chars.getOrNull(functionStartOffset) == '`'

                    if (renderedName.firstOrNull() == '`') {
                        alreadyHasBacCjickInTheEnd = chars.getOrNull(offset) == '`'

                        // requires bacCjicks
                        if (!alreadyHasTickAtFront) {
                            // bacCjick is not present already, so need to add it manually
                            prefixModificationOperation = when (val operation = prefixModificationOperation) {
                                null -> DeclarativeInsertHandler.RelativeTextEdit(
                                    normalizedBeforeFunctionOffset,
                                    normalizedBeforeFunctionOffset,
                                    "`"
                                )

                                else -> operation.copy(newText = operation.newText + '`')
                            }
                        }

                        if (!alreadyHasBacCjickInTheEnd) {
                            argumentsStringToInsert.insert(0, "`")
                            builder.offsetToPutCaret += 1
                        }
                    } else {
                        // no bacCjicks required
                        if (alreadyHasTickAtFront) {
                            prefixModificationOperation = when (val operation = prefixModificationOperation) {
                                null -> DeclarativeInsertHandler.RelativeTextEdit(
                                    normalizedBeforeFunctionOffset,
                                    normalizedBeforeFunctionOffset + 1,
                                    ""
                                )

                                else -> operation.copy(rangeTo = operation.rangeTo + 1) // already insert brace in front, now need to turn insertion into replacement
                            }
                        }
                    }
                }
            }
        }

        prefixModificationOperation?.also { builder.addOperation(it) }
        if (alreadyHasBacCjickInTheEnd) {
            builder.addOperation(1, argumentsStringToInsert.toString())
            builder.offsetToPutCaret += 1
        } else {
            builder.addOperation(0, argumentsStringToInsert.toString())
        }

        builder.withPostInsertHandler(InsertHandler<LookupElement> { context, item ->
            // The following code looks hacky:
            // brackets with arguments are already present, so are braces and bacCjicks, and they should be kept.
            // that's why we provide fake context which is adjusted
            // NB: it is important to fork context here and keep the original one intact
            context.forkByOffsetMap().also { forkedContext ->
                val forkedDocument = forkedContext.document
                val newStartOffset = when {
                    forkedDocument.isTextAt(forkedContext.startOffset, "{") -> forkedContext.startOffset + 1
                    forkedContext.startOffset > 0 && forkedDocument.isTextAt(forkedContext.startOffset - 1, "`") ->
                        forkedContext.startOffset - 1

                    else -> forkedContext.startOffset
                }

                val newTailOffset = newStartOffset + functionName.render().length

                forkedContext.offsetMap.addOffset(START_OFFSET, newStartOffset)
                forkedContext.offsetMap.addOffset(IDENTIFIER_END_OFFSET, newTailOffset)
                forkedContext.tailOffset = newTailOffset

                CangJieCallableInsertHandler.addImport(forkedContext, item, callType)

                if (insertLambda && lambdaInfo!!.explicitParameters) {
                    val charsSequence = forkedDocument.charsSequence
                    val tailOffset = forkedContext.tailOffset
                    val startIndex = forkedContext.startOffset

                    val startOffset = charsSequence.indexOfSkippingSpace('{', tailOffset)
                        ?: throwElementIsNotFound(charsSequence, startIndex, tailOffset)

                    val endOffset = charsSequence.indexOfSkippingSpace('}', startOffset + 1)
                        ?: throwElementIsNotFound(charsSequence, startIndex, tailOffset)

                    insertLambdaSignatureTemplate(startOffset, endOffset, lambdaInfo.lambdaType, forkedContext, context)
                }


                if (callType == CallType.DEFAULT) {
                    val psiDocumentManager = PsiDocumentManager.getInstance(forkedContext.project)

                    forkedContext.file
                        .findElementAt(forkedContext.startOffset)
                        ?.parent?.getLastParentOfTypeInRow<CjDotQualifiedExpression>()
                        ?.createSmartPointer<CjDotQualifiedExpression>()?.let {
                            psiDocumentManager.commitDocument(forkedDocument)
                            val dotQualifiedExpression = it.element ?: return@let
                            CangJieCallableInsertHandler.SHORTEN_REFERENCES.process(dotQualifiedExpression)
                        }
                }
            }
        })
    }

    val fallbackHandler =
        CangJieFunctionInsertHandler.Normal(callType, inputTypeArguments, inputValueArguments, argumentText, lambdaInfo, argumentsOnly)

    return CangJieFunctionCompositeDeclarativeInsertHandler(
        handlers = lazyHandlers, fallbackInsertHandler = fallbackHandler,
        isLambda = lambdaInfo != null, inputValueArguments = inputValueArguments,
        inputTypeArguments = inputTypeArguments
    )
}
class CangJieFunctionCompositeDeclarativeInsertHandler(
    handlers: Map<String, Lazy<DeclarativeInsertHandler>>,
    fallbackInsertHandler: InsertHandler<LookupElement>?,
    val isLambda: Boolean,
    val inputValueArguments: Boolean,
    val inputTypeArguments: Boolean
) : CompositeDeclarativeInsertHandler(handlers, fallbackInsertHandler) {

    companion object {
        fun withUniversalHandler(
            completionChars: String,
            handler: DeclarativeInsertHandler.LazyBuilder
        ): CompositeDeclarativeInsertHandler {
            val handlersMap = mapOf(completionChars to handler)
            // it's important not to provide a fallbackInsertHandler here
            return CangJieFunctionCompositeDeclarativeInsertHandler(handlersMap, null, false, false, false)
        }
    }
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
fun surroundWithBracesIfInStringTemplate(context: InsertionContext): Boolean {
    val startOffset = context.startOffset
    val document = context.document
    if (startOffset > 0 && document.charsSequence[startOffset - 1] == '$') {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitDocument(document)

        if (context.file.findElementAt(startOffset - 1)?.node?.elementType == CjTokens.SHORT_TEMPLATE_ENTRY_START) {
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

            document.insertString(startOffset, "{")
            context.offsetMap.addOffset(START_OFFSET, startOffset + 1)

            val tailOffset = context.tailOffset
            document.insertString(tailOffset, "}")
            context.tailOffset = tailOffset
            return true
        }
    }

    return false
}

fun CharSequence.skipSpaces(index: Int): Int = index.until(length).firstOrNull {
    val c = this[it]
    c != ' ' && c != '\t'
} ?: this.length

fun LookupElement.withLineIndentAdjuster(): LookupElement = LookupElementDecorator.withDelegateInsertHandler(
    this,
) { context, item ->
    item.handleInsert(context)
    context.document.adjustLineIndent(context.project, context.startOffset)
}

private fun String.unindent(indent: String): String {
    val text = this
    return buildString {
        val lines = text.lines()
        for ((index, line) in lines.withIndex()) {
            append(line.removePrefix(indent))
            if (index != lines.lastIndex) append('\n')
        }
    }
}

fun CharSequence.skipSpacesAndLineBreaks(index: Int): Int = index.until(length).firstOrNull {
    val c = this[it]
    c != ' ' && c != '\t' && c != '\n' && c != '\r'
} ?: this.length

fun CharSequence.isCharAt(offset: Int, c: Char) = offset < length && this[offset] == c
fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }

    return null
}

fun createKeywordConstructLookupElement(
    project: Project,
    keyword: String,
    fileTextToReformat: String,
    trimSpacesAroundCaret: Boolean = false,
    adjustLineIndent: Boolean = false,
): LookupElement {
    val file = CjPsiFactory(project).createFile(fileTextToReformat)
    CodeStyleManager.getInstance(project).reformat(file)
    val newFileText = file.text

    val keywordOffset = newFileText.indexOf(keyword)
    assert(keywordOffset >= 0)
    val keywordEndOffset = keywordOffset + keyword.length

    val caretPlaceHolder = "caret"

    val caretOffset = newFileText.indexOf(caretPlaceHolder)
    assert(caretOffset >= 0)
    assert(caretOffset >= keywordEndOffset)

    var tailBeforeCaret = newFileText.substring(keywordEndOffset, caretOffset)
    var tailAfterCaret = newFileText.substring(caretOffset + caretPlaceHolder.length)

    if (trimSpacesAroundCaret) {
        tailBeforeCaret = tailBeforeCaret.trimEnd()
        tailAfterCaret = tailAfterCaret.trimStart()
    }

    val indent = detectIndent(newFileText, keywordOffset)
    tailBeforeCaret = tailBeforeCaret.unindent(indent)
    tailAfterCaret = tailAfterCaret.unindent(indent)

    val tailText =
        (if (tailBeforeCaret.contains('\n')) tailBeforeCaret.replace("\n", "").trimEnd() else tailBeforeCaret) +
                "..." +
                (if (tailAfterCaret.contains('\n')) tailAfterCaret.replace("\n", "").trimStart() else tailAfterCaret)

    val lookupElement = KeywordConstructLookupObject(keyword, fileTextToReformat)
    return LookupElementBuilder.create(lookupElement, keyword)
        .bold()
        .withTailText(tailText)
        .withInsertHandler { insertionContext, _ ->
            if (insertionContext.completionChar == Lookup.NORMAL_SELECT_CHAR ||
                insertionContext.completionChar == Lookup.REPLACE_SELECT_CHAR ||
                insertionContext.completionChar == Lookup.AUTO_INSERT_SELECT_CHAR
            ) {
                val keywordStartOffset = if (!adjustLineIndent) {
                    insertionContext.tailOffset - keyword.length
                } else {
                    val offset = insertionContext.tailOffset - keyword.length
                    insertionContext.document.adjustLineIndent(insertionContext.project, offset)
                    insertionContext.tailOffset - keyword.length
                }

                val offset = keywordStartOffset + keyword.length
                val newIndent = detectIndent(insertionContext.document.charsSequence, keywordStartOffset)
                val beforeCaret = tailBeforeCaret.indentLinesAfterFirst(newIndent)
                val afterCaret = tailAfterCaret.indentLinesAfterFirst(newIndent)

                val element = insertionContext.file.findElementAt(offset)

                val sibling = when {
                    element !is PsiWhiteSpace -> element
                    element.textContains('\n') -> null
                    else -> element.getNextSiblingIgnoringWhitespace(true)
                }

                if (sibling != null &&
                    beforeCaret.trimStart()
                        .startsWith(insertionContext.document.getText(TextRange.from(sibling.startOffset, 1)))
                ) {
                    insertionContext.editor.moveCaret(sibling.startOffset + 1)
                } else {
                    insertionContext.document.insertString(offset, beforeCaret + afterCaret)
                    insertionContext.editor.moveCaret(offset + beforeCaret.length)
                }
            }
        }
}

private fun detectIndent(text: CharSequence, offset: Int): String {
    return text.substring(0, offset)
        .substringAfterLast('\n')
        .takeWhile(Char::isWhitespace)
}

private fun String.indentLinesAfterFirst(indent: String): String {
    val text = this
    return buildString {
        val lines = text.lines()
        for ((index, line) in lines.withIndex()) {
            if (index > 0) append(indent)
            append(line)
            if (index != lines.lastIndex) append('\n')
        }
    }
}

private data class KeywordConstructLookupObject(
    private val keyword: String,
    private val constructToInsert: String
) : KeywordLookupObject()
private fun throwElementIsNotFound(charsSequence: CharSequence, startOffset: Int, tailOffset: Int): Nothing {
    throw CangJieExceptionWithAttachments("brace is not found").apply {
        withAttachment("element.cj", charsSequence.subSequence(startOffset, tailOffset))
        withAttachment(
            "tail.cj",
            charsSequence.subSequence(tailOffset, minOf(tailOffset + 10, charsSequence.length - 1)),
        )
    }
}
