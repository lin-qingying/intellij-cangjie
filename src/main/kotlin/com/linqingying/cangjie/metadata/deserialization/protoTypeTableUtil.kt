
package com.linqingying.cangjie.metadata.deserialization

import com.linqingying.cangjie.metadata.ProtoBuf


// TODO: return null and report a diagnostic instead of throwing exceptions

fun ProtoBuf.Class.supertypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    supertypeList.takeIf(Collection<*>::isNotEmpty) ?: supertypeIdList.map { typeTable[it] }

fun ProtoBuf.Class.inlineClassUnderlyingType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasInlineClassUnderlyingType() -> inlineClassUnderlyingType
    hasInlineClassUnderlyingTypeId() -> typeTable[inlineClassUnderlyingTypeId]
    else -> null
}

fun ProtoBuf.Type.Argument.type(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasType() -> type
    hasTypeId() -> typeTable[typeId]
    else -> null
}

fun ProtoBuf.Type.flexibleUpperBound(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasFlexibleUpperBound() -> flexibleUpperBound
    hasFlexibleUpperBoundId() -> typeTable[flexibleUpperBoundId]
    else -> null
}

fun ProtoBuf.TypeParameter.upperBounds(typeTable: TypeTable): List<ProtoBuf.Type> =
    upperBoundList.takeIf(Collection<*>::isNotEmpty) ?: upperBoundIdList.map { typeTable[it] }

fun ProtoBuf.Function.returnType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasReturnType() -> returnType
    hasReturnTypeId() -> typeTable[returnTypeId]
    else -> error("No returnType in ProtoBuf.Function")
}

fun ProtoBuf.Function.hasReceiver(): Boolean = hasReceiverType() || hasReceiverTypeId()

fun ProtoBuf.Function.receiverType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasReceiverType() -> receiverType
    hasReceiverTypeId() -> typeTable[receiverTypeId]
    else -> null
}
fun ProtoBuf.Variable.returnType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasReturnType() -> returnType
    hasReturnTypeId() -> typeTable[returnTypeId]
    else -> error("No returnType in ProtoBuf.Property")
}

fun ProtoBuf.Property.returnType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasReturnType() -> returnType
    hasReturnTypeId() -> typeTable[returnTypeId]
    else -> error("No returnType in ProtoBuf.Property")
}

fun ProtoBuf.Property.hasReceiver(): Boolean = hasReceiverType() || hasReceiverTypeId()
fun ProtoBuf.Variable.receiverType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasReceiverType() -> receiverType
    hasReceiverTypeId() -> typeTable[receiverTypeId]
    else -> null
}
fun ProtoBuf.Property.receiverType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasReceiverType() -> receiverType
    hasReceiverTypeId() -> typeTable[receiverTypeId]
    else -> null
}

fun ProtoBuf.ValueParameter.type(typeTable: TypeTable): ProtoBuf.Type = when {
    hasType() -> type
    hasTypeId() -> typeTable[typeId]
    else -> error("No type in ProtoBuf.ValueParameter")
}

fun ProtoBuf.ValueParameter.varargElementType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasVarargElementType() -> varargElementType
    hasVarargElementTypeId() -> typeTable[varargElementTypeId]
    else -> null
}

fun ProtoBuf.Type.outerType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasOuterType() -> outerType
    hasOuterTypeId() -> typeTable[outerTypeId]
    else -> null
}

fun ProtoBuf.Type.abbreviatedType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasAbbreviatedType() -> abbreviatedType
    hasAbbreviatedTypeId() -> typeTable[abbreviatedTypeId]
    else -> null
}

fun ProtoBuf.TypeAlias.underlyingType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasUnderlyingType() -> underlyingType
    hasUnderlyingTypeId() -> typeTable[underlyingTypeId]
    else -> error("No underlyingType in ProtoBuf.TypeAlias")
}

fun ProtoBuf.TypeAlias.expandedType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasExpandedType() -> expandedType
    hasExpandedTypeId() -> typeTable[expandedTypeId]
    else -> error("No expandedType in ProtoBuf.TypeAlias")
}

fun ProtoBuf.Expression.isInstanceType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasIsInstanceType() -> isInstanceType
    hasIsInstanceTypeId() -> typeTable[isInstanceTypeId]
    else -> null
}

fun ProtoBuf.Class.contextReceiverTypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    contextReceiverTypeList.takeIf(Collection<*>::isNotEmpty) ?: contextReceiverTypeIdList.map { typeTable[it] }

fun ProtoBuf.Function.contextReceiverTypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    contextReceiverTypeList.takeIf(Collection<*>::isNotEmpty) ?: contextReceiverTypeIdList.map { typeTable[it] }
fun ProtoBuf.Variable.contextReceiverTypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    contextReceiverTypeList.takeIf(Collection<*>::isNotEmpty) ?: contextReceiverTypeIdList.map { typeTable[it] }

fun ProtoBuf.Property.contextReceiverTypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    contextReceiverTypeList.takeIf(Collection<*>::isNotEmpty) ?: contextReceiverTypeIdList.map { typeTable[it] }
