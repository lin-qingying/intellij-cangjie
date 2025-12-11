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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames.FqNames.optionUFqName
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.VArrayTypeDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.constants.FloatLiteralTypeConstructor
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstructor
import org.cangnova.cangjie.resolve.isAnnotatinoDescriptor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import org.cangnova.cangjie.types.TypeUtils.contains
import org.cangnova.cangjie.types.TypeUtils.makeOptionalAsSpecified
import org.cangnova.cangjie.types.TypeUtils.makeProjection
import org.cangnova.cangjie.types.checker.*
import org.cangnova.cangjie.types.checker.CangJieTypeChecker.Companion.DEFAULT
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isMarkedOption
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.model.TypeArgumentMarker
import org.cangnova.cangjie.types.model.TypeVariableTypeConstructorMarker
import org.cangnova.cangjie.utils.SmartSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 仓颉语言类型工具类
 *
 * 提供类型系统的核心工具函数，包括：
 * - Option类型操作（检测、解包、创建）
 * - 类型关系检查（子类型、超类型）
 * - 类型参数处理
 * - 类型推断支持
 * - 特殊类型处理
 *
 * 核心功能：
 * - isOptionType()：检测类型是否为Option类型
 * - unboxOptionType()：解包Option类型
 * - countOptionNestedLevel()：计算Option嵌套层级
 * - makeOptionType()：创建Option类型
 * - acceptsOption()：检查类型是否接受Option值
 *
 * 示例：
 * ```kotlin
 * // Option类型检测
 * val intType: CangJieType = BasicType(...)
 * isOptionType(intType) // false
 *
 * val optionIntType: CangJieType = OptionType(intType)
 * isOptionType(optionIntType) // true
 *
 * // Option类型解包
 * val unboxedType = unboxOptionType(optionIntType) // 返回 Int 类型
 *
 * // 嵌套Option处理
 * val nestedOption = OptionType(optionIntType)
 * countOptionNestedLevel(nestedOption) // 2
 *
 * // 创建Option类型
 * val newOptionType = makeOptionType(intType) // 创建 Option<Int>
 * ```
 */

/**
 * 获取类型的内置类型系统
 *
 * 示例：
 * ```kotlin
 * val builtIns = someType.builtIns
 * val intType = builtIns.intType
 * ```
 */
val CangJieType.builtIns: CangJieBuiltIns
    get() = constructor.builtIns

/**
 * 类型工具对象
 *
 * 提供静态的类型操作工具函数，包括类型检查、转换和推断功能。
 */
object TypeUtils {

    /**
     * 表示"不关心"的特殊类型
     * 用于类型推断中表示不需要特定类型的情况
     */
    val DONT_CARE: SimpleType get() = ErrorUtils.createErrorType(ErrorTypeKind.DONT_CARE)

    /**
     * 检查类型是否为非具体化的类型参数
     *
     * 类型参数在编译时被擦除，运行时无法获取其具体类型信息。
     *
     * 示例：
     * ```kotlin
     * val typeParam: CangJieType = TypeParameterType(...)
     * isNonReifiedTypeParameter(typeParam) // true
     *
     * val concreteType: CangJieType = BasicType(...)
     * isNonReifiedTypeParameter(concreteType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 如果类型是非具体化的类型参数则返回true
     */
    fun isNonReifiedTypeParameter(type: CangJieType): Boolean {
        val typeParameterDescriptor = getTypeParameterDescriptorOrNull(type)
        return typeParameterDescriptor != null
    }

    /**
     * 检查类型是否为特殊类型
     *
     * 特殊类型包括错误类型、占位符类型等非标准类型。
     *
     * 示例：
     * ```kotlin
     * val errorType: CangJieType = ErrorType(...)
     * isSpecialType(errorType) // true
     *
     * val normalType: CangJieType = BasicType(...)
     * isSpecialType(normalType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 如果类型是特殊类型则返回true
     */
    fun isSpecialType(type: CangJieType): Boolean {
        return type is SpecialType
    }

    /**
     * 为类型参数创建类型投影
     *
     * 使用类型参数的默认类型创建类型投影。
     *
     * 示例：
     * ```kotlin
     * val typeParam: TypeParameterDescriptor = ...
     * val projection = makeProjection(typeParam)
     * // 创建基于typeParam默认类型的投影
     * ```
     *
     * @param parameterDescriptor 类型参数描述符
     * @return 类型投影
     */
    fun makeProjection(parameterDescriptor: TypeParameterDescriptor): TypeProjection {
        return TypeProjectionImpl(parameterDescriptor.defaultType)
    }

    /**
     * 为类型参数创建带属性的类型投影
     *
     * 使用指定的擦除类型属性创建类型投影。
     *
     * 示例：
     * ```kotlin
     * val typeParam: TypeParameterDescriptor = ...
     * val attr = ErasureTypeAttributes(...)
     * val projection = makeProjection(typeParam, attr)
     * // 创建带特定属性的类型投影
     * ```
     *
     * @param parameterDescriptor 类型参数描述符
     * @param attr 擦除类型属性
     * @return 类型投影
     */
    fun makeProjection(
        parameterDescriptor: TypeParameterDescriptor, attr: ErasureTypeAttributes
    ): TypeProjection {
//        return if (attr.howThisTypeIsUsed ==  TypeUsage.SUPERTYPE) {
        return TypeProjectionImpl(parameterDescriptor.projectionType())
//        } else {
//           StarProjectionImpl(parameterDescriptor)
//        }
    }

    /**
     * 检查类型是否接受Option值
     *
     * 与isOptionType不同，此方法还考虑类型参数：
     * acceptsOption(T) <=> T有Option下界
     * 语义应该与isSubtype(Nothing?, T)相同
     *
     * 示例：
     * ```kotlin
     * val optionType: CangJieType = OptionType(intType)
     * acceptsOption(optionType) // true
     *
     * val flexibleType: CangJieType = FlexibleType(...)
     * acceptsOption(flexibleType) // 取决于上界是否接受Option
     *
     * val intType: CangJieType = BasicType(...)
     * acceptsOption(intType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 如果类型可以接受Option值则返回true
     */
    fun acceptsOption(type: CangJieType): Boolean {
        if (type.isOption) {
            return true
        }
        if (type.isFlexible() && acceptsOption(type.asFlexibleType().upperBound)) {
            return true
        }
        return false
    }

    /**
     * 检查类型是否有Option超类型
     *
     * 检查类型的继承层次中是否存在Option类型。
     * 类/特征不能有Option超类型。
     *
     * 示例：
     * ```kotlin
     * // 假设有继承关系：MyClass <: Option<Base>
     * val myClassType: CangJieType = ...
     * hasOptionSuperType(myClassType) // true
     *
     * val simpleType: CangJieType = BasicType(...)
     * hasOptionSuperType(simpleType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 如果类型有Option超类型则返回true
     */
    fun hasOptionSuperType(type: CangJieType): Boolean {
        if (type.constructor.declarationDescriptor is ClassDescriptor) {
            // A class/trait cannot have an option supertype
            return false
        }

        for (supertype in getImmediateSupertypes(type)) {
            if (isOptionType(supertype)) return true
        }

        return false
    }

    /**
     * 检查类型是否为类型参数
     *
     * 包括类型参数描述符和新的类型变量构造器。
     *
     * 示例：
     * ```kotlin
     * val typeParam: CangJieType = TypeParameterType(...)
     * isTypeParameter(typeParam) // true
     *
     * val newTypeVar: CangJieType = NewTypeVariableType(...)
     * isTypeParameter(newTypeVar) // true
     *
     * val concreteType: CangJieType = BasicType(...)
     * isTypeParameter(concreteType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 如果类型是类型参数则返回true
     */
    fun isTypeParameter(type: CangJieType): Boolean {
        return getTypeParameterDescriptorOrNull(type) != null || type.constructor is NewTypeVariableConstructor
    }

