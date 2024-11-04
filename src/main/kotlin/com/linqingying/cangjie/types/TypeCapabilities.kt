package com.linqingying.cangjie.types


// To facilitate laziness, any CangJieType implementation may inherit from this trait,
// even if it turns out that the type an instance represents is not actually a type parameter
// (i.e. it is not derived from a type parameter), see isTypeParameter
interface CustomTypeParameter {
    val isTypeParameter: Boolean

    // Throws an exception when isTypeParameter == false
    fun substitutionResult(replacement: CangJieType): CangJieType
}
// That interface is needed to provide information about definitely not null

//   type parameters (e.g. from @NotNull annotation) to type system
interface NotNullTypeParameter : CustomTypeParameter
fun CangJieType.getCustomTypeParameter(): CustomTypeParameter? =
    (unwrap() as? CustomTypeParameter)?.let {
        if (it.isTypeParameter) it else null
    }
fun sameTypeConstructors(first: CangJieType, second: CangJieType): Boolean {
    return (first.unwrap() as? SubtypingRepresentatives)?.sameTypeConstructor(second) ?: false
            || (second.unwrap() as? SubtypingRepresentatives)?.sameTypeConstructor(first) ?: false
}
fun CangJieType.getSubtypeRepresentative(): CangJieType =
    (unwrap() as? SubtypingRepresentatives)?.subTypeRepresentative ?: this
fun CangJieType.getSupertypeRepresentative(): CangJieType =
    (unwrap() as? SubtypingRepresentatives)?.superTypeRepresentative ?: this
fun CangJieType.isCustomTypeParameter(): Boolean = (unwrap() as? CustomTypeParameter)?.isTypeParameter ?: false
