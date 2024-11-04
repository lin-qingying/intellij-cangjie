/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("Attributes")

package com.linqingying.cangjie.metadata

import com.linqingying.cangjie.metadata.internal.*
import com.linqingying.cangjie.metadata.node.*
import kotlin.contracts.ExperimentalContracts
import com.linqingying.cangjie.metadata.deserialization.Flags as ProtoFlags

// --- ANNOTATIONS ---

/**
 * Indicates that the corresponding class has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a class has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
var CmClass.hasAnnotations: Boolean by annotationsOn(CmClass::flags)

/**
 * Indicates that the corresponding constructor has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a constructor has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
var CmConstructor.hasAnnotations: Boolean by annotationsOn(CmConstructor::flags)

/**
 * Indicates that the corresponding function has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a function has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
var CmFunction.hasAnnotations: Boolean by annotationsOn(CmFunction::flags)

/**
 * Indicates that the corresponding property has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a property has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
var CmProperty.hasAnnotations: Boolean by annotationsOn(CmProperty::flags)

/**
 * Indicates that the corresponding property accessor has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a property accessor has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
var CmPropertyAccessorAttributes.hasAnnotations: Boolean by annotationsOn(CmPropertyAccessorAttributes::flags)

/**
 * Indicates that the corresponding value parameter has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a value parameter has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
var CmValueParameter.hasAnnotations: Boolean by annotationsOn(CmValueParameter::flags)

/**
 * Indicates that the corresponding type alias has at least one annotation.
 *
 * Type aliases store their annotation in metadata directly (accessible via [CmTypeAlias.annotations]) and
 * in the class file at the same time.
 * As a result, Kotlin compiler still writes this flag for them, and this extension is left for completeness.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files and metadata.
 */
var CmTypeAlias.hasAnnotations: Boolean by annotationsOn(CmTypeAlias::flags)

// CmType and CmTypeParameter have annotations in it, and this flag for them is not written

// --- CLASS ---

/**
 * Represents modality of the corresponding class.
 *
 * Modality determines when and where it is possible to extend/implement a class/interface.
 */
var CmClass.modality: Modality by modalityDelegate(CmClass::flags)

/**
 * Represents visibility of the corresponding class.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
var CmClass.visibility: Visibility by visibilityDelegate(CmClass::flags)

/**
 * Represents kind of the corresponding class — whether it is a regular class or an interface, companion object, et cetera.
 */
var CmClass.kind: ClassKind by EnumFlagDelegate(
    CmClass::flags,
    ProtoFlags.CLASS_KIND,
    ClassKind.entries,
    ClassKind.entries.map { it.flag },
)

/**
 * Indicates that the corresponding class is `inner`.
 */
var CmClass.isInner: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_INNER))

/**
 * Indicates that the corresponding `class` or `object` is `data`.
 * Always false for other kinds.
 */
var CmClass.isData: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_DATA))

/**
 * Indicates that the corresponding class is `external`.
 */
var CmClass.isExternal: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_EXTERNAL_CLASS))

/**
 * Indicates that the corresponding class is `expect`.
 */
var CmClass.isExpect: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_EXPECT_CLASS))

/**
 * Indicates that the corresponding class is either a pre-Kotlin-1.5 `inline` class, or a 1.5+ `value` class.
 *
 * Note that it does not imply that the class has [JvmInline] annotation and will be inlined.
 * Currently, it is impossible to declare a value class without this annotation, but this can be changed in the future.
 */
var CmClass.isValue: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_VALUE_CLASS))

/**
 * Indicates that the corresponding class is a functional interface, i.e., marked with the keyword `fun`.
 *
 * Always `false` if [CmClass.kind] is not an interface.
 */
var CmClass.isFunInterface: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_FUN_INTERFACE))

/**
 * Indicates that the corresponding enum class has synthetic ".entries" property in bytecode.
 *
 * Always `false` if [CmClass.kind] is not an enum.
 * Enum classes always have enum entries property starting from Kotlin 1.9.0.
 */
var CmClass.hasEnumEntries: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.HAS_ENUM_ENTRIES))

// --- CONSTRUCTOR ---