    /**
     * 获取类型的类型参数描述符（如果存在）
     *
     * 如果类型的构造器声明描述符是类型参数描述符，则返回它。
     *
     * 示例：
     * ```kotlin
     * val typeParam: CangJieType = TypeParameterType(...)
     * val descriptor = getTypeParameterDescriptorOrNull(typeParam) // 返回TypeParameterDescriptor
     *
     * val concreteType: CangJieType = BasicType(...)
     * val descriptor = getTypeParameterDescriptorOrNull(concreteType) // 返回null
     * ```
     *
     * @param type 要检查的类型
     * @return 类型参数描述符，如果类型不是类型参数则返回null
     */
    fun getTypeParameterDescriptorOrNull(type: CangJieType): TypeParameterDescriptor? {
        if (type.constructor.declarationDescriptor is TypeParameterDescriptor) {
            return type.constructor.declarationDescriptor as TypeParameterDescriptor
        }
        return null
    }


    /**
     * 将其他类型添加到存根类型的泛型参数中
     *
     * 如果存根类型的参数数量与提供的类型数量不匹配，则返回原始存根。
     *
     * 示例：
     * ```kotlin
     * val stub: CangJieType = StubType(...)
     * val otherTypes = arrayOf(intType, stringType)
     * val result = addTypeParameterToStub(stub, *otherTypes)
     * // 将intType和stringType作为泛型参数添加到stub中
     * ```
     *
     * @param stub 存根类型
     * @param other 要添加的类型
     * @return 更新后的类型
     */
    fun addTypeParameterToStub(stub: CangJieType, vararg other: CangJieType): CangJieType {

        if (stub.constructor.parameters.size != other.size) return stub
        val arguments = mutableListOf<TypeProjection>()
        other.forEach {
            arguments.add(TypeProjectionImpl(it))
        }

        return CangJieTypeFactory.simpleType(
            stub.attributes, stub.constructor, arguments, true, null
        )

    }

    /**
     * 创建替换后的超类型
     *
     * 使用类型替换器对超类型进行替换，并根据子类型的Option状态调整结果。
     *
     * 示例：
     * ```kotlin
     * val subType: CangJieType = ...
     * val superType: CangJieType = ...
     * val substitutor: TypeSubstitutor = TypeSubstitutor.create(subType)
     * val result = createSubstitutedSupertype(subType, superType, substitutor)
     * // 返回替换后的超类型，并根据subType的Option状态调整
     * ```
     *
     * @param subType 子类型
     * @param superType 超类型
     * @param substitutor 类型替换器
     * @return 替换后的超类型，如果替换失败则返回null
     */
    fun createSubstitutedSupertype(
        subType: CangJieType, superType: CangJieType, substitutor: TypeSubstitutor
    ): CangJieType? {
        val substitutedType: CangJieType? = substitutor.substitute(superType, Variance.INVARIANT)
        if (substitutedType != null) {
            return makeOptionalIfNeeded(substitutedType, subType.isOption)
        }
        return null
    }

