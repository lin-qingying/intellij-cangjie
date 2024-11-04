package com.linqingying.cangjie.descriptors.annotations;


import com.linqingying.cangjie.resolve.constants.*;

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
