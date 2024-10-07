package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.analyzer.CjAnalysisSession
import com.huawei.cangjie.analyzer.api.types.CjType


public class CjTypeRenderer private constructor(
//    public val capturedTypeRenderer: CjCapturedTypeRenderer,
//    public val definitelyNotNullTypeRenderer: CjDefinitelyNotNullTypeRenderer,
//    public val dynamicTypeRenderer: CjDynamicTypeRenderer,
//    public val flexibleTypeRenderer: CjFlexibleTypeRenderer,
//    public val functionalTypeRenderer: CjFunctionalTypeRenderer,
//    public val integerLiteralTypeRenderer: CjIntegerLiteralTypeRenderer,
//    public val intersectionTypeRenderer: CjIntersectionTypeRenderer,
//    public val typeErrorTypeRenderer: CjTypeErrorTypeRenderer,
//    public val typeParameterTypeRenderer: CjTypeParameterTypeRenderer,
//    public val unresolvedClassErrorTypeRenderer: CjUnresolvedClassErrorTypeRenderer,
//    public val usualClassTypeRenderer: CjUsualClassTypeRenderer,
//
//    public val classIdRenderer: CjClassTypeQualifierRenderer,
//    public val typeNameRenderer: CjTypeNameRenderer,
//    public val typeApproximator: CjRendererTypeApproximator,
//    public val typeProjectionRenderer: CjTypeProjectionRenderer,
//    public val annotationsRenderer: CjAnnotationRenderer,
//    public val contextReceiversRenderer: CjContextReceiversRenderer,
//    public val keywordsRenderer: CjKeywordsRenderer,
) {
    context(CjAnalysisSession)
    public fun renderType(type: CjType, printer: PrettyPrinter) {
//        when (type) {
//            is CjCapturedType -> capturedTypeRenderer.renderType(type, printer)
//            is CjFunctionalType -> functionalTypeRenderer.renderType(type, printer)
//            is CjUsualClassType -> usualClassTypeRenderer.renderType(type, printer)
//            is CjDefinitelyNotNullType -> definitelyNotNullTypeRenderer.renderType(type, printer)
//            is CjDynamicType -> dynamicTypeRenderer.renderType(type, printer)
//            is CjFlexibleType -> flexibleTypeRenderer.renderType(type, printer)
//            is CjIntegerLiteralType -> integerLiteralTypeRenderer.renderType(type, printer)
//            is CjIntersectionType -> intersectionTypeRenderer.renderType(type, printer)
//            is CjTypeParameterType -> typeParameterTypeRenderer.renderType(type, printer)
//            is CjClassErrorType -> unresolvedClassErrorTypeRenderer.renderType(type, printer)
//            is CjTypeErrorType -> typeErrorTypeRenderer.renderType(type, printer)
//        }
    }

    public fun with(action: Builder.() -> Unit): CjTypeRenderer {
        val renderer = this
        return CjTypeRenderer {
//            this.capturedTypeRenderer = renderer.capturedTypeRenderer
//            this.definitelyNotNullTypeRenderer = renderer.definitelyNotNullTypeRenderer
//            this.dynamicTypeRenderer = renderer.dynamicTypeRenderer
//            this.flexibleTypeRenderer = renderer.flexibleTypeRenderer
//            this.functionalTypeRenderer = renderer.functionalTypeRenderer
//            this.integerLiteralTypeRenderer = renderer.integerLiteralTypeRenderer
//            this.intersectionTypeRenderer = renderer.intersectionTypeRenderer
//            this.typeErrorTypeRenderer = renderer.typeErrorTypeRenderer
//            this.typeParameterTypeRenderer = renderer.typeParameterTypeRenderer
//            this.unresolvedClassErrorTypeRenderer = renderer.unresolvedClassErrorTypeRenderer
//            this.usualClassTypeRenderer = renderer.usualClassTypeRenderer
//            this.classIdRenderer = renderer.classIdRenderer
//            this.typeNameRenderer = renderer.typeNameRenderer
//            this.typeApproximator = renderer.typeApproximator
//            this.typeProjectionRenderer = renderer.typeProjectionRenderer
//            this.annotationsRenderer = renderer.annotationsRenderer
//            this.contextReceiversRenderer = renderer.contextReceiversRenderer
//            this.keywordsRenderer = renderer.keywordsRenderer
//            action()
        }
    }

    public companion object {
        public operator fun invoke(action: Builder.() -> Unit): CjTypeRenderer =
            Builder().apply(action).build()
    }

    public class Builder {
//        public lateinit var capturedTypeRenderer: CjCapturedTypeRenderer
//        public lateinit var definitelyNotNullTypeRenderer: CjDefinitelyNotNullTypeRenderer
//        public lateinit var dynamicTypeRenderer: CjDynamicTypeRenderer
//        public lateinit var flexibleTypeRenderer: CjFlexibleTypeRenderer
//        public lateinit var functionalTypeRenderer: CjFunctionalTypeRenderer
//        public lateinit var integerLiteralTypeRenderer: CjIntegerLiteralTypeRenderer
//        public lateinit var intersectionTypeRenderer: CjIntersectionTypeRenderer
//        public lateinit var typeErrorTypeRenderer: CjTypeErrorTypeRenderer
//        public lateinit var typeParameterTypeRenderer: CjTypeParameterTypeRenderer
//        public lateinit var unresolvedClassErrorTypeRenderer: CjUnresolvedClassErrorTypeRenderer
//        public lateinit var usualClassTypeRenderer: CjUsualClassTypeRenderer
//        public lateinit var classIdRenderer: CjClassTypeQualifierRenderer
//        public lateinit var typeNameRenderer: CjTypeNameRenderer
//        public lateinit var typeApproximator: CjRendererTypeApproximator
//        public lateinit var typeProjectionRenderer: CjTypeProjectionRenderer
//        public lateinit var annotationsRenderer: CjAnnotationRenderer
//        public lateinit var contextReceiversRenderer: CjContextReceiversRenderer
//        public lateinit var keywordsRenderer: CjKeywordsRenderer

        public fun build(): CjTypeRenderer = CjTypeRenderer(
//            capturedTypeRenderer,
//            definitelyNotNullTypeRenderer,
//            dynamicTypeRenderer,
//            flexibleTypeRenderer,
//            functionalTypeRenderer,
//            integerLiteralTypeRenderer,
//            intersectionTypeRenderer,
//            typeErrorTypeRenderer,
//            typeParameterTypeRenderer,
//            unresolvedClassErrorTypeRenderer,
//            usualClassTypeRenderer,
//            classIdRenderer,
//            typeNameRenderer,
//            typeApproximator,
//            typeProjectionRenderer,
//            annotationsRenderer,
//            contextReceiversRenderer,
//            keywordsRenderer,
        )
    }
}
