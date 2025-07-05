/*
 * Copyright 2025 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.renderer

import cn.cangnova.cangjie.renderer.DescriptorRenderer.Companion.FQ_NAMES_IN_TYPES
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.checker.NewCapturedTypeConstructor
import cn.cangnova.cangjie.types.isDynamic
import cn.cangnova.cangjie.types.util.approximateFlexibleTypes
import cn.cangnova.cangjie.types.util.builtIns

object IdeDescriptorRenderers {
    @JvmField
    val APPROXIMATE_FLEXIBLE_TYPES: (CangJieType) -> CangJieType = {
        it.approximateFlexibleTypes(preferNotNull = false)
    }

    @JvmField
    val FQ_NAMES_IN_TYPES_WITH_NORMALIZER: DescriptorRenderer = FQ_NAMES_IN_TYPES.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
        typeNormalizer = { APPROXIMATE_FLEXIBLE_TYPES(unwrapAnonymousType(it)) }
        renderUnabbreviatedType = false
    }

    private val BASE: DescriptorRenderer = DescriptorRenderer.withOptions {
        normalizedVisibilities = true
        withDefinedIn = false
        renderDefaultVisibility = false
        overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
        unitReturnType = false
        enhancedTypes = true
        modifiers = DescriptorRendererModifier.ALL
        renderUnabbreviatedType = false
        annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
        annotationFilter = { false }
    }

    private fun unwrapAnonymousType(type: CangJieType): CangJieType {
        if (type.isDynamic()) return type
        if (type.constructor is NewCapturedTypeConstructor) return type

        val classifier = type.constructor.declarationDescriptor
        if (classifier != null && !classifier.name.isSpecial) return type

        type.constructor.supertypes.singleOrNull()?.let { return it }

        val builtIns = type.builtIns

        return builtIns.anyType
    }

    @JvmField
    val SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS: DescriptorRenderer = BASE.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SHORT
        typeNormalizer = { APPROXIMATE_FLEXIBLE_TYPES(unwrapAnonymousType(it)) }
        modifiers = modifiers - DescriptorRendererModifier.ANNOTATIONS
    }

    @JvmField
    val SOURCE_CODE: DescriptorRenderer = BASE.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
        typeNormalizer = { APPROXIMATE_FLEXIBLE_TYPES(unwrapAnonymousType(it)) }
    }
}
