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

package org.cangnova.cangjie.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * [AttributeArrayOwner] based on different implementations of [ArrayMap] and switches them
 *   depending on array map fullness
 * [AttributeArrayOwner] can be used in classes with many instances,
 *   like user data for Fir elements or attributes for cone types
 *
 * Note that you can remove attributes from [AttributeArrayOwner] despite
 *   from components in [ComponentArrayOwner]
 */
abstract class AttributeArrayOwner<K : Any, T : Any> protected constructor(
    arrayMap: ArrayMap<T>
) : AbstractArrayMapOwner<K, T>(){
    @Suppress("UNCHECKED_CAST")
    constructor() : this(EmptyArrayMap as ArrayMap<T>)
    final override var arrayMap: ArrayMap<T> = arrayMap
        private set


    fun isEmpty(): Boolean = arrayMap.size == 0

}
class ArrayMapAccessor<K : Any, V : Any, T : V>(
    private val keyQualifiedName: String,
    id: Int,
    val default: T? = null
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, V> {
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T {
        return extractValue(thisRef)
            ?: default
            ?: error("No '$keyQualifiedName'($id) in array owner: $thisRef")
    }
}

class NullableArrayMapAccessor<K : Any, V : Any, T : V>(
    id: Int
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, V?> {
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T? {
        return extractValue(thisRef)
    }
}
abstract class TypeRegistry<K : Any, V : Any> {
    private val idPerType = ConcurrentHashMap<String, Int>()
    private val idCounter = AtomicInteger(0)

    fun <T : V, KK : K> generateAccessor(kClass: KClass<KK>, default: T? = null): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(kClass.qualifiedName!!, getId(kClass), default)
    }

    fun <T : V> generateAccessor(keyQualifiedName: String, default: T? = null): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(keyQualifiedName, getId(keyQualifiedName), default)
    }

    fun <T : V, KK : K> generateNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, T> {
        return NullableArrayMapAccessor(getId(kClass))
    }

    fun <KK : K> generateAnyNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, *> {
        return NullableArrayMapAccessor(getId(kClass))
    }

    fun <T : K> getId(kClass: KClass<T>): Int {
        return getId(kClass.qualifiedName!!)
    }

    fun getId(keyQualifiedName: String): Int {
        return idPerType.customComputeIfAbsent(keyQualifiedName) { idCounter.getAndIncrement() }
    }


    abstract fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
        key: String,
        compute: (String) -> Int
    ): Int

    fun allValuesThreadUnsafeForRendering(): Map<String, Int> {
        return idPerType
    }

    protected val indices: Collection<Int>
        get() = idPerType.values
}
