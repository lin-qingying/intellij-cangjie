/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.metadata.model.*
import org.cangnova.cangjie.metadata.deserialization.TypeTable
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.StandardClassIds.ArrayClassId
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.CangJieTypeFactory
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeAttributeTranslator
import org.cangnova.cangjie.types.TypeAttributes
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeProjectionImpl
import org.cangnova.cangjie.types.builtIns
import org.cangnova.cangjie.types.createFunctionType
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.getContextReceiverTypesFromFunctionType
import org.cangnova.cangjie.types.getReceiverTypeFromFunctionType
import org.cangnova.cangjie.types.getValueParameterTypesFromFunctionType
import org.cangnova.cangjie.types.isFunctionType

/**
 * 类型反序列化器，负责将flatbuffer格式的类型信息转换为CangJieType对象。
 *
 * 该类是CangJie语言反序列化系统的核心组件，处理以下类型的反序列化：
 * - 类和接口类型
 * - 类型参数（泛型参数）
 * - 类型别名
 * - 函数类型（包括挂起函数类型）
 * - 可空类型和灵活类型
 *
 * 支持嵌套类型和复杂类型结构的反序列化，通过缓存机制优化性能。
 *
 * @param c 反序列化上下文，提供必要的反序列化组件和配置
 * @param parent 父类型反序列化器，用于处理嵌套作用域中的类型参数
 * @param generic 泛型信息，定义当前作用域中的类型参数和约束
 * @param debugName 调试名称，用于日志和调试输出
 * @param containerPresentableName 容器的可展示名称，用于错误消息和调试信息
 */