    /**
     * 检查参数是否低于类型参数的边界
     *
     * 检查类型参数的实际类型是否满足其上界约束。
     *
     * @param typeChecker 类型检查器
     * @param argument 实际类型参数
     * @param parameterDescriptor 类型参数描述符
     * @return 如果参数低于边界则返回true
     */
    private fun lowerThanBound(
        typeChecker: CangJieTypeChecker, argument: CangJieType, parameterDescriptor: TypeParameterDescriptor
    ): Boolean {
        for (bound in parameterDescriptor.upperBounds) {
            if (typeChecker.isSubtypeOf(argument, bound)) {
                if (argument.constructor != bound.constructor) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 检查类型是否可以有子类型
     *
     * 检查给定类型是否可以有更具体的子类型。
     * Option类型总是可以有子类型。
     *
     * 示例：
     * ```kotlin
     * val optionType: CangJieType = OptionType(intType)
     * canHaveSubtypes(typeChecker, optionType) // true
     *
     * val finalType: CangJieType = FinalType(...)
     * canHaveSubtypes(typeChecker, finalType) // false
     * ```
     *
     * @param typeChecker 类型检查器
     * @param type 要检查的类型
     * @return 如果类型可以有子类型则返回true
     */
    @JvmStatic
    fun canHaveSubtypes(
        typeChecker: CangJieTypeChecker, type: CangJieType
    ): Boolean {
        if (type.isOption) {
            return true
        }
//        if (!type.constructor.isFinal()) {
//            return true
//        }

        val parameters: List<TypeParameterDescriptor> = type.constructor.parameters
        val arguments: List<TypeProjection> = type.arguments
        var i = 0
        val parametersSize = parameters.size
        while (i < parametersSize) {
            val parameterDescriptor: TypeParameterDescriptor = parameters[i]
            val typeProjection: TypeProjection = arguments[i]


            val projectionKind: Variance = typeProjection.projectionKind
            val argument: CangJieType = typeProjection.type

            when (parameterDescriptor.variance) {
                Variance.INVARIANT -> when (projectionKind) {
                    Variance.INVARIANT -> if (lowerThanBound(
                            typeChecker, argument, parameterDescriptor
                        ) || canHaveSubtypes(typeChecker, argument)
                    ) {
                        return true
                    }


                }


            }
            i++
        }
        return false
    }


    /**
     * 获取类型的类描述符
     *
     * 如果类型的构造器声明描述符是类描述符，则返回它。
     *
     * 示例：
     * ```kotlin
     * val classType: CangJieType = ClassType(...)
     * val classDescriptor = getClassDescriptor(classType) // 返回ClassDescriptor
     *
     * val primitiveType: CangJieType = BasicType(...)
     * val classDescriptor = getClassDescriptor(primitiveType) // 返回null
     * ```
     *
     * @param type 要检查的类型
     * @return 类描述符，如果类型不是类类型则返回null
     */
    @JvmStatic
    fun getClassDescriptor(type: CangJieType): ClassDescriptor? {
        val declarationDescriptor = type.constructor.declarationDescriptor
        if (declarationDescriptor is ClassDescriptor) {
            return declarationDescriptor
        }
        return null
    }

    /**
     * 检查类型是否为"不关心"占位符
     *
     * 检查类型是否为特殊的"不关心"占位符类型。
     *
     * 示例：
     * ```kotlin
     * val dontCareType: CangJieType = DONT_CARE
     * isDontCarePlaceholder(dontCareType) // true
     *
     * val normalType: CangJieType = BasicType(...)
     * isDontCarePlaceholder(normalType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 如果类型是"不关心"占位符则返回true
     */
    fun isDontCarePlaceholder(type: CangJieType?): Boolean {
        return type != null && type.constructor === DONT_CARE.constructor
    }

    /**
     * 获取类型的直接超类型列表
     *
     * 返回类型的所有直接超类型，使用类型替换器进行替换。
     *
     * 示例：
     * ```kotlin
     * val classType: CangJieType = ClassType(...)
     * val supertypes = getImmediateSupertypes(classType)
     * // 返回所有直接超类型，如 [BaseClass, Interface1, Interface2]
     * ```
     *
     * @param type 要获取超类型的类型
     * @return 直接超类型列表
     */
    fun getImmediateSupertypes(type: CangJieType): List<CangJieType> {

        val substitutor: TypeSubstitutor = TypeSubstitutor.create(type)
        val originalSupertypes: Collection<CangJieType> = type.constructor.supertypes
        val result = ArrayList<CangJieType>(originalSupertypes.size)
        for (supertype in originalSupertypes) {
            val substitutedType = createSubstitutedSupertype(type, supertype, substitutor)
            if (substitutedType != null) {
                result.add(substitutedType)
            }
        }
        return result
    }

    /**
     * 收集类型的所有超类型
     *
     * 递归收集类型的所有超类型，包括直接和间接超类型。
     *
     * @param type 要收集超类型的类型
     * @param result 用于存储结果的集合
     */
    private fun collectAllSupertypes(type: CangJieType, result: MutableSet<CangJieType>) {
        val immediateSupertypes: List<CangJieType> = getImmediateSupertypes(type)
        result.addAll(immediateSupertypes)
        for (supertype in immediateSupertypes) {
            collectAllSupertypes(supertype, result)
        }
    }

    /**
     * 获取类型的所有超类型集合
     *
     * 递归收集类型的所有超类型，包括直接和间接超类型。
     *
     * 示例：
     * ```kotlin
     * val classType: CangJieType = ClassType(...)
     * val allSupertypes = getAllSupertypes(classType)
     * // 返回所有超类型，包括直接和间接继承的类型
     * ```
     *
     * @param type 要获取超类型的类型
     * @return 所有超类型的集合
     */
    fun getAllSupertypes(type: CangJieType): Set<CangJieType> {


        val result = LinkedHashSet<CangJieType>(15)
        collectAllSupertypes(type, result)
        return result
    }

    /**
     * 检查两个类型是否相等
     *
     * 使用默认的类型检查器比较两个类型是否相等。
     *
     * 示例：
     * ```kotlin
     * val type1: CangJieType = BasicType(...)
     * val type2: CangJieType = BasicType(...)
     * equalTypes(type1, type2) // true 或 false
     * ```
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 如果两个类型相等则返回true
     */
    @JvmStatic
    fun equalTypes(a: CangJieType, b: CangJieType): Boolean {
        return DEFAULT.equalTypes(a, b)
    }

    @JvmStatic

    fun getDefaultPrimitiveNumberType(numberValueTypeConstructor: IntegerValueTypeConstructor): CangJieType {
        val type = getDefaultPrimitiveNumberType(numberValueTypeConstructor.supertypes) ?: error(
            "Strange number value type constructor: " + numberValueTypeConstructor + ". " + "Super types doesn't contain double, int or long: " + numberValueTypeConstructor.supertypes
        )
        return type
    }

    /**
     * 从超类型集合中获取默认的原始数字类型
     *
     * 根据预定义的优先级从超类型集合中选择默认的数字类型。
     * 如果集合为空，返回null。
     *
     * 示例：
     * ```kotlin
     * val supertypes = listOf(intType, doubleType, longType)
     * val defaultType = getDefaultPrimitiveNumberType(supertypes) // 返回doubleType
     * ```
     *
     * @param supertypes 超类型集合
     * @return 默认的原始数字类型，如果集合为空则返回null
     */
    @JvmStatic
    fun getDefaultPrimitiveNumberType(supertypes: Collection<CangJieType>): CangJieType? {
        if (supertypes.isEmpty()) {
            return null
        }

//        val builtIns:  CangJieBuiltIns =
//            supertypes.iterator().next().constructor.getBuiltIns()
//        val doubleType:  CangJieType = builtIns.getDoubleType()
//        if (supertypes.contains(doubleType)) {
//            return doubleType
//        }
//        val intType:  CangJieType = builtIns.getIntType()
//        if (supertypes.contains(intType)) {
//            return intType
//        }
//        val longType:  CangJieType = builtIns.getLongType()
//        if (supertypes.contains(longType)) {
//            return longType
//        }
//
//        val uIntType:  CangJieType =
//             TypeUtils.findByFqName(supertypes, uIntFqName)
//        if (uIntType != null) return uIntType
//
//        val uLongType:  CangJieType =
//             TypeUtils.findByFqName(supertypes, uLongFqName)
//        if (uLongType != null) return uLongType

        return null
    }

    /**
     * 根据完全限定名在超类型集合中查找类型
     *
     * 遍历超类型集合，查找具有指定完全限定名的类型。
     *
     * @param supertypes 超类型集合
     * @param fqName 要查找的完全限定名
     * @return 找到的类型，如果不存在则返回null
     */
    private fun findByFqName(
        supertypes: Collection<CangJieType>, fqName: FqName
    ): CangJieType? {
        for (supertype in supertypes) {
            val descriptor: ClassifierDescriptor = supertype.constructor.declarationDescriptor ?: continue

            val descriptorFqName: FqNameUnsafe = DescriptorUtils.getFqName(descriptor)
            if (descriptorFqName == fqName.toUnsafe()) {
                return supertype
            }
        }
        return null
    }

    /**
     * 根据浮点数字面量类型构造器和期望类型获取原始数字类型
     *
     * 如果期望类型为空或错误，返回近似类型。
     * 否则，从可能的类型中选择与期望类型兼容的类型。
     *
     * 示例：
     * ```kotlin
     * val floatLiteralConstructor = FloatLiteralTypeConstructor(...)
     * val expectedType: CangJieType = Double类型
     * val resultType = getPrimitiveNumberType(floatLiteralConstructor, expectedType)
     * // 返回与期望类型兼容的原始数字类型
     * ```
     *
     * @param literalTypeConstructor 浮点数字面量类型构造器
     * @param expectedType 期望类型
     * @return 原始数字类型
     */
    @JvmStatic

    fun getPrimitiveNumberType(
        literalTypeConstructor: FloatLiteralTypeConstructor, expectedType: CangJieType
    ): CangJieType {
        if (noExpectedType(expectedType) || expectedType.isError) {
            return literalTypeConstructor.getApproximatedType()
        }

        // If approximated type does not match expected type then expected type is very
        //  specific type (e.g. Comparable<Byte>), so only one of possible types could match it
        val approximatedType: CangJieType = literalTypeConstructor.getApproximatedType()
        if (DEFAULT.isSubtypeOf(approximatedType, expectedType)) {
            return approximatedType
        }

        for (primitiveNumberType in literalTypeConstructor.possibleTypes) {
            if (DEFAULT.isSubtypeOf(
                    primitiveNumberType, expectedType
                )
            ) {
                return primitiveNumberType
            }
        }
        return literalTypeConstructor.getApproximatedType()
    }

    /**
     * 根据整数字面量类型构造器和期望类型获取原始数字类型
     *
     * 如果期望类型为空或错误，返回近似类型。
     * 否则，从可能的类型中选择与期望类型兼容的类型。
     *
     * 示例：
     * ```kotlin
     * val intLiteralConstructor = IntegerLiteralTypeConstructor(...)
     * val expectedType: CangJieType = Int类型
     * val resultType = getPrimitiveNumberType(intLiteralConstructor, expectedType)
     * // 返回与期望类型兼容的原始数字类型
     * ```
     *
     * @param literalTypeConstructor 整数字面量类型构造器
     * @param expectedType 期望类型
     * @return 原始数字类型
     */
    @JvmStatic

    fun getPrimitiveNumberType(
        literalTypeConstructor: IntegerLiteralTypeConstructor, expectedType: CangJieType
    ): CangJieType {
        if (noExpectedType(expectedType) || expectedType.isError) {
            return literalTypeConstructor.getApproximatedType()
        }

        // If approximated type does not match expected type then expected type is very
        //  specific type (e.g. Comparable<Byte>), so only one of possible types could match it
        val approximatedType: CangJieType = literalTypeConstructor.getApproximatedType()
        if (DEFAULT.isSubtypeOf(approximatedType, expectedType)) {
            return approximatedType
        }

        for (primitiveNumberType in literalTypeConstructor.possibleTypes) {
            if (DEFAULT.isSubtypeOf(
                    primitiveNumberType, expectedType
                )
            ) {
                return primitiveNumberType
            }
        }
        return literalTypeConstructor.getApproximatedType()
    }

    /**
     * 根据数字值类型构造器和期望类型获取原始数字类型
     *
     * 如果期望类型为空或错误，返回默认的原始数字类型。
     * 否则，从可能的类型中选择与期望类型兼容的类型。
     *
     * 示例：
     * ```kotlin
     * val numberValueConstructor = IntegerValueTypeConstructor(...)
     * val expectedType: CangJieType = Number类型
     * val resultType = getPrimitiveNumberType(numberValueConstructor, expectedType)
     * // 返回与期望类型兼容的原始数字类型
     * ```
     *
     * @param numberValueTypeConstructor 数字值类型构造器
     * @param expectedType 期望类型
     * @return 原始数字类型
     */
    @JvmStatic
    fun getPrimitiveNumberType(
        numberValueTypeConstructor: IntegerValueTypeConstructor, expectedType: CangJieType
    ): CangJieType {
        if (noExpectedType(expectedType) || expectedType.isError) {
            return getDefaultPrimitiveNumberType(numberValueTypeConstructor)
        }
        for (primitiveNumberType in numberValueTypeConstructor.supertypes) {
            if (DEFAULT.isSubtypeOf(
                    primitiveNumberType, expectedType
                )
            ) {
                return primitiveNumberType
            }
        }
        return getDefaultPrimitiveNumberType(numberValueTypeConstructor)
    }

    /**
     * 将类型转换为Option类型
     *
     * 将任意类型转换为Option类型，如果已经是Option类型则返回自身。
     *
     * 示例：
     * ```kotlin
     * val intType: CangJieType = BasicType(...)
     * val optionIntType = makeOptional(intType) // 返回 OptionType(intType)
     * optionIntType.isOption // true
     * ```
     *
     * @param type 要转换的类型
     * @return Option类型
     */
    @JvmStatic
    fun makeOptional(type: CangJieType): CangJieType {
        return makeOptionalAsSpecified(type, true)
    }

    /**
     * 将类型转换为非Option类型
     *
     * 将Option类型转换为非Option类型，如果已经是非Option类型则返回自身。
     *
     * 示例：
     * ```kotlin
     * val optionIntType: CangJieType = OptionType(intType)
     * val intType = makeNonOption(optionIntType) // 返回 intType
     * intType.isOption // false
     * ```
     *
     * @param type 要转换的类型
     * @return 非Option类型
     */
    @JvmStatic
    fun makeNonOption(type: CangJieType): CangJieType {
        return makeOptionalAsSpecified(type, false)
    }


    /**
     * 将类型转换为指定的Option状态
     *
     * 通过调用UnwrappedType.makeOptionAsSpecified方法，将类型转换为指定的Option状态。
     * 如果类型已经是目标状态，则返回自身；否则进行相应的转换。
     *
     * 示例：
     * ```kotlin
     * val intType: CangJieType = BasicType(...)
     *
     * // 转换为Option类型
     * val optionIntType = makeOptionalAsSpecified(intType, true) // 返回 OptionType(intType)
     * optionIntType.isOption // true
     *
     * // 转换为非Option类型
     * val nonOptionType = makeOptionalAsSpecified(optionIntType, false) // 返回 intType
     * nonOptionType.isOption // false
     *
     * // 已经是目标状态时返回自身
     * val sameType = makeOptionalAsSpecified(intType, false) // 返回 intType（无变化）
     * val sameOptionType = makeOptionalAsSpecified(optionIntType, true) // 返回 optionIntType（无变化）
     * ```
     *
     * @param type 要转换的类型
     * @param optional 目标Option状态
     * @return 转换后的类型
     */
    @JvmStatic
    fun makeOptionalAsSpecified(
        type: CangJieType, optional: Boolean
    ): CangJieType {
        return type.unwrap().makeOptionAsSpecified(optional)
    }

    /**
     * 根据需要将类型转换为Option类型
     *
     * 只有当optional为true且类型不是Option类型时，才进行转换。
     * 如果optional为false或类型已经是Option类型，则返回原类型。
     *
     * 示例：
     * ```kotlin
     * val intType: SimpleType = BasicType(...)
     *
     * // 需要转换为Option类型
     * val optionIntType = makeOptionalIfNeeded(intType, true) // 返回 OptionType(intType)
     * optionIntType.isOption // true
     *
     * // 不需要转换为Option类型
     * val sameType = makeOptionalIfNeeded(intType, false) // 返回 intType（无变化）
     * val sameOptionType = makeOptionalIfNeeded(optionIntType, true) // 返回 optionIntType（无变化）
     * ```
     *
     * @param type 要转换的类型
     * @param optional 是否需要转换为Option类型
     * @return 转换后的类型
     */
    @JvmStatic
    fun makeOptionalIfNeeded(
        type: SimpleType, optional: Boolean
    ): SimpleType {
        if (optional) {
            return type.makeOptionAsSpecified(true)
        }
        return type
    }

    @JvmStatic
    fun makeOptionalIfNeeded(
        type: CangJieType,
        optional: Boolean
    ): CangJieType {
        if (optional) {
            return makeOptional(type)
        }
        return type
    }

    /**
     * 获取类型参数的默认类型投影列表
     *
     * 为每个类型参数创建默认的类型投影。
     *
     * 示例：
     * ```kotlin
     * val parameters = listOf(typeParam1, typeParam2)
     * val projections = getDefaultTypeProjections(parameters)
     * // 返回每个参数的默认类型投影
     * ```
     *
     * @param parameters 类型参数列表
     * @return 默认类型投影列表
     */
    @JvmStatic
    fun getDefaultTypeProjections(parameters: List<TypeParameterDescriptor>): List<TypeProjection> {
        val result: MutableList<TypeProjection> = mutableListOf()
        for (parameterDescriptor in parameters) {
            result.add(TypeProjectionImpl(parameterDescriptor.defaultType))
        }
        return result.toList()
    }

    /**
     * 检查类型是否为无期望类型
     *
     * 检查类型是否为特殊的无期望类型或单元期望类型。
     *
     * 示例：
     * ```kotlin
     * val noExpectedType: CangJieType = NO_EXPECTED_TYPE
     * noExpectedType(noExpectedType) // true
     *
     * val normalType: CangJieType = BasicType(...)
     * noExpectedType(normalType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 如果类型是无期望类型则返回true
     */
    @JvmStatic
    fun noExpectedType(type: CangJieType): Boolean {
        return type === NO_EXPECTED_TYPE || type === UNIT_EXPECTED_TYPE
    }

    //    fun makeStarProjection(parameterDescriptor:  TypeParameterDescriptor):  TypeProjection {
//        return  StarProjectionImpl(parameterDescriptor)
//    }

    /**
     * 创建未替换的类型
     *
     * 使用类型构造器和未替换的成员作用域创建简单类型。
     *
     * 示例：
     * ```kotlin
     * val typeConstructor: TypeConstructor = ...
     * val memberScope: MemberScope = ...
     * val refinedTypeFactory: (CangJieTypeRefiner) -> SimpleType? = { ... }
     * val unsubstitutedType = makeUnsubstitutedType(typeConstructor, memberScope, refinedTypeFactory)
     * ```
     *
     * @param typeConstructor 类型构造器
     * @param unsubstitutedMemberScope 未替换的成员作用域
     * @param refinedTypeFactory 精化类型工厂函数
     * @return 未替换的简单类型
     */
    @JvmStatic

    fun makeUnsubstitutedType(
        typeConstructor: TypeConstructor,
        unsubstitutedMemberScope: MemberScope,
        refinedTypeFactory: (CangJieTypeRefiner) -> SimpleType?
    ): SimpleType {
        val arguments: List<TypeProjection> = getDefaultTypeProjections(typeConstructor.parameters)
        return simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty, typeConstructor, arguments, false, unsubstitutedMemberScope, refinedTypeFactory
        )
    }

    /**
     * 根据分类器描述符创建未替换的类型
     *
     * 使用分类器描述符和未替换的成员作用域创建简单类型。
     * 如果分类器描述符是错误类型，则创建错误类型。
     *
     * 示例：
     * ```kotlin
     * val classifierDescriptor: ClassifierDescriptor = ...
     * val memberScope: MemberScope = ...
     * val refinedTypeFactory: (CangJieTypeRefiner) -> SimpleType? = { ... }
     * val unsubstitutedType = makeUnsubstitutedType(classifierDescriptor, memberScope, refinedTypeFactory)
     * ```
     *
     * @param classifierDescriptor 分类器描述符
     * @param unsubstitutedMemberScope 未替换的成员作用域
     * @param refinedTypeFactory 精化类型工厂函数
     * @return 未替换的简单类型
     */
    @JvmStatic

    fun makeUnsubstitutedType(
        classifierDescriptor: ClassifierDescriptor,
        unsubstitutedMemberScope: MemberScope,
        refinedTypeFactory: (CangJieTypeRefiner) -> SimpleType?
    ): SimpleType {
        if (ErrorUtils.isError(classifierDescriptor)) {
            return ErrorUtils.createErrorType(
                ErrorTypeKind.UNABLE_TO_SUBSTITUTE_TYPE, classifierDescriptor.toString()
            )
        }
        val typeConstructor: TypeConstructor = classifierDescriptor.typeConstructor
        return makeUnsubstitutedType(
            typeConstructor, unsubstitutedMemberScope, refinedTypeFactory
        )
    }

    /**
     * 检查类型是否包含特殊类型
     *
     * 递归检查类型及其组件是否包含满足条件的特殊类型。
     * 使用访问集合避免循环引用。
     *
     * @param type 要检查的类型
     * @param isSpecialType 判断是否为特殊类型的函数
     * @param visited 已访问的类型集合，用于避免循环引用
     * @return 如果类型包含特殊类型则返回true
     */
    private fun contains(
        type: CangJieType?, isSpecialType: Function1<UnwrappedType, Boolean>, visited: SmartSet<CangJieType>?
    ): Boolean {
        var visited: SmartSet<CangJieType>? = visited
        if (type == null) return false

        val unwrappedType: UnwrappedType = type.unwrap()

        if (noExpectedType(type)) return isSpecialType.invoke(unwrappedType)
        if (visited != null && visited.contains(type)) return false
        if (isSpecialType.invoke(unwrappedType)) return true

        if (visited == null) {
            visited = SmartSet.create()
        }
        visited.add(type)
//
//        val flexibleType:  FlexibleType? =
//            if (unwrappedType is  FlexibleType) unwrappedType as  FlexibleType else null
//        if (flexibleType != null
//            && (contains(flexibleType.lowerBound, isSpecialType, visited)
//                    || contains(flexibleType.upperBound, isSpecialType, visited))
//        ) {
//            return true
//        }

//        if (unwrappedType is  DefinitelyNonOptionType &&
//            contains(
//                (unwrappedType as  DefinitelyNonOptionType).original,
//                isSpecialType,
//                visited
//            )
//        ) {
//            return true
//        }

        val typeConstructor: TypeConstructor = type.constructor
//        if (typeConstructor is  IntersectionTypeConstructor) {
//            val intersectionTypeConstructor:  IntersectionTypeConstructor =
//                typeConstructor as  IntersectionTypeConstructor
//            for (supertype in intersectionTypeConstructor.getSupertypes()) {
//                if (contains(supertype, isSpecialType, visited)) return true
//            }
//            return false
//        }

        for (projection in type.arguments) {

            if (contains(projection.type, isSpecialType, visited)) return true
        }
        return false
    }

    /**
     * 检查类型是否包含特殊类型（简化版本）
     *
     * 使用默认的访问集合检查类型是否包含满足条件的特殊类型。
     *
     * @param type 要检查的类型
     * @param isSpecialType 判断是否为特殊类型的函数
     * @return 如果类型包含特殊类型则返回true
     */
    fun contains(
        type: CangJieType?, isSpecialType: Function1<UnwrappedType, Boolean>
    ): Boolean {
        return contains(type, isSpecialType, null)
    }

    /**
     * 检查类型是否包含指定的特殊类型
     *
     * 检查类型是否包含与指定特殊类型相等的类型。
     *
     * @param type 要检查的类型
     * @param specialType 要查找的特殊类型
     * @return 如果类型包含指定的特殊类型则返回true
     */
    fun contains(type: CangJieType?, specialType: CangJieType): Boolean {
        return contains(type) { type: UnwrappedType? -> specialType.equals(type) }
    }

    /**
     * 无法推断函数参数类型的错误类型
     */
    val CANNOT_INFER_FUNCTION_PARAM_TYPE: SimpleType by lazy {
        ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)
    }

    /**
     * 单元期望类型
     * 表示不需要返回值的地方，一般用于if、try之类的多结果表达式
     */
    @JvmField
    val UNIT_EXPECTED_TYPE: SimpleType = SpecialType("UNIT_EXPECTED_TYPE")


    /**
     * 检测类型是否为Option类型
     *
     * 这个函数用于检查一个类型是否为Option类型（如 Int?、Option<Int>）。
     * 它会递归检查类型的Option属性，包括：
     * - 直接Option类型（isOption = true）
     * - 灵活类型的上界
     * - 类型参数的Option超类型
     * - 交集类型的Option超类型
     * - 内置Option类型
     *
     * 示例：
     * ```kotlin
     * // 基础类型
     * val intType: CangJieType = BasicType(...)
     * isOptionType(intType) // false
     *
     * // Option类型
     * val optionIntType: CangJieType = OptionType(intType)
     * isOptionType(optionIntType) // true
     *
     * // 嵌套Option类型
     * val nestedOptionType: CangJieType = OptionType(optionIntType)
     * isOptionType(nestedOptionType) // true
     *
     * // 灵活类型
     * val flexibleType: CangJieType = FlexibleType(Int类型, Option<Int>类型)
     * isOptionType(flexibleType) // true
     *
     * // 类型参数
     * val typeParam: CangJieType = TypeParameterType(...)
     * isOptionType(typeParam) // 取决于类型参数的约束
     * ```
     *
     * @param type 要检查的类型
     * @return true 如果类型是Option类型，false 否则
     */
    @JvmStatic
    fun isOptionType(type: CangJieType): Boolean {
        if (type.isOption) {
            return true
        }
        if (type.isFlexible() && isOptionType(type.asFlexibleType().upperBound)) {
            return true
        }
        if (type.isDefinitelyNonOptionType) {
            return false
        }
        if (isTypeParameter(type)) {
            return hasOptionSuperType(type)
        }
        if (type is AbstractStubType) {
            val typeVariableConstructor: NewTypeVariableConstructor = type.originalTypeVariable
            val typeParameter = typeVariableConstructor.originalTypeParameter
            return typeParameter == null || hasOptionSuperType(typeParameter.defaultType)
        }
        val constructor: TypeConstructor = type.constructor
        if (constructor is IntersectionTypeConstructor) {
            for (supertype in constructor.supertypes) {
                if (isOptionType(supertype)) return true
            }
        }

        return CangJieBuiltIns.isOptionType(type)


    }

    /**
     * 检查构造方法是否有参数
     *
     * 检查类描述符的所有构造方法是否都有参数。
     * 如果所有构造方法都有参数，返回true；否则返回false。
     *
     * 示例：
     * ```kotlin
     * val classDescriptor: ClassDescriptor = ...
     * val hasParams = checkConstructorsNotParameter(classDescriptor)
     * // 如果所有构造方法都有参数，返回true
     * ```
     *
     * @param classDescriptor 类描述符
     * @return true 如果所有构造方法都有参数，false 否则
     */
    @JvmStatic
    fun checkConstructorsNotParameter(classDescriptor: ClassDescriptor): Boolean {
        val constructors = classDescriptor.constructors

        return !constructors.any {
            it.valueParameters.isEmpty()
        }

    }

    /**
     * 特殊类型基类
     *
     * 表示特殊的类型，如无期望类型、单元期望类型等。
     * 这些类型在类型系统中具有特殊的行为。
     */
    open class SpecialType(private val name: String) : DelegatingSimpleType() {
        override val delegate: SimpleType
            get() {
                throw IllegalStateException(name)
            }


        override fun makeOptionAsSpecified(isOption: Boolean): SimpleType {
            throw IllegalStateException(name)

        }


        override fun toString(): String {
            return name
        }


        override fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType {
            throw IllegalStateException(name)
        }

        override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
            throw IllegalStateException(name)

        }


//        override val arguments: List<TypeProjection>
//            get() = TODO("Not yet implemented")
//        override val attributes: TypeAttributes
//            get() = TODO("Not yet implemented")

//        
//        override fun refine(CangJieTypeRefiner:  checker.CangJieTypeRefiner): SpecialType {
//            return this
//        }
    }

