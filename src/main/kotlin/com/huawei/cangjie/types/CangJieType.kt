package com.huawei.cangjie.types

import com.huawei.cangjie.types.model.TypeArgumentListMarker

sealed class CangJieType :  CangJieTypeMarker{

    abstract val constructor: TypeConstructor

}

sealed class UnwrappedType : CangJieType()
abstract class SimpleType : UnwrappedType(), SimpleTypeMarker, TypeArgumentListMarker