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

package org.cangnova.cangjie.extensions

import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.psi.CjModifierListOwner


interface DeclarationAttributeAltererExtension {
    companion object {
        val EP_NAME: ExtensionPointName<DeclarationAttributeAltererExtension> =
            ExtensionPointName.create("org.cangnova.cangjie.declarationAttributeAltererExtension")
    }


    /**
     * Returns the new modality for the [declaration], or null if the [currentModality] is good enough.
     */
    fun refineDeclarationModality(
        modifierListOwner: CjModifierListOwner,
        declaration: DeclarationDescriptor?,
        containingDeclaration: DeclarationDescriptor?,
        currentModality: Modality,
        isImplicitModality: Boolean
    ): Modality? = null

    @Deprecated(
        "Use refineDeclarationModality(modifierListOwner, declaration, containingDeclaration, currentModality, bindingContext, isImplicitModality)",
        ReplaceWith("refineDeclarationModality(modifierListOwner, declaration, containingDeclaration, currentModality, bindingContext, false)")
    )
    fun refineDeclarationModality(
        modifierListOwner: CjModifierListOwner,
        declaration: DeclarationDescriptor?,
        containingDeclaration: DeclarationDescriptor?,
        currentModality: Modality
    ): Modality? {
        return refineDeclarationModality(modifierListOwner, declaration, containingDeclaration, currentModality, false)
    }

    fun shouldConvertFirstSAMParameterToReceiver(function: FunctionDescriptor): Boolean = false
}
