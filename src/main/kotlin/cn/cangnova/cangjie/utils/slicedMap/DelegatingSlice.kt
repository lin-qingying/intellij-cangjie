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

package cn.cangnova.cangjie.utils.slicedMap
//
//open class DelegatingSlice<K, V>(val delegate: WritableSlice<K, V>) : WritableSlice<K, V> {
//
//
//    override fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V? {
//        return delegate.computeValue(map, key, value, valueNotFound)
//
//    }
//
//    override fun makeRawValueVersion(): ReadOnlySlice<K, V>? {
//        return delegate.makeRawValueVersion()
//
//    }
//
//    override fun getKey(): KeyWithSlice<K, V, WritableSlice<K, V>> {
//        TODO("Not yet implemented")
//    }
//
//    override fun check(key: K, value: V?): Boolean {
//        return delegate.check(key, value)
//
//    }
//
//    override fun afterPut(map: MutableSlicedMap, key: K, value: V) {
//        delegate.afterPut(map, key, value)
//
//    }
//
//    override val rewritePolicy: RewritePolicy
//        get() = delegate.rewritePolicy
//
//    override val isCollective: Boolean
//        get() = delegate.isCollective
//}