    //    表示不需要返回值的地方，一般用于if  try 之类的多结果表达式
//    @JvmField
//    val EXPRESSION_TYPE: SimpleType =
//        SpecialType("EXPRESSION_TYPE")

    //    表示没有指定类型，需要推断，用于需要返回值的地方
    @JvmField
    val NO_EXPECTED_TYPE: SimpleType = SpecialType("NO_EXPECTED_TYPE")

}


//=======================================================

/**
 * 检查类型是否包含满足条件的类型
 *
 * 扩展函数，用于检查CangJieType是否包含满足指定条件的类型。
 *
 * @param predicate 判断条件函数
 * @return true 如果类型包含满足条件的类型
 */
fun CangJieType.contains(predicate: (UnwrappedType) -> Boolean) = contains(this, predicate)

/**
 * 检查类型是否需要更新
 *
 * 检查类型是否为null或包含需要更新的特殊类型。
 *
 * @return true 如果类型需要更新
 */
fun CangJieType?.shouldBeUpdated() =
    this == null || contains { it is StubTypeForBuilderInference || it.constructor is TypeVariableTypeConstructorMarker || it.isError }

// 简化Option类型工具方法，适合IDE插件

/**
 * 检测类型是否为Option类型
 *
 * 简化版本的Option类型检测，直接检查类型的isOption属性。
 * 与TypeUtils.isOptionType()不同，这个函数不会递归检查复杂类型。
 *
 * 示例：
 * ```kotlin
 * val intType: CangJieType = BasicType(...)
 * isOptionType(intType) // false
 *
 * val optionIntType: CangJieType = OptionType(intType)
 * isOptionType(optionIntType) // true
 * ```
 *
 * @param type 要检查的类型
 * @return true 如果类型是Option类型，false 否则
 */
