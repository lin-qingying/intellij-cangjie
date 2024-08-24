package com.huawei.cangjie.ide.completion.back.weighers

import com.huawei.cangjie.ide.completion.back.positionContext.CangJieRawPositionContext
import com.huawei.cangjie.ide.completion.back.positionContext.CangJieSuperReceiverNameReferencePositionContext
import com.huawei.cangjie.psi.UserDataProperty
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.openapi.util.Key

//
//import com.huawei.cangjie.analyzer.CjAnalysisSession
//import com.intellij.codeInsight.completion.CompletionSorter
//import com.intellij.codeInsight.lookup.LookupElement
//import com.intellij.codeInsight.lookup.WeighingContext
//
//
internal object Weighers {
//    context(CjAnalysisSession)
//    fun applyWeighsToLookupElement(
//        context: WeighingContext,
//        lookupElement: LookupElement,
//        symbolWithOrigin: CjSymbolWithOrigin?,
//    ) {
//        ExpectedTypeWeigher.addWeight(context, lookupElement, symbolWithOrigin?.symbol)
//        KindWeigher.addWeight(lookupElement, symbolWithOrigin?.symbol, context)
//
//        if (symbolWithOrigin == null) return
//        val symbol = symbolWithOrigin.symbol
//
//        val availableWithoutImport = symbolWithOrigin.origin is CompletionSymbolOrigin.Scope
//
//        DeprecatedWeigher.addWeight(lookupElement, symbol)
//        PreferGetSetMethodsToPropertyWeigher.addWeight(lookupElement, symbol)
//        NotImportedWeigher.addWeight(context, lookupElement, symbol, availableWithoutImport)
//        ClassifierWeigher.addWeight(lookupElement, symbol, symbolWithOrigin.origin)
//        VariableOrFunctionWeigher.addWeight(lookupElement, symbol)
//        K2SoftDeprecationWeigher.addWeight(lookupElement, symbol, context.languageVersionSettings)
//
//        if (symbol !is CjCallableSymbol) return
//
//        PreferContextualCallablesWeigher.addWeight(lookupElement, symbol, context.contextualSymbolsCache)
//        PreferFewerParametersWeigher. addWeight(lookupElement, symbol)
//    }
//
//    context(CjAnalysisSession)
//    fun applyWeighsToLookupElementForCallable(
//        context: WeighingContext,
//        lookupElement: LookupElement,
//        signature: CjCallableSignature<*>,
//        symbolOrigin: CompletionSymbolOrigin,
//    ) {
//        CallableWeigher.addWeight(context, lookupElement, signature, symbolOrigin)
//
//        applyWeighsToLookupElement(context, lookupElement, CjSymbolWithOrigin(signature.symbol, symbolOrigin))
//    }
//
    fun addWeighersToCompletionSorter(sorter: CompletionSorter, positionContext: CangJieRawPositionContext): CompletionSorter =
        sorter
            .weighBefore(
                PlatformWeighersIds.STATS,
                CompletionContributorGroupWeigher.Weigher,
//                ExpectedTypeWeigher.Weigher,
//                DeprecatedWeigher.Weigher,
//                PriorityWeigher.Weigher,
//                PreferGetSetMethodsToPropertyWeigher.Weigher,
//                NotImportedWeigher.Weigher,
//                KindWeigher.Weigher,
//                CallableWeigher.Weigher,
//                ClassifierWeigher.Weigher,
            )
            .weighAfter(
                PlatformWeighersIds.STATS,
//                VariableOrFunctionWeigher.Weigher
            )
            .weighBefore(
                PlatformWeighersIds.PREFIX,
//                K2SoftDeprecationWeigher.Weigher,
//                VariableOrParameterNameWithTypeWeigher.Weigher
            )
            .weighAfter(
                PlatformWeighersIds.PROXIMITY,
//                ByNameAlphabeticalWeigher.Weigher,
//                PreferFewerParametersWeigher.Weigher,
            )
//            .weighBefore(getBeforeIdForContextualCallablesWeigher(positionContext), PreferContextualCallablesWeigher.Weigher)
//
    private fun getBeforeIdForContextualCallablesWeigher(positionContext: CangJieRawPositionContext): String =
        when (positionContext) {
            // prefer contextual callable when completing reference after "super."
//            is CangJieSuperReceiverNameReferencePositionContext -> ExpectedTypeWeigher.WEIGHER_ID
            else -> PlatformWeighersIds.PROXIMITY
        }

    private object PlatformWeighersIds {
        const val PREFIX = "prefix"
        const val STATS = "stats"
        const val PROXIMITY = "proximity"
    }
}
internal object CompletionContributorGroupWeigher {
    object Weigher : LookupElementWeigher(WEIGHER_ID) {
        override fun weigh(element: LookupElement): Comparable<*>? =
            element.groupPriority
    }

    var LookupElement.groupPriority by UserDataProperty(Key<Int>("GROUP_PRIORITY"))


    const val WEIGHER_ID = "cangjie.group.id"
}


