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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.TupleClassDescriptor

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
    return CangJieTypeFactory.simpleType(annotations.toDefaultAttributes(), classDescriptor, arguments)


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


    ): List<TypeArgument> {
    val arguments =
        ArrayList<TypeArgument>(parameterTypes.size)


    parameterTypes.mapIndexedTo(arguments) { index, type ->


        type.asTypeArgument()
    }



    return arguments
}
