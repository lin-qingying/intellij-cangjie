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
 */

package org.cangnova.cangjie.test

/**
 * 对位 Kotlin `Directives` 的文本指令容器。
 *
 * 该层只建模测试文件中的 `// DIRECTIVE` / `// DIRECTIVE: value` 约定，
 * 保持 generated / multi-file / multi-module 测试在框架层拥有稳定的指令访问方式。
 */
class Directives {
    private val directives: MutableMap<String, MutableList<String>?> = mutableMapOf()

    operator fun contains(key: String): Boolean = key in directives

    operator fun get(key: String): String? = directives[key]?.single()

    fun getValue(key: String): String {
        val values = directives[key]
        return when {
            values == null -> error("'$key' is not found")
            values.size > 1 -> error("Too many '$key' directives")
            else -> values.single()
        }
    }

    fun getBooleanValue(key: String): Boolean = contains(key) && getValue(key) == "TRUE"

    fun put(key: String, value: String?) {
        if (value == null) {
            directives[key] = null
            return
        }

        directives.getOrPut(key) { arrayListOf() }.let { values ->
            values?.add(value) ?: error("Null value was already passed to $key via a valueless directive")
        }
    }

    /**
     * 支持：
     * 1. 多次重复声明同一 directive；
     * 2. 单行中使用逗号分隔多个值。
     */
    fun listValues(name: String): List<String>? {
        return directives[name]?.flatMap { value ->
            value.split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
        }
    }
}
