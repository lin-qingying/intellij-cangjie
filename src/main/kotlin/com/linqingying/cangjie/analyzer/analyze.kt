package com.linqingying.cangjie.analyzer

import com.linqingying.cangjie.analyzer.session.CjAnalysisSessionProvider
import com.linqingying.cangjie.psi.CjElement

//@file:OptIn(CjAnalysisApiInternals::class)

/**
 * Executes the given [action] in a [CangJieAnalysisSession] context.
 *
 * The project will be analyzed from the perspective of [useSiteCjElement]'s module, also called the use-site module.
 *
 * @see CangJieAnalysisSession
 */
inline fun <R> analyze(
    useSiteCjElement: CjElement,
    action: CangJieAnalysisSession.() -> R
): R =
    CjAnalysisSessionProvider.getInstance(useSiteCjElement.project)
        .analyse(useSiteCjElement, action)
