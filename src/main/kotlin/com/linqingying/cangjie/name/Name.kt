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

package com.linqingying.cangjie.name


data class Name(private val name: String, val isSpecial: Boolean) : Comparable<Name> {

    fun asString(): String {
        return name
    }


    fun asStringRoot(): String {
        return "${name}:${isRoot}"
    }

    var isRoot: Boolean = false

    val identifier: String
        get() {
            check(!isSpecial) { "not identifier: $this" }
            return asString()
        }

    fun asStringStripSpecialMarkers(): String {
        return if (isSpecial) asString().substring(1, asString().length - 1) else asString()
    }

    override operator fun compareTo(other: Name): Int {
        return name.compareTo(other.name)
    }

    val identifierOrNullIfSpecial: String?
        get() = if (isSpecial) null else asString()

    override fun toString(): String {
        return name
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Name) return false
        if (isSpecial != other.isSpecial) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + if (isSpecial) 1 else 0
        return result
    }

    companion object {
        @JvmField
        val ERROR_NAME = Name("<error>", true)
        @JvmStatic
        fun identifier(name: String): Name {
            return Name(name, false)
        }

        @JvmStatic
        fun isValidIdentifier(name: String): Boolean {
            if (name.isEmpty() || name.startsWith("<")) return false
            for (element in name) {
                if (element == '.' || element == '/' || element == '\\') {
                    return false
                }
            }
            return true
        }

        @JvmStatic
        fun identifierIfValid(name: String): Name? {
            return if (!isValidIdentifier(name)) null else identifier(name)
        }

        @JvmStatic
        fun special(name: String): Name {
            require(name.startsWith("<")) { "special name must start with '<': $name" }
            return Name(name, true)
        }

        @JvmStatic
        fun guessByFirstCharacter(name: String): Name {
            return if (name.startsWith("<")) {
                special(name)
            } else {
                identifier(name)
            }
        }

        @JvmStatic
        fun guessByFirstCharacterRoot(name: String): Name {
            val (_name, isRoot) = name.split(":")
            return if (_name.startsWith("<")) {
                special(_name)
            } else {
                identifier(_name)
            }.apply {
                this.isRoot = isRoot.toBoolean()
            }
        }


    }
}

