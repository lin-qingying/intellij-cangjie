package com.huawei.cangjie.resolve.constants

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeRefinement
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

class IntegerValueTypeConstructor(
    private val value: Long,
    private val module: ModuleDescriptor,
    parameters: CompileTimeConstant.Parameters
) : TypeConstructor {
    private val supertypes = ArrayList<CangJieType>(4)

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

    override fun getSupertypes(): Collection<CangJieType> = supertypes

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

//    override fun isFinal() = false

    override fun isDenotable() = false

    override fun getDeclarationDescriptor() = null

    fun getValue(): Long = value

    override fun getBuiltIns(): CangJieBuiltIns {
        return module.builtIns
    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    override fun toString() = "IntegerValueType($value)"
}

