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

class CangJieBasicTypeStubImpl(
    parent: StubElement<out PsiElement>?,
    override val basicType: String
) : CangJieStubBaseImpl<CjBasicType>(parent, CjStubElementTypes.BASIC_TYPE), CangJieBasicTypeStub

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
