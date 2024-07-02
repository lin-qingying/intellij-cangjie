package com.huawei.cangjie.types.model

import com.huawei.cangjie.types.TypeCheckerState

interface SimpleTypeMarker : CangJieTypeMarker
interface TypeArgumentMarker
interface FlexibleTypeMarker : CangJieTypeMarker
interface TypeVariableMarker

interface CangJieTypeMarker
interface CapturedTypeConstructorMarker : TypeConstructorMarker
interface CapturedTypeMarker : SimpleTypeMarker

interface DynamicTypeMarker : FlexibleTypeMarker
interface TypeSubstitutorMarker
interface TypeConstructorMarker
interface TypeParameterMarker
interface TypeArgumentListMarker

interface TypeSystemOptimizationContext {
    /**
     *  @return true is a.arguments == b.arguments, or false if not supported
     */
    fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker) = false
}

/**
 * Defines common kotlin type operations with types for abstract types
 */
interface TypeSystemContext : TypeSystemOptimizationContext {
    /**
     * @returns substituted type or [type] if there were no substitution
     */
    fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker
    fun CangJieTypeMarker.asSimpleType(): SimpleTypeMarker?
    fun CangJieTypeMarker.asFlexibleType(): FlexibleTypeMarker?
    fun CangJieTypeMarker.lowerBoundIfFlexible(): SimpleTypeMarker =
        this.asFlexibleType()?.lowerBound() ?: this.asSimpleType()!!

    fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker
    fun CangJieTypeMarker.upperBoundIfFlexible(): SimpleTypeMarker =
        this.asFlexibleType()?.upperBound() ?: this.asSimpleType()!!

    fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker
    fun createEmptySubstitutor(): TypeSubstitutorMarker
    fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker
    fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker
    fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker
    fun TypeVariableMarker.defaultType(): SimpleTypeMarker

}

interface IntersectionTypeConstructorMarker : TypeConstructorMarker

/**
 * Context that allow type-impl agnostic access to common types
 */
interface TypeSystemBuiltInsContext {
    fun nullableNothingType(): SimpleTypeMarker
    fun nullableAnyType(): SimpleTypeMarker
    fun nothingType(): SimpleTypeMarker
    fun anyType(): SimpleTypeMarker

}
enum class CaptureStatus {
    FOR_SUBTYPING,
    FOR_INCORPORATION,
    FROM_EXPRESSION
}
interface TypeVariableTypeConstructorMarker : TypeConstructorMarker

// This interface is only used to declare that implementing class is supposed to be used as a TypeSystemInferenceExtensionContext component
// Otherwise clash happens during DI container initialization: there are a lot of components that extend TypeSystemInferenceExtensionContext
// but they only has it among supertypes to bring additional receiver into their scopes, i.e. they are not intended to be used as
// component implementation for TypeSystemInferenceExtensionContext
interface TypeSystemInferenceExtensionContextDelegate : TypeSystemInferenceExtensionContext

/**
 * Factory, that constructs [TypeCheckerState], which defines type-checker behaviour
 * Implementation is recommended to be [TypeSystemContext]
 */
interface TypeCheckerProviderContext {
    fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState
}

/**
 * Context that allow construction of types
 */
interface TypeSystemTypeFactoryContext : TypeSystemBuiltInsContext

/**
 * Extended type system context, which defines set of operations specific to common super-type calculation
 */
interface TypeSystemCommonSuperTypesContext : TypeSystemContext, TypeSystemTypeFactoryContext,
    TypeCheckerProviderContext

interface TypeSystemInferenceExtensionContext : TypeSystemContext, TypeSystemBuiltInsContext,
    TypeSystemCommonSuperTypesContext {

    fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean

}
