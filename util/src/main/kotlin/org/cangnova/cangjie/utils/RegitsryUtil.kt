package org.cangnova.cangjie.utils

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.NonNls
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun registryFlag(@NonNls key: String): ReadWriteProperty<Any?, Boolean> {
    return object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = Registry.`is`(key)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = Registry.get(key).setValue(value)
    }
}