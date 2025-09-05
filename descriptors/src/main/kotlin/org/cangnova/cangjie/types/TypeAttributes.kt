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

package org.cangnova.cangjie.types


import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.types.model.AnnotationMarker
import org.cangnova.cangjie.utils.AttributeArrayOwner
import org.cangnova.cangjie.utils.TypeRegistry
import com.intellij.util.containers.addIfNotNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass


abstract class TypeAttribute<out T : TypeAttribute<T>> : AnnotationMarker {
    abstract fun union(other: @UnsafeVariance T?): T?
    abstract fun intersect(other: @UnsafeVariance T?): T?

    /*
     * This function is used to decide how multiple attributes should be united in presence of typealiases:
     * typealias B = @SomeAttribute(1) A
     * typealias C = @SomeAttribute(2) B
     *
     * For determining attribute value of expanded type of C we should add @SomeAttribute(2) to @SomeAttribute(1)
     *
     * This function must be symmetrical: a.add(b) == b.add(a)
     */
    abstract fun add(other: @UnsafeVariance T?): T
    abstract fun isSubtypeOf(other: @UnsafeVariance T?): Boolean

    abstract val key: KClass<out T>
}
fun Annotations.toDefaultAttributes(): TypeAttributes = DefaultTypeAttributeTranslator.toAttributes(this)

class TypeAttributes private constructor(attributes: List<TypeAttribute<*>>) :
    AttributeArrayOwner<TypeAttribute<*>, TypeAttribute<*>>(),
    Iterable<TypeAttribute<*>> {
    fun add(other: TypeAttributes): TypeAttributes {
        return perform(other) { this.add(it) }
    }
    fun union(other: TypeAttributes): TypeAttributes {
        return perform(other) { this.union(it) }
    }
    fun intersect(other: TypeAttributes): TypeAttributes {
        return perform(other) { this.intersect(it) }
    }
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
    companion object : TypeRegistry<TypeAttribute<*>, TypeAttribute<*>>() {
        inline fun <reified T : TypeAttribute<T>> attributeAccessor(): ReadOnlyProperty<TypeAttributes, T?> {
            @Suppress("UNCHECKED_CAST")
            return generateNullableAccessor<TypeAttribute<*>, T>(T::class) as ReadOnlyProperty<TypeAttributes, T?>
        }

        fun create(attributes: List<TypeAttribute<*>>): TypeAttributes {
            return if (attributes.isEmpty()) {
                Empty
            } else {
                TypeAttributes(attributes)
            }
        }
        override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(key: String, compute: (String) -> Int): Int {
            return this[key] ?: synchronized(this) {
                this[key] ?: compute(key).also { this.putIfAbsent(key, it) }
            }
        }

        val Empty: TypeAttributes = TypeAttributes(emptyList())

    }
    operator fun plus(attribute: TypeAttribute<*>): TypeAttributes {
        if (attribute in this) return this
        if (isEmpty()) return TypeAttributes(attribute)
        val newAttributes = this.toList() + attribute
        return create(newAttributes)
    }

    fun remove(attribute: TypeAttribute<*>): TypeAttributes {
        if (isEmpty()) return this
        val attributes = arrayMap.filter { it != attribute }
        if (attributes.size == arrayMap.size) return this
        return create(attributes)
    }
    private constructor(attribute: TypeAttribute<*>) : this(listOf(attribute))
}

fun TypeAttributes.replaceAnnotations(newAnnotations: Annotations): TypeAttributes {
    if (annotations === newAnnotations) return this
    val withoutAnnotations = annotationsAttribute?.let { this.remove(it) } ?: this
    // Check if iterator hasNext to handle FilteredAnnotations.isEmpty() with OldInference
    if (!newAnnotations.iterator().hasNext() && newAnnotations.isEmpty()) return withoutAnnotations
    return withoutAnnotations.plus(AnnotationsTypeAttribute(newAnnotations))
}
