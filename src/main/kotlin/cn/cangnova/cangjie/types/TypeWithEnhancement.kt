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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.renderer.DescriptorRendererOptions
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner

interface TypeWithEnhancement {
    val origin: UnwrappedType
    val enhancement: CangJieType
}
fun UnwrappedType.inheritEnhancement(origin: CangJieType): UnwrappedType = wrapEnhancement(origin.getEnhancement())

fun UnwrappedType.inheritEnhancement(origin: CangJieType, transform: (CangJieType) -> CangJieType): UnwrappedType =
    wrapEnhancement(origin.getEnhancement()?.let(transform))
fun CangJieType.getEnhancement(): CangJieType? = when (this) {
//    is TypeWithEnhancement -> enhancement
    else -> null
}
fun UnwrappedType.wrapEnhancement(enhancement: CangJieType?): UnwrappedType {
    if (this is TypeWithEnhancement) {
        return origin.wrapEnhancement(enhancement)
    }
    if (enhancement == null || enhancement == this) {
        return this
    }
    return when (this) {
        is SimpleType -> SimpleTypeWithEnhancement(this, enhancement)
        is FlexibleType -> FlexibleTypeWithEnhancement(this, enhancement)
    }
}

class FlexibleTypeWithEnhancement(
    override val origin: FlexibleType,
    override val enhancement: CangJieType
) : FlexibleType(origin.lowerBound, origin.upperBound),
    TypeWithEnhancement {

    override fun replaceAttributes(newAttributes: TypeAttributes): UnwrappedType =
        origin.replaceAttributes(newAttributes).wrapEnhancement(enhancement)

    override fun makeOptionalAsSpecified(newNullability: Boolean): UnwrappedType =
        origin.makeOptionalAsSpecified(newNullability).wrapEnhancement(enhancement.unwrap().makeOptionalAsSpecified(newNullability))


    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String {
//        if (options.enhancedTypes) {
//            return renderer.renderType(enhancement)
//        }
        return origin.render(renderer, options)
    }

    override val delegate: SimpleType get() = origin.delegate

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(cangjieTypeRefiner:CangJieTypeRefiner) =
        FlexibleTypeWithEnhancement(
            cangjieTypeRefiner.refineType(origin) as FlexibleType,
            cangjieTypeRefiner.refineType(enhancement)
        )

    override fun toString(): String =
        "[@EnhancedForWarnings($enhancement)] $origin"
}
class SimpleTypeWithEnhancement(
    override val delegate: SimpleType,
    override val enhancement: CangJieType
) : DelegatingSimpleType(),
    TypeWithEnhancement {

    override val origin get() = delegate

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        origin.replaceAttributes(newAttributes).wrapEnhancement(enhancement) as SimpleType
//
    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType = origin.makeOptionalAsSpecified(newNullability)
        .wrapEnhancement(enhancement.unwrap().makeOptionalAsSpecified(newNullability)) as SimpleType

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = SimpleTypeWithEnhancement(delegate, enhancement)
//
//    @TypeRefinement
//    @OptIn(TypeRefinement::class)
//    override fun refine(cangnjieTypeRefiner: CangJieTypeRefiner): SimpleTypeWithEnhancement =
//        SimpleTypeWithEnhancement(
//            cangnjieTypeRefiner.refineType(delegate) as SimpleType,
//            cangnjieTypeRefiner.refineType(enhancement)
//        )

    override fun toString(): String =
        "[@EnhancedForWarnings($enhancement)] $origin"
}
