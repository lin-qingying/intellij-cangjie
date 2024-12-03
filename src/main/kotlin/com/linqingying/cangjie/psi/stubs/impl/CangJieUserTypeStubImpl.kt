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

package com.linqingying.cangjie.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.CjBasicType
import com.linqingying.cangjie.psi.CjProjectionKind
import com.linqingying.cangjie.psi.CjUserType
import com.linqingying.cangjie.psi.stubs.CangJieBasicTypeStub
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.types.model.CangJieTypeMarker
import com.linqingying.cangjie.types.model.FlexibleTypeMarker
import com.linqingying.cangjie.types.model.SimpleTypeMarker
import com.linqingying.cangjie.types.model.TypeArgumentMarker

//class CangJieBasicTypeStubImpl(
//    parent: StubElement<out PsiElement>?,
//    override val basicType: String
//) : CangJieStubBaseImpl<CjBasicType>(parent, CjStubElementTypes.BASIC_TYPE), CangJieBasicTypeStub

class CangJieUserTypeStubImpl(

    parent: StubElement<out PsiElement>?,
    val upperBound: CangJieTypeBean? = null,
    val abbreviatedType: CangJieClassTypeBean? = null,
) : CangJieStubBaseImpl<CjUserType>(parent, CjStubElementTypes.USER_TYPE), CangJieUserTypeStub

sealed interface CangJieTypeBean : CangJieTypeMarker {
    val nullable: Boolean
}

data class CangJieTypeParameterTypeBean(
    val typeParameterName: String,
    override val nullable: Boolean,
    val definitelyNotNull: Boolean
) : CangJieTypeBean, SimpleTypeMarker

data class CangJieClassTypeBean(
    val classId: ClassId,
    val arguments: List<CangJieTypeArgumentBean>,
    override val nullable: Boolean,
    val abbreviatedType: CangJieClassTypeBean?,

    ) : CangJieTypeBean, SimpleTypeMarker

data class CangJieTypeArgumentBean(val projectionKind: CjProjectionKind, val type: CangJieTypeBean?) :
    TypeArgumentMarker


data class CangJieFlexibleTypeBean(val lowerBound: CangJieTypeBean, val upperBound: CangJieTypeBean) : CangJieTypeBean,
    FlexibleTypeMarker {
    override val nullable: Boolean
        get() = lowerBound.nullable
}
