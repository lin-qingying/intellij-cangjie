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

package org.cangnova.cangjie.descriptors


/**
 * 类类型枚举
 * @property codeRepresentation 代码中的表示形式(可能为null)
 */
enum class ClassKind(val codeRepresentation: String?) {
    /** 结构体类型 */
    STRUCT("struct"),
    /** 类类型 */
    CLASS("class"),
    /** 接口类型 */
    INTERFACE("interface"),
    /** 元组类型 */
    TUPLE("tuple"),
    /** 枚举类型 */
    ENUM("enum"),
    /** 扩展类型 */
    EXTEND("extend"),

    /** 基本类型(无代码表示) */
    BASIC(null),
    /** 内置类型(无代码表示) */
    BUILTIN(null),

    ;

    /** 是否是结构体类型 */
    val isStruct: Boolean
        get() = this == STRUCT


    /** 是否是枚举类型 */
    val isEnum: Boolean
        get() = this == ENUM


}
/** 是否是接口类型 */
inline val ClassKind.isInterface: Boolean
    get() = this == ClassKind.INTERFACE

/** 是否是类类型 */
inline val ClassKind.isClass: Boolean
    get() = this == ClassKind.CLASS
