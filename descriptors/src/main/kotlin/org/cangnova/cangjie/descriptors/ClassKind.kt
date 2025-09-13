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

package org.cangnova.cangjie.descriptors


enum class ClassKind(val codeRepresentation: String?) {
    STRUCT("struct"),
    CLASS("class"),
    INTERFACE("interface"),
    TUPLE("tuple"),
    ENUM("enum"),
    EXTEND("extend"),
    ENUM_ENTRY(null),
    BASIC(null),
    BUILTIN(null),

    ;

    val isStruct: Boolean
        get() = this == STRUCT
    val isEnumEntry: Boolean
        get() = this == ENUM_ENTRY
    val isObject: Boolean
        get() =  isEnumEntry
    val isEnum: Boolean
        get() = this == ENUM || this == ENUM_ENTRY

    val isSingleton: Boolean
        get() =  this == ENUM_ENTRY
}
inline val ClassKind.isInterface: Boolean
    get() = this == ClassKind.INTERFACE

inline val ClassKind.isClass: Boolean
    get() = this == ClassKind.CLASS
