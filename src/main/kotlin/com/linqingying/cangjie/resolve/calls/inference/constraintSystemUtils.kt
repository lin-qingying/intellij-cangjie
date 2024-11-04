package com.linqingying.cangjie.resolve.calls.inference

import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import com.linqingying.cangjie.resolve.calls.inference.constraintPosition.derivedFrom
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariable
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeProjection
import com.linqingying.cangjie.types.TypeProjectionImpl
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


        result.add(typeProjection)

        typeProjection.type.arguments.forEach { stack.add(it) }
    }
    return result
}
