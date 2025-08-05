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

import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.renderer.DescriptorRendererOptions
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner

/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


fun CangJieType.isFlexible(): Boolean = unwrap() is FlexibleType
fun CangJieType.asFlexibleType(): FlexibleType = unwrap() as FlexibleType

fun CangJieType.isNullabilityFlexible(): Boolean {
    val flexibility = unwrap() as? FlexibleType ?: return false
            return flexibility.lowerBound.isOption != flexibility.upperBound.isOption
}

// This function is intended primarily for sets: since CangJieType.equals() represents _syntactical_ equality of types,
// whereas CangJieTypeChecker.DEFAULT.equalsTypes() represents semantic equality
// A set of types (e.g. exact bounds etc) may contain, for example, X, X? and X!
// These are not equal syntactically (by CangJieType.equals()), but X! is _compatible_ with others as exact bounds,
// moreover, X! is a better fit.
//
// So, we are looking for a type among this set such that it is equal to all others semantically
// (by CangJieTypeChecker.DEFAULT.equalsTypes()), and fits at least as well as they do.
fun Collection<CangJieType>.singleBestRepresentative(): CangJieType? {
    if (this.size == 1) return this.first()

    return this.firstOrNull { candidate ->
        this.all { other ->
            // We consider error types equal to anything here, so that intersections like
            // {Array<String>, Array<[ERROR]>} work correctly
            candidate == other
//                    || ErrorTypesAreEqualToAnything.equalTypes(candidate, other)
        }
    }
}


fun Collection<TypeProjection>.singleBestRepresentative(): TypeProjection? {
    if (this.size == 1) return this.first()

    val projectionKinds = this.map { it.projectionKind }.toSet()
    if (projectionKinds.size != 1) return null

    val bestType = this.map { it.type }.singleBestRepresentative() ?: return null

    return TypeProjectionImpl(projectionKinds.single(), bestType)
}

fun CangJieType.lowerIfFlexible(): SimpleType = with(unwrap()) {
    when (this) {
        is FlexibleType -> lowerBound
        is SimpleType -> this
    }
}

fun CangJieType.upperIfFlexible(): SimpleType = with(unwrap()) {
    when (this) {
        is FlexibleType -> upperBound
        is SimpleType -> this
    }
}

class FlexibleTypeImpl(lowerBound: SimpleType, upperBound: SimpleType) : FlexibleType(lowerBound, upperBound), CustomTypeParameter {
    companion object {
        @JvmField
        var RUN_SLOW_ASSERTIONS = false
    }

    // These assertions are needed for checking invariants of flexible types.
    //
    // Unfortunately isSubtypeOf is running resolve for lazy types.
    // Because of this we can't run these assertions when we are creating this type. See EA-74904
    //
    // Also isSubtypeOf is not a very fast operation, so we are running assertions only if ASSERTIONS_ENABLED.
    private var assertionsDone = false

    private fun runAssertions() {
        if (!RUN_SLOW_ASSERTIONS || assertionsDone) return
        assertionsDone = true

        assert(!lowerBound.isFlexible()) { "Lower bound of a flexible type can not be flexible: $lowerBound" }
        assert(!upperBound.isFlexible()) { "Upper bound of a flexible type can not be flexible: $upperBound" }

        assert(lowerBound != upperBound) { "Lower and upper bounds are equal: $lowerBound == $upperBound" }
//        assert(CangJieTypeChecker.DEFAULT.isSubtypeOf(lowerBound, upperBound)) {
//            "Lower bound $lowerBound of a flexible type must be a subtype of the upper bound $upperBound"
//        }
    }

    override val delegate: SimpleType
        get() {
            runAssertions()
            return lowerBound
        }

    override val isTypeParameter: Boolean
        get() = lowerBound.constructor.declarationDescriptor is TypeParameterDescriptor
                && lowerBound.constructor == upperBound.constructor

    override fun substitutionResult(replacement: CangJieType): CangJieType {
        val unwrapped = replacement.unwrap()
        return when (unwrapped) {
            is FlexibleType -> unwrapped
            is SimpleType -> CangJieTypeFactory.flexibleType(unwrapped, unwrapped.makeOptionAsSpecified(true))
        }.inheritEnhancement(unwrapped)
    }

    override fun replaceAttributes(newAttributes: TypeAttributes): UnwrappedType =
        CangJieTypeFactory.flexibleType(lowerBound.replaceAttributes(newAttributes), upperBound.replaceAttributes(newAttributes))

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String {
//        if (options.debugMode) {
//            return "(${renderer.renderType(lowerBound)}..${renderer.renderType(upperBound)})"
//        }
        return renderer.renderFlexibleType(renderer.renderType(lowerBound), renderer.renderType(upperBound), builtIns)
    }

    override fun toString() = "($lowerBound..$upperBound)"

    override fun makeOptionAsSpecified(isOption: Boolean): UnwrappedType = CangJieTypeFactory.flexibleType(
        lowerBound.makeOptionAsSpecified(isOption),
        upperBound.makeOptionAsSpecified(isOption)
    )

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): FlexibleType {
        return FlexibleTypeImpl(
            cangjieTypeRefiner.refineType(lowerBound) as SimpleType,
            cangjieTypeRefiner.refineType(upperBound) as SimpleType
        )
    }
}
