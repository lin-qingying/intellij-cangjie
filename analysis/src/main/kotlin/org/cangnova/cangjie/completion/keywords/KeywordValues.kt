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

package org.cangnova.cangjie.completion.keywords

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.completion.smart.ExpectedInfoMatch
import org.cangnova.cangjie.completion.smart.SmartCompletionItemPriority
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.types.TypeSubstitutor
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.indices.IfConditionAdditionalData
import org.cangnova.cangjie.indices.fuzzyType
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.types.isBoolean


object KeywordValues {
    interface Consumer {
        /**
         * @param suitableOnPsiLevel together with [expectedInfoMatcher] the function is called to make a decision whether
         * [factory's][factory] output should be tagged with [priority]. Note that the function is used only as a fallback (in the case
         * where [expectedInfoMatcher] has no input data to process).
         *
         * Function receiver is a current position of the caret.
         */
        fun consume(
            lookupString: String,
            expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
            suitableOnPsiLevel: PsiElement.() -> Boolean = { false },
            priority: SmartCompletionItemPriority,
            factory: () -> LookupElement
        )
    }

    fun process(
        consumer: Consumer,
        position: PsiElement,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade,
        moduleDescriptor: ModuleDescriptor,

        ) {
        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val booleanInfoMatcher = matcher@{ info: ExpectedInfo ->
                // no sense in true or false as if-condition or when entry for when with no subject
                val skipTrueFalse = when (val additionalData = info.additionalData) {
                    is IfConditionAdditionalData -> true
//                    is MatchEntryAdditionalData -> !additionalData.matchWithSubject
                    else -> false
                }
                if (skipTrueFalse) {
                    return@matcher ExpectedInfoMatch.noMatch
                }

                if (info.fuzzyType?.type?.isBoolean == true)
                    ExpectedInfoMatch.match(TypeSubstitutor.EMPTY)
                else
                    ExpectedInfoMatch.noMatch
            }
            consumer.consume(CjTokens.TRUE_KEYWORD.value, booleanInfoMatcher, priority = SmartCompletionItemPriority.TRUE) {
                LookupElementBuilder.create(KeywordLookupObject(), CjTokens.TRUE_KEYWORD.value).bold()
            }
            consumer.consume(CjTokens.FALSE_KEYWORD.value, booleanInfoMatcher, priority = SmartCompletionItemPriority.FALSE) {
                LookupElementBuilder.create(KeywordLookupObject(), CjTokens.FALSE_KEYWORD.value).bold()
            }

//            val nullMatcher = { info: ExpectedInfo ->
//                when {
//                    (info.additionalData as? ComparisonOperandAdditionalData)?.suppressNullLiteral == true -> ExpectedInfoMatch.noMatch
//
//                    info.fuzzyType?.type?.isMarkedOption == true -> ExpectedInfoMatch.match(TypeSubstitutor.EMPTY)
//
//                    else -> ExpectedInfoMatch.noMatch
//                }
//            }

//            if (!position.isInsideAnnotationEntryArgumentList()) {
//                consumer.consume(
//                    CjTokens.NULL_KEYWORD.value,
//                    nullMatcher,
//                    { isPositionSuitableForNull(this) },
//                    SmartCompletionItemPriority.NULL
//                ) {
//                    LookupElementBuilder.create(KeywordLookupObject(), CjTokens.NULL_KEYWORD.value).bold()
//                }
//            }
        }


    }
}
