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


package cn.cangnova.cangjie.metadata.serialization

class Interner<T>(private val parent: Interner<T>? = null) {
    private val firstIndex: Int = parent?.run { interned.size + firstIndex } ?: 0
    private val interned = hashMapOf<T, Int>()

    val allInternedObjects: List<T>
        get() = interned.keys.sortedBy(interned::get)

    val isEmpty: Boolean
        get() = interned.isEmpty() && parent?.isEmpty != false

    private fun find(obj: T): Int? {
        assert(parent == null || parent.interned.size + parent.firstIndex == firstIndex) {
            "Parent changed in parallel with child: indexes will be wrong"
        }
        return parent?.find(obj) ?: interned[obj]
    }

    fun intern(obj: T): Int =
        find(obj) ?: (firstIndex + interned.size).also {
            interned[obj] = it
        }
}
