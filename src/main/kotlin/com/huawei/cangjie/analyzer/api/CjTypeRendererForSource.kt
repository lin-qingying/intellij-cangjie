package com.huawei.cangjie.analyzer.api

public object CjTypeRendererForSource {
    public val WITH_QUALIFIED_NAMES: CjTypeRenderer = CjTypeRenderer {
//        capturedTypeRenderer = CjCapturedTypeRenderer.AS_PROJECTION
//        definitelyNotNullTypeRenderer = CjDefinitelyNotNullTypeRenderer.AS_TYPE_INTERSECTION
//        dynamicTypeRenderer = CjDynamicTypeRenderer.AS_DYNAMIC_WORD
//        flexibleTypeRenderer = CjFlexibleTypeRenderer.AS_SHORT
//        functionalTypeRenderer = CjFunctionalTypeRenderer.AS_CLASS_TYPE_FOR_REFLECTION_TYPES
//        integerLiteralTypeRenderer = CjIntegerLiteralTypeRenderer.AS_ILT_WITH_VALUE
//        intersectionTypeRenderer = CjIntersectionTypeRenderer.AS_INTERSECTION
//        typeErrorTypeRenderer = CjTypeErrorTypeRenderer.AS_CODE_IF_POSSIBLE
//        typeParameterTypeRenderer = CjTypeParameterTypeRenderer.AS_SOURCE
//        unresolvedClassErrorTypeRenderer = CjUnresolvedClassErrorTypeRenderer.UNRESOLVED_QUALIFIER
//        usualClassTypeRenderer = CjUsualClassTypeRenderer.AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS
//        classIdRenderer = CjClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
//        typeNameRenderer = CjTypeNameRenderer.QUOTED
//        typeApproximator = CjRendererTypeApproximator.TO_DENOTABLE
//        typeProjectionRenderer = CjTypeProjectionRenderer.WITH_VARIANCE
//        annotationsRenderer = CjAnnotationRendererForSource.WITH_QUALIFIED_NAMES
//        contextReceiversRenderer = CjContextReceiversRendererForSource.WITH_LABELS
//        keywordsRenderer = CjKeywordsRenderer.AS_WORD
    }

    public val WITH_SHORT_NAMES: CjTypeRenderer = WITH_QUALIFIED_NAMES.with {
//        classIdRenderer = CjClassTypeQualifierRenderer.WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS
//        annotationsRenderer = CjAnnotationRendererForSource.WITH_SHORT_NAMES
    }
}
