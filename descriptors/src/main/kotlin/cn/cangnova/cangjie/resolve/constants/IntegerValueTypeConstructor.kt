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

package cn.cangnova.cangjie.resolve.constants

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.StandardNames
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.TypeRefinement
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
class FloatValueTypeConstructor(
    private val value: Double,
    private val module: ModuleDescriptor,
    parameters: CompileTimeConstant.Parameters
) : TypeConstructor {
    override val supertypes = ArrayList<CangJieType>(4)

    init {

        val isConvertable = parameters.isConvertableConstVal

        if ( isConvertable) {
            assert(hasUnsignedTypesInModuleDependencies(module)) {
                "Unsigned types should be on classpath to create an unsigned type constructor"
            }
        }

        when {
            isConvertable -> {
                addSignedSuperTypes()

            }



            else -> addSignedSuperTypes()
        }
    }

    private fun addSignedSuperTypes() {
        checkBoundsAndAddSuperType(value, builtIns.float16Type)
        checkBoundsAndAddSuperType(value, builtIns.float32Type)

        supertypes.add(builtIns.float64Type)
    }


    private fun checkBoundsAndAddSuperType(value: Double, cangjieType: CangJieType) {
        if (value.toLong() in cangjieType.minValue()..cangjieType.maxValue()) {
            supertypes.add(cangjieType)
        }
    }

    override val parameters: List<TypeParameterDescriptor> = emptyList()

    override val isFinal: Boolean = false
    override val isDenotable: Boolean = false
    override val declarationDescriptor: ClassifierDescriptor? = null
    fun getValue(): Double = value


    override val builtIns: CangJieBuiltIns
        get() = module.builtIns
    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    override fun toString() = "IntegerValueType($value)"
}


class IntegerValueTypeConstructor(
    private val value: Long,
    private val module: ModuleDescriptor,
    parameters: CompileTimeConstant.Parameters
) : TypeConstructor {
    override val supertypes = ArrayList<CangJieType>(4)

    init {
        // order of types matters
        // 'getPrimitiveNumberType' returns first of supertypes that is a subtype of expected type
        // for expected type 'Any' result type 'Int' should be returned
        val isUnsigned = parameters.isUnsignedNumberLiteral
        val isConvertable = parameters.isConvertableConstVal

        if (isUnsigned || isConvertable) {
            assert(hasUnsignedTypesInModuleDependencies(module)) {
                "Unsigned types should be on classpath to create an unsigned type constructor"
            }
        }

        when {
            isConvertable -> {
                addSignedSuperTypes()
                addUnsignedSuperTypes()
            }

            isUnsigned -> addUnsignedSuperTypes()

            else -> addSignedSuperTypes()
        }
    }

    private fun addSignedSuperTypes() {
        checkBoundsAndAddSuperType(value, builtIns.int32Type)
        checkBoundsAndAddSuperType(value, builtIns.int8Type)
        checkBoundsAndAddSuperType(value, builtIns.int16Type)
        supertypes.add(builtIns.int64Type)
    }

    private fun addUnsignedSuperTypes() {
        checkBoundsAndAddSuperType(value, module.unsignedType(StandardNames.FqNames.uInt32ClassId))
        checkBoundsAndAddSuperType(value, module.unsignedType(StandardNames.FqNames.uInt8ClassId))
        checkBoundsAndAddSuperType(value, module.unsignedType(StandardNames.FqNames.uInt16ClassId))
        supertypes.add(module.unsignedType(StandardNames.FqNames.uInt64ClassId))
    }

    private fun checkBoundsAndAddSuperType(value: Long, cangjieType: CangJieType) {
        if (value in cangjieType.minValue()..cangjieType.maxValue()) {
            supertypes.add(cangjieType)
        }
    }

    override val parameters: List<TypeParameterDescriptor> = emptyList()
    override val isFinal: Boolean  = false
    override val isDenotable: Boolean = false
    override val declarationDescriptor: ClassifierDescriptor? = null

    fun getValue(): Long = value



    override val builtIns: CangJieBuiltIns
        get() = module.builtIns

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    override fun toString() = "IntegerValueType($value)"
}

