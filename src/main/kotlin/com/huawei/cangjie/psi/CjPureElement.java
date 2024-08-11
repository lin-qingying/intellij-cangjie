//package com.huawei.cangjie.psi;
//
//import com.intellij.psi.PsiElement;
//import org.jetbrains.annotations.NotNull;
//
///**
// * A minimal interface that {@link CjElement} implements for the purpose of code-generation that does not need the full power of PSI.
// * This interface can be easily implemented by synthetic elements to generate code for them.
// */
//public interface CjPureElement {
//    /**
//     * Returns this or parent source element (for synthetic element declarations).
//     * Use it only for the purposes of source attribution.
//     */
//    @NotNull
//    CjElement getPsiOrParent();
//
//    /**
//     * Returns parent source element.
//     */
//    PsiElement getParent();
//
//    @NotNull
//    CjFile getContainingCjFile();
//}
