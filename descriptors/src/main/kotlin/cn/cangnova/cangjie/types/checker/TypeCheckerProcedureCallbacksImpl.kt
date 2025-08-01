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

import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.TypeProjection

internal class TypeCheckerProcedureCallbacksImpl : TypeCheckingProcedureCallbacks {
    override fun assertEqualTypes(
        a: CangJieType,
        b: CangJieType,
        typeCheckingProcedure: TypeCheckingProcedure
    ): Boolean {
        return typeCheckingProcedure.equalTypes(a, b)
    }

    override fun assertEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean {
        return a == b
    }

    override fun assertSubtype(
        subtype: CangJieType,
        supertype: CangJieType,
        typeCheckingProcedure: TypeCheckingProcedure
    ): Boolean {
        return typeCheckingProcedure.isSubtypeOf(subtype, supertype)
    }

    override fun capture(type: CangJieType, typeProjection: TypeProjection): Boolean {
        return false
    }

    override fun noCorrespondingSupertype(subtype: CangJieType, supertype: CangJieType): Boolean {
        return false // type checking fails
    }
}
