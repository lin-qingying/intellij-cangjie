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

package org.cangnova.cangjie.utils


private val IDENTITY: (Any?) -> Any? = { it }

@Suppress("UNCHECKED_CAST") fun <T> identity(): (T) -> T = IDENTITY as (T) -> T


private val ALWAYS_TRUE: (Any?) -> Boolean = { true }

fun <T> alwaysTrue(): (T) -> Boolean = ALWAYS_TRUE

private val ALWAYS_NULL: (Any?) -> Any? = { null }

@Suppress("UNCHECKED_CAST")
fun <T, R: Any> alwaysNull(): (T) -> R? = ALWAYS_NULL as (T) -> R?

val DO_NOTHING: (Any?) -> Unit = { }
val DO_NOTHING_2: (Any?, Any?) -> Unit = { _, _ -> }
val DO_NOTHING_3: (Any?, Any?, Any?) -> Unit = { _, _, _ -> }

fun <T> doNothing(): (T) -> Unit = DO_NOTHING

fun doNothing() {}
