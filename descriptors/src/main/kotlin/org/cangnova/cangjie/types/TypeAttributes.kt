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


import com.intellij.util.containers.addIfNotNull
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.types.model.AnnotationMarker
import org.cangnova.cangjie.utils.AttributeArrayOwner
import org.cangnova.cangjie.utils.TypeRegistry
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

/**
 * 类型属性抽象基类
 *
 * 表示附加到类型上的元数据属性，例如注解、可空性等。
 * 类型属性支持并集、交集、子类型判定等操作，用于类型系统的高级特性。
 *
 * 类型属性的关键操作：
 * - [union]: 合并两个属性（取并集）
 * - [intersect]: 相交两个属性（取交集）
 * - [add]: 组合两个属性（用于类型别名展开）
 * - [isSubtypeOf]: 判断子类型关系
 *
 * @param T 类型属性的具体类型
 */
abstract class TypeAttribute<out T : TypeAttribute<T>> : AnnotationMarker {
    /**
     * 属性并集操作
     *
     * 计算当前属性与另一个属性的并集。
     * 例如，两个可空性属性的并集可能表示"至少一个为空"。
     *
     * @param other 另一个属性，可能为 null
     * @return 并集结果，可能为 null（表示没有有意义的并集）
     */
    abstract fun union(other: @UnsafeVariance T?): T?

    /**
     * 属性交集操作
     *
     * 计算当前属性与另一个属性的交集。
     * 例如，两个可空性属性的交集可能表示"两者都为空"。
     *
     * @param other 另一个属性，可能为 null
     * @return 交集结果，可能为 null（表示没有有意义的交集）
     */
    abstract fun intersect(other: @UnsafeVariance T?): T?

    /**
     * 属性组合操作
     *
     * 用于在类型别名存在时决定如何组合多个属性。
     *
     * 示例场景：
     * ```kotlin
     * typealias B = @SomeAttribute(1) A
     * typealias C = @SomeAttribute(2) B
     * ```
     *
     * 为了确定 C 展开类型的属性值，需要将 @SomeAttribute(2) 添加到 @SomeAttribute(1)。
     *
     * **重要约束**: 此函数必须满足交换律：`a.add(b) == b.add(a)`
     *
     * @param other 另一个属性，可能为 null
     * @return 组合后的属性
     */
    abstract fun add(other: @UnsafeVariance T?): T

    /**
     * 子类型判定
     *
     * 判断当前属性是否是另一个属性的子类型。
     * 用于类型系统的子类型关系判定。
     *
     * @param other 另一个属性，可能为 null
     * @return 如果是子类型返回 true，否则返回 false
     */
    abstract fun isSubtypeOf(other: @UnsafeVariance T?): Boolean

    /**
     * 属性键
     *
     * 用于标识属性类型的唯一键，基于 Kotlin 类型。
     */
    abstract val key: KClass<out T>
}

/**
 * 将注解转换为默认的类型属性
 *
 * @receiver 注解集合
 * @return 对应的类型属性
 */
fun Annotations.toDefaultAttributes(): TypeAttributes = DefaultTypeAttributeTranslator.toAttributes(this)

/**
 * 类型属性集合
 *
 * 表示附加到类型上的一组属性，提供高效的属性存储和操作。
 * 使用数组映射结构来快速访问和修改属性。
 *
 * 主要特性：
 * - 不可变集合，所有修改操作返回新实例
 * - 支持属性的并集、交集和组合操作
 * - 高效的属性查询和访问
 * - 线程安全的属性注册机制
 *
 * 使用示例：
 * ```kotlin
 * val attributes = TypeAttributes.create(listOf(annotationAttribute))
 * val newAttributes = attributes + nullabilityAttribute
 * val merged = attributes.union(otherAttributes)
 * ```
 *
 * @property attributes 属性列表
 */