fun isOptionType(type: CangJieType): Boolean {
    return type.isOption
}

/**
 * 解包Option类型
 *
 * 递归解包Option类型，直到得到非Option的内部类型。
 * 对于嵌套的Option类型（如 Option<Option<Int>>），会完全解包到最内层类型。
 *
 * 示例：
 * ```kotlin
 * // 简单Option类型
 * val optionIntType: CangJieType = OptionType(intType)
 * val unboxedType = unboxOptionType(optionIntType) // 返回 Int 类型
 *
 * // 嵌套Option类型
 * val nestedOptionType: CangJieType = OptionType(optionIntType)
 * val unboxedType = unboxOptionType(nestedOptionType) // 返回 Int 类型
 *
 * // 非Option类型
 * val intType: CangJieType = BasicType(...)
 * val unboxedType = unboxOptionType(intType) // 返回 Int 类型（无变化）
 * ```
 *
 * @param type 要解包的Option类型
 * @return 解包后的内部类型
 */
fun unboxOptionType(type: CangJieType): CangJieType {
    var currentType = type
    while (currentType.isOption) {
        currentType = currentType.unwrapOption()
    }
    return currentType
}

/**
 * 计算Option嵌套层级
 *
 * 计算类型的Option嵌套深度，即Option包装的层数。
 * 对于非Option类型返回0，对于嵌套Option类型返回嵌套层数。
 *
 * 示例：
 * ```kotlin
 * // 非Option类型
 * val intType: CangJieType = BasicType(...)
 * countOptionNestedLevel(intType) // 0
 *
 * // 简单Option类型
 * val optionIntType: CangJieType = OptionType(intType)
 * countOptionNestedLevel(optionIntType) // 1
 *
 * // 嵌套Option类型
 * val nestedOptionType: CangJieType = OptionType(optionIntType)
 * countOptionNestedLevel(nestedOptionType) // 2
 *
 * // 三重嵌套
 * val tripleNestedType: CangJieType = OptionType(nestedOptionType)
 * countOptionNestedLevel(tripleNestedType) // 3
 * ```
 *
 * @param type 要计算嵌套层级的类型
 * @return Option嵌套层级数
 */
