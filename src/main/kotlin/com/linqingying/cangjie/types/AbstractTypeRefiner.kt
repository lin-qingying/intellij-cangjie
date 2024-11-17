/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.types

import com.linqingying.cangjie.types.model.CangJieTypeMarker

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
