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
import org.cangnova.cangjie.descriptors.impl.FunctionClassDescriptor
import org.cangnova.cangjie.descriptors.impl.TupleClassDescriptor
import org.cangnova.cangjie.metadata.model.fb.FbSemaTy
import org.cangnova.cangjie.metadata.model.fb.FbSemaTyInfo
import org.cangnova.cangjie.metadata.model.fb.FbTypeKind
import org.cangnova.cangjie.metadata.model.fb.FbFullId
import org.cangnova.cangjie.metadata.PackageIndex
import org.cangnova.cangjie.metadata.model.wrapper.TypeParameterWrapper
import org.cangnova.cangjie.metadata.model.wrapper.TypeWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.StandardClassIds
import org.cangnova.cangjie.name.StandardClassIds.ArrayClassId
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
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
import kotlin.toString

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
    typeParameters: List<TypeParameterWrapper>,
    private val debugName: String,
    private val containerPresentableName: String
) {
    private val classifierDescriptors: (ClassId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { id ->
            computeClassifierDescriptor(id)
        }
    private val typeAliasDescriptors: (ClassId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { fqNameIndex ->
            computeTypeAliasDescriptor(fqNameIndex)
        }

    private fun computeTypeAliasDescriptor(id: ClassId): ClassifierDescriptor? {
        return c.components.moduleDescriptor.findTypeAliasAcrossModuleDependencies(id)
    }

    private fun computeClassifierDescriptor(id: ClassId): ClassifierDescriptor? {
        return c.components.moduleDescriptor.findClassifierAcrossModuleDependencies(id)
    }


    private val classifierDescriptorsByFullId: (FbFullId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { id ->
            computeClassifierDescriptorByFullId(id)
        }
    private val typeAliasDescriptorsByFullId: (FbFullId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { fqNameIndex ->
            computeTypeAliasDescriptorByFullId(fqNameIndex)
        }

    private fun computeTypeAliasDescriptorByFullId(id: FbFullId): TypeAliasDescriptor? {
        return c.fullIdFinder.findTypeAliasDescriptorByFullId(id)
    }

    private fun computeClassifierDescriptorByFullId(id: FbFullId): ClassifierDescriptor? {
        return c.fullIdFinder.findClassifierDescriptorByFullId(id)
    }

    /**
     * 类型参数描述符映射，将类型参数ID映射到对应的类型参数描述符。
     *
     * 根据Generic中的类型参数信息创建对应的类型参数描述符，
     * 用于处理泛型类型中的类型参数引用。
     */
    private val typeParameterDescriptors: Map<FbSemaTy, TypeParameterDescriptor> =
        if (typeParameters.isEmpty()) {
            emptyMap()
        } else {
            val result = LinkedHashMap<FbSemaTy, TypeParameterDescriptor>()
            for ((index, type) in typeParameters.withIndex()) {
                result[type.ownType.original] = DeserializedTypeParameterDescriptor(c, type, index)
            }
            result
        }

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
    fun type(semaTy: TypeWrapper): CangJieType {
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
    fun simpleType(semaTy: TypeWrapper, expandTypeAliases: Boolean = true): SimpleType {
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
        val arguments = semaTy.typeArgs.mapIndexed { index, type ->

            TypeProjectionImpl(type(type))
        }

        val declarationDescriptor = constructor.declarationDescriptor

        val simpleType = when {
            expandTypeAliases && declarationDescriptor is TypeAliasDescriptor -> {
                val expandedType = with(CangJieTypeFactory) { declarationDescriptor.computeExpandedType(arguments) }
                expandedType.replaceAttributes(attributes)
            }

            semaTy.kind == FbTypeKind.Tuple -> {
                // Handle function types
                val nullable = false // TODO: Extract nullability from semaTy
                constructor as TupleClassDescriptor.TupleTypeConstructor



                constructor.declarationDescriptor.createTupleType(
                    semaTy.typeArgs.map { type(it) },

                    )
            }

            semaTy.kind == FbTypeKind.Func -> {
                // Handle function types
                val nullable = false // TODO: Extract nullability from semaTy
                constructor as FunctionClassDescriptor.FunctionTypeConstructor
                semaTy.info as FbSemaTyInfo.FbFuncTyInfo
                constructor.declarationDescriptor.createFunctionType(
                    semaTy.typeArgs.map { type(it) },

                    type(
                        c.`package`.typeTable.get((semaTy.info as FbSemaTyInfo.FbFuncTyInfo).retType)
                            .let { TypeWrapper(it, c.declTable, c.typeTable) },
                    )
                )
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
    private fun typeConstructor(semaTy: TypeWrapper): TypeConstructor {


        fun notFoundClass(classId: ClassId): ClassDescriptor {

            val typeParametersCount =
                listOf(semaTy).map { it.typeArgs.size }.toMutableList()

//                generateSequence(semaTy) { it/*.outerType(c.typeTable)*/ }.map { it.typeArgs.size }.toMutableList()
            val classNestingLevel = generateSequence(classId, ClassId::outerClassId).count()
            while (typeParametersCount.size < classNestingLevel) {
                typeParametersCount.add(0)
            }
            return c.components.notFoundClasses.getClass(classId, typeParametersCount)
        }

        fun getBasicClassId(kind: FbTypeKind) = when (kind) {
            FbTypeKind.Unit -> StandardClassIds.UNITClassId
            FbTypeKind.Bool -> StandardClassIds.BOOLClassId

            // Int types
            FbTypeKind.Int8 -> StandardClassIds.INT8ClassId
            FbTypeKind.Int16 -> StandardClassIds.INT16ClassId
            FbTypeKind.Int32 -> StandardClassIds.INT32ClassId
            FbTypeKind.Int64 -> StandardClassIds.INT64ClassId
            FbTypeKind.IntNative -> StandardClassIds.INTNATIVEClassId

            // UInt types
            FbTypeKind.UInt8 -> StandardClassIds.UINT8ClassId
            FbTypeKind.UInt16 -> StandardClassIds.UINT16ClassId
            FbTypeKind.UInt32 -> StandardClassIds.UINT32ClassId
            FbTypeKind.UInt64 -> StandardClassIds.UINT64ClassId
            FbTypeKind.UIntNative -> StandardClassIds.UINTNATIVEClassId

            // Float types
            FbTypeKind.Float16 -> StandardClassIds.FLOAT16ClassId
            FbTypeKind.Float32 -> StandardClassIds.FLOAT32ClassId
            FbTypeKind.Float64 -> StandardClassIds.FLOAT64ClassId

            // Other primitive types
            FbTypeKind.Rune -> StandardClassIds.RUNEClassId
            FbTypeKind.Nothing -> StandardClassIds.NOTHINGClassId

            else -> error("Unsupported classId: $kind")
        }

        val decl: ClassifierDescriptor? = when (semaTy.kind) {
            // 基本类型
            FbTypeKind.Unit,
            FbTypeKind.Bool,
            FbTypeKind.Int8,
            FbTypeKind.Int16,
            FbTypeKind.Int32,
            FbTypeKind.Int64,
            FbTypeKind.IntNative,
            FbTypeKind.UInt8,
            FbTypeKind.UInt16,
            FbTypeKind.UInt32,
            FbTypeKind.UInt64,
            FbTypeKind.UIntNative,
            FbTypeKind.Float16,
            FbTypeKind.Float32,
            FbTypeKind.Float64,
            FbTypeKind.Rune,
            FbTypeKind.Nothing -> (classifierDescriptors(getBasicClassId(kind = semaTy.kind)) ?: notFoundClass(
                getBasicClassId(kind = semaTy.kind)
            ))
//如果是Array，说明该包是std.core，那么Array的声明只会在同一包中出现，所以直接在本包中查找声明，通过name的方法
            FbTypeKind.Array -> {
                (classifierDescriptors(ArrayClassId) ?: notFoundClass(ArrayClassId))
            }
//            对于Type,Class, Interface, Struct, Enum,Generic 需要去查找对应的声明
            // 复合类型 (Class, Interface, Struct, Enum)
            FbTypeKind.Class, FbTypeKind.Interface, FbTypeKind.Struct, FbTypeKind.Enum ->
                when (val info = semaTy.info) {
                    is FbSemaTyInfo.FbCompositeTyInfo -> {

                        info.declPtr?.let { classifierDescriptorsByFullId(it) }

                    }

                    else -> null
                }


            FbTypeKind.Type ->
                when (val info = semaTy.info) {
                    is FbSemaTyInfo.FbCompositeTyInfo -> {

                        info.declPtr?.let { typeAliasDescriptorsByFullId(it) }

                    }

                    else -> null

                }

//            // 泛型类型
            FbTypeKind.Generic -> {
                when (val info = semaTy.info) {
                    is FbSemaTyInfo.FbGenericTyInfo -> {
//                        info.declPtr?.let { classifierDescriptorsByFullId(it) }
                        loadTypeParameter(semaTy.original)

                    }

                    else -> null
                }
            }

            // 函数类型
            FbTypeKind.Func -> {
                when (val info = semaTy.info) {
                    is FbSemaTyInfo.FbFuncTyInfo -> {
                        val arity = semaTy.typeArgs.size
                        if (arity >= 0) {
                            c.builtIns.getFunction(arity)
                        } else {
                            null
                        }
                    }

                    else -> null
                }
            }

            // 数组类型
//            TypeKind.VArray -> {
//                c.builtIns.array.typeConstructor
//            }

            // 元组类型
            FbTypeKind.Tuple -> {
                val arity = semaTy.typeArgs.size
                if (arity >= 0) {
                    c.builtIns.getTuple(arity)
                } else {
                    null
                }
            }

            // 其他类型
            else -> null
        }
        return decl?.typeConstructor ?: ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
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
    private fun loadTypeParameter(typeParameterId: FbSemaTy): TypeParameterDescriptor? =
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