fun countOptionNestedLevel(type: CangJieType): Int {
    return type.countOptionNestedLevel()
}

/**
 * 创建Option类型
 *
 * 将给定的内部类型包装为Option类型。
 * 这是创建Option类型的标准方法，适用于IDE插件。
 *
 * 示例：
 * ```kotlin
 * val intType: CangJieType = BasicType(...)
 * val optionIntType = makeOptionType(intType) // 创建 Option<Int>
 *
 * val stringType: CangJieType = BasicType(...)
 * val optionStringType = makeOptionType(stringType) // 创建 Option<String>
 *
 * // 嵌套Option
 * val nestedOptionType = makeOptionType(optionIntType) // 创建 Option<Option<Int>>
 * ```
 *
 * @param innerType 要包装的内部类型
 * @return 包装后的Option类型
 */
fun makeOptionType(innerType: CangJieType): CangJieType {
    return OptionType(innerType)
}

fun CangJieType.makeOptional() = TypeUtils.makeOptional(this)
fun CangJieType.makeNonOption() = TypeUtils.makeNonOption(this)

/**
 * 创建嵌套的Option类型
 *
 * 创建指定层级的嵌套Option类型。level参数指定要包装的Option层数。
 *
 * 示例：
 * ```kotlin
 * val intType: CangJieType = BasicType(...)
 *
 * // 创建1层Option
 * val optionIntType = createNestedOptionType(intType, 1) // Option<Int>
 *
 * // 创建2层Option
 * val nestedOptionType = createNestedOptionType(intType, 2) // Option<Option<Int>>
 *
 * // 创建3层Option
 * val tripleNestedType = createNestedOptionType(intType, 3) // Option<Option<Option<Int>>>
 *
 * // level为0或负数时返回原类型
 * val sameType = createNestedOptionType(intType, 0) // 返回 intType
 * ```
 *
 * @param baseType 基础类型
 * @param level 要创建的Option嵌套层级
 * @return 嵌套的Option类型
 */
fun createNestedOptionType(baseType: CangJieType, level: Int): CangJieType {
    if (level <= 0) return baseType

    var result = baseType
    repeat(level) {
        result = OptionType(result)
    }
    return result
}

/**
 * 检查类型是否接受Option值
 *
 * 检查一个类型是否可以接受Option类型的值。
 * 这个函数用于类型检查中确定赋值兼容性。
 *
 * 示例：
 * ```kotlin
 * // Option类型可以接受Option值
 * val optionIntType: CangJieType = OptionType(intType)
 * acceptsOptionType(optionIntType) // true
 *
 * // 灵活类型的上界是Option类型时也可以接受
 * val flexibleType: CangJieType = FlexibleType(Int类型, Option<Int>类型)
 * acceptsOptionType(flexibleType) // true
 *
 * // 普通类型不能接受Option值
 * val intType: CangJieType = BasicType(...)
 * acceptsOptionType(intType) // false
 * ```
 *
 * @param type 要检查的类型
 * @return true 如果类型可以接受Option值，false 否则
 */
fun acceptsOptionType(type: CangJieType): Boolean {
    if (type.isOption) {
        return true
    }
    if (type.isFlexible() && acceptsOptionType(type.asFlexibleType().upperBound)) {
        return true
    }
    return false
}

/**
 * 获取Option类型的内部类型
 *
 * 如果类型是Option类型，返回其内部类型；否则返回null。
 *
 * 示例：
 * ```kotlin
 * val optionIntType: CangJieType = OptionType(intType)
 * val innerType = getOptionInnerType(optionIntType) // 返回 intType
 *
 * val intType: CangJieType = BasicType(...)
 * val innerType = getOptionInnerType(intType) // 返回 null
 * ```
 *
 * @param type 要检查的类型
 * @return Option类型的内部类型，如果不是Option类型则返回null
 */
fun getOptionInnerType(type: CangJieType): CangJieType? {
    return if (type.isOption) {
        type.unwrapOption()
    } else {
        null
    }
}

/**
 * 创建Option类型的字符串表示
 *
 * 将Option类型转换为标准的字符串表示形式。
 *
 * 示例：
 * ```kotlin
 * val optionIntType: CangJieType = OptionType(intType)
 * optionTypeToString(optionIntType) // "Option<Int>"
 *
 * val nestedOptionType: CangJieType = OptionType(optionIntType)
 * optionTypeToString(nestedOptionType) // "Option<Option<Int>>"
 * ```
 *
 * @param type 要转换的类型
 * @return Option类型的字符串表示
 */
fun optionTypeToString(type: CangJieType): String {
    return when (type) {
        is OptionType -> {
            "Option<${optionTypeToString(type.innerType)}>"
        }

        else -> type.toString()
    }
}

/**
 * 语法糖字符串表示
 *
 * 将Option类型转换为语法糖形式的字符串表示。
 *
 * 示例：
 * ```kotlin
 * val optionIntType: CangJieType = OptionType(intType)
 * optionTypeToSugarString(optionIntType) // "Int?"
 *
 * val nestedOptionType: CangJieType = OptionType(optionIntType)
 * optionTypeToSugarString(nestedOptionType) // "Int??"
 * ```
 *
 * @param type 要转换的类型
 * @return Option类型的语法糖字符串表示
 */
fun optionTypeToSugarString(type: CangJieType): String {
    return when (type) {
        is OptionType -> {
            "${optionTypeToSugarString(type.innerType)}?"
        }

        else -> type.toString()
    }
}

fun CangJieType.replaceAnnotations(newAnnotations: Annotations): CangJieType {
    if (annotations.isEmpty() && newAnnotations.isEmpty()) return this
    return unwrap().replaceAttributes(attributes.replaceAnnotations(newAnnotations))
}

fun CangJieType.asTypeProjection(): TypeProjection = TypeProjectionImpl(this)


fun CangJieType.supertypes(): Collection<CangJieType> = TypeUtils.getAllSupertypes(this)

fun CangJieType.isTypeParameter(): Boolean = TypeUtils.isTypeParameter(this)

