package com.huawei.cangjie.utils

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

    /*
     * This function is needed for compatibility with JDK 6
     * ArrayMap and other infrastructure is used in CangJieType, declared in :core:descriptors module, which is
     *   compiled against JDK 6 (because it's used in kotlin-reflect, which is still compatible with Java 6)
     * So the problem is that JDK 6 does not have thread-safe computeIfAbsent for ConcurrentHashMap,
     *   and we need this method to add ability to provide thread-safe implementation by hand
     */
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
