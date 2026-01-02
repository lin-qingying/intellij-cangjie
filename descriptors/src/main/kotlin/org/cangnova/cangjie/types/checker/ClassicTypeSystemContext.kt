/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types.checker

import com.intellij.util.containers.addIfNotNull
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames.FqNames
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import org.cangnova.cangjie.descriptors.isFinalClass
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.call.inference.CapturedType
import org.cangnova.cangjie.resolve.classId
import org.cangnova.cangjie.resolve.constants.FloatLiteralTypeConstructor
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.functions.FunctionTypeKind
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.firstIsInstanceOrNull
import org.cangnova.cangjie.types.isSignedOrUnsignedNumberType as classicIsSignedOrUnsignedNumberType
import org.cangnova.cangjie.types.isStubType as isSimpleTypeStubType
import org.cangnova.cangjie.types.isStubTypeForBuilderInference as isSimpleTypeStubTypeForBuilderInference
import org.cangnova.cangjie.types.isStubTypeForVariableInSubtyping as isSimpleTypeStubTypeForVariableInSubtyping

/**
 * 生成错误消息的辅助函数
 *
 * 用于在类型系统上下文无法处理特定对象时生成详细的错误信息
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun Any.errorMessage(): String {
    return "ClassicTypeSystemContext couldn't handle: $this, ${this::class}"
}

/**
 * 抛出"仅在类型推断上下文中支持"的错误
 */
private fun errorSupportedOnlyInTypeInference(): Nothing {
    error("supported only in type inference context")
}

/**
 * 经典类型系统上下文
 *
 * 这是仓颉类型系统的核心接口，提供了类型检查、类型推断和类型操作所需的所有功能。
 * 它实现了类型系统推断扩展上下文和通用后端上下文，支持：
 *
 * 主要功能：
 * - 类型构造器的检查和比较
 * - 类型参数和类型投影的处理
 * - 子类型关系的判定
 * - 类型属性和注解的处理
 * - 捕获类型和 stub 类型的支持
 * - 函数类型和可选类型的特殊处理
 *
 * 此接口是类型检查和类型推断系统的基础，所有类型相关的操作都通过它来完成。
 */
interface ClassicTypeSystemContext : TypeSystemInferenceExtensionContext, TypeSystemCommonBackendContext {

    /**
     * 内置类型集合
     *
     * 提供对仓颉内置类型（如 Int、String、Any 等）的访问
     * 默认实现抛出异常，子类需要提供具体实现
     */
    val builtIns: CangJieBuiltIns
        get() = throw UnsupportedOperationException("Not supported")

