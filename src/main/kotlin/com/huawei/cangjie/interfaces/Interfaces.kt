package com.huawei.cangjie.interfaces

import kotlin.reflect.KProperty

/**
 * Base interface that can be used for implementing property delegates of read-only properties.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param T the type of object which owns the delegated property.
 * @param V the type of the property value.
 */
public fun interface ReadOnlyProperty<in T, out V> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public operator fun getValue(thisRef: T, property: KProperty<*>): V
}
