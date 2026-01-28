/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.checker.*
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeConstructorMarker


class OverridingUtilTypeSystemContext(
    val matchingTypeConstructors: Map<TypeConstructor, TypeConstructor>?,
    private val equalityAxioms: CangJieTypeChecker.TypeConstructorEquality,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
    private val cangjieTypePreparator: CangJieTypePreparator,
    private val customSubtype: ((CangJieType, CangJieType) -> Boolean)? = null,
) : ClassicTypeSystemContext {


    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        require(c1 is TypeConstructor)
        require(c2 is TypeConstructor)
        return super.areEqualTypeConstructors(c1, c2) || areEqualTypeConstructorsByAxioms(c1, c2)
    }

    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean,
        allowOptionBoxing: Boolean
    ): TypeCheckerState {
        // 仓颉语言的 TypeCheckerState 已简化
        // customSubtype 功能已不再需要，因为新的类型系统已内置所有必要的子类型检查
        return createClassicTypeCheckerState(
            errorTypesEqualToAnything,
            stubTypesEqualToAnything,
            typeSystemContext = this,
            cangjieTypeRefiner = cangjieTypeRefiner,
            cangjieTypePreparator = cangjieTypePreparator,
            allowOptionBoxing = allowOptionBoxing
        )
    }





    private fun areEqualTypeConstructorsByAxioms(a: TypeConstructor, b: TypeConstructor): Boolean {
        if (equalityAxioms.equals(a, b)) return true
        if (matchingTypeConstructors == null) return false
        val img1 = matchingTypeConstructors[a]
        val img2 = matchingTypeConstructors[b]
        return img1 != null && img1 == b || img2 != null && img2 == a
    }

}
