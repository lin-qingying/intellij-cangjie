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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner


class TypeApproximator(
    builtIns: CangJieBuiltIns,
    languageVersionSettings: LanguageVersionSettings,
) : AbstractTypeApproximator(
    ClassicTypeSystemContextForCS(builtIns, CangJieTypeRefiner.Default),
    languageVersionSettings
) {
    fun approximateDeclarationType(baseType: CangJieType, local: Boolean): UnwrappedType {
        // 仓颉语言始终使用新的类型推断系统
        val configuration =
            if (local) TypeApproximatorConfiguration.LocalDeclaration else TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes
        val preparedType = if (local) baseType.unwrap() else substituteAlternativesInPublicType(baseType)
        return approximateToSuperType(preparedType, configuration) ?: preparedType
    }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
        super.approximateToSuperType(type, conf) as UnwrappedType?

    //    // resultType <: type
    fun approximateToSubType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
        super.approximateToSubType(type, conf) as UnwrappedType?

    fun approximateTo(type: UnwrappedType, conf: TypeApproximatorConfiguration, toSuperType: Boolean): UnwrappedType? =
        if (toSuperType) approximateToSuperType(type, conf) else approximateToSubType(type, conf)
}
