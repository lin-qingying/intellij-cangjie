package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.psi.CjBasicType
import com.huawei.cangjie.psi.CjProjectionKind
import com.huawei.cangjie.psi.CjUserType
import com.huawei.cangjie.psi.stubs.CangJieBasicTypeStub
import com.huawei.cangjie.psi.stubs.CangJieUserTypeStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.FlexibleTypeMarker
import com.huawei.cangjie.types.model.SimpleTypeMarker
import com.huawei.cangjie.types.model.TypeArgumentMarker

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
class CangJieBasicTypeStubImpl(
    parent: StubElement<out PsiElement>?,
    override val basicType: String
) : CangJieStubBaseImpl<CjBasicType>(parent, CjStubElementTypes.BASIC_TYPE), CangJieBasicTypeStub

class CangJieUserTypeStubImpl(
    parent: StubElement<out PsiElement>?, val upperBound: CangJieTypeBean? = null
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
) : CangJieTypeBean, SimpleTypeMarker

data class CangJieTypeArgumentBean(val projectionKind: CjProjectionKind, val type: CangJieTypeBean?) :
    TypeArgumentMarker


data class CangJieFlexibleTypeBean(val lowerBound: CangJieTypeBean, val upperBound: CangJieTypeBean) : CangJieTypeBean,
    FlexibleTypeMarker {
    override val nullable: Boolean
        get() = lowerBound.nullable
}
