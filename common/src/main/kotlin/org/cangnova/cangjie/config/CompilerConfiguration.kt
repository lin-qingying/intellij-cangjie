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
import org.cangnova.cangjie.cli.messages.CompilerMessageLocation
import org.cangnova.cangjie.cli.messages.CompilerMessageSeverity
import org.cangnova.cangjie.cli.messages.MessageCollector
import java.util.*

fun CompilerConfiguration.report(
    severity: CompilerMessageSeverity,
    message: String,
    location: CompilerMessageLocation? = null
) {
    messageCollector.report(severity, message, location)
}
var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
    set(value) = put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)

var CompilerConfiguration.messageCollector: MessageCollector
    get() = get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    set(value) = put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, value)


class CompilerConfiguration {
    private val map: MutableMap<Key<*>, Any> = LinkedHashMap<Key<*>, Any>()
    private var readOnly = false

    fun <T> get(key: CompilerConfigurationKey<T>): T? {
        val data = map[key.ideaKey] as T?
        return if (data == null) null else unmodifiable<T>(data)
    }

    fun <T> get(key: CompilerConfigurationKey<T>, defaultValue: T): T {
        val data = get(key)
        return data ?: defaultValue
    }

    fun <T> getNotNull(key: CompilerConfigurationKey<T>): T {
        val data = checkNotNull(get(key)) { "No value for configuration key: $key" }
        return data
    }

    fun getBoolean(key: CompilerConfigurationKey<Boolean>): Boolean {
        return get(key, false)
    }

    fun <T> getList(key: CompilerConfigurationKey<MutableList<T>>): MutableList<T> {
        val data = get(key)
        return data ?: mutableListOf()
    }

    fun <K, V> getMap(key: CompilerConfigurationKey<MutableMap<K, V>>): MutableMap<K, V> {
        val data = get(key)
        return data ?: mutableMapOf()
    }

    fun <T> put(key: CompilerConfigurationKey<T>, value: T) {
        checkReadOnly()
        map[key.ideaKey] = value as Any
    }

    fun <T> putIfAbsent(key: CompilerConfigurationKey<T>, value: T): T {
        val data = get(key)
        if (data != null) return data

        checkReadOnly()
        put(key, value)
        return value
    }

    fun <T> putIfNotNull(key: CompilerConfigurationKey<T>, value: T?) {
        if (value != null) {
            put(key, value)
        }
    }

    fun <T> add(key: CompilerConfigurationKey<List<T>>, value: T) {
        checkReadOnly()
        val ideaKey = key.ideaKey
        map.computeIfAbsent(ideaKey) { k: Key<*>? -> ArrayList<T>() }
        val list = map[ideaKey] as MutableList<T>
        list.add(value)
    }

    fun <K, V> put(configurationKey: CompilerConfigurationKey<MutableMap<K, V>>, key: K, value: V) {
        checkReadOnly()
        val ideaKey = configurationKey.ideaKey
        map.computeIfAbsent(ideaKey) { k: Key<*>? -> HashMap<K, V>() }
        val data = map[ideaKey] as MutableMap<K?, V?>
        data[key] = value
    }

    fun <T> addAll(key: CompilerConfigurationKey<MutableList<T>>, values: MutableCollection<T>) {
        if (values != null) {
            addAll(key, getList(key).size, values)
        }
    }

    fun <T> addAll(key: CompilerConfigurationKey<MutableList<T>>, index: Int, values: MutableCollection<T>) {
        checkReadOnly()
        checkForNullElements(values)
        val ideaKey = key.ideaKey
        map.computeIfAbsent(ideaKey) { k: Key<*>? -> ArrayList<T>() }
        val list = map[ideaKey] as MutableList<T>
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
            return when (`object`) {
                is MutableList<*> -> {
                    Collections.unmodifiableList<Any?>(`object` as MutableList<*>) as T
                }

                is MutableMap<*, *> -> {
                    Collections.unmodifiableMap<Any?, Any?>(`object` as MutableMap<*, *>) as T
                }

                is MutableSet<*> -> {
                    Collections.unmodifiableSet<Any?>(`object` as MutableSet<*>) as T
                }

                is MutableCollection<*> -> {
                    Collections.unmodifiableCollection<Any?>(`object` as MutableCollection<*>) as T
                }

                else -> {
                    `object`
                }
            }
        }

        private fun <T> checkForNullElements(values: MutableCollection<T>) {
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
