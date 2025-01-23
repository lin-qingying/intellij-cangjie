package com.intellij.codeInsight.daemon.impl;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

public class UnresolveReferenceQuickFixUtil {

    static public void registerQuickFixesLater(@NotNull PsiReference ref, @NotNull HighlightInfo.Builder info) {
        ((HighlightInfoB) info).setUnresolvedReference(ref);
    }


}
