package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.name.ClassId
import com.huawei.cangjie1.psi.CjProjectionKind
import com.huawei.cangjie1.psi.CjUserType
import com.huawei.cangjie1.psi.stubs.CangJieUserTypeStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie1.types.CangJieTypeMarker
import com.huawei.cangjie1.types.FlexibleTypeMarker
import com.huawei.cangjie1.types.SimpleTypeMarker
import com.huawei.cangjie1.types.TypeArgumentMarker
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


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
