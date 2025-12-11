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

import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.INCOMPATIBLE_ENUM_COMPARISON_ERROR

import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing

fun checkEnumsForCompatibility(
    context: ResolutionContext<*>,
    reportOn: CjElement,
    typeA: CangJieType,
    typeB: CangJieType
) {
    if (isIncompatibleEnums(typeA, typeB)) {
//        val diagnostic = if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitComparisonOfIncompatibleEnums)) {
//            Errors.INCOMPATIBLE_ENUM_COMPARISON_ERROR
//        } else {
//            Errors.INCOMPATIBLE_ENUM_COMPARISON
//        }
        val diagnostic = INCOMPATIBLE_ENUM_COMPARISON_ERROR
        context.trace.report(diagnostic.on(reportOn, typeA, typeB))
    }
}

private fun isIncompatibleEnums(typeA: CangJieType, typeB: CangJieType): Boolean {
    if (!typeA.isEnum && !typeB.isEnum) return false
    if (TypeUtils.isOptionType(typeA) && TypeUtils.isOptionType(typeB)) return false


    // For now, this check is needed as isSubClass contains bug wrt Nothing
    if (typeA.isNothing() || typeB.isNothing()) return false

    val representativeTypeA = typeA.representativeTypeForTypeParameter()
    val representativeTypeB = typeB.representativeTypeForTypeParameter()

    val classA = representativeTypeA.constructor.declarationDescriptor as? ClassDescriptor ?: return false
    val classB = representativeTypeB.constructor.declarationDescriptor as? ClassDescriptor ?: return false

    return !DescriptorUtils.isSubclass(classA, classB) && !DescriptorUtils.isSubclass(classB, classA)
}

private fun CangJieType.representativeTypeForTypeParameter(): CangJieType {
    val descriptor = constructor.declarationDescriptor
    return if (descriptor is TypeParameterDescriptor) descriptor.representativeUpperBound else this
}
