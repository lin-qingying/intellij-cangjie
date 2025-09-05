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


/**
 * 类型反序列化器，负责将ProtoBuf格式的类型信息转换为CangJieType对象。
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
 * @param typeParameterProtos 类型参数原型列表，定义当前作用域中的类型参数
 * @param debugName 调试名称，用于日志和调试输出
 * @param containerPresentableName 容器的可展示名称，用于错误消息和调试信息
 */
class TypeDeserializer(
    private val c: DeserializationContext,
    private val parent: TypeDeserializer?,
    typeParameterProtos: List<ProtoBuf.TypeParameter>,
    private val debugName: String,
    private val containerPresentableName: String
) {
    /**
     * 类描述符缓存，根据类名索引缓存已解析的类描述符。
     *
     * 使用memoized函数实现延迟加载和缓存，避免重复解析相同的类名。
     * 返回null表示找不到对应的类描述符。
     */
    private val classifierDescriptors: (Int) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { fqNameIndex ->
            computeClassifierDescriptor(fqNameIndex)
        }

    /**
     * 类型别名描述符缓存，根据类型别名索引缓存已解析的类型别名描述符。
     *
     * 使用memoized函数实现延迟加载和缓存，避免重复解析相同的类型别名。
     * 返回null表示找不到对应的类型别名描述符。
     */
    private val typeAliasDescriptors: (Int) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { fqNameIndex ->
            computeTypeAliasDescriptor(fqNameIndex)
        }

    /**
     * 类型参数描述符映射，将类型参数ID映射到对应的类型参数描述符。
     *
     * 根据ProtoBuf中的类型参数原型创建对应的类型参数描述符，
     * 用于处理泛型类型中的类型参数引用。
     */
    private val typeParameterDescriptors: Map<Int, TypeParameterDescriptor> =
        if (typeParameterProtos.isEmpty()) {
            emptyMap()
        } else {
            val result = LinkedHashMap<Int, TypeParameterDescriptor>()
            for ((index, proto) in typeParameterProtos.withIndex()) {
                result[proto.id] = DeserializedTypeParameterDescriptor(c, proto, index)
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
     * 将ProtoBuf类型转换为CangJieType对象。
     * 
     * 这是类型反序列化的主入口方法，处理以下情况：
     * - 灵活类型（Flexible Type）：具有上下界的类型
     * - 简单类型：通过simpleType方法处理
     * 
     * @param proto ProtoBuf格式的类型定义
     * @return 转换后的CangJieType对象
     */
    // TODO: don't load identical types from TypeTable more than once
    fun type(proto: ProtoBuf.Type): CangJieType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = c.nameResolver.getString(proto.flexibleTypeCapabilitiesId)
            val lowerBound = simpleType(proto)
            val upperBound = simpleType(proto.flexibleUpperBound(c.typeTable)!!)
            return c.components.flexibleTypeDeserializer.create(proto, id, lowerBound, upperBound)
        }

        return simpleType(proto, expandTypeAliases = true)
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
     * 将ProtoBuf类型转换为SimpleType对象。
     * 
     * 处理各种类型的反序列化，包括：
     * - 本地类型替换
     * - 类型构造器解析
     * - 类型参数处理
     * - 类型别名展开
     * - 挂起函数类型处理
     * - 非空类型处理
     * 
     * @param proto ProtoBuf格式的类型定义
     * @param expandTypeAliases 是否展开类型别名，默认为true
     * @return 转换后的SimpleType对象
     */
    fun simpleType(proto: ProtoBuf.Type, expandTypeAliases: Boolean = true): SimpleType {
        val localClassifierType = when {
            proto.hasClassName() -> computeLocalClassifierReplacementType(proto.className)
            proto.hasTypeAliasName() -> computeLocalClassifierReplacementType(proto.typeAliasName)
            else -> null
        }

        if (localClassifierType != null) return localClassifierType

        val constructor = typeConstructor(proto)
        if (ErrorUtils.isError(constructor.declarationDescriptor)) {
            return ErrorUtils.createErrorType(
                ErrorTypeKind.TYPE_FOR_ERROR_TYPE_CONSTRUCTOR,
                constructor,
                constructor.toString()
            )
        }

        val annotations = DeserializedAnnotations(c.storageManager) {
            c.components.annotationAndConstantLoader.loadTypeAnnotations(proto, c.nameResolver)
        }

        val attributes =
            c.components.typeAttributeTranslators.toAttributes(annotations, constructor, c.containingDeclaration)

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(c.typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().mapIndexed { index, argumentProto ->
            typeArgument(constructor.parameters.getOrNull(index), argumentProto)
        }.toList()

        val declarationDescriptor = constructor.declarationDescriptor

        val simpleType = when {
            expandTypeAliases && declarationDescriptor is TypeAliasDescriptor -> {
                val expandedType = with(CangJieTypeFactory) { declarationDescriptor.computeExpandedType(arguments) }
                val expandedAttributes = c.components.typeAttributeTranslators.toAttributes(
                    Annotations.create(annotations + expandedType.annotations),
                    constructor,
                    c.containingDeclaration
                )
                expandedType
                    .makeOptionalAsSpecified(expandedType.isNullable() || proto.nullable)
                    .replaceAttributes(expandedAttributes)
            }

            Flags.SUSPEND_TYPE.get(proto.flags) ->
                createSuspendFunctionType(attributes, constructor, arguments, proto.nullable)

            else ->
                CangJieTypeFactory.simpleType(attributes, constructor, arguments, proto.nullable).let {
                    if (Flags.DEFINITELY_NOT_NULL_TYPE.get(proto.flags))
                        DefinitelyNotNullType.makeDefinitelyNotNull(it, useCorrectedNullabilityForTypeParameters = true)
                            ?: error("null DefinitelyNotNullType for '$it'")
                    else
                        it
                }
        }

        val computedType = proto.abbreviatedType(c.typeTable)?.let {
            // The abbreviation type is expected to be a typealias, and it should not get expanded, we need to keep it
            simpleType.withAbbreviation(simpleType(it, expandTypeAliases = false))
        } ?: simpleType

        return computedType
    }

    /**
     * 从ProtoBuf类型中提取类型构造器。
     * 
     * 根据ProtoBuf类型中的信息，解析并返回对应的类型构造器。处理以下情况：
     * - 类名引用：解析为类描述符的类型构造器
     * - 类型参数引用：解析为类型参数的类型构造器
     * - 类型别名引用：解析为类型别名的类型构造器
     * - 未找到的类：创建占位符类描述符
     * 
     * @param proto ProtoBuf格式的类型定义
     * @return 对应的类型构造器
     */
    private fun typeConstructor(proto: ProtoBuf.Type): TypeConstructor {
        fun notFoundClass(classIdIndex: Int): ClassDescriptor {
            val classId = c.nameResolver.getClassId(classIdIndex)
            val typeParametersCount =
                generateSequence(proto) { it.outerType(c.typeTable) }.map { it.argumentCount }.toMutableList()
            val classNestingLevel = generateSequence(classId, ClassId::outerClassId).count()
            while (typeParametersCount.size < classNestingLevel) {
                typeParametersCount.add(0)
            }
            return c.components.notFoundClasses.getClass(classId, typeParametersCount)
        }

        val classifier = when {
            proto.hasClassName() ->
                classifierDescriptors(proto.className) ?: notFoundClass(proto.className)

            proto.hasTypeParameter() ->
                loadTypeParameter(proto.typeParameter)
                    ?: return ErrorUtils.createErrorTypeConstructor(
                        ErrorTypeKind.CANNOT_LOAD_DESERIALIZE_TYPE_PARAMETER,
                        proto.typeParameter.toString(),
                        containerPresentableName
                    )

            proto.hasTypeParameterName() -> {
                val name = c.nameResolver.getString(proto.typeParameterName)
                ownTypeParameters.find { it.name.asString() == name }
                    ?: return ErrorUtils.createErrorTypeConstructor(
                        ErrorTypeKind.CANNOT_LOAD_DESERIALIZE_TYPE_PARAMETER_BY_NAME,
                        name,
                        c.containingDeclaration.toString()
                    )
            }

            proto.hasTypeAliasName() ->
                typeAliasDescriptors(proto.typeAliasName) ?: notFoundClass(proto.typeAliasName)

            else -> return ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
        }
        return classifier.typeConstructor
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
            funType.getValueParameterTypesFromFunctionType().dropLast(1).map(TypeProjection::getType),
            // TODO: names
            null,
            suspendReturnType,

            ).makeOptionalAsSpecified(funType.isMarkedOption)
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
     * 计算分类器描述符。
     * 
     * 根据全限定名索引查找对应的分类器描述符。处理本地类和跨模块依赖的类。
     * 
     * @param fqNameIndex 全限定名索引
     * @return 找到的分类器描述符，未找到则返回null
     */
    private fun computeClassifierDescriptor(fqNameIndex: Int): ClassifierDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        if (id.isLocal) {
            // Local classes can't be found in scopes
            return c.components.deserializeClass(id)
        }
        return c.components.moduleDescriptor.findClassifierAcrossModuleDependencies(id)
    }

    /**
     * 计算本地分类器的替代类型。
     * 
     * 对于本地类，返回配置的替代类型。这通常用于处理无法直接序列化的本地类。
     * 
     * @param className 类名索引
     * @return 本地分类器的替代类型，如果不是本地类则返回null
     */
    private fun computeLocalClassifierReplacementType(className: Int): SimpleType? {
        if (c.nameResolver.getClassId(className).isLocal) {
            return c.components.localClassifierTypeSettings.replacementTypeForLocalClassifiers
        }
        return null
    }

    /**
     * 创建类型反序列化器函数，用于将ProtoBuf类型转换为Kotlin类型。
     *
     * @param typeTable 类型表，用于解析类型引用
     * @return 类型反序列化函数，接收ProtoBuf类型并返回对应的Kotlin类型
     *
     * 处理流程：
     * 1. 加载类型注解
     * 2. 处理缩写类型（如类型别名）
     * 3. 提取类型构造器（包括分类器和类型参数）
     * 4. 创建最终的Kotlin类型实例
     */
    private fun computeTypeAliasDescriptor(fqNameIndex: Int): ClassifierDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        return if (id.isLocal) {

            return null
        } else {
            c.components.moduleDescriptor.findTypeAliasAcrossModuleDependencies(id)
        }
    }

    /**
     * 将ProtoBuf类型参数转换为类型投影。
     * 
     * 处理类型参数的变型（协变、逆变或不变）和类型转换。
     * 
     * @param parameter 对应的类型参数描述符，可能为null
     * @param typeArgumentProto ProtoBuf格式的类型参数
     * @return 转换后的类型投影
     */
    private fun typeArgument(
        parameter: TypeParameterDescriptor?,
        typeArgumentProto: ProtoBuf.Type.Argument
    ): TypeProjection {


        val projection = ProtoEnumFlags.variance(typeArgumentProto.projection)
        val type = typeArgumentProto.type(c.typeTable)
            ?: return TypeProjectionImpl(
                ErrorUtils.createErrorType(
                    ErrorTypeKind.NO_RECORDED_TYPE,
                    typeArgumentProto.toString()
                )
            )

        return TypeProjectionImpl(projection, type(type))
    }

    /**
     * 返回类型反序列化器的字符串表示。
     * 
     * 包含调试名称和父反序列化器的信息（如果存在）。
     * 
     * @return 类型反序列化器的字符串表示
     */
    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")
}
