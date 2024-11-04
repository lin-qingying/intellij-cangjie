package com.linqingying.cangjie.ide.completion.keywords

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.ide.*
import com.linqingying.cangjie.ide.completion.smart.ExpectedInfoMatch
import com.linqingying.cangjie.ide.completion.smart.SmartCompletionItemPriority
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.ResolutionFacade
import com.linqingying.cangjie.types.TypeSubstitutor
import com.linqingying.cangjie.types.util.isBooleanOrNullableBoolean
import com.linqingying.cangjie.utils.CallTypeAndReceiver
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement


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

                if (info.fuzzyType?.type?.isBooleanOrNullableBoolean() == true)
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
