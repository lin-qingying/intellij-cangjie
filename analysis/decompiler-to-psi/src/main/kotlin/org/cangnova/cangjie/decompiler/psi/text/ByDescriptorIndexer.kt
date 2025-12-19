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

package org.cangnova.cangjie.decompiler.psi.text

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.unwrapOptional
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

fun getQualifiedName(typeElement: CjTypeElement? ): String? {
    val referencedName = when (typeElement) {
        is CjUserType -> getQualifiedName(typeElement)
        is CjFunctionType -> {
            var parametersCount = typeElement.parameters.size
            typeElement.receiverTypeReference?.let { parametersCount++ }

                StandardNames.getFunctionClassId(parametersCount).asFqNameString()

        }
        is CjOptionType -> getQualifiedName(typeElement.unwrapOptional())
        else -> null
    }
    return referencedName
}

private fun getQualifiedName(userType: CjUserType): String? {
    val qualifier = userType.qualifier ?: return userType.referencedName
    return getQualifiedName(qualifier) + "." + userType.referencedName
}

fun CjElementImplStub<*>.getAllModifierLists(): Array<out CjDeclarationModifierList> =

    getStubOrPsiChildren(CjStubElementTypes.MODIFIER_LIST, CjStubElementTypes.MODIFIER_LIST.arrayFactory)
