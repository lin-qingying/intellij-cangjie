/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.types.model

/**
 * 仓颉类型系统上下文扩展方法
 *
 * 这个文件提供了便捷的扩展方法，用于在 context receiver 函数中调用 TypeSystemContext 的方法。
 * 这些扩展方法会自动将调用委托给 TypeSystemContext。
 *
 * 参考 Kotlin 的 TypeSystemContextContextual.kt 实现
 */

private const val USELESS_CALL_MESSAGE = "This call does effectively nothing, please drop it"

// ============================================
// CangJieTypeMarker 扩展方法
// ============================================

context(c: TypeSystemContext)
fun CangJieTypeMarker.asSimpleType(): SimpleTypeMarker? = with(c) { asSimpleType() }
context(c: TypeSystemContext)
fun SimpleTypeMarker.asCapturedTypeUnwrappingDnn(): CapturedTypeMarker? = with(c) { asCapturedTypeUnwrappingDnn() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun SimpleTypeMarker.asSimpleType(): SimpleTypeMarker = this

context(c: TypeSystemContext)
fun CangJieTypeMarker.asFlexibleType(): FlexibleTypeMarker? = with(c) { asFlexibleType() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun FlexibleTypeMarker.asFlexibleType(): FlexibleTypeMarker = this

context(c: TypeSystemContext)
fun CangJieTypeMarker.isError(): Boolean = with(c) { isError() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isError(): Boolean = with(c) { isError() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isUninferredParameter(): Boolean = with(c) { isUninferredParameter() }

context(c: TypeSystemContext)
fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? = with(c) { asDynamicType() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun DynamicTypeMarker.asDynamicType(): DynamicTypeMarker = this

context(c: TypeSystemContext)
fun CangJieTypeMarker.isRawType(): Boolean = with(c) { isRawType() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isDynamic(): Boolean = with(c) { isDynamic() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isCapturedType() = with(c) { isCapturedType() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isNonOptionTypeParameter(): Boolean = with(c) { isNonOptionTypeParameter() }

// ============================================
// FlexibleTypeMarker 扩展方法
// ============================================

context(c: TypeSystemContext)
fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker = with(c) { upperBound() }

context(c: TypeSystemContext)
fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker = with(c) { lowerBound() }

// ============================================
// SimpleTypeMarker 扩展方法
// ============================================

context(c: TypeSystemContext)
fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? = with(c) { asCapturedType() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun CapturedTypeMarker.asCapturedType(): CapturedTypeMarker = this


context(c: TypeSystemContext)
fun CangJieTypeMarker.isMarkedOption(): Boolean = with(c) { isMarkedOption() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isMarkedOption(): Boolean = with(c) { isMarkedOption() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.withOption(isOption: Boolean): SimpleTypeMarker = with(c) { withOption(isOption) }

context(c: TypeSystemContext)
fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker = with(c) { typeConstructor() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.withOption(isOption: Boolean): CangJieTypeMarker = with(c) { withOption(isOption) }

// ============================================
// CapturedTypeMarker 扩展方法
// ============================================

context(c: TypeSystemContext)
fun CapturedTypeMarker.isOldCapturedType(): Boolean = with(c) { isOldCapturedType() }

context(c: TypeSystemContext)
fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker = with(c) { typeConstructor() }

context(c: TypeSystemContext)
fun CapturedTypeMarker.captureStatus(): CaptureStatus = with(c) { captureStatus() }

context(c: TypeSystemContext)
fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker = with(c) { projection() }

context(c: TypeSystemContext)
fun CapturedTypeMarker.lowerType(): CangJieTypeMarker? = with(c) { lowerType() }

// ============================================
// 类型参数和参数列表扩展方法
// ============================================

context(c: TypeSystemContext)
fun CangJieTypeMarker.argumentsCount(): Int = with(c) { argumentsCount() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.getArgument(index: Int): TypeArgumentMarker = with(c) { getArgument(index) }

context(c: TypeSystemContext)
fun CangJieTypeMarker.getArguments(): List<TypeArgumentMarker> = with(c) { getArguments() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? = with(c) { getArgumentOrNull(index) }

context(c: TypeSystemContext)
fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker = with(c) { asTypeArgument() }

context(c: TypeSystemContext)
fun TypeArgumentMarker.getType(): CangJieTypeMarker = with(c) { getType() }

context(c: TypeSystemContext)
fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker = with(c) { replaceType(newType) }

// ============================================
// Stub 类型扩展方法
// ============================================

context(c: TypeSystemContext)
fun SimpleTypeMarker.isStubType(): Boolean = with(c) { isStubType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isStubTypeForVariableInSubtyping(): Boolean = with(c) { isStubTypeForVariableInSubtyping() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isStubTypeForBuilderInference(): Boolean = with(c) { isStubTypeForBuilderInference() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker = with(c) { unwrapStubTypeVariableConstructor() }

// ============================================
// TypeConstructorMarker 扩展方法
// ============================================

context(c: TypeSystemContext)
fun TypeConstructorMarker.parametersCount(): Int = with(c) { parametersCount() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker = with(c) { getParameter(index) }

context(c: TypeSystemContext)
fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> = with(c) { getParameters() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker> = with(c) { supertypes() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntersection(): Boolean = with(c) { isIntersection() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isClassTypeConstructor(): Boolean = with(c) { isClassTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isInterface(): Boolean = with(c) { isInterface() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean = with(c) { isIntegerLiteralTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean = with(c) { isIntegerLiteralConstantTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean = with(c) { isIntegerConstantOperatorTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isFloatLiteralTypeConstructor(): Boolean = with(c) { isFloatLiteralTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isLocalType(): Boolean = with(c) { isLocalType() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isAnonymous(): Boolean = with(c) { isAnonymous() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? = with(c) { getTypeParameterClassifier() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean = with(c) { isTypeParameterTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isFunctionTypeConstructor(): Boolean = with(c) { isFunctionTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isTupleTypeConstructor(): Boolean = with(c) { isTupleTypeConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isAnyConstructor(): Boolean = with(c) { isAnyConstructor() }
context(c: TypeSystemContext)
fun CangJieTypeMarker.isAny() = with(c) { isAny() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isNothingConstructor(): Boolean = with(c) { isNothingConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isArrayConstructor(): Boolean = with(c) { isArrayConstructor() }

context(c: TypeSystemContext)
fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean = with(c) { isCommonFinalClassConstructor() }

// ============================================
// TypeParameterMarker 扩展方法
// ============================================

context(c: TypeSystemContext)
val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
    get() = with(c) { typeParameter }

context(c: TypeSystemContext)
fun TypeParameterMarker.upperBoundCount(): Int = with(c) { upperBoundCount() }

context(c: TypeSystemContext)
fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker = with(c) { getUpperBound(index) }

context(c: TypeSystemContext)
fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker> = with(c) { getUpperBounds() }

context(c: TypeSystemContext)
fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker = with(c) { getTypeConstructor() }

context(c: TypeSystemContext)
fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean =
    with(c) { hasRecursiveBounds(selfConstructor) }

// ============================================
// 灵活类型边界扩展方法
// ============================================

context(c: TypeSystemContext)
fun CangJieTypeMarker.lowerBoundIfFlexible(): SimpleTypeMarker = with(c) { lowerBoundIfFlexible() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.upperBoundIfFlexible(): SimpleTypeMarker = with(c) { upperBoundIfFlexible() }

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun SimpleTypeMarker.lowerBoundIfFlexible(): SimpleTypeMarker = this

@Deprecated(level = DeprecationLevel.ERROR, message = USELESS_CALL_MESSAGE)
context(_: TypeSystemContext)
fun SimpleTypeMarker.upperBoundIfFlexible(): SimpleTypeMarker = this

context(c: TypeSystemContext)
fun CangJieTypeMarker.isFlexible(): Boolean = with(c) { isFlexible() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.hasFlexibleOption() = with(c) { hasFlexibleOption() }

// ============================================
// 类型判断扩展方法
// ============================================

context(c: TypeSystemContext)
fun CangJieTypeMarker.typeConstructor(): TypeConstructorMarker = with(c) { typeConstructor() }
context(c: TypeSystemContext)
fun TypeConstructorMarker.isDenotable(): Boolean = with(c) { isDenotable() }
context(c: TypeSystemContext)
fun CangJieTypeMarker.isFlexibleWithDifferentTypeConstructors(): Boolean = with(c) { isFlexibleWithDifferentTypeConstructors() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isOptionAny() = with(c) { isOptionAny() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isNothing() = with(c) { isNothing() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isFlexibleNothing() = with(c) { isFlexibleNothing() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isOptionNothing() = with(c) { isOptionNothing() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isOptionType(): Boolean = with(c) { isOptionType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isClassType(): Boolean = with(c) { isClassType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? =
    with(c) { fastCorrespondingSupertypes(constructor) }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isIntegerLiteralType(): Boolean = with(c) { isIntegerLiteralType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isFloatLiteralType(): Boolean = with(c) { isFloatLiteralType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isFunctionType(): Boolean = with(c) { isFunctionType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isTupleType(): Boolean = with(c) { isTupleType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker> = with(c) { possibleIntegerTypes() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.possibleFloatTypes(): Collection<CangJieTypeMarker> = with(c) { possibleFloatTypes() }

// ============================================
// TypeArgumentListMarker 扩展方法
// ============================================

context(c: TypeSystemContext)
fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker = with(c) { asArgumentList() }

context(c: TypeSystemContext)
operator fun TypeArgumentListMarker.get(index: Int) = with(c) { get(index) }

context(c: TypeSystemContext)
fun TypeArgumentListMarker.size(): Int = with(c) { size() }

context(c: TypeSystemContext)
operator fun TypeArgumentListMarker.iterator() = with(c) { iterator() }

// ============================================
// 其他扩展方法
// ============================================

context(c: TypeSystemContext)
fun CangJieTypeMarker.isSingleClassifierType(): Boolean = with(c) { asSimpleType()?.isSingleClassifierType() == true }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isSingleClassifierType(): Boolean = with(c) { isSingleClassifierType() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isSimpleType(): Boolean = with(c) { isSimpleType() }

context(c: TypeSystemContext)
fun SimpleTypeMarker.isPrimitiveType(): Boolean = with(c) { isPrimitiveType() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker> = with(c) { getAttributes() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.getCustomAttributes(): List<AnnotationMarker> = with(c) { getCustomAttributes() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.hasCustomAttributes(): Boolean = with(c) { hasCustomAttributes() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isTypeVariableType(): Boolean = with(c) { isTypeVariableType() }

context(c: TypeSystemContext)
fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker = with(c) { safeSubstitute(type) }

context(c: TypeSystemContext)
fun CangJieTypeMarker.isCapturedDynamic(): Boolean = with(c) { isCapturedDynamic() }

context(c: TypeSystemContext)
fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker> =
    with(c) { extractArgumentsForFunctionTypeOrSubtype() }

context(c: TypeSystemContext)
fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean =
    with(c) { areEqualTypeConstructors(c1, c2) }
