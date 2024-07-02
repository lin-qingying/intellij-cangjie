package com.huawei.cangjie.types

import com.huawei.cangjie.types.model.CangJieTypeMarker

/**
 * This annotation marks part of internal compiler API related to type refinement.
 *
 * Marking such API explicitly has two objectives:
 * - prevent unconscious abuse of invasive API (like DelegatingSimpleType.replaceDelegate),
 *   which shouldn't be needed by anything outside of type refinement
 * - improve readability of classes by separating API
 *
 * If you're using related API outside of MPP context, it's a nice idea to consider
 * either finding some other API or removing @TypeRefinement (and thus "publishing"
 * API for broader use)
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class TypeRefinement


abstract class AbstractTypeRefiner {


    @TypeRefinement
    abstract fun refineType(type: CangJieTypeMarker): CangJieTypeMarker

    object Default : AbstractTypeRefiner() {
        @TypeRefinement
        override fun refineType(type: CangJieTypeMarker): CangJieTypeMarker {
            return type
        }
    }
}
