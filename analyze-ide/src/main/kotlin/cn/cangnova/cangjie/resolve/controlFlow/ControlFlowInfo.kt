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

package cn.cangnova.cangjie.resolve.controlFlow

import cn.cangnova.cangjie.utils.ImmutableHashMap
import cn.cangnova.cangjie.utils.ImmutableMap

abstract class ControlFlowInfo<S : ControlFlowInfo<S, K, D>, K : Any, D : Any>
internal constructor(
    protected val map: ImmutableMap<K, D> = ImmutableHashMap.empty()
) : ImmutableMap<K, D> by map, ReadOnlyControlFlowInfo<K, D> {
    protected abstract fun copy(newMap: ImmutableMap<K, D>): S

    override fun put(key: K, value: D): S = put(key, value, this[key].getOrElse(null as D?))

    /**
     * This overload exists just for sake of optimizations: in some cases we've just retrieved the old value,
     * so we don't need to scan through the persistent hashmap again
     */
    fun put(key: K, value: D, oldValue: D?): S {
        @Suppress("UNCHECKED_CAST")
        // Avoid a copy instance creation if new value is the same
        if (value == oldValue) return this as S
        return copy(map.put(key, value))
    }

    override fun getOrNull(key: K): D? = this[key].getOrElse(null as D?)
    override fun asMap() = this

    fun retainAll(predicate: (K) -> Boolean): S = copy(map.removeAll(map.keySet().filterNot(predicate)))

    override fun equals(other: Any?) = map == (other as? ControlFlowInfo<*, *, *>)?.map

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()
}
interface ReadOnlyControlFlowInfo<K : Any, D : Any> {
    fun getOrNull(key: K): D?
    // Only used in tests
    fun asMap(): ImmutableMap<K, D>
}
