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

package org.cangnova.cangjie.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.DiagnosticFactory3
import org.cangnova.cangjie.diagnostics.infos.errors.EXPOSED_TYPEALIAS_EXPANDED_TYPE
import org.cangnova.cangjie.diagnostics.infos.warnings.EXPOSED_FROM_PRIVATE_IN_FILE
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.isError

// 用于检查所有七种 EXPOSED_* 错误的检查器
// 所有函数在一切正常时返回 true，如果存在任何错误则返回 false
class ExposedVisibilityChecker(
    private val languageVersionSettings: LanguageVersionSettings,
    private val trace: BindingTrace? = null
) {

    private fun <E : PsiElement> reportExposure(
        diagnostic: DiagnosticFactory3<E, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility>,
        element: E,
        elementVisibility: EffectiveVisibility,
        restrictingDescriptor: DescriptorWithRelation
    ) {
        val trace = trace ?: return
        val restrictingVisibility = restrictingDescriptor.effectiveVisibility()

        if (/*!languageVersionSettings.supportsFeature(LanguageFeature.PrivateInFileEffectiveVisibility) &&*/
            elementVisibility == EffectiveVisibility.PrivateInFile
        ) {
            trace.report(
                EXPOSED_FROM_PRIVATE_IN_FILE.on(
                    element,
                    elementVisibility,
                    restrictingDescriptor,
                    restrictingVisibility
                )
            )
        } else {
            trace.report(diagnostic.on(element, elementVisibility, restrictingDescriptor, restrictingVisibility))
        }
    }

    fun checkTypeAlias(typeAlias: CjTypeAlias, typeAliasDescriptor: TypeAliasDescriptor) {
        val expandedType = typeAliasDescriptor.expandedType
        if (expandedType.isError) return

        val typeAliasVisibility = typeAliasDescriptor.effectiveVisibility()
        val restricting = expandedType.leastPermissiveDescriptor(typeAliasVisibility)
        if (restricting != null) {
            reportExposure(
                EXPOSED_TYPEALIAS_EXPANDED_TYPE,
                typeAlias.nameIdentifier ?: typeAlias,
                typeAliasVisibility,
                restricting
            )
        }
    }

}
