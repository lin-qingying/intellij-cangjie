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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.parsing

data class NumberWithRadix(val number: String, val radix: Int)

fun extractRadix(value: String): NumberWithRadix = when {
    value.startsWith("0x") || value.startsWith("0X") -> NumberWithRadix(value.substring(2), 16)
    value.startsWith("0b") || value.startsWith("0B") -> NumberWithRadix(value.substring(2), 2)
    else -> NumberWithRadix(value, 10)
}
