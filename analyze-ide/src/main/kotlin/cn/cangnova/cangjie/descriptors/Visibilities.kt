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

package cn.cangnova.cangjie.descriptors


object Visibilities {
    object Private : Visibility("private", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object PrivateToThis : Visibility("private_to_this", isPublicAPI = false) {
        override val internalDisplayName: String
            get() = "private/*private to this*/"

        override fun mustCheckInImports(): Boolean = true
    }

    object Protected : Visibility("protected", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Internal : Visibility("internal", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Public : Visibility("public", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = false
    }

    object Local : Visibility("local", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Inherited : Visibility("inherited", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for INHERITED myVisibility")
        }
    }

    object InvisibleFake : Visibility("invisible_fake", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true

        override val externalDisplayName: String
            get() = "invisible (private in a supertype)"
    }

    object Unknown : Visibility("unknown", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for UNKNOWN myVisibility")
        }
    }

    private val ORDERED_VISIBILITIES: Map<Visibility, Int> = buildMap {
        put(PrivateToThis, 0)
        put(Private, 0)
        put(Internal, 1)
        put(Protected, 1)
        put(Public, 2)
    }

    fun compare(first: Visibility, second: Visibility): Int? {
        val result = first.compareTo(second)
        if (result != null) {
            return result
        }
        val oppositeResult = second.compareTo(first)
        return if (oppositeResult != null) {
            -oppositeResult
        } else null
    }

    /**
     * 比较两个可见性对象
     * 此函数旨在评估两个可见性对象是否相等，以及它们在预定顺序中的相对位置
     * 它主要用于需要理解可见性层次结构的场景
     *
     * @param first 第一个可见性对象，用于比较
     * @param second 第二个可见性对象，与第一个可见性对象进行比较
     * @return 返回值表示两个可见性对象的相对顺序：
     * - 如果两个可见性对象完全相同，返回0
     * - 如果任何一个可见性对象不在预定的顺序列表中，或者它们的顺序相同，则返回null
     * - 否则，返回它们在预定顺序中的相对差值，正值表示第一个可见性对象更可见，负值表示第二个可见性对象更可见
     */
    internal fun compareLocal(first: Visibility, second: Visibility): Int? {
        // 检查两个可见性对象是否完全相同
        if (first === second) return 0
        // 获取两个可见性对象在预定顺序中的索引
        val firstIndex = ORDERED_VISIBILITIES[first]
        val secondIndex = ORDERED_VISIBILITIES[second]
        // 如果任何一个索引为空，或者两个索引相同，则无法进行比较，返回null
        return if (firstIndex == null || secondIndex == null || firstIndex == secondIndex) {
            null
        } else firstIndex - secondIndex
    }

    fun isPrivate(visibility: Visibility): Boolean {
        return visibility === Private || visibility === PrivateToThis
    }

    val DEFAULT_VISIBILITY = Public
}
