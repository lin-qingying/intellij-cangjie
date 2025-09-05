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


interface AnnotationArgumentVisitor<R, D> {
    fun visitArrayValue(value: ArrayValue, data: D): R

    fun visitStringValue(value: StringValue, data: D): R

    fun visitUnitValue(value: UnitValue, data: D): R

    fun visitErrorValue(value: ErrorValue, data: D): R

    fun visitInt32Value(value: Int32Value, data: D): R

    fun visitInt8Value(value: Int8Value, data: D): R

    fun visitInt64Value(value: Int64Value, data: D): R

    fun visitRuneValue(value: RuneValue, data: D): R

    fun visitInt16Value(value: Int16Value, data: D): R

    fun visitFloat64Value(value: Float64Value, data: D): R

    fun visitFloat16Value(value: Float16Value, data: D): R

    fun visitFloat32Value(value: Float32Value, data: D): R

    fun visitBoolValue(value: BoolValue, data: D): R

    fun visitUInt32Value(value: UInt32Value, data: D): R

    fun visitUInt64Value(value: UInt64Value, data: D): R

    fun visitUInt16Value(value: UInt16Value, data: D): R

    fun visitUInt8Value(value: UInt8Value, data: D): R
}