class TypeDeserializer(
    private val c: DeserializationContext,
    private val parent: TypeDeserializer?,
//    generic: Generic?,
    private val debugName: String,
    private val containerPresentableName: String
) {
    private val classifierDescriptors: (ClassId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { id ->
            computeClassifierDescriptor(id)
        }

    private fun computeClassifierDescriptor(id: ClassId): ClassifierDescriptor? {

//        if (id.isLocal) {
//            // Local classes can't be found in scopes
//            return c.components.deserializeClass(id)
//        }
        return c.components.moduleDescriptor.findClassifierAcrossModuleDependencies(id)
    }

    /**
     * 类型参数描述符映射，将类型参数ID映射到对应的类型参数描述符。
     *
     * 根据Generic中的类型参数信息创建对应的类型参数描述符，
     * 用于处理泛型类型中的类型参数引用。
     */
    private val typeParameterDescriptors: Map<Int, TypeParameterDescriptor> = emptyMap()
//        if (generic?.typeParameters?.isEmpty() != false) {
//            emptyMap()
//        } else {
//            val result = LinkedHashMap<Int, TypeParameterDescriptor>()
//            for ((index, typeParameterId) in generic.typeParameters.withIndex()) {
//                // Find corresponding constraint for this type parameter
//                val constraint = generic.constraints.find { it.type.toInt() == typeParameterId }
//                result[typeParameterId] = DeserializedTypeParameterDescriptor(c, typeParameterId, index, constraint)
//            }
//            result
//        }

    /**
     * 当前作用域中定义的所有类型参数列表。
     *
     * 按定义顺序返回类型参数描述符，用于处理类型参数的顺序依赖。
     */
    val ownTypeParameters: List<TypeParameterDescriptor>
        get() = typeParameterDescriptors.values.toList()

    /**
     * 将SemaTy类型转换为CangJieType对象。
     *
     * 这是类型反序列化的主入口方法，处理各种类型的转换。
     *
     * @param semaTy flatbuffer格式的语义类型定义
     * @return 转换后的CangJieType对象
     */
    // TODO: don't load identical types from TypeTable more than once
    fun type(semaTy: SemaTy): CangJieType {
        return simpleType(semaTy, expandTypeAliases = true)
    }

    /**
     * 将类型注解转换为类型属性。
     *
     * 这个扩展函数将类型注解列表转换为类型属性集合，用于构建类型时附加元数据。
     *
     * @param annotations 类型的注解集合
     * @param constructor 类型构造器
     * @param containingDeclaration 包含该类型的声明描述符
     * @return 转换后的类型属性集合
     */
    private fun List<TypeAttributeTranslator>.toAttributes(
        annotations: Annotations,
        constructor: TypeConstructor,
        containingDeclaration: DeclarationDescriptor
    ): TypeAttributes {
        val translated = this.map { translator ->
            translator.toAttributes(annotations, constructor, containingDeclaration)
        }.flatten()
        return TypeAttributes.create(translated)
    }

    /**
     * 将SemaTy类型转换为SimpleType对象。
     *
     * 处理各种类型的反序列化，包括：
     * - 基本类型处理
     * - 类型构造器解析
     * - 类型参数处理
     * - 类型别名展开
     * - 函数类型处理
     *
     * @param semaTy flatbuffer格式的语义类型定义
     * @param expandTypeAliases 是否展开类型别名，默认为true
     * @return 转换后的SimpleType对象
     */
    fun simpleType(semaTy: SemaTy, expandTypeAliases: Boolean = true): SimpleType {
        val constructor = typeConstructor(semaTy)
        if (ErrorUtils.isError(constructor.declarationDescriptor)) {
            return ErrorUtils.createErrorType(
                ErrorTypeKind.TYPE_FOR_ERROR_TYPE_CONSTRUCTOR,
                constructor,
                constructor.toString()
            )
        }

        // For now, use empty annotations since the new model doesn't have annotation loading yet
        val annotations = Annotations.EMPTY
        val attributes = TypeAttributes.Empty

        // Convert type arguments from indices to TypeProjection objects
        val arguments = semaTy.typeArgs.mapIndexed { index, typeIndex ->
            val typeArg = c.typeTable[typeIndex]
            TypeProjectionImpl(type(typeArg))
        }

        val declarationDescriptor = constructor.declarationDescriptor

        val simpleType = when {
            expandTypeAliases && declarationDescriptor is TypeAliasDescriptor -> {
                val expandedType = with(CangJieTypeFactory) { declarationDescriptor.computeExpandedType(arguments) }
                expandedType.replaceAttributes(attributes)
            }

            semaTy.kind == TypeKind.Func -> {
                // Handle function types
                val nullable = false // TODO: Extract nullability from semaTy
                CangJieTypeFactory.simpleType(attributes, constructor, arguments, nullable)
            }

            else -> {
                val nullable = false // TODO: Extract nullability from semaTy  
                CangJieTypeFactory.simpleType(attributes, constructor, arguments, nullable)
            }
        }

        return simpleType
    }

    /**
     * 从SemaTy类型中提取类型构造器。
     *
     * 根据SemaTy类型中的信息，解析并返回对应的类型构造器。处理以下情况：
     * - 基本类型：返回内置类型构造器
     * - 复合类型：解析为类描述符的类型构造器
     * - 泛型类型：解析为类型参数的类型构造器
     * - 函数类型：返回函数类型构造器
     *
     * @param semaTy flatbuffer格式的语义类型定义
     * @return 对应的类型构造器
     */
    private fun typeConstructor(semaTy: SemaTy): TypeConstructor {


        fun notFoundClass(classId: ClassId): ClassDescriptor {

            val typeParametersCount =
                listOf(semaTy)  .map { it.typeArgs.size }.toMutableList()

//                generateSequence(semaTy) { it/*.outerType(c.typeTable)*/ }.map { it.typeArgs.size }.toMutableList()
            val classNestingLevel = generateSequence(classId, ClassId::outerClassId).count()
            while (typeParametersCount.size < classNestingLevel) {
                typeParametersCount.add(0)
            }
            return c.components.notFoundClasses.getClass(classId, typeParametersCount)
        }

        return when (semaTy.kind) {
            // 基本类型
            TypeKind.Unit -> c.builtIns.unitType.constructor
            TypeKind.Bool -> c.builtIns.boolType.constructor
            TypeKind.Int8 -> c.builtIns.int8Type.constructor
            TypeKind.Int16 -> c.builtIns.int16Type.constructor
            TypeKind.Int32 -> c.builtIns.int32Type.constructor
            TypeKind.Int64 -> c.builtIns.int64Type.constructor
            TypeKind.IntNative ->
                c.builtIns.intNativeType.constructor

            TypeKind.UInt8 -> c.builtIns.uint8Type.constructor
            TypeKind.UInt16 -> c.builtIns.uint16Type.constructor
            TypeKind.UInt32 -> c.builtIns.uint32Type.constructor
            TypeKind.UInt64 -> c.builtIns.uint64Type.constructor
            TypeKind.UIntNative ->
                c.builtIns.uintNativeType.constructor // TODO: Add unsigned int support
            TypeKind.Float16 -> c.builtIns.float16Type.constructor
            TypeKind.Float32 -> c.builtIns.float32Type.constructor
            TypeKind.Float64 ->
                c.builtIns.float64Type.constructor

            TypeKind.Rune -> c.builtIns.runeType.constructor
            TypeKind.Nothing -> c.builtIns.nothingType.constructor

//如果是Array，说明该包是std.core，那么Array的声明只会在同一包中出现，所以直接在本包中查找声明，通过name的方法
            TypeKind.Array -> {
                val info = (semaTy.info as SemaTyInfo.Array).info
                (classifierDescriptors(ArrayClassId) ?: notFoundClass(ArrayClassId)).typeConstructor
            }

            // 复合类型 (Class, Interface, Struct, Enum)
           TypeKind.Class, TypeKind.Interface, TypeKind.Struct, TypeKind.Enum -> {
                when (val info = semaTy.info) {


                    else -> ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
                }
            }
//
//            // 泛型类型
//            TypeKind.Generic -> {
//                when (val info = semaTy.info) {
//                    is SemaTyInfo.Generic -> {
//                        if (info.info.declPtr != null) {
//                            val decl = c.declResolver.resolve(info.info.declPtr)
//                            (decl as? ClassifierDescriptor)?.typeConstructor
//                                ?: ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
//                        } else {
//                            // 可能是类型参数引用
//                            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
//                        }
//                    }
//
//                    else -> ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
//                }
//            }

            // 函数类型
            TypeKind.Func -> {
                when (val info = semaTy.info) {
                    is SemaTyInfo.Func -> {
                        val arity = semaTy.typeArgs.size - 1 // 减去返回类型
                        if (arity >= 0) {
                            c.builtIns.getFunction(arity).typeConstructor
                        } else {
                            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
                        }
                    }

                    else -> ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
                }
            }

            // 数组类型
//            TypeKind.VArray -> {
//                c.builtIns.array.typeConstructor
//            }

            // 元组类型
            TypeKind.Tuple -> {
                // TODO: 实现元组类型支持
                ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
            }

            // 其他类型
            else -> ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
        }
    }

    /**
     * 创建挂起函数类型。
     *
     * 根据提供的类型属性、函数类型构造器和类型参数，创建表示挂起函数的类型。
     * 处理不同版本编译器生成的挂起函数类型格式。
     *
     * @param attributes 类型属性
     * @param functionTypeConstructor 函数类型构造器
     * @param arguments 类型参数列表
     * @param nullable 类型是否可空
     * @return 创建的挂起函数类型
     */
    private fun createSuspendFunctionType(
        attributes: TypeAttributes,
        functionTypeConstructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean
    ): SimpleType {
        val result = when (functionTypeConstructor.parameters.size - arguments.size) {
            0 -> createSuspendFunctionTypeForBasicCase(attributes, functionTypeConstructor, arguments, nullable)
            // This case for types written by eap compiler 1.1
            1 -> {
                val arity = arguments.size - 1
                if (arity >= 0) {
                    CangJieTypeFactory.simpleType(
                        attributes,
                        functionTypeConstructor.builtIns.getFunction(arity).typeConstructor,
                        arguments,
                        nullable
                    )
                } else {
                    null
                }
            }

            else -> null
        }
        return result ?: ErrorUtils.createErrorTypeWithArguments(
            ErrorTypeKind.INCONSISTENT_SUSPEND_FUNCTION, arguments, functionTypeConstructor
        )
    }

    /**
     * 为基本情况创建挂起函数类型。
     *
     * 处理标准情况下的挂起函数类型创建，验证类型是否为函数类型。
     *
     * @param attributes 类型属性
     * @param functionTypeConstructor 函数类型构造器
     * @param arguments 类型参数列表
     * @param nullable 类型是否可空
     * @return 创建的挂起函数类型，如果不是函数类型则返回null
     */
    private fun createSuspendFunctionTypeForBasicCase(
        attributes: TypeAttributes,
        functionTypeConstructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean
    ): SimpleType? {
        val functionType = CangJieTypeFactory.simpleType(attributes, functionTypeConstructor, arguments, nullable)
        return if (!functionType.isFunctionType) null
        else functionType
    }

//    private fun transformRuntimeFunctionTypeToSuspendFunction(funType: CangJieType): SimpleType? {
//        val continuationArgumentType = funType.getValueParameterTypesFromFunctionType().lastOrNull()?.type ?: return null
//        val continuationArgumentFqName = continuationArgumentType.constructor.declarationDescriptor?.fqNameSafe
//        // Before 1.6 we put experimental continuation as last parameter of suspend functional types to .kotlin_metadata files.
//        // Read them as suspend functional types instead of ordinary types with experimental continuation parameter.
//        if (continuationArgumentType.arguments.size != 1 ||
//            !(continuationArgumentFqName == CONTINUATION_INTERFACE_FQ_NAME || continuationArgumentFqName == EXPERIMENTAL_CONTINUATION_FQ_NAME)
//        ) {
//            return funType as SimpleType?
//        }
//
//        val suspendReturnType = continuationArgumentType.arguments.single().type
//
//        // Load kotlin.suspend as accepting and returning suspend function type independent of its version requirement
//        if ((c.containingDeclaration as? CallableDescriptor)?.fqNameOrNull() == CANGJIE_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME) {
//            return createSimpleSuspendFunctionType(funType, suspendReturnType)
//        }
//
//        return createSimpleSuspendFunctionType(funType, suspendReturnType)
//    }

    /**
     * 创建简单的挂起函数类型。
     *
     * 从现有函数类型和挂起返回类型创建挂起函数类型。
     * 处理挂起函数的特殊返回类型转换。
     *
     * @param funType 原始函数类型
     * @param suspendReturnType 挂起函数的返回类型
     * @return 创建的挂起函数类型
     */
    private fun createSimpleSuspendFunctionType(
        funType: CangJieType,
        suspendReturnType: CangJieType
    ): SimpleType {
        return createFunctionType(
            funType.builtIns,
            funType.annotations,
            funType.getReceiverTypeFromFunctionType(),
            funType.getContextReceiverTypesFromFunctionType(),
            funType.getValueParameterTypesFromFunctionType().dropLast(1).map(TypeProjection::type),
            // TODO: names
            null,
            suspendReturnType,

            ).makeOptionAsSpecified(funType.isOption)
    }

    /**
     * 加载类型参数描述符。
     *
     * 根据类型参数ID查找对应的类型参数描述符。首先在当前作用域查找，
     * 如果未找到则在父作用域中继续查找。
     *
     * @param typeParameterId 类型参数ID
     * @return 找到的类型参数描述符，未找到则返回null
     */
    private fun loadTypeParameter(typeParameterId: Int): TypeParameterDescriptor? =
        typeParameterDescriptors[typeParameterId] ?: parent?.loadTypeParameter(typeParameterId)

    /**
     * 返回类型反序列化器的字符串表示。
     *
     * 包含调试名称和父反序列化器的信息（如果存在）。
     *
     * @return 类型反序列化器的字符串表示
     */
    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")
}
