package com.huawei.cangjie.types


// To facilitate laziness, any KotlinType implementation may inherit from this trait,
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
