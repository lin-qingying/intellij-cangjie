package com.linqingying.cangjie.psi

interface CjAnnotated : CjElement {
    val annotations: List<CjAnnotation >

    val annotationEntries: List<CjAnnotationEntry >
}
