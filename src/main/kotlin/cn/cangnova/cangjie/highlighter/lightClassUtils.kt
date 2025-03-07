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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement


// Returns original declaration if given PsiElement is a CangJie light element, and element itself otherwise
val PsiElement.unwrapped: PsiElement?
    get() = when (this) {
//        is PsiElementWithOrigin<*> -> origin
        is CjLightElement<*, *> -> cangjieOrigin
//        is CjLightElementBase -> cangjieOrigin
        else -> this
    }
val PsiElement.namedUnwrappedElement: PsiNamedElement?
    get() = unwrapped?.getNonStrictParentOfType()
//
//fun CjElement.toLightElements(): List<PsiNamedElement> = when (this) {
//    is CjTypeStatement -> listOfNotNull(toLightClass())
//    is CjNamedFunction,
//    is CjConstructor<*> -> LightClassUtil.getLightClassMethods(this as CjFunction)
//    is CjProperty -> LightClassUtil.getLightClassPropertyMethods(this).allDeclarations
//    is CjPropertyAccessor -> listOfNotNull(LightClassUtil.getLightClassAccessorMethod(this))
//    is CjParameter -> mutableListOf<PsiNamedElement>().also { elements ->
//        toPsiParameters().toCollection(elements)
//        LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)
//        toAnnotationLightMethod()?.let(elements::add)
//    }
//
//    is CjTypeParameter -> toPsiTypeParameters()
//    is CjFile -> listOfNotNull(findFacadeClass())
//    else -> listOf()
//}
//
//fun CjElement.toLightElements(): List<PsiNamedElement> = when (this) {
//    is CjClassOrObject -> listOfNotNull(toLightClass())
//    is CjNamedFunction,
//    is CjConstructor<*> -> LightClassUtil.getLightClassMethods(this as CjFunction)
//    is CjProperty -> LightClassUtil.getLightClassPropertyMethods(this).allDeclarations
//    is CjPropertyAccessor -> listOfNotNull(LightClassUtil.getLightClassAccessorMethod(this))
//    is CjParameter -> mutableListOf<PsiNamedElement>().also { elements ->
//        toPsiParameters().toCollection(elements)
//        LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)
//        toAnnotationLightMethod()?.let(elements::add)
//    }
//
//    is CjTypeParameter -> toPsiTypeParameters()
//    is CjFile -> listOfNotNull(findFacadeClass())
//    else -> listOf()
//}
