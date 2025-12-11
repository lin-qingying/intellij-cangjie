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
package org.cangnova.cangjie.config

import com.intellij.openapi.util.Key
import java.util.*


class CompilerConfiguration {
    private val map: MutableMap<Key<*>?, Any?> = LinkedHashMap<Key<*>?, Any?>()
    private var readOnly = false

    fun <T> get(key: CompilerConfigurationKey<T?>): T? {
        val data = map.get(key.ideaKey) as T?
        return if (data == null) null else unmodifiable<T?>(data)
    }

    fun <T> get(key: CompilerConfigurationKey<T?>, defaultValue: T): T {
        val data = get<T?>(key)
        return if (data == null) defaultValue else data
    }

    fun <T> getNotNull(key: CompilerConfigurationKey<T?>): T {
        val data = checkNotNull(get<T?>(key)) { "No value for configuration key: " + key }
        return data
    }

    fun getBoolean(key: CompilerConfigurationKey<Boolean?>): Boolean {
        return get<Boolean?>(key, false)!!
    }

    fun <T> getList(key: CompilerConfigurationKey<MutableList<T?>?>): MutableList<T?> {
        val data = get<MutableList<T?>?>(key)
        return if (data == null) mutableListOf<T?>() else data
    }

    fun <K, V> getMap(key: CompilerConfigurationKey<MutableMap<K?, V?>?>): MutableMap<K?, V?> {
        val data = get<MutableMap<K?, V?>?>(key)
        return if (data == null) mutableMapOf<K?, V?>() else data
    }

    fun <T> put(key: CompilerConfigurationKey<T?>, value: T) {
        checkReadOnly()
        map.put(key.ideaKey, value)
    }

    fun <T> putIfAbsent(key: CompilerConfigurationKey<T?>, value: T): T? {
        val data = get<T?>(key)
        if (data != null) return data

        checkReadOnly()
        put<T?>(key, value)
        return value
    }

    fun <T> putIfNotNull(key: CompilerConfigurationKey<T?>, value: T?) {
        if (value != null) {
            put<T?>(key, value)
        }
    }

    fun <T> add(key: CompilerConfigurationKey<MutableList<T?>?>, value: T) {
        checkReadOnly()
        val ideaKey = key.ideaKey
        map.computeIfAbsent(ideaKey) { k: Key<*>? -> ArrayList<T?>() }
        val list = map.get(ideaKey) as MutableList<T?>
        list.add(value)
    }

    fun <K, V> put(configurationKey: CompilerConfigurationKey<MutableMap<K?, V?>?>, key: K, value: V) {
        checkReadOnly()
        val ideaKey = configurationKey.ideaKey
        map.computeIfAbsent(ideaKey) { k: Key<*>? -> HashMap<K?, V?>() }
        val data = map.get(ideaKey) as MutableMap<K?, V?>
        data.put(key, value)
    }

    fun <T> addAll(key: CompilerConfigurationKey<MutableList<T?>?>, values: MutableCollection<T?>?) {
        if (values != null) {
            addAll<T?>(key, getList<T?>(key).size, values)
        }
    }

    fun <T> addAll(key: CompilerConfigurationKey<MutableList<T?>?>, index: Int, values: MutableCollection<T?>) {
        checkReadOnly()
        checkForNullElements<T?>(values)
        val ideaKey = key.ideaKey
        map.computeIfAbsent(ideaKey) { k: Key<*>? -> ArrayList<T?>() }
        val list = map.get(ideaKey) as MutableList<T?>
        list.addAll(index, values)
    }

    fun copy(): CompilerConfiguration {
        val copy = CompilerConfiguration()
        copy.map.putAll(map)
        return copy
    }

    private fun checkReadOnly() {
        check(!readOnly) { "CompilerConfiguration is read-only" }
    }

    fun setReadOnly(readOnly: Boolean) {
        if (readOnly != this.readOnly) {
            this.readOnly = readOnly
        }
    }

    fun isReadOnly(): Boolean {
        return readOnly
    }

    override fun toString(): String {
        return map.toString()
    }

    companion object {
        @JvmField
        var EMPTY: CompilerConfiguration = CompilerConfiguration()

        init {
            EMPTY.setReadOnly(true)
        }

        private fun <T> unmodifiable(`object`: T): T {
            if (`object` is MutableList<*>) {
                return Collections.unmodifiableList<Any?>(`object` as MutableList<*>) as T
            } else if (`object` is MutableMap<*, *>) {
                return Collections.unmodifiableMap<Any?, Any?>(`object` as MutableMap<*, *>) as T
            } else if (`object` is MutableSet<*>) {
                return Collections.unmodifiableSet<Any?>(`object` as MutableSet<*>) as T
            } else if (`object` is MutableCollection<*>) {
                return Collections.unmodifiableCollection<Any?>(`object` as MutableCollection<*>) as T
            } else {
                return `object`
            }
        }

        private fun <T> checkForNullElements(values: MutableCollection<T?>) {
            var index = 0
            for (value in values) {
                requireNotNull(value) {
                    ("Element " + index
                            + " is null, while null values in compiler configuration are not allowed")
                }
                index++
            }
        }
    }
}
