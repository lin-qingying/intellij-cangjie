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
package org.cangnova.cangjie.descriptors.annotations

import org.cangnova.cangjie.resolve.constants.*

/**
 * 注解参数访问者接口，用于以访问者模式遍历不同类型的注解常量值。
 *
 * @param R 访问方法的返回类型
 * @param D 传递给访问方法的额外数据类型
 */
interface AnnotationArgumentVisitor<R, D> {
    /** 访问数组类型的注解值 */
    fun visitArrayValue(value: ArrayValue, data: D): R

    /** 访问字符串类型的注解值 */
    fun visitStringValue(value: StringValue, data: D): R

    /** 访问单元（无值）类型的注解值 */
    fun visitUnitValue(value: UnitValue, data: D): R

    /** 访问错误类型的注解值（用于错误恢复） */
    fun visitErrorValue(value: ErrorValue, data: D): R

    /** 访问 32 位整型的注解值 */
    fun visitInt32Value(value: Int32Value, data: D): R

    /** 访问 8 位整型的注解值 */
    fun visitInt8Value(value: Int8Value, data: D): R

    /** 访问 64 位整型的注解值 */
    fun visitInt64Value(value: Int64Value, data: D): R

    /** 访问 rune（字符）类型的注解值 */
    fun visitRuneValue(value: RuneValue, data: D): R

    /** 访问 16 位整型的注解值 */
    fun visitInt16Value(value: Int16Value, data: D): R

    /** 访问 64 位浮点类型的注解值 */
    fun visitFloat64Value(value: Float64Value, data: D): R

    /** 访问 16 位浮点类型的注解值 */
    fun visitFloat16Value(value: Float16Value, data: D): R

    /** 访问 32 位浮点类型的注解值 */
    fun visitFloat32Value(value: Float32Value, data: D): R

    /** 访问布尔类型的注解值 */
    fun visitBoolValue(value: BoolValue, data: D): R

    /** 访问无符号 32 位整型的注解值 */
    fun visitUInt32Value(value: UInt32Value, data: D): R

    /** 访问无符号 64 位整型的注解值 */
    fun visitUInt64Value(value: UInt64Value, data: D): R

    /** 访问无符号 16 位整型的注解值 */
    fun visitUInt16Value(value: UInt16Value, data: D): R

    /** 访问无符号 8 位整型的注解值 */
    fun visitUInt8Value(value: UInt8Value, data: D): R
}
