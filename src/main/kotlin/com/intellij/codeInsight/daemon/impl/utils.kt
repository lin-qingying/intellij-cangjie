package com.intellij.codeInsight.daemon.impl

import com.intellij.psi.PsiReference

/**
 * Tell highlighting subsystem that this `info` was generated to highlight unresolved reference `ref`.
 * This call triggers background calculation of quick fixes supplied by [UnresolvedReferenceQuickFixProvider]
 * You can only call it from the highlighting (e.g. inside your [HighlightVisitor])
 */

fun registerQuickFixesLater(ref: PsiReference, info: HighlightInfo.Builder) {
    (info as? HighlightInfoB)?.setUnresolvedReference(ref)

}
