package com.huawei.cangjie.types.util

import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.types.*

fun CangJieType.replaceAnnotations(newAnnotations: Annotations): CangJieType {
    if (annotations.isEmpty() && newAnnotations.isEmpty()) return this
    return unwrap().replaceAttributes(attributes.replaceAnnotations(newAnnotations))
}
fun CangJieType.makeNullable() = TypeUtils.makeNullable(this)


fun UnwrappedType.unCapture(): UnwrappedType = when (this) {
    is AbbreviatedType -> unCapture()
    is SimpleType -> unCapture()
    is FlexibleType -> unCapture()
}

fun CangJieType.expandIntersectionTypeIfNecessary(): Collection<CangJieType> {
    if (constructor !is IntersectionTypeConstructor) return listOf(this)
    val types = constructor.supertypes
    return if (isMarkedNullable) {
        types.map { it.makeNullable() }
    } else {
        types
    }
}
