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

package cn.cangnova.cangjie.descriptors.annotations;


import cn.cangnova.cangjie.resolve.constants.*;

public interface AnnotationArgumentVisitor<R, D> {
    R visitArrayValue(ArrayValue value, D data);

    R visitStringValue(StringValue value, D data);
    R visitUnitValue(UnitValue value, D data);

    R visitErrorValue(ErrorValue value, D data);

    R visitInt32Value(Int32Value value, D data);

    R visitInt8Value(Int8Value value, D data);

    R visitInt64Value(Int64Value value, D data);

    R visitRuneValue(RuneValue value, D data);

    R visitInt16Value(Int16Value value, D data);

    R visitFloat64Value(Float64Value value, D data);
    R visitFloat16Value(Float16Value value, D data);

    R visitFloat32Value(Float32Value value, D data);

    R visitBoolValue(BoolValue value, D data);
    R visitUInt32Value(UInt32Value value, D data);
    R visitUInt64Value(UInt64Value value, D data);

    R visitUInt16Value(UInt16Value value, D data);

    R visitUInt8Value(UInt8Value value, D data);

//    R visitLongValue(@NotNull LongValue value, D data);
//
//    R visitIntValue(Int32Value value, D data);
//
//    R visitErrorValue(ErrorValue value, D data);
//
//    R visitShortValue(ShortValue value, D data);
//
//    R visitByteValue(ByteValue value, D data);
//
//    R visitDoubleValue(DoubleValue value, D data);
//
//    R visitFloatValue(FloatValue value, D data);
//
//    R visitBooleanValue(BooleanValue value, D data);
//
//    R visitCharValue(CharValue value, D data);
//
//    R visitStringValue(StringValue value, D data);
//
//    R visitNullValue(NullValue value, D data);
//
//    R visitEnumValue(EnumValue value, D data);
//
//    R visitArrayValue(ArrayValue value, D data);
//
//    R visitAnnotationValue(AnnotationValue value, D data);
//
//    R visitKClassValue(KClassValue value, D data);
//
//    R visitUByteValue(UByteValue value, D data);
//
//    R visitUShortValue(UShortValue value, D data);
//
//    R visitUIntValue(UIntValue value, D data);
//
//    R visitULongValue(ULongValue value, D data);
}
