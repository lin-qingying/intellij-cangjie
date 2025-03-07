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

package cn.cangnova.cangjie.types.checker

import cn.cangnova.cangjie.types.model.CangJieTypeMarker
import cn.cangnova.cangjie.types.model.SimpleTypeMarker
import cn.cangnova.cangjie.types.model.TypeSystemContext

object AbstractStrictEqualityTypeChecker {
    fun strictEqualTypes(context: TypeSystemContext, a: CangJieTypeMarker, b: CangJieTypeMarker) = context.strictEqualTypesInternal(a, b)

    /**
     * Note that:
     * - `String!` != `String`
     * - `A<String!>` != `A<String>`
     * - `A<in Nothing>` != `A<out Any?>`
     * - `A<*>` != `A<out Any?>`
     *
     * Also different error types are not equal even if errorTypeEqualToAnything is true
     */
    private fun TypeSystemContext.strictEqualTypesInternal(a: CangJieTypeMarker, b: CangJieTypeMarker): Boolean {
        if (a === b) return true

        val simpleA = a.asSimpleType()
        val simpleB = b.asSimpleType()
        if (simpleA != null && simpleB != null) return strictEqualSimpleTypes(simpleA, simpleB)

        val flexibleA = a.asFlexibleType()
        val flexibleB = b.asFlexibleType()
        if (flexibleA != null && flexibleB != null) {
            return strictEqualSimpleTypes(flexibleA.lowerBound(), flexibleB.lowerBound()) &&
                    strictEqualSimpleTypes(flexibleA.upperBound(), flexibleB.upperBound())
        }
        return false
    }

    private fun TypeSystemContext.strictEqualSimpleTypes(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        if (a.argumentsCount() != b.argumentsCount()
            || a.isMarkedNullable() != b.isMarkedNullable()
            || (a.asDefinitelyNotNullType() == null) != (b.asDefinitelyNotNullType() == null)
            || !areEqualTypeConstructors(a.typeConstructor(), b.typeConstructor())
        ) {
            return false
        }

        if (identicalArguments(a, b)) return true

        for (i in 0 until a.argumentsCount()) {
            val aArg = a.getArgument(i)
            val bArg = b.getArgument(i)

            // both non-star

                if (aArg.getVariance() != bArg.getVariance()) return false
                if (!strictEqualTypesInternal(aArg.getType(), bArg.getType())) return false

        }
        return true
    }

}
