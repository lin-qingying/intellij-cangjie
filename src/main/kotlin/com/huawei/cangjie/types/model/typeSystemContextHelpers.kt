package com.huawei.cangjie.types.model

fun CangJieTypeMarker.typeConstructor(context: TypeSystemContext): TypeConstructorMarker =
    with(context) { typeConstructor() }

fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(context: TypeSystemContext): Boolean =
    with(context) { isIntegerLiteralTypeConstructor() }
