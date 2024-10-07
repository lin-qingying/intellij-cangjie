package com.huawei.cangjie.analyzer

import com.huawei.cangjie.analyzer.session.CjAnalysisSessionProvider
import com.huawei.cangjie.psi.CjElement

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
