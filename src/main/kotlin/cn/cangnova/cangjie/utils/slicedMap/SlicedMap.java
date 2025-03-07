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

package cn.cangnova.cangjie.utils.slicedMap;

import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public interface SlicedMap {

    SlicedMap DO_NOTHING = new SlicedMap() {
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return slice.computeValue(this, key, null, true);
        }

        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return Collections.emptySet();
        }

        @Override
        public void forEach(@NotNull Function3<WritableSlice, Object, Object, Void> f) {
        }
    };

    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must return true
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);

    void forEach(@NotNull Function3<WritableSlice, Object, Object, Void> f);
}
