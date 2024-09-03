package com.huawei.cangjie.builtins

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.TupleClassDescriptor
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.util.asTypeProjection

fun createTupleType(

    builtIns: CangJieBuiltIns,
    annotations: Annotations,

    parameterTypes: List<CangJieType>,


    ): SimpleType {

    val arguments =
        getTupleTypeArgumentProjections(

            parameterTypes,

            )
    val classDescriptor = getTupleDescriptor(builtIns, parameterTypes.size)
    return CangJieTypeFactory.simpleNotNullType(annotations.toDefaultAttributes(), classDescriptor, arguments)


}

val CangJieType.isBuiltinTupleType: Boolean
    get() = constructor.declarationDescriptor?.isBuiltinTupleClassDescriptor == true

val DeclarationDescriptor.isBuiltinTupleClassDescriptor: Boolean
    get() {


        return this is TupleClassDescriptor

    }

fun getTupleDescriptor(builtIns: CangJieBuiltIns, parameterCount: Int) =
    /*   if (isSuspendFunction) builtIns.getSuspendFunction(parameterCount) else*/ builtIns.getTuple(parameterCount)

fun getTupleTypeArgumentProjections(

    parameterTypes: List<CangJieType>,


    ): List<TypeProjection> {
    val arguments =
        ArrayList<TypeProjection>(parameterTypes.size)


    parameterTypes.mapIndexedTo(arguments) { index, type ->


        type.asTypeProjection()
    }



    return arguments
}
