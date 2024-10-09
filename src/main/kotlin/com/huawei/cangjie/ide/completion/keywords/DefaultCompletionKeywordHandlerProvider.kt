package com.huawei.cangjie.ide.completion.keywords

import com.huawei.cangjie.ide.completion.handlers.createKeywordConstructLookupElement
import com.huawei.cangjie.ide.completion.handlers.withLineIndentAdjuster
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.prevLeaf
import com.intellij.codeInsight.completion.CompletionParameters

class CompletionKeywordHandlers<CONTEXT>(vararg handlers: CompletionKeywordHandler<CONTEXT>) {
    private val handlerByKeyword = handlers.associateBy { it.keyword.value }

    internal fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler<CONTEXT>? =
        handlerByKeyword[keyword]
}
abstract class CompletionKeywordHandlerProvider<CONTEXT> {
    protected abstract val handlers: CompletionKeywordHandlers<CONTEXT>

    fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler<CONTEXT>? =
        handlers.getHandlerForKeyword(keyword)
}

object DefaultCompletionKeywordHandlerProvider : CompletionKeywordHandlerProvider<CompletionKeywordHandler.NO_CONTEXT>() {
//    private val CONTRACT_HANDLER = completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(CjTokens.CONTRACT_KEYWORD) { _, _, _, _ ->
//        emptyList()
//    }

    private val GETTER_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(CjTokens.GET_KEYWORD) { parameters, _, lookupElement, project ->
            buildList {
                add(lookupElement.withLineIndentAdjuster())
                if (!parameters.isUseSiteAnnotationTarget) {
                    add(
                        createKeywordConstructLookupElement(
                            project,
                            CjTokens.GET_KEYWORD.value,
                            "prop v:Int{ get()=caret}",
                            adjustLineIndent = true,
                        )
                    )
                    add(
                        createKeywordConstructLookupElement(
                            project,
                            CjTokens.GET_KEYWORD.value,
                            "prop v:Int{ get(){caret}}",
                            trimSpacesAroundCaret = true,
                            adjustLineIndent = true,
                        )
                    )
                }
            }
        }

    private val SETTER_HANDLER =
        completionKeywordHandler<CompletionKeywordHandler.NO_CONTEXT>(CjTokens.SET_KEYWORD) { parameters, _, lookupElement, project ->
            buildList {
                add(lookupElement.withLineIndentAdjuster())
                if (!parameters.isUseSiteAnnotationTarget) {
                    add(
                        createKeywordConstructLookupElement(
                            project,
                            CjTokens.SET_KEYWORD.value,
                            "mut prop v:Int{ set(value)=caret}",
                            adjustLineIndent = true,
                        )
                    )

                    add(
                        createKeywordConstructLookupElement(
                            project,
                            CjTokens.SET_KEYWORD.value,
                            "mut prop v:Int{ set(value){caret}}",
                            trimSpacesAroundCaret = true,
                            adjustLineIndent = true,
                        )
                    )
                }
            }
        }

    override val handlers = CompletionKeywordHandlers(
        GETTER_HANDLER, SETTER_HANDLER,
//        CONTRACT_HANDLER,
    )
}

private val CompletionParameters.isUseSiteAnnotationTarget
    get() = position.prevLeaf()?.node?.elementType == CjTokens.AT
