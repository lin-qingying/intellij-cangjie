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

package org.cangnova.cangjie.name

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.StandardNames.FqNames.arrayFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.boolFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.cpointerFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.cstringFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.float16FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.float32FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.float64FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int16FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int32FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int64FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int8FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int_nativeFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.nothingFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.runeFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint16FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint32FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint64FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint8FqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint_nativeFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.unitFqName


object StandardClassIds {
    val BASE_CANGJIE_PACKAGE = FqName("cangjie")
    val BASE_STD_PACKAGE = FqName("std")
    val BASE_STD_CORE_PACKAGE = BASE_STD_PACKAGE.child(Name.identifier("core"))


    val BASE_STD_PACKAGES = setOf(BASE_STD_PACKAGE, BASE_STD_CORE_PACKAGE)

    val builtInsPackagesWithDefaultNamedImport: Set<FqName> = setOf(

    )

    val builtInsPackages = setOf(
        BASE_STD_CORE_PACKAGE
//          *BASE_STD_PACKAGES.toTypedArray()
    )


    fun byName(name: String) = name.baseId()


    @Suppress("FunctionName")
    fun FunctionN(n: Int): ClassId {
        return "Function$n".baseId()
    }

    val CStringClassId = cstringFqName.toClassId()
    val CPointerClassId = cpointerFqName.toClassId()
    val ArrayClassId = arrayFqName.toClassId()

    // Int types
    val INT8ClassId = int8FqName.toClassId()
    val INT16ClassId = int16FqName.toClassId()
    val INT32ClassId = int32FqName.toClassId()
    val INT64ClassId = int64FqName.toClassId()
    val INTNATIVEClassId = int_nativeFqName.toClassId()

    // UInt types
    val UINT8ClassId = uint8FqName.toClassId()
    val UINT16ClassId = uint16FqName.toClassId()
    val UINT32ClassId = uint32FqName.toClassId()
    val UINT64ClassId = uint64FqName.toClassId()
    val UINTNATIVEClassId = uint_nativeFqName.toClassId()

    // Float types
    val FLOAT16ClassId = float16FqName.toClassId()
    val FLOAT32ClassId = float32FqName.toClassId()
    val FLOAT64ClassId = float64FqName.toClassId()

    // Other primitive types
    val BOOLClassId = boolFqName.toClassId()
    val RUNEClassId = runeFqName.toClassId()
    val UNITClassId = unitFqName.toClassId()
    val NOTHINGClassId = nothingFqName.toClassId()


    object Annotations {

        object ParameterNames
    }

    object Callables

    object Collections


}

private fun String.baseId() = ClassId(StandardClassIds.BASE_CANGJIE_PACKAGE, Name.identifier(this))
