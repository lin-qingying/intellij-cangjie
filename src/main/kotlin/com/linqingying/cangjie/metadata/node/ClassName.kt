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


@file:JvmName("ClassNameKt")

package com.linqingying.cangjie.metadata.node
/**
 *
 * 该名称中的包名用 '/' 分隔，类名用 '.' 分隔，例如：`"org/foo/bar/Baz.Nested"`。
 *
 */
typealias ClassName = String

/**
 * 检查类名 [this] 是否表示一个局部类或匿名对象。
 *
 * 如果类名以 '.'（点）开头，则表示一个局部类或匿名对象。
 */
fun ClassName.isLocalClassName(): Boolean = this.startsWith(".")