fun classFqNameEquals(
    descriptor: ClassifierDescriptor, fqName: FqNameUnsafe
): Boolean {

    return descriptor.name == fqName.shortName() && fqName == DescriptorUtils.getFqName(
        descriptor
    )

}

//获取类型的classkind
val CangJieType.classKind: ClassKind
    get() {
        return when (val descriptor = this.constructor.declarationDescriptor) {
            is ClassDescriptor -> descriptor.kind
            else -> error("not a class: $this")
        }

    }

fun isSpecialType(type: CangJieType): Boolean {
    return type is TypeUtils.SpecialType
}

fun isNotNullConstructedFromGivenClass(
    type: CangJieType, fqName: FqNameUnsafe
): Boolean {
    if (isSpecialType(type)) return false

    return isTypeConstructorForGivenClass(type.constructor, fqName)
}

fun isConstructedFromGivenClass(
    type: CangJieType, fqName: FqNameUnsafe
): Boolean {
    if (isSpecialType(type)) return false
    return if (isTypeConstructorForGivenClass(type.constructor, fqName)) {
        true
    } else if (type.constructor.declarationDescriptor is ClassDescriptor && DescriptorUtils.getFqName(type.constructor.declarationDescriptor!!) == optionUFqName) {

        isConstructedFromGivenClass(type.arguments[0].type, fqName)

    } else {
        false
    }
}


fun isTypeConstructorForGivenClass(
    typeConstructor: TypeConstructor, fqName: FqNameUnsafe
): Boolean {

    val descriptor = typeConstructor.declarationDescriptor
    return descriptor is ClassDescriptor && classFqNameEquals(
        descriptor, fqName
    )
}

