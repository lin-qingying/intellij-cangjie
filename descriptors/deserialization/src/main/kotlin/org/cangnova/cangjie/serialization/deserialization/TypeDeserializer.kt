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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.FunctionClassDescriptor
import org.cangnova.cangjie.descriptors.impl.TupleClassDescriptor
import org.cangnova.cangjie.metadata.model.fb.FbFullId
import org.cangnova.cangjie.metadata.model.fb.FbSemaTy
import org.cangnova.cangjie.metadata.model.fb.FbSemaTyInfo
import org.cangnova.cangjie.metadata.model.fb.FbTypeKind
import org.cangnova.cangjie.metadata.model.wrapper.TypeParameterWrapper
import org.cangnova.cangjie.metadata.model.wrapper.TypeWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.StandardClassIds
import org.cangnova.cangjie.name.StandardClassIds.ArrayClassId
import org.cangnova.cangjie.name.StandardClassIds.CPointerClassId
import org.cangnova.cangjie.name.StandardClassIds.CStringClassId
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.error.ErrorTypeKind

/**
 * 类型反序列化器
 *
 * 负责将 FlatBuffer 格式的类型信息转换为 CangJieType 对象。
 * 这是 CangJie 语言反序列化系统的核心组件，处理以下类型的反序列化：
 * - 类和接口类型
 * - 类型参数（泛型参数）
 * - 类型别名
 * - 函数类型（包括挂起函数类型）
 * - 元组类型
 * - 可空类型和灵活类型
 *
 * 支持嵌套类型和复杂类型结构的反序列化，通过缓存机制优化性能。
 *
 * @param c 反序列化上下文，提供必要的反序列化组件和配置
 * @param parent 父类型反序列化器，用于处理嵌套作用域中的类型参数
 * @param typeParameters 泛型信息，定义当前作用域中的类型参数和约束
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
    // ==================== 分类器描述符缓存 ====================

    /**
     * 通过 ClassId 查找分类器描述符的缓存函数
     * 用于处理类、接口、结构体、枚举等类型
     */
    private val classifierDescriptors: (ClassId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { id ->
            c.components.moduleDescriptor.findClassifierAcrossModuleDependencies(id)
        }

    /**
     * 通过 ClassId 查找类型别名描述符的缓存函数
     */
    private val typeAliasDescriptors: (ClassId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { id ->
            c.components.moduleDescriptor.findTypeAliasAcrossModuleDependencies(id)
        }

    /**
     * 通过 FullId 查找分类器描述符的缓存函数
     * FullId 包含完整的模块和路径信息，用于跨模块查找
     */
    private val classifierDescriptorsByFullId: (FbFullId) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { id ->
            c.fullIdFinder.findClassifierDescriptorByFullId(id)
        }

    /**
     * 通过 FullId 查找类型别名描述符的缓存函数
     */
    private val typeAliasDescriptorsByFullId: (FbFullId) -> TypeAliasDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { id ->
            c.fullIdFinder.findTypeAliasDescriptorByFullId(id)
        }

    // ==================== 类型参数管理 ====================

    /**
     * 类型参数描述符映射
     *
     * 将类型参数的原始 FbSemaTy 映射到对应的类型参数描述符。
     * 用于处理泛型类型中的类型参数引用。
     */
    private val typeParameterDescriptors: Map<FbSemaTy, TypeParameterDescriptor> =
        if (typeParameters.isEmpty()) {
            emptyMap()
        } else {
            typeParameters.withIndex().associate { (index, type) ->
                type.type.original to DeserializedTypeParameterDescriptor(c, type, index)
            }
        }

    /**
     * 当前作用域中定义的所有类型参数列表
     * 按定义顺序返回类型参数描述符
     */
    val ownTypeParameters: List<TypeParameterDescriptor>
        get() = typeParameterDescriptors.values.toList()

    // ==================== 类型转换核心方法 ====================

    /**
     * 将 TypeWrapper 转换为 CangJieType 对象
     *
     * 这是类型反序列化的主入口方法，默认展开类型别名。
     *
     * @param semaTy FlatBuffer 格式的语义类型定义
     * @return 转换后的 CangJieType 对象
     */
    fun type(semaTy: TypeWrapper): CangJieType {
        return simpleType(semaTy)
    }

    /**
     * 将 TypeWrapper 转换为 SimpleType 对象
     *
     * 处理各种类型的反序列化，包括：
     * - 基本类型处理
     * - 类型构造器解析
     * - 类型参数处理
     * - 类型别名展开
     * - 函数类型和元组类型处理
     *
     * @param semaTy FlatBuffer 格式的语义类型定义
     * @param expandTypeAliases 是否展开类型别名，默认为 true
     * @return 转换后的 SimpleType 对象
     */
    fun simpleType(semaTy: TypeWrapper, expandTypeAliases: Boolean = true): SimpleType {
        val constructor = typeConstructor(semaTy)

        // 检查类型构造器是否有效
        if (ErrorUtils.isError(constructor.declarationDescriptor)) {
            return ErrorUtils.createErrorType(
                ErrorTypeKind.TYPE_FOR_ERROR_TYPE_CONSTRUCTOR,
                constructor,
                constructor.toString()
            )
        }

        // TODO 目前使用空注解，因为新模型尚未支持注解加载
        val annotations = Annotations.EMPTY
        val attributes = TypeAttributes.Empty

        // 将类型参数索引转换为 TypeArgument 对象
        val arguments = semaTy.typeArgs.map { TypeArgumentImpl(type(it)) }

        val declarationDescriptor = constructor.declarationDescriptor

        // 根据类型种类构建对应的简单类型
        return when {
            // 展开类型别名
            expandTypeAliases && declarationDescriptor is TypeAliasDescriptor -> {
                val expandedType = with(CangJieTypeFactory) {
                    declarationDescriptor.computeExpandedType(arguments)
                }
                expandedType.replaceAttributes(attributes)
            }

            // 处理元组类型
            semaTy.kind == FbTypeKind.Tuple -> {
                (constructor as TupleClassDescriptor.TupleTypeConstructor)
                    .declarationDescriptor
                    .createTupleType(semaTy.typeArgs.map { type(it) })
            }

            // 处理函数类型
            semaTy.kind == FbTypeKind.Func -> {
                val funcInfo = semaTy.info as FbSemaTyInfo.FbFuncTyInfo
                val returnType = type(
                    TypeWrapper(
                        c.`package`.typeTable[funcInfo.retType],
                        c.declTable,
                        c.typeTable
                    )
                )
                (constructor as FunctionClassDescriptor.FunctionTypeConstructor)
                    .declarationDescriptor
                    .createFunctionType(semaTy.typeArgs.map { type(it) }, returnType)
            }

            // 处理普通类型
            else -> {

                CangJieTypeFactory.simpleType(attributes, constructor, arguments)
            }
        }
    }

    // ==================== 类型构造器解析 ====================

    /**
     * 从 TypeWrapper 中提取类型构造器
     *
     * 根据 TypeWrapper 中的信息，解析并返回对应的类型构造器。处理以下情况：
     * - 基本类型：返回内置类型构造器
     * - 复合类型：解析为类描述符的类型构造器
     * - 泛型类型：解析为类型参数的类型构造器
     * - 函数类型：返回函数类型构造器
     * - 元组类型：返回元组类型构造器
     *
     * @param semaTy FlatBuffer 格式的语义类型定义
     * @return 对应的类型构造器
     */
    private fun typeConstructor(semaTy: TypeWrapper): TypeConstructor {
        val decl: ClassifierDescriptor? = when (semaTy.kind) {
            // 基本类型
            FbTypeKind.Unit, FbTypeKind.Bool,
            FbTypeKind.Int8, FbTypeKind.Int16, FbTypeKind.Int32, FbTypeKind.Int64, FbTypeKind.IntNative,
            FbTypeKind.UInt8, FbTypeKind.UInt16, FbTypeKind.UInt32, FbTypeKind.UInt64, FbTypeKind.UIntNative,
            FbTypeKind.Float16, FbTypeKind.Float32, FbTypeKind.Float64,
            FbTypeKind.Rune, FbTypeKind.Nothing ->
                findBasicType(semaTy.kind, semaTy)

            // 数组类型
            FbTypeKind.Array ->
                classifierDescriptors(ArrayClassId) ?: notFoundClass(ArrayClassId, semaTy)

            // C 互操作类型
            FbTypeKind.CPointer ->
                classifierDescriptors(CPointerClassId) ?: notFoundClass(CPointerClassId, semaTy)

            FbTypeKind.CString ->
                classifierDescriptors(CStringClassId) ?: notFoundClass(CStringClassId, semaTy)

            // 复合类型（类、接口、结构体、枚举）
            FbTypeKind.Class, FbTypeKind.Interface, FbTypeKind.Struct, FbTypeKind.Enum -> {
                (semaTy.info as? FbSemaTyInfo.FbCompositeTyInfo)
                    ?.declPtr
                    ?.let { classifierDescriptorsByFullId(it) }
            }

            // 类型别名
            FbTypeKind.Type -> {
                (semaTy.info as? FbSemaTyInfo.FbCompositeTyInfo)
                    ?.declPtr
                    ?.let { typeAliasDescriptorsByFullId(it) }
            }

            // 泛型类型参数
            FbTypeKind.Generic -> {
                loadTypeParameter(semaTy.original)
            }

            // 函数类型
            FbTypeKind.Func -> {
                c.builtIns.getFunction(semaTy.typeArgs.size)
            }

            // 元组类型
            FbTypeKind.Tuple -> {

                c.builtIns.getTuple(semaTy.typeArgs.size)
            }

            // 未知类型
            else -> null
        }

        return decl?.typeConstructor
            ?: ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.UNKNOWN_TYPE)
    }

    // ==================== 辅助方法 ====================

    /**
     * 查找基本类型的分类器描述符
     *
     * @param kind 类型种类
     * @param semaTy 语义类型定义
     * @return 对应的分类器描述符
     */
    private fun findBasicType(kind: FbTypeKind, semaTy: TypeWrapper): ClassifierDescriptor {

        val classId = getBasicClassId(kind)



        return classifierDescriptors(classId) ?: notFoundClass(classId, semaTy)
    }

    /**
     * 获取基本类型对应的 ClassId
     *
     * @param kind 类型种类
     * @return 对应的 ClassId
     */
    private fun getBasicClassId(kind: FbTypeKind): ClassId = when (kind) {
        FbTypeKind.Unit -> StandardClassIds.UNITClassId
        FbTypeKind.Bool -> StandardClassIds.BOOLClassId

        // 整数类型
        FbTypeKind.Int8 -> StandardClassIds.INT8ClassId
        FbTypeKind.Int16 -> StandardClassIds.INT16ClassId
        FbTypeKind.Int32 -> StandardClassIds.INT32ClassId
        FbTypeKind.Int64 -> StandardClassIds.INT64ClassId
        FbTypeKind.IntNative -> StandardClassIds.INTNATIVEClassId

        // 无符号整数类型
        FbTypeKind.UInt8 -> StandardClassIds.UINT8ClassId
        FbTypeKind.UInt16 -> StandardClassIds.UINT16ClassId
        FbTypeKind.UInt32 -> StandardClassIds.UINT32ClassId
        FbTypeKind.UInt64 -> StandardClassIds.UINT64ClassId
        FbTypeKind.UIntNative -> StandardClassIds.UINTNATIVEClassId

        // 浮点数类型
        FbTypeKind.Float16 -> StandardClassIds.FLOAT16ClassId
        FbTypeKind.Float32 -> StandardClassIds.FLOAT32ClassId
        FbTypeKind.Float64 -> StandardClassIds.FLOAT64ClassId

        // 其他基本类型
        FbTypeKind.Rune -> StandardClassIds.RUNEClassId
        FbTypeKind.Nothing -> StandardClassIds.NOTHINGClassId

        else -> error("不支持的基本类型: $kind")
    }

    /**
     * 创建未找到的类描述符
     *
     * 当类型引用的类无法找到时，创建一个占位符类描述符。
     * 这样可以继续反序列化过程，而不会因为缺少依赖而失败。
     *
     * @param classId 类的标识符
     * @param semaTy 语义类型定义，用于获取类型参数信息
     * @return 未找到类的描述符
     */
    private fun notFoundClass(classId: ClassId, semaTy: TypeWrapper): ClassDescriptor {
        // 计算类型参数数量
        val typeParametersCount = mutableListOf(semaTy.typeArgs.size)

        // 计算类嵌套层级
        val classNestingLevel = generateSequence(classId, ClassId::outerClassId).count()

        // 补齐嵌套层级所需的类型参数数量
        while (typeParametersCount.size < classNestingLevel) {
            typeParametersCount.add(0)
        }

        return c.components.notFoundClasses.getClass(classId, typeParametersCount)
    }

    /**
     * 加载类型参数描述符
     *
     * 根据类型参数 ID 查找对应的类型参数描述符。
     * 首先在当前作用域查找，如果未找到则递归在父作用域中继续查找。
     *
     * @param typeParameterId 类型参数 ID
     * @return 找到的类型参数描述符，未找到则返回 null
     */
    private fun loadTypeParameter(typeParameterId: FbSemaTy): TypeParameterDescriptor? =
        typeParameterDescriptors[typeParameterId] ?: parent?.loadTypeParameter(typeParameterId)

    /**
     * 返回类型反序列化器的字符串表示
     * 包含调试名称和父反序列化器的信息（如果存在）
     */
    override fun toString(): String =
        debugName + (parent?.let { ". 子反序列化器: ${it.debugName}" } ?: "")
}