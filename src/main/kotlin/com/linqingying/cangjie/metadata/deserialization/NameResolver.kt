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

package com.linqingying.cangjie.metadata.deserialization

/**
 * NameResolver 接口用于从给定索引解析名称。
 * 主要功能包括获取字符串值、完全限定类名以及判断类名是否为局部类名。
 */
interface NameResolver {
    /**
     * 获取指定索引处的字符串值。
     *
     * @param index 要检索的字符串值的索引。
     * @return 指定索引处的字符串值。
     */
    fun getString(index: Int): String

    /**
     * 获取指定索引处的类的完全限定名。
     * 完全限定名格式为：`org/foo/bar/Test.Inner`。
     *
     * @param index 要检索的类名的索引。
     * @return 指定索引处的类的完全限定名。
     */
    fun getQualifiedClassName(index: Int): String

    /**
     * 判断指定索引处的类名是否为局部类名。
     *
     * @param index 要检查的类名的索引。
     * @return 如果类名是局部类名则返回 `true`，否则返回 `false`。
     */
    fun isLocalClassName(index: Int): Boolean
}