/**
 * Represents visibility of the corresponding constructor.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
var CmConstructor.visibility: Visibility by visibilityDelegate(CmConstructor::flags)

/**
 * Indicates that the corresponding constructor is secondary, i.e., declared not in the class header, but in the class body.
 */
var CmConstructor.isSecondary: Boolean by constructorBooleanFlag(FlagImpl(ProtoFlags.IS_SECONDARY))

/**
 * Indicates that the corresponding constructor has non-stable parameter names, i.e., cannot be called with named arguments.
 *
 * Currently, this attribute is Kotlin/Native-specific and is never set by Kotlin/JVM compiler.
 * This may be changed in the future.
 */
var CmConstructor.hasNonStableParameterNames: Boolean by constructorBooleanFlag(FlagImpl(ProtoFlags.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES))

// --- FUNCTION ---

/**
 * Represents kind of the corresponding function.
 *
 * Kind indicates the origin of a declaration within a containing class. For details, see [MemberKind].
 */
var CmFunction.kind: MemberKind by memberKindDelegate(CmFunction::flags)

/**
 * Represents visibility of the corresponding function.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
var CmFunction.visibility: Visibility by visibilityDelegate(CmFunction::flags)

/**
 * Represents modality of the corresponding function.
 *
 * Modality determines when and where it is possible or mandatory to override a declaration.
 * For additional details, see [Modality].
 *
 * [Modality.SEALED] is not applicable for [CmFunction] and setting it as a value results in undefined behavior.
 */
var CmFunction.modality: Modality by modalityDelegate(CmFunction::flags)

/**
 * Indicates that the corresponding function is `operator`.
 */
var CmFunction.isOperator: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_OPERATOR))






// --- PROPERTY ---

/**
 * Represents visibility of the corresponding property.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
var CmProperty.visibility: Visibility by visibilityDelegate(CmProperty::flags)

/**
 * Represents modality of the corresponding property.
 *
 * Modality determines when and where it is possible or mandatory to override a declaration.
 * For additional details, see [Modality].
 *
 * [Modality.SEALED] is not applicable for [CmProperty] and setting it as a value results in undefined behavior.
 */
var CmProperty.modality: Modality by modalityDelegate(CmProperty::flags)

/**
 * Represents kind of the corresponding property.
 *
 * Kind indicates the origin of a declaration within a containing class. For details, see [MemberKind].
 */
var CmProperty.kind: MemberKind by memberKindDelegate(CmProperty::flags)

/**
 * Indicates that the corresponding property is `var`.
 *
 * Note that setting [CmProperty.isVar] to true does not automatically create [CmProperty.setter] and vice versa. This has to be done explicitly.
 */
var CmProperty.isVar: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_VAR))

/**
 * Indicates that the corresponding property is `const`.
 */
var CmProperty.isConst: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_CONST))

/**
 * Indicates that the corresponding property is `lateinit`.
 */
var CmProperty.isLateinit: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_LATEINIT))

/**
 * Indicates that the corresponding property has a constant value. On JVM, this flag allows an optimization similarly to
 * [CmProperty.hasAnnotations]: constant values of properties are written to the bytecode directly, and this flag can be used to avoid
 * reading the value from the bytecode in case it is not there.
 *
 * Not to be confused with [CmProperty.isConst], because `const` modifier is applicable only to properties on top-level and inside objects,
 * while property in a regular class can also have constant value.
 * Whether the property has a constant value is ultimately decided by the compiler and its optimizations.
 * Generally, a property initializer that can be computed in compile time is likely to have a constant value written to the bytecode.
 * In the following example, properties `a` and `b` have `hasConstant = true`, while `c` has `hasConstant = false`:
 *
 * ```
 * class X {
 *   val a = 1
 *   val b = a
 *
 *   fun x() = 2
 *   val c = x()
 * }
 * ```
 */
var CmProperty.hasConstant: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.HAS_CONSTANT))



// --- PROPERTY ACCESSOR ---

/**
 * Represents visibility of the corresponding property accessor.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
var CmPropertyAccessorAttributes.visibility: Visibility by visibilityDelegate(CmPropertyAccessorAttributes::flags)

/**
 * Represents modality of the corresponding property accessor.
 *
 * Modality determines when and where it is possible or mandatory to override a declaration.
 * For additional details, see [Modality].
 *
 * [Modality.SEALED] is not applicable for [CmPropertyAccessorAttributes] and setting it as a value results in undefined behavior.
 */
