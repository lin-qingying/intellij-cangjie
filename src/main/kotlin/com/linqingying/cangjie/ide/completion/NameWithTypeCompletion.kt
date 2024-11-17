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

package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.ide.completion.handlers.isCharAt
import com.linqingying.cangjie.ide.completion.handlers.skipSpaces
import com.linqingying.cangjie.ide.formatter.cangjieCustomSettings
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.containingTypeStatement
import com.linqingying.cangjie.psi.psiUtil.moveCaret
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext

object NameWithTypeCompletion {
    fun shouldCompleteParameter(parameter: CjParameter): Boolean {
        val list = parameter.parent as? CjParameterList ?: return false
        return when (val owner = list.parent) {
            is CjCatchClause, is CjPropertyAccessor, is CjFunctionLiteral -> false
            is CjNamedFunction -> owner.nameIdentifier != null
            is CjPrimaryConstructor -> /*!owner.containingTypeStatement.isAnnotation()*/ true
            else -> true
        }
    }

    /**
     * This pattern is used to check if completion needs to be restarted (which is true when an upper case letter is typed
     * and new completion suggestions may appear)
     */
    val prefixEndsWithUppercaseLetterPattern =
        StandardPatterns.string().with(object : PatternCondition<String>("Prefix ends with uppercase letter") {
            override fun accepts(prefix: String, context: ProcessingContext?) = prefix.isNotEmpty() && prefix.last().isUpperCase()
        })
}

/**
 * @param typeIdString a string which contains a qualified type or a qualified class name, depending on how the element was obtained;
 * it is used to compare lookup elements.
 */
class NameWithTypeLookupElementDecorator(
    private val parameterName: String,
    private val typeIdString: String,
    typeLookupElement: LookupElement,
    private val shouldInsertType: Boolean
) : LookupElementDecorator<LookupElement>(typeLookupElement) {

    private val lookupString = parameterName + if (shouldInsertType) ": " + delegate.lookupString else ""

    override fun getLookupString() = lookupString
    override fun getAllLookupStrings() = setOf(lookupString)

    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)
        if (shouldInsertType) {
            presentation.itemText = parameterName + ": " + presentation.itemText
        } else {
            presentation.prependTailText(": " + presentation.itemText, true)
            presentation.itemText = parameterName
        }
    }

    override fun getDelegateInsertHandler(): InsertHandler<LookupElement> = InsertHandler { context, element ->
        if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) {
            val tailOffset = context.tailOffset

            val chars = context.document.charsSequence
            var offset = chars.skipSpaces(tailOffset)
            if (chars.isCharAt(offset, ',')) {
                offset++
                offset = chars.skipSpaces(offset)
                context.editor.moveCaret(offset)
            }
        }
        val settings = context.file.cangjieCustomSettings
        val spaceBefore = if (settings.SPACE_BEFORE_TYPE_COLON) " " else ""
        val spaceAfter = if (settings.SPACE_AFTER_TYPE_COLON) " " else ""

        val startOffset = context.startOffset
        if (shouldInsertType) {
            val text = "$parameterName$spaceBefore:$spaceAfter"
            context.document.insertString(startOffset, text)

            // update start offset so that it does not include the text we inserted
            context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, startOffset + text.length)

            element.handleInsert(context)
        } else {
            context.document.replaceString(startOffset, context.tailOffset, parameterName)

            context.commitDocument()
        }
    }

    override fun equals(other: Any?) = other is NameWithTypeLookupElementDecorator &&
            parameterName == other.parameterName &&
            typeIdString == other.typeIdString &&
            shouldInsertType == other.shouldInsertType

    override fun hashCode() = parameterName.hashCode()
}