private fun CangJieType.containsSelfTypeParameter(
    baseConstructor: TypeConstructor, visitedTypeParameters: Set<TypeParameterDescriptor>?
): Boolean {
    if (this.constructor == baseConstructor) return true

    val typeParameters =
        (constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
    return arguments.withIndex().any { (i, argument) ->
        val typeParameter = typeParameters?.getOrNull(i)
        val isTypeParameterVisited =
            typeParameter != null && visitedTypeParameters != null && typeParameter in visitedTypeParameters
        if (isTypeParameterVisited) return@any false
        argument.type.containsSelfTypeParameter(baseConstructor, visitedTypeParameters)
    }
}

@JvmOverloads
fun hasTypeParameterRecursiveBounds(
    typeParameter: TypeParameterDescriptor,
    selfConstructor: TypeConstructor? = null,
    visitedTypeParameters: Set<TypeParameterDescriptor>? = null
): Boolean = typeParameter.upperBounds.any { upperBound ->
    upperBound.containsSelfTypeParameter(
        typeParameter.defaultType.constructor,
        visitedTypeParameters
    ) && (selfConstructor == null || upperBound.constructor == selfConstructor)
}

@OptIn(ExperimentalContracts::class)
fun isUnresolvedType(type: CangJieType): Boolean {
    contract {
        returns(true) implies (type is ErrorType)
    }
    return type is ErrorType && type.kind.isUnresolved
}

fun CangJieType.isStubType() = this is AbstractStubType || isDefNonOptionStubType<AbstractStubType>()
private inline fun <reified S : AbstractStubType> CangJieType.isDefNonOptionStubType() =
    this is DefinitelyNonOptionType && this.original is S

fun CangJieType.isStubTypeForBuilderInference(): Boolean =
    this is StubTypeForBuilderInference || isDefNonOptionStubType<StubTypeForBuilderInference>()

fun CangJieType.isStubTypeForVariableInSubtyping(): Boolean =
    this is StubTypeForTypeVariablesInSubtyping || isDefNonOptionStubType<StubTypeForTypeVariablesInSubtyping>()

fun CangJieType.isSignedOrUnsignedNumberType(): Boolean = isPrimitiveNumberType() /*|| isUnsignedNumberType()*/

/**
 * 封装了一个类型替换的过程，其中一个 <code>CangJieType</code> 类型被另一个 <code>CangJieType</code> 类型替代。
 * 该类表示一个映射关系，将特定类型（forType）替换为新的类型（byType）。
 *
 * @property forType 被替换的原始类型。
 * @property byType 用来替代的新的类型。
 */
internal class CangJieTypeSubstitution(val forType: CangJieType, val byType: CangJieType)

fun CangJieType.isPrimitiveNumberType(): Boolean = CangJieBuiltIns.isPrimitiveType(this) && !isBoolean
val CangJieType.isBoolean: Boolean get() = CangJieBuiltIns.isBoolean(this)
fun CangJieType.replaceArgumentsWithProjections() = replaceArgumentsByParametersWith(::makeProjection)
val CangJieType.isUnit: Boolean get() = CangJieBuiltIns.isUnit(this)
fun CangJieType.substitute(byType: CangJieType): CangJieType {
    return substitute(CangJieTypeSubstitution(this, byType))
}

internal fun CangJieType.substitute(substitution: CangJieTypeSubstitution): CangJieType {
    val nullable = isOption
    val currentType = makeNonOption()

    return if (DEFAULT.equalTypes(currentType, substitution.forType)
    ) {
        makeOptionalAsSpecified(substitution.byType, nullable)
    } else {
        val newArguments = arguments.zip(constructor.parameters).map { pair ->
            val (projection, typeParameter) = pair
            TypeProjectionImpl(Variance.INVARIANT, projection.type.substitute(substitution))
        }
        simpleTypeWithNonTrivialMemberScope(
            annotations.toDefaultAttributes(),
            constructor,
            newArguments,
            isOption,
            memberScope
        )
    }
}

inline fun SimpleType.replaceArgumentsByExistingArgumentsWith(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleType {
    if (arguments.isEmpty()) return this
    return replace(newArguments = arguments.map { replacement(it) as TypeProjection })
}

inline fun CangJieType.replaceArgumentsByParametersWith(replacement: (TypeParameterDescriptor) -> TypeProjection): CangJieType {
    val unwrapped = unwrap()
    return when (unwrapped) {
        is FlexibleType -> CangJieTypeFactory.flexibleType(
            unwrapped.lowerBound.replaceArgumentsByParametersWith(replacement),
            unwrapped.upperBound.replaceArgumentsByParametersWith(replacement)
        )

        is SimpleType -> unwrapped.replaceArgumentsByParametersWith(replacement)
    }.inheritEnhancement(unwrapped)
}

inline fun SimpleType.replaceArgumentsByParametersWith(replacement: (TypeParameterDescriptor) -> TypeProjection): SimpleType {
    if (constructor.parameters.isEmpty() || constructor.declarationDescriptor == null) return this

    val newArguments = constructor.parameters.map(replacement)

    return replace(newArguments)
}

fun CangJieType.extractTypeParametersFromUpperBounds(visitedTypeParameters: Set<TypeParameterDescriptor>?): Set<TypeParameterDescriptor> =
    mutableSetOf<TypeParameterDescriptor>().also {
        extractTypeParametersFromUpperBounds(
            this,
            it,
            visitedTypeParameters
        )
    }

private fun CangJieType.extractTypeParametersFromUpperBounds(
    baseType: CangJieType,
    to: MutableSet<TypeParameterDescriptor>,
    visitedTypeParameters: Set<TypeParameterDescriptor>?
) {
    val declarationDescriptor = constructor.declarationDescriptor

    if (declarationDescriptor is TypeParameterDescriptor) {
        if (constructor != baseType.constructor) {
            to += declarationDescriptor
        } else {
            for (upperBound in declarationDescriptor.upperBounds) {
                upperBound.extractTypeParametersFromUpperBounds(baseType, to, visitedTypeParameters)
            }
        }
    } else {
        val typeParameters =
            (constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
        for ((i, argument) in arguments.withIndex()) {
            val typeParameter = typeParameters?.getOrNull(i) // TODO: support inner classes' type parameters
            val isTypeParameterVisited =
                typeParameter != null && visitedTypeParameters != null && typeParameter in visitedTypeParameters
            if (isTypeParameterVisited) continue
            if (argument.type.constructor.declarationDescriptor in to || argument.type.constructor == baseType.constructor) continue
            argument.type.extractTypeParametersFromUpperBounds(baseType, to, visitedTypeParameters)
        }
    }
}

val CangJieType.isEnum: Boolean get() = constructor.declarationDescriptor is EnumDescriptor
val CangJieType.source: CjElement? get() = constructor.declarationDescriptor?.source?.getPsi() as? CjElement
val CangJieType.isStruct: Boolean get() = (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.STRUCT

fun CangJieType.containsTypeParameter(): Boolean = contains(this) { t -> TypeUtils.isTypeParameter(t) }
val CangJieType.deccriptorClass: ClassDescriptor?
    get() {

        return constructor.declarationDescriptor as? ClassDescriptor
    }

fun CangJieType.isNothing(): Boolean = CangJieBuiltIns.isNothing(this)

fun CangJieType.isAny(): Boolean = CangJieBuiltIns.isAny(this)
fun CangJieType.containsError() = ErrorUtils.containsErrorType(this)
fun CangJieType.isSubtypeOf(superType: CangJieType): Boolean = DEFAULT.isSubtypeOf(this, superType)

fun createProjection(
    type: CangJieType,
    projectionKind: Variance,
    typeParameterDescriptor: TypeParameterDescriptor?
): TypeProjection =
    TypeProjectionImpl(
        if (typeParameterDescriptor?.variance == projectionKind) Variance.INVARIANT else projectionKind,
        type
    )

fun List<CangJieType>.defaultProjections(): List<TypeProjection> = map(::TypeProjectionImpl)

fun CangJieType.isDefaultBound(): Boolean = CangJieBuiltIns.isDefaultBound(getSupertypeRepresentative())

fun CangJieTypeChecker.equalTypesOrNulls(type1: CangJieType?, type2: CangJieType?): Boolean {
    if (type1 === type2) return true
    if (type1 == null || type2 == null) return false
    return equalTypes(type1, type2)
}

fun CangJieType.isInterface(): Boolean =
    (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.INTERFACE

/**
 * 根据Name获取指定父类型 ，如果获取不到，则返回第一个父类型
 * @param name Name
 */
fun CangJieType.extractSuperType(name: Name): CangJieType {
    val superTypes = this.supertypes()
    for (superType in superTypes) {
        if (superType.constructor.declarationDescriptor?.name == name) {
            return superType
        }
    }
    return superTypes.firstOrNull() ?: this

}

fun CangJieType?.isArrayOfNothing(): Boolean {
    if (this == null || !CangJieBuiltIns.isArray(this)) return false
    val typeArg = arguments.firstOrNull()?.type
    return typeArg != null && CangJieBuiltIns.isNothingOrNullableNothing(typeArg)
}

fun CangJieType.constituentTypes(): Collection<CangJieType> =
    constituentTypes(listOf(this))

private fun constituentTypes(result: MutableSet<CangJieType>, types: Collection<CangJieType>) {
    result.addAll(types)
    for (type in types) {
        if (type.isFlexible()) {
            with(type.asFlexibleType()) { constituentTypes(result, setOf(lowerBound, upperBound)) }
        } else {
            constituentTypes(result, type.arguments.map { it.type })
        }
    }
}

fun constituentTypes(types: Collection<CangJieType>): Collection<CangJieType> {
    val result = hashSetOf<CangJieType>()
    constituentTypes(result, types)
    return result
}

fun createVArrayType(
    builtIns: CangJieBuiltIns,
    argument: CangJieType,
    size: Int
): VArrayType {
    val descriptor = VArrayTypeDescriptor(

        builtIns.builtInsModule, builtIns, builtIns.storageManager
    )
    descriptor.init(argument, size)
    return descriptor.defaultType

}
fun CangJieType.replaceArgument(vararg newType: CangJieType): CangJieType {

    val arguments = newType.map {
        TypeProjectionImpl(it)
    }
    return simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        arguments,
        isOption,
        memberScope
    )
}

val CangJieType.isVArray: Boolean
    get() {
        return this is VArrayType
    }
fun CangJieType.containsTypeAliases(): Boolean =
    contains {
        it.constructor.declarationDescriptor is TypeAliasDescriptor
    }
fun ClassifierDescriptor.isTypeAliasParameter(): Boolean =
    this is TypeParameterDescriptor && containingDeclaration is TypeAliasDescriptor

fun CangJieType.containsTypeAliasParameters(): Boolean =
    contains {
        it.constructor.declarationDescriptor?.isTypeAliasParameter() ?: false
    }
fun CangJieType.isPrimitiveNumber(): Boolean =
    CangJieBuiltIns.isPrimitiveType(this) &&
            !CangJieBuiltIns.isBoolean(this) &&
            !CangJieBuiltIns.isRune(this)

fun CangJieType.immediateSupertypes(): Collection<CangJieType> =  TypeUtils.getImmediateSupertypes(this)

fun CangJieType.isGenericArrayOfTypeParameter(): Boolean {
    if (!CangJieBuiltIns.isArray(this)) return false
    val argument0 = arguments[0]

    val argument0type = argument0.type
    return argument0type.isTypeParameter() ||
            argument0type.isGenericArrayOfTypeParameter()
}

val CangJieType.isInt8 get() = CangJieBuiltIns.isInt8(this)
val CangJieType.isRune get() = CangJieBuiltIns.isRune(this)
val CangJieType.isInt16 get() = CangJieBuiltIns.isInt16(this)
val CangJieType.isInt32 get() = CangJieBuiltIns.isInt32(this)
val CangJieType.isInt64 get() = CangJieBuiltIns.isInt64(this)
val CangJieType.isFloat16 get() = CangJieBuiltIns.isFloat16(this)
val CangJieType.isFloat32 get() = CangJieBuiltIns.isFloat32(this)
val CangJieType.isFloat64 get() = CangJieBuiltIns.isFloat64(this)
val TypeParameterDescriptor.representativeUpperBound: CangJieType
    get() {
        assert(upperBounds.isNotEmpty()) { "Upper bounds should not be empty: $this" }

        return upperBounds.firstOrNull {
            val classDescriptor = it.constructor.declarationDescriptor as? ClassDescriptor ?: return@firstOrNull false
            classDescriptor.kind != ClassKind.INTERFACE && !classDescriptor.isAnnotatinoDescriptor()
        } ?: upperBounds.first()
    }

fun unCaptureProjection(projection: TypeProjection): TypeProjection {
    val unCapturedProjection = (projection.type.constructor as? NewCapturedTypeConstructor)?.projection ?: projection
    if (unCapturedProjection.type is ErrorType) return unCapturedProjection

    val newArguments = unCapturedProjection.type.arguments.map(::unCaptureProjection)
    return TypeProjectionImpl(
        unCapturedProjection.projectionKind,
        unCapturedProjection.type.replace(newArguments)
    )
}
fun CangJieType.unCapture(): CangJieType = unwrap().unCapture()

fun SimpleType.unCapture(): UnwrappedType {
    if (this is ErrorType) return this
    if (this is NewCapturedType)
        return unCaptureTopLevelType()

    val newArguments = arguments.map(::unCaptureProjection)
    return replace(newArguments).unwrap()
}
private fun NewCapturedType.unCaptureTopLevelType(): UnwrappedType {
    if (lowerType != null) return lowerType

    val supertypes = constructor.supertypes
    if (supertypes.isNotEmpty()) return intersectTypes(supertypes)

    return constructor.projection.type.unwrap()
}
fun AbbreviatedType.unCapture(): SimpleType {
    val newType = expandedType.unCapture()
    return AbbreviatedType(newType as? SimpleType ?: expandedType, abbreviation)
}
fun CangJieType.expandIntersectionTypeIfNecessary(): Collection<CangJieType> {
    if (constructor !is IntersectionTypeConstructor) return listOf(this)
    val types = constructor.supertypes
    return if (isMarkedOption()) {
        types.map { it.makeOptional() }
    } else {
        types
    }
}

fun UnwrappedType.unCapture(): UnwrappedType = when (this) {

    is AbbreviatedType -> unCapture()
    is SimpleType -> unCapture()
    is FlexibleType -> unCapture()

}
fun FlexibleType.unCapture(): FlexibleType {
    val unCapturedLowerBound = when (val unCaptured = lowerBound.unCapture()) {
        is SimpleType -> unCaptured
        is FlexibleType -> unCaptured.lowerBound
    }

    val unCapturedUpperBound = when (val unCaptured = upperBound.unCapture()) {
        is SimpleType -> unCaptured
        is FlexibleType -> unCaptured.upperBound
    }

    return FlexibleTypeImpl(unCapturedLowerBound, unCapturedUpperBound)
}