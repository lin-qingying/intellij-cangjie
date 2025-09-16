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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.resolve.OverridingUtil
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.AbstractNullabilityChecker.hasNotNullSupertype

object SimpleClassicTypeSystemContext : ClassicTypeSystemContext

fun UnwrappedType.hasSupertypeWithGivenTypeConstructor(typeConstructor: TypeConstructor) =
    createClassicTypeCheckerState(isErrorTypeEqualsToAnything = false).anySupertype(lowerIfFlexible(), {
        require(it is SimpleType)
        it.constructor == typeConstructor
    }, { TypeCheckerState.SupertypesPolicy.LowerIfFlexible })

interface NewCangJieTypeChecker : CangJieTypeChecker {
    val cangjieTypeRefiner: CangJieTypeRefiner
    val cangjieTypePreparator: CangJieTypePreparator
    val overridingUtil: OverridingUtil

    companion object {
        val Default = NewCangJieTypeCheckerImpl(CangJieTypeRefiner.Default)
    }
}
object StrictEqualityTypeChecker {

    /**
     * String! != String & A<String!> != A<String>, also A<in Nothing> != A<out Any?>
     * also A<*> != A<out Any?>
     * different error types non-equals even errorTypeEqualToAnything
     */
    fun strictEqualTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
    }

    fun strictEqualTypes(a: SimpleType, b: SimpleType): Boolean {
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
    }

}
object ErrorTypesAreEqualToAnything : CangJieTypeChecker {
    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).equalsIgnoringGenerics(
                a.unwrap(),
                b.unwrap()
            )
        }

    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).isSubtypeOf(
                subtype.unwrap(),
                supertype.unwrap()
            )
        }

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).equalTypes(a.unwrap(), b.unwrap())
        }
}

class NewCangJieTypeCheckerImpl(
    override val cangjieTypeRefiner: CangJieTypeRefiner,
    override val cangjieTypePreparator: CangJieTypePreparator = CangJieTypePreparator.Default
) : NewCangJieTypeChecker {
    override val overridingUtil: OverridingUtil = OverridingUtil.createWithTypeRefiner(cangjieTypeRefiner)
    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean =
        createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalsIgnoringGenerics(a.unwrap(), b.unwrap())

    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean =
        createClassicTypeCheckerState(
            true, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean =
        createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalTypes(a.unwrap(), b.unwrap())

    fun TypeCheckerState.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalTypes(this, a, b)
    }

    fun TypeCheckerState.equalsIgnoringGenerics(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalsIgnoringGenerics(this, a, b)
    }

    fun TypeCheckerState.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
        return AbstractTypeChecker.isSubtypeOf(this, subType, superType)
    }
}

object OptionChecker {
    fun isSubtypeOfAny(type: UnwrappedType): Boolean =
        SimpleClassicTypeSystemContext
            .newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .hasNotNullSupertype(type.lowerIfFlexible(), TypeCheckerState.SupertypesPolicy.LowerIfFlexible)
}