class TypeAttributes private constructor(attributes: List<TypeAttribute<*>>) :
    AttributeArrayOwner<TypeAttribute<*>, TypeAttribute<*>>(),
    Iterable<TypeAttribute<*>> {

    /**
     * 组合两个类型属性集合
     *
     * 使用 [TypeAttribute.add] 操作组合每对对应的属性。
     *
     * @param other 另一个类型属性集合
     * @return 组合后的新类型属性集合
     */
    fun add(other: TypeAttributes): TypeAttributes {
        return perform(other) { this.add(it) }
    }

    /**
     * 计算两个类型属性集合的并集
     *
     * 使用 [TypeAttribute.union] 操作合并每对对应的属性。
     *
     * @param other 另一个类型属性集合
     * @return 并集结果
     */
    fun union(other: TypeAttributes): TypeAttributes {
        return perform(other) { this.union(it) }
    }

    /**
     * 计算两个类型属性集合的交集
     *
     * 使用 [TypeAttribute.intersect] 操作相交每对对应的属性。
     *
     * @param other 另一个类型属性集合
     * @return 交集结果
     */
    fun intersect(other: TypeAttributes): TypeAttributes {
        return perform(other) { this.intersect(it) }
    }

    /**
     * 执行属性操作的通用方法
     *
     * @param other 另一个类型属性集合
     * @param op 要执行的操作
     * @return 操作后的新类型属性集合
     */
    private inline fun perform(other: TypeAttributes, op: TypeAttribute<*>.(TypeAttribute<*>?) -> TypeAttribute<*>?): TypeAttributes {
        if (this.isEmpty() && other.isEmpty()) return this
        val attributes = mutableListOf<TypeAttribute<*>>()
        for (index in indices) {
            val a = arrayMap[index]
            val b = other.arrayMap[index]
            val res = if (a == null) b?.op(a) else a.op(b)
            attributes.addIfNotNull(res)
        }
        return create(attributes)
    }

    /**
     * 伴生对象 - 类型属性工厂和注册表
     *
     * 管理类型属性的全局注册表，提供属性创建和访问的工具方法。
     */
    companion object : TypeRegistry<TypeAttribute<*>, TypeAttribute<*>>() {
        /**
         * 创建类型属性访问器
         *
         * 生成用于访问特定类型属性的委托属性。
         *
         * @param T 属性类型
         * @return 属性访问器
         */
        inline fun <reified T : TypeAttribute<T>> attributeAccessor(): ReadOnlyProperty<TypeAttributes, T?> {
            @Suppress("UNCHECKED_CAST")
            return generateNullableAccessor<TypeAttribute<*>, T>(T::class) as ReadOnlyProperty<TypeAttributes, T?>
        }

        /**
         * 创建类型属性集合
         *
         * 根据属性列表创建类型属性集合。
         * 如果属性列表为空，返回空单例以节省内存。
         *
         * @param attributes 属性列表
         * @return 类型属性集合
         */
        fun create(attributes: List<TypeAttribute<*>>): TypeAttributes {
            return if (attributes.isEmpty()) {
                Empty
            } else {
                TypeAttributes(attributes)
            }
        }

        /**
         * 自定义并发 Map 的 computeIfAbsent 实现
         *
         * 提供线程安全的懒加载计算逻辑。
         *
         * @receiver 并发 HashMap
         * @param key 键
         * @param compute 计算函数
         * @return 计算或已存在的值
         */
        override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(key: String, compute: (String) -> Int): Int {
            return this[key] ?: synchronized(this) {
                this[key] ?: compute(key).also { this.putIfAbsent(key, it) }
            }
        }

        /**
         * 空类型属性单例
         *
         * 表示没有任何属性的类型属性集合，用于优化内存使用。
         */
        val Empty: TypeAttributes = TypeAttributes(emptyList())

    }

    /**
     * 添加单个属性
     *
     * 如果属性已存在，返回当前集合；否则创建包含新属性的集合。
     *
     * @param attribute 要添加的属性
     * @return 包含该属性的类型属性集合
     */
    operator fun plus(attribute: TypeAttribute<*>): TypeAttributes {
        if (attribute in this) return this
        if (isEmpty()) return TypeAttributes(attribute)
        val newAttributes = this.toList() + attribute
        return create(newAttributes)
    }

    /**
     * 移除指定属性
     *
     * 创建一个不包含指定属性的新集合。
     * 如果属性不存在，返回当前集合。
     *
     * @param attribute 要移除的属性
     * @return 不包含该属性的新集合
     */
    fun remove(attribute: TypeAttribute<*>): TypeAttributes {
        if (isEmpty()) return this
        val attributes = arrayMap.filter { it != attribute }
        if (attributes.size == arrayMap.size) return this
        return create(attributes)
    }

    /**
     * 单属性构造器
     *
     * @param attribute 单个属性
     */
    private constructor(attribute: TypeAttribute<*>) : this(listOf(attribute))
}

/**
 * 替换类型属性中的注解
 *
 * 创建一个新的类型属性集合，用新的注解替换原有的注解属性。
 *
 * @receiver 原类型属性集合
 * @param newAnnotations 新的注解
 * @return 替换注解后的类型属性集合
 */
fun TypeAttributes.replaceAnnotations(newAnnotations: Annotations): TypeAttributes {
    if (annotations === newAnnotations) return this
    val withoutAnnotations = annotationsAttribute?.let { this.remove(it) } ?: this
    // Check if iterator hasNext to handle FilteredAnnotations.isEmpty() with OldInference
    if (!newAnnotations.iterator().hasNext() && newAnnotations.isEmpty()) return withoutAnnotations
    return withoutAnnotations.plus(AnnotationsTypeAttribute(newAnnotations))
}