var CmPropertyAccessorAttributes.modality: Modality by modalityDelegate(CmPropertyAccessorAttributes::flags)

/**
 * Indicates that the corresponding property accessor is not default, i.e., it has a body and/or annotations in the source code,
 * or the property is delegated.
 */
var CmPropertyAccessorAttributes.isNotDefault: Boolean by propertyAccessorBooleanFlag(FlagImpl(ProtoFlags.IS_NOT_DEFAULT))

/**
 * Indicates that the corresponding property accessor is `external`.
 */
var CmPropertyAccessorAttributes.isExternal: Boolean by propertyAccessorBooleanFlag(FlagImpl(ProtoFlags.IS_EXTERNAL_ACCESSOR))

/**
 * Indicates that the corresponding property accessor is `inline`.
 */
var CmPropertyAccessorAttributes.isInline: Boolean by propertyAccessorBooleanFlag(FlagImpl(ProtoFlags.IS_INLINE_ACCESSOR))

// --- TYPE & TYPE_PARAM

/**
 * Indicates that the corresponding type is marked as nullable, i.e., has a question mark at the end of its notation.
 */
var CmType.isNullable: Boolean by typeBooleanFlag(FlagImpl(0, 1, 1))

/**
 * Indicates that the corresponding type is `suspend`.
 */
var CmType.isSuspend: Boolean by typeBooleanFlag(
    FlagImpl(
        ProtoFlags.SUSPEND_TYPE.offset + 1,
        ProtoFlags.SUSPEND_TYPE.bitWidth,
        1
    )
)

/**
 * Indicates that the corresponding type is [definitely non-null](https://kotlinlang.org/docs/whatsnew17.html#stable-definitely-non-nullable-types).
 */
var CmType.isDefinitelyNonNull: Boolean by typeBooleanFlag(
    FlagImpl(
        ProtoFlags.DEFINITELY_NOT_NULL_TYPE.offset + 1,
        ProtoFlags.DEFINITELY_NOT_NULL_TYPE.bitWidth,
        1
    )
)


/**
 * Indicates that the corresponding type parameter is `reified`.
 */
var CmTypeParameter.isReified: Boolean by BooleanFlagDelegate(CmTypeParameter::flags, FlagImpl(0, 1, 1))

// --- TYPE ALIAS ---

/**
 * Represents visibility of the corresponding type alias.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
var CmTypeAlias.visibility: Visibility by visibilityDelegate(CmTypeAlias::flags)


// --- VALUE PARAMETER ---


/**
 * Indicates that the corresponding value parameter declares a default value. Note that the default value itself can be a complex
 * expression and is not available via metadata. Also note that in case of an override of a parameter with default value, the
 * parameter in the derived method does _not_ declare the default value, but the parameter is
 * still optional at the call site because the default value from the base method is used.
 */
var CmValueParameter.declaresDefaultValue: Boolean by valueParameterBooleanFlag(FlagImpl(ProtoFlags.DECLARES_DEFAULT_VALUE))

/**
 * Indicates that the corresponding value parameter is `crossinline`.
 */
var CmValueParameter.isCrossinline: Boolean by valueParameterBooleanFlag(FlagImpl(ProtoFlags.IS_CROSSINLINE))

/**
 * Indicates that the corresponding value parameter is `noinline`.
 */
//var CmValueParameter.isNoinline: Boolean by valueParameterBooleanFlag(FlagImpl(ProtoFlags.IS_NOINLINE))

// --- EFFECT EXPRESSION ---

/**
 * Indicates that the corresponding effect expression should be negated to compute the proposition or the conclusion of an effect.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
var CmEffectExpression.isNegated: Boolean by BooleanFlagDelegate(
    CmEffectExpression::flags,
    FlagImpl(ProtoFlags.IS_NEGATED)
)

/**
 * Indicates that the corresponding effect expression checks whether a value of some variable is `null`.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
var CmEffectExpression.isNullCheckPredicate: Boolean by BooleanFlagDelegate(
    CmEffectExpression::flags,
    FlagImpl(ProtoFlags.IS_NULL_CHECK_PREDICATE)
)