    /**
     * 检查类型构造器是否为浮点字面量类型构造器
     *
     * @return 如果是浮点字面量类型构造器则返回 true
     */
    override fun TypeConstructorMarker.isFloatLiteralTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is FloatLiteralTypeConstructor
    }

    /**
     * 检查类型构造器是否为整数字面量类型构造器
     *
     * @return 如果是整数字面量类型构造器则返回 true
     */
    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is IntegerLiteralTypeConstructor
    }

    /**
     * 检查类型构造器是否为函数类型构造器
     *
     * 用于实现函数类型的特殊子类型检查（参数逆变，返回值协变）
     * 参考编译器: TypeManager.cpp:870-900 IsFuncSubtype
     *
     * @return 如果是函数类型构造器则返回 true
     */
    override fun TypeConstructorMarker.isFunctionTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        // FunctionClassDescriptor.FunctionTypeConstructor
        return this.declarationDescriptor is org.cangnova.cangjie.descriptors.impl.FunctionClassDescriptor
    }

    /**
     * 检查类型构造器是否为元组类型构造器
     *
     * 用于实现元组类型的严格不变子类型检查（implicitBoxed = false）
     * 参考编译器: TypeManager.cpp:902-913 IsTupleSubtype
     *
     * @return 如果是元组类型构造器则返回 true
     */
    override fun TypeConstructorMarker.isTupleTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        // TupleClassDescriptor.TupleTypeConstructor
        return this.declarationDescriptor is org.cangnova.cangjie.descriptors.impl.TupleClassDescriptor
    }

    /**
     * 提取函数类型或其子类型的类型参数
     *
     * 对于函数类型 (A, B) -> C，返回 [A, B, C]
     *
     * @return 函数类型的参数类型列表
     */
    override fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker> {
        require(this is CangJieType, this::errorMessage)
        return this.getPureArgumentsForFunctionalTypeOrSubtype()
    }

    /**
     * 获取类型的自定义属性
     *
     * 排除注解类型属性，只返回其他自定义属性
     *
     * @return 自定义属性列表
     */
    override fun CangJieTypeMarker.getCustomAttributes(): List<AnnotationMarker> {
        require(this is CangJieType, this::errorMessage)
        return this.attributes.filterNot { it is AnnotationsTypeAttribute }
    }

    /**
     * 检查类型是否有自定义属性
     *
     * @return 如果有自定义属性则返回 true
     */
    override fun CangJieTypeMarker.hasCustomAttributes(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return !this.attributes.isEmpty() && this.getCustomAttributes().size > 0
    }

    /**
     * 为交集结果创建带上界的类型
     *
     * 当计算类型交集时，如果需要创建新的类型来表示结果，使用此方法
     *
     * @param firstCandidate 第一个候选类型
     * @param secondCandidate 第二个候选类型
     * @return 带上界的新类型
     */
    override fun createTypeWithUpperBoundForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker,
    ): CangJieTypeMarker {
        require(firstCandidate is CangJieType, this::errorMessage)
        require(secondCandidate is CangJieType, this::errorMessage)

        (firstCandidate.constructor as? IntersectionTypeConstructor)?.let { intersectionConstructor ->
            val intersectionTypeWithAlternative = intersectionConstructor.setAlternative(secondCandidate).createType()
            return if (firstCandidate.isOption) intersectionTypeWithAlternative.makeOptionAsSpecified(true)
            else intersectionTypeWithAlternative

        } ?: error("Expected intersection type, found $firstCandidate")
    }

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean {
        return isIntegerLiteralTypeConstructor()
    }


    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.isLocalType(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor?.classId?.isLocal == true
    }

    override fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker? {
        return captureFromExpressionInternal(type as UnwrappedType)
    }

    override fun TypeConstructorMarker.isAnonymous(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor?.classId?.shortClassName == SpecialNames.ANONYMOUS
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() {
            require(this is NewTypeVariableConstructor, this::errorMessage)
            return this.originalTypeParameter
        }

    override fun SimpleTypeMarker.possibleFloatTypes(): Collection<CangJieTypeMarker> {
        val typeConstructor = typeConstructor()
        require(typeConstructor is FloatLiteralTypeConstructor, this::errorMessage)
        return typeConstructor.possibleTypes
    }

    override fun SimpleTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker> {
        val typeConstructor = typeConstructor()
        require(typeConstructor is IntegerLiteralTypeConstructor, this::errorMessage)
        return typeConstructor.possibleTypes
    }

    override fun SimpleTypeMarker.withOption(isOption: Boolean): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return this.makeOptionAsSpecified(isOption)
    }

    override fun CangJieTypeMarker.isError(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return this.isError
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        require(this is TypeConstructor && ErrorUtils.isError(declarationDescriptor), this::errorMessage)
        return ErrorUtils.createErrorType(ErrorTypeKind.RESOLUTION_ERROR_TYPE, "from type constructor $this")
    }

    override fun CangJieTypeMarker.isUninferredParameter(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return ErrorUtils.isUninferredTypeVariable(this)
    }

    override fun SimpleTypeMarker.isStubType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubType()
    }

    override fun SimpleTypeMarker.isStubTypeForVariableInSubtyping(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubTypeForVariableInSubtyping()
    }

    override fun SimpleTypeMarker.isStubTypeForBuilderInference(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubTypeForBuilderInference()
    }

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker {
        return this
    }

    override fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker {
        require(this is AbstractStubType, this::errorMessage)
        return this.originalTypeVariable as TypeVariableTypeConstructorMarker
    }

    override fun CapturedTypeMarker.lowerType(): CangJieTypeMarker? {
        require(this is NewCapturedType, this::errorMessage)
        return this.lowerType
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is IntersectionTypeConstructor
    }

    override fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        require(a is SimpleType, a::errorMessage)
        require(b is SimpleType, b::errorMessage)
        return a.arguments === b.arguments
    }

    override fun CangJieTypeMarker.asSimpleType(): SimpleTypeMarker? {
        require(this is CangJieType, this::errorMessage)
        return this.unwrap() as? SimpleType
    }

    override fun CangJieTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        require(this is CangJieType, this::errorMessage)
        return this.unwrap() as? FlexibleType
    }

    override fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? {
        require(this is FlexibleType, this::errorMessage)
        return this as? DynamicType
    }

    override fun CangJieTypeMarker.isRawType(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return this is RawType
    }

    override fun CangJieTypeMarker.convertToNonRaw(): CangJieTypeMarker {
        error("Is not expected to be called in K1")
    }

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.upperBound
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.lowerBound
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        require(this is SimpleType, this::errorMessage)
        return if (this is SimpleTypeWithEnhancement) origin.asCapturedType() else this as? NewCapturedType
    }

    override fun SimpleTypeMarker.asDefinitelyNonOptionType(): DefinitelyNonOptionTypeMarker? {
        require(this is SimpleType, this::errorMessage)
        return this as? DefinitelyNonOptionType
    }

    @OptIn(ObsoleteTypeKind::class)
    override fun CangJieTypeMarker.isNonOptionTypeParameter(): Boolean = this is NonOptionTypeParameter

    override fun SimpleTypeMarker.isMarkedOption(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isOption
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker {
        require(this is SimpleType, this::errorMessage)
        return this.constructor
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker {
        require(this is NewCapturedType, this::errorMessage)
        return this.constructor
    }

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker {
        require(this is NewCapturedTypeConstructor, this::errorMessage)
        return this.projection
    }

    override fun CangJieTypeMarker.argumentsCount(): Int {
        require(this is CangJieType, this::errorMessage)
        return this.arguments.size
    }

    override fun CangJieTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        require(this is CangJieType, this::errorMessage)
        return this.arguments[index]
    }

    override fun CangJieTypeMarker.getArguments(): List<TypeArgumentMarker> {
        require(this is CangJieType, this::errorMessage)
        return this.arguments
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        require(this is TypeProjection, this::errorMessage)
        return this.projectionKind.convertVariance()
    }

    override fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker {
        require(this is TypeProjection, this::errorMessage)
        require(newType is CangJieType, this::errorMessage)
        return this.replaceType(newType)
    }

    override fun TypeArgumentMarker.getType(): CangJieTypeMarker {
        require(this is TypeProjection, this::errorMessage)
        return this.type.unwrap()
    }


    override fun TypeConstructorMarker.parametersCount(): Int {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters.size
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters[index]
    }

    override fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters
    }

    /**
     * 获取类型构造器的超类型集合
     *
     * ## 实现编译器的 implicitBoxed 机制
     *
     * 根据 cangjie_compiler/src/Sema/TypeManager.cpp:1004-1014，当 implicitBoxed=true（默认值）时，
     * 基本类型和内置类型可以作为 Any 的子类型，即使它们在定义时没有显式继承 Any。
     *
     * ### 处理逻辑：
     * 1. 获取类型构造器自身定义的超类型
     * 2. 如果超类型为空，且该类型是基本类型或内置类型（除了 Any 和 Nothing），则隐式添加 Any
     * 3. 这样实现了"默认实现 Any"的语义，与编译器保持一致
     *
     * ### 示例：
     * ```
     * Int32.supertypes()  // 返回 [Any]（隐式添加）
     * String.supertypes() // 返回 [Any]（隐式添加）
     * Any.supertypes()    // 返回 []（Any 没有超类型）
     * Nothing.supertypes() // 返回 []（Nothing 没有超类型，但它是所有类型的子类型）
     * ```
     */
    override fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker> {
        require(this is TypeConstructor, this::errorMessage)

        val explicitSupertypes = this.supertypes

        // 如果已经有显式的超类型，直接返回
        if (explicitSupertypes.isNotEmpty()) {
            return explicitSupertypes
        }

        // implicitBoxed 机制：为基本类型和内置类型隐式添加 Any 超类型
        val declarationDescriptor = this.declarationDescriptor

        // 检查是否是基本类型或内置类型（但不是 Any 和 Nothing）
        if (declarationDescriptor is ClassDescriptor) {
            val isAny = CangJieBuiltIns.isAny(declarationDescriptor)
            val isNothing = CangJieBuiltIns.isNothing(declarationDescriptor.defaultType)
            val isPrimitive = CangJieBuiltIns.isPrimitiveType(declarationDescriptor.defaultType)
            val isBuiltIn = CangJieBuiltIns.isBuiltIn(declarationDescriptor)

            // 如果是基本类型或内置类型，但不是 Any 和 Nothing，隐式添加 Any
            if ((isPrimitive || isBuiltIn) && !isAny && !isNothing) {
                return listOf(builtIns.stdlibTypes.anyType)
            }
        }

        return explicitSupertypes
    }


    override fun TypeParameterMarker.getVariance(): TypeVariance {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.variance.convertVariance()
    }

    override fun TypeParameterMarker.upperBoundCount(): Int {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds.size
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds[index]
    }

    override fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker> {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.typeConstructor
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        require(this is TypeParameterDescriptor, this::errorMessage)
        require(selfConstructor is TypeConstructor?, this::errorMessage)

        return hasTypeParameterRecursiveBounds(this, selfConstructor)
    }

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        require(c1 is TypeConstructor, c1::errorMessage)
        require(c2 is TypeConstructor, c2::errorMessage)
        return c1 == c2
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor is ClassDescriptor
    }

    override fun TypeConstructorMarker.isInterface(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return DescriptorUtils.isInterface(declarationDescriptor)
    }


    override fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is SimpleType, this::errorMessage)
        return this
    }

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is SimpleType, type::errorMessage)
        return org.cangnova.cangjie.types.checker.captureFromArguments(type, status)
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return isTypeConstructorForGivenClass(this, FqNames.anyUFqName)
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return isTypeConstructorForGivenClass(this, FqNames.nothingUFqName)
    }

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return isTypeConstructorForGivenClass(this, FqNames.arrayUFqName)
    }

    override fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is CangJieType, this::errorMessage)
        return this.asTypeProjection()
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return isTypeConstructorForGivenClass(this, FqNames.unitUFqName)
    }

    /**
     *
     * SingleClassifierType is one of the following types:
     *  - classType
     *  - type for type parameter
     *  - captured type
     *
     * Such types can contain error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return !isError &&
                constructor.declarationDescriptor !is TypeAliasDescriptor &&
                (constructor.declarationDescriptor != null || this is CapturedType || this is NewCapturedType || this is DefinitelyNonOptionType || constructor is IntegerLiteralTypeConstructor || isSingleClassifierTypeWithEnhancement())
    }

    private fun SimpleTypeMarker.isSingleClassifierTypeWithEnhancement() =
        this is SimpleTypeWithEnhancement && origin.isSingleClassifierType()

    override fun CangJieTypeMarker.contains(predicate: (CangJieTypeMarker) -> Boolean): Boolean {
        require(this is CangJieType, this::errorMessage)
        return containsInternal(this, predicate)
    }

    override fun TypeConstructorMarker.isFinalClassConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        val classDescriptor = declarationDescriptor as? ClassDescriptor ?: return false
        return classDescriptor.isFinalClass
    }

    //
    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is SimpleType, this::errorMessage)
        if (this is TypeUtils.SpecialType) return 0

        val maxInArguments = arguments.maxOfOrNull {
            it.type.unwrap().typeDepth()
        } ?: 0

        return maxInArguments + 1
    }

    override fun intersectTypes(types: List<CangJieTypeMarker>): CangJieTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return org.cangnova.cangjie.types.checker.intersectTypes(types as List<UnwrappedType>)
    }

    override fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return org.cangnova.cangjie.types.checker.intersectTypes(types as List<SimpleType>)
    }

    override fun Collection<CangJieTypeMarker>.singleBestRepresentative(): CangJieTypeMarker? {
        @Suppress("UNCHECKED_CAST")
        return singleBestRepresentative(this as Collection<CangJieType>)
    }

    override fun CangJieTypeMarker.isUnit(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return CangJieBuiltIns.isUnit(this)
    }

    override fun CangJieTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return isBuiltinFunctionalTypeOrSubtype
    }

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): CangJieTypeMarker {
        require(lowerBound is SimpleType, this::errorMessage)
        require(upperBound is SimpleType, this::errorMessage)
        return CangJieTypeFactory.flexibleType(lowerBound, upperBound)
    }

    override fun CangJieTypeMarker.withOption(isOption: Boolean): CangJieTypeMarker {
        return when (this) {
            is SimpleTypeMarker -> this.withOption(isOption)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().withOption(isOption),
                upperBound().withOption(isOption)
            )

            else -> error("sealed")
        }
    }


    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState {
        return createClassicTypeCheckerState(
            errorTypesEqualToAnything,
            stubTypesEqualToAnything,
            typeSystemContext = this
        )
    }

    override fun nothingType(): SimpleTypeMarker {
        return builtIns.nothingType
    }

    override fun anyType(): SimpleTypeMarker {
        return builtIns.stdlibTypes.anyType
    }


    override fun CangJieTypeMarker.makeDefinitelyNonOptionOrNonOption(): CangJieTypeMarker {
        require(this is UnwrappedType, this::errorMessage)
        return makeDefinitelyNonOptionOrNonOptionInternal(this)
    }


    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNonOptionOrNonOption(): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return makeSimpleTypeDefinitelyNonOptionOrNonOptionInternal(this)
    }

    override fun CangJieTypeMarker.removeAnnotations(): CangJieTypeMarker {
        require(this is UnwrappedType, this::errorMessage)
        return this.replaceAnnotations(Annotations.EMPTY)
    }


    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker {
        return when (this) {
            is NewCapturedType -> this.constructor.projection
            is CapturedType -> this.typeProjection
            else -> error("Unsupported captured type")
        }
    }

    override fun CapturedTypeMarker.withNonOptionProjection(): CangJieTypeMarker {
        require(this is NewCapturedType, this::errorMessage)

        return NewCapturedType(
            captureStatus,
            constructor,
            lowerType,
            attributes,
            isOption,
            isProjectionNotNull = true
        )
    }

    override fun CapturedTypeMarker.isProjectionNonOption(): Boolean {
        require(this is NewCapturedType, this::errorMessage)
        return this.isProjectionNotNull
    }

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        require(this is NewCapturedType, this::errorMessage)
        return this.constructor.typeParameter
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is NewCapturedType, this::errorMessage)
        return this.captureStatus
    }

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = this is CapturedType

    override fun CapturedTypeMarker.hasRawSuperType(): Boolean {
        error("Is not expected to be called in K1")
    }

    override fun CangJieTypeMarker.isOptionType(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return TypeUtils.isOptionType(this)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        isOption: Boolean,
        isExtensionFunction: Boolean,
        attributes: List<AnnotationMarker>?
    ): SimpleTypeMarker {
        require(constructor is TypeConstructor, constructor::errorMessage)

        // CangJie does not have extension function types, so we just use the original annotations
        val ourAnnotations = attributes?.firstIsInstanceOrNull<AnnotationsTypeAttribute>()?.annotations?.toList()
        val resultingAnnotations = if (ourAnnotations.isNullOrEmpty()) Annotations.EMPTY else Annotations.create(ourAnnotations)

        @Suppress("UNCHECKED_CAST")
        return CangJieTypeFactory.simpleType(
            DefaultTypeAttributeTranslator.toAttributes(resultingAnnotations),
            constructor,
            arguments as List<TypeProjection>,
            isOption
        )
    }

    override fun createTypeArgument(type: CangJieTypeMarker, variance: TypeVariance): TypeArgumentMarker {
        require(type is CangJieType, type::errorMessage)
        return TypeProjectionImpl(variance.convertVariance(), type)
    }


    override fun CangJieTypeMarker.canHaveUndefinedOption(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return constructor is NewTypeVariableConstructor ||
                constructor.declarationDescriptor is TypeParameterDescriptor ||
                this is NewCapturedType
    }

    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        // CangJie does not have extension function types
        return false
    }

    override fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        @Suppress("UNCHECKED_CAST")
        return this.replace(newArguments as List<TypeProjection>)
    }

    override fun SimpleTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return this.replaceArgumentsByExistingArgumentsWith(replacement)
    }

    override fun DefinitelyNonOptionTypeMarker.original(): SimpleTypeMarker {
        require(this is DefinitelyNonOptionType, this::errorMessage)
        return this.original
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<CangJieTypeMarker>,
        lowerType: CangJieTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        errorSupportedOnlyInTypeInference()
    }


    override fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker {
        error("Only for K2")
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker {
        require(type is UnwrappedType, type::errorMessage)
        require(this is TypeSubstitutor, this::errorMessage)
        return safeSubstitute(type, Variance.INVARIANT)
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun CangJieTypeMarker.isSpecial(): Boolean {
        require(this is CangJieType)
        return this is TypeUtils.SpecialType
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        errorSupportedOnlyInTypeInference()
    }

    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        errorSupportedOnlyInTypeInference()
    }

    override fun CangJieTypeMarker.isSignedOrUnsignedNumberType(): Boolean {
        require(this is CangJieType)
        return classicIsSignedOrUnsignedNumberType() || constructor is IntegerLiteralTypeConstructor
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
        @Suppress("UNCHECKED_CAST")
        explicitSupertypes as List<SimpleType>
        return IntegerLiteralTypeConstructor.findCommonSuperType(explicitSupertypes)
    }

    override fun unionTypeAttributes(types: List<CangJieTypeMarker>): List<AnnotationMarker> {
        @Suppress("UNCHECKED_CAST")
        return (types as List<CangJieType>).map {
            it.unwrap().attributes

        }.reduce { x, y ->
            x.union(y)
        }.toList()


    }

    override fun CangJieTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): CangJieTypeMarker {
        require(this is CangJieType)
        @Suppress("UNCHECKED_CAST")
        val attributes =
            (newAttributes as List<TypeAttribute<*>>).filterNot { it is AnnotationsTypeAttribute }.toMutableList()
        attributes.addIfNotNull(this.attributes.annotationsAttribute)
        return this.unwrap().replaceAttributes(
            TypeAttributes.create(attributes)
        )
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return ErrorUtils.isError(declarationDescriptor)
    }

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): CangJieTypeMarker {
        require(this is IntegerLiteralTypeConstructor, this::errorMessage)
        return this.getApproximatedType().unwrap()
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return CangJieBuiltIns.isPrimitiveType(this)
    }

    override fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker> {
        require(this is CangJieType, this::errorMessage)
        return this.attributes.toList()
    }

//    override fun CangJieTypeMarker.hasCustomAttributes(): Boolean {
//        require(this is CangJieType, this::errorMessage)
//        return !this.attributes.isEmpty() && this.getCustomAttributes().size > 0
//    }
//
//    override fun CangJieTypeMarker.getCustomAttributes(): List<AnnotationMarker> {
//        require(this is CangJieType, this::errorMessage)
//        return this.attributes.filterNot { it is AnnotationsTypeAttribute }
//    }

//    override fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker? {
//        return captureFromExpressionInternal(type as UnwrappedType)
//    }

    override fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker {
        return ErrorUtils.createErrorType(ErrorTypeKind.RESOLUTION_ERROR_TYPE, debugName)
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker {
        return ErrorUtils.createErrorType(
            ErrorTypeKind.UNINFERRED_TYPE_VARIABLE,
            constructor as TypeConstructor,
            constructor.toString()
        )
    }

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        return this is NewCapturedTypeConstructor
    }

    override fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker {
        val eraser = TypeParameterUpperBoundEraser(
            ErasureProjectionComputer(),
            TypeParameterErasureOptions(leaveNonTypeParameterTypes = true, intersectUpperBounds = true)
        )
        val typeParameters = this.extractTypeParameters()
            .map { it as TypeParameterDescriptor }
            .associateWith {
                TypeProjectionImpl(
                    Variance.INVARIANT,
                    eraser.getErasedUpperBound(it, ErasureTypeAttributes(TypeUsage.COMMON))
                )
            }
        return TypeConstructorSubstitution.createByParametersMap(typeParameters).buildSubstitutor().safeSubstitute(this)
    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean {
        return this is ClassifierBasedTypeConstructor && this.declarationDescriptor is AbstractTypeParameterDescriptor
    }

    //    override fun arrayType(componentType: CangJieTypeMarker): SimpleTypeMarker {
//        require(componentType is CangJieType, this::errorMessage)
//        return builtIns.getArrayType(Variance.INVARIANT, componentType)
//    }
//
//    override fun CangJieTypeMarker.isArrayOrOptionArray(): Boolean {
//        require(this is CangJieType, this::errorMessage)
//        return CangJieBuiltIns.isArray(this)
//    }
//
    override fun CangJieTypeMarker.hasAnnotation(fqName: FqName): Boolean {
        require(this is CangJieType, this::errorMessage)
        return annotations.hasAnnotation(fqName)
    }
//
//    override fun CangJieTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? {
//        require(this is CangJieType, this::errorMessage)
//        return annotations.findAnnotation(fqName)?.allValueArguments?.values?.firstOrNull()?.value
//    }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor as? TypeParameterDescriptor
    }


    override fun createTypeWithAlternativeForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker,
    ): CangJieTypeMarker {
        require(firstCandidate is CangJieType, this::errorMessage)
        require(secondCandidate is CangJieType, this::errorMessage)

        (firstCandidate.constructor as? IntersectionTypeConstructor)?.let { intersectionConstructor ->
            val intersectionTypeWithAlternative = intersectionConstructor.setAlternative(secondCandidate).createType()
            return if (firstCandidate.isOption) intersectionTypeWithAlternative.makeOptionAsSpecified(true)
            else intersectionTypeWithAlternative

        } ?: error("Expected intersection type, found $firstCandidate")
    }

//    override fun CangJieTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean {
//        require(this is CangJieType, this::errorMessage)
//        return this.isFunctionOrKFunctionTypeWithAnySuspendability
//    }

//    override fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker> {
//        require(this is CangJieType, this::errorMessage)
//        return this.getPureArgumentsForFunctionalTypeOrSubtype()
//    }

    override fun CangJieTypeMarker.getFunctionTypeFromSupertypes(): CangJieTypeMarker {
        require(this is CangJieType)
        return this.extractFunctionalTypeFromSupertypes()
    }

    override fun CangJieTypeMarker.functionTypeKind(): FunctionTypeKind? {
        require(this is CangJieType)
        return this.functionTypeKind
    }

    override fun CangJieTypeMarker.isFunctionWithAny(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return this.isFunctionType
    }

    override fun getNonReflectFunctionTypeConstructor(
        parametersNumber: Int,
        kind: FunctionTypeKind
    ): TypeConstructorMarker {
        return getFunctionDescriptor(
            builtIns,
            parametersNumber,
//            isSuspendFunction = kind.nonReflectKind() == FunctionTypeKind.SuspendFunction
        ).typeConstructor
    }

    override fun getReflectFunctionTypeConstructor(
        parametersNumber: Int,
        kind: FunctionTypeKind
    ): TypeConstructorMarker {
        return getFunctionDescriptor(
            builtIns,
            parametersNumber,

            ).typeConstructor
    }

    override fun createSubstitutorForSuperTypes(baseType: CangJieTypeMarker): TypeSubstitutorMarker? {
        require(baseType is CangJieType, baseType::errorMessage)
        return (baseType.memberScope as? SubstitutingScope)?.substitutor
    }

    override fun useRefinedBoundsForTypeVariableInFlexiblePosition(): Boolean = false

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): TypeCheckerState.SupertypesPolicy {
        require(type is SimpleType, type::errorMessage)
        val substitutor = TypeConstructorSubstitution.create(type).buildSubstitutor()

        return object : TypeCheckerState.SupertypesPolicy.DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker): SimpleTypeMarker {
                return substitutor.safeSubstitute(
                    type.lowerBoundIfFlexible() as CangJieType,
                    Variance.INVARIANT
                ).asSimpleType()!!
            }
        }
    }

    override fun CangJieTypeMarker.isTypeVariableType(): Boolean {
        return this is UnwrappedType && constructor is NewTypeVariableConstructor
    }


}

private fun makeSimpleTypeDefinitelyNonOptionOrNonOptionInternal(type: SimpleType): SimpleType {
    return type.makeSimpleTypeDefinitelyNonOptionOrNonOption()
}

private fun makeDefinitelyNonOptionOrNonOptionInternal(type: UnwrappedType): UnwrappedType {
    return type.makeDefinitelyNonOptionOrNonOption()
}

//private fun hasExactInternal(type: UnwrappedType): Boolean {
//    return type.hasExactAnnotation()
//}

private fun singleBestRepresentative(collection: Collection<CangJieType>) = collection.singleBestRepresentative()

private fun containsInternal(type: CangJieType, predicate: (CangJieTypeMarker) -> Boolean): Boolean =
    type.contains(predicate)

fun TypeVariance.convertVariance(): Variance {
    return when (this) {
        TypeVariance.INV -> Variance.INVARIANT

    }
}

private fun captureFromExpressionInternal(type: UnwrappedType) = captureFromExpression(type)
