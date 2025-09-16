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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.renderer.DescriptorRendererOptions
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

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

// 该函数主要用于集合：因为 CangJieType.equals() 表示类型的语法等价性，
// 而 CangJieTypeChecker.DEFAULT.equalsTypes() 表示语义等价性。
// 集合中的类型（例如精确上界等）可能包含例如 X、X? 和 X!。
// 这些在语法上不相等（按 CangJieType.equals()），但 X! 在语义上与其他类型兼容，
// 而且作为精确上界它更合适。
//
// 因此，我们在集合中寻找一个类型，使其在语义上等于所有其他类型
// （按 CangJieTypeChecker.DEFAULT.equalsTypes()），并且至少与它们一样合适。
fun Collection<CangJieType>.singleBestRepresentative(): CangJieType? {
    if (this.size == 1) return this.first()

    return this.firstOrNull { candidate ->
        this.all { other ->
            // 在此处我们认为错误类型与任何类型都相等，以便类似
            // {Array<String>, Array<[ERROR]>} 的交集正常工作
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

    // 这些断言用于检查可变类型的约束。
    //
    // 不幸的是 isSubtypeOf 会对延迟类型进行解析。
    // 因此在创建此类型时不能运行这些断言。详见 EA-74904
    //
    // 此外 isSubtypeOf 不是一个很快的操作，因此我们只在 ASSERTIONS_ENABLED 时运行断言。
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
