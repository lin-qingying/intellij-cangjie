package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.derivedFrom
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariable
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeProjection
import com.huawei.cangjie.types.TypeProjectionImpl
import java.util.*

fun ConstraintSystem.getNestedTypeVariables(type: CangJieType): List<TypeVariable> {
    val nestedTypeParameters = type.getNestedTypeParameters().toSet()
    return typeVariables.filter { it.originalTypeParameter in nestedTypeParameters }
}
fun ConstraintSystem.filterConstraintsOut(excludePositionKind: ConstraintPositionKind): ConstraintSystem {
    return toBuilder { !it.derivedFrom(excludePositionKind) }.build()
}
internal fun CangJieType.getNestedTypeParameters(): List<TypeParameterDescriptor> {
    return getNestedArguments().mapNotNull { typeProjection ->
        typeProjection.type.constructor.declarationDescriptor as? TypeParameterDescriptor
    }
}

internal fun CangJieType.getNestedArguments(): List<TypeProjection> {
    val result = ArrayList<TypeProjection>()

    val stack = ArrayDeque<TypeProjection>()
    stack.push(TypeProjectionImpl(this))

    while (!stack.isEmpty()) {
        val typeProjection = stack.pop()
        if (typeProjection.isStarProjection) continue

        result.add(typeProjection)

        typeProjection.type.arguments.forEach { stack.add(it) }
    }
    return result
}
