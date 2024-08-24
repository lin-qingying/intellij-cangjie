package com.huawei.cangjie.analyzer

import com.huawei.cangjie.analyzer.session.CjAnalysisSessionProvider
import com.huawei.cangjie.psi.CjElement


/**
 * Executes the given [action] in a [CjAnalysisSession] context.
 *
 * The project will be analyzed from the perspective of [useSiteCjElement]'s module, also called the use-site module.
 *
 * @see CjAnalysisSession
 */
inline fun <R> analyze(
    useSiteCjElement: CjElement,
    action: CjAnalysisSession.() -> R
): R =
    CjAnalysisSessionProvider.getInstance(useSiteCjElement.project)
        .analyse(useSiteCjElement, action)
