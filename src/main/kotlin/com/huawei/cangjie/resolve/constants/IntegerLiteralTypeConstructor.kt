package com.huawei.cangjie.resolve.constants

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner


class IntegerLiteralTypeConstructor : TypeConstructor {
    companion object {
        fun findCommonSuperType(types: Collection<SimpleType>): SimpleType? =
            findCommonSuperTypeOrIntersectionType(types, Companion.Mode.COMMON_SUPER_TYPE)

        fun findIntersectionType(types: Collection<SimpleType>): SimpleType? =
            findCommonSuperTypeOrIntersectionType(types, Companion.Mode.INTERSECTION_TYPE)

        private enum class Mode {
            COMMON_SUPER_TYPE, INTERSECTION_TYPE
        }

        /**
         * intersection(ILT(types), PrimitiveType) = commonSuperType(ILT(types), PrimitiveType) =
         *      PrimitiveType  in types  -> PrimitiveType
         *      PrimitiveType !in types -> null
         *
         * intersection(ILT(types_1), ILT(types_2)) = ILT(types_1 union types_2)
         *
         * commonSuperType(ILT(types_1), ILT(types_2)) = ILT(types_1 intersect types_2)
         */
        private fun findCommonSuperTypeOrIntersectionType(types: Collection<SimpleType>, mode: Mode): SimpleType? {
            if (types.isEmpty()) return null
            return types.reduce { left: SimpleType?, right: SimpleType? -> fold(left, right, mode) }
        }

        private fun fold(left: SimpleType?, right: SimpleType?, mode: Mode): SimpleType? {
            if (left == null || right == null) return null
            val leftConstructor = left.constructor
            val rightConstructor = right.constructor
            return when {
                leftConstructor is IntegerLiteralTypeConstructor && rightConstructor is IntegerLiteralTypeConstructor ->
                    fold(leftConstructor, rightConstructor, mode)

                leftConstructor is IntegerLiteralTypeConstructor -> fold(leftConstructor, right)
                rightConstructor is IntegerLiteralTypeConstructor -> fold(rightConstructor, left)
                else -> null
            }
        }

        private fun fold(left: IntegerLiteralTypeConstructor, right: IntegerLiteralTypeConstructor, mode: Mode): SimpleType? {
            val possibleTypes = when (mode) {
                Mode.COMMON_SUPER_TYPE -> left.possibleTypes intersect right.possibleTypes
                Mode.INTERSECTION_TYPE -> left.possibleTypes union right.possibleTypes
            }
            val constructor = IntegerLiteralTypeConstructor(left.value, left.module, possibleTypes)
            return CangJieTypeFactory.integerLiteralType(TypeAttributes.Empty, constructor, false)
        }

        private fun fold(left: IntegerLiteralTypeConstructor, right: SimpleType): SimpleType? =
            if (right in left.possibleTypes) right else null

    }

    private val value: Long
    private val module: ModuleDescriptor
    val possibleTypes: Set<CangJieType>

    constructor(value: Long, module: ModuleDescriptor, parameters: CompileTimeConstant.Parameters) {
        this.value = value
        this.module = module

        val possibleTypes = mutableSetOf<CangJieType>()

        fun checkBoundsAndAddPossibleType(value: Long, cangjieType: CangJieType) {
            if (value in cangjieType.minValue()..cangjieType.maxValue()) {
                possibleTypes.add(cangjieType)
            }
        }

        fun addSignedPossibleTypes() {
            checkBoundsAndAddPossibleType(value, builtIns.int32Type)
            possibleTypes.add(builtIns.int64Type)
            checkBoundsAndAddPossibleType(value, builtIns.int8Type)
            checkBoundsAndAddPossibleType(value, builtIns.int16Type)
        }

        fun addUnsignedPossibleTypes() {
            checkBoundsAndAddPossibleType(value, module.uInt32Type)
            possibleTypes.add(module.uInt64Type)
            checkBoundsAndAddPossibleType(value, module.uInt8Type)
            checkBoundsAndAddPossibleType(value, module.uInt16Type)
        }

        val isUnsigned = parameters.isUnsignedNumberLiteral
        val isConvertable = parameters.isConvertableConstVal

//        if (isUnsigned || isConvertable) {
//            assert(hasUnsignedTypesInModuleDependencies(module)) {
//                "Unsigned types should be on classpath to create an unsigned type constructor"
//            }
//        }

        when {
            isConvertable -> {
                addSignedPossibleTypes()
                addUnsignedPossibleTypes()
            }

            isUnsigned -> addUnsignedPossibleTypes()

            else -> addSignedPossibleTypes()
        }

        this.possibleTypes = possibleTypes
    }

    private constructor(value: Long, module: ModuleDescriptor, possibleTypes: Set<CangJieType>) {
        this.value = value
        this.module = module
        this.possibleTypes = possibleTypes
    }

    private val type = CangJieTypeFactory.integerLiteralType(TypeAttributes.Empty, this, false)

    private fun isContainsOnlyUnsignedTypes(): Boolean = module.allSignedLiteralTypes.all { it !in possibleTypes }

    private val supertypes: List<CangJieType> by lazy {
        val result = mutableListOf(builtIns.comparable.defaultType.replace(listOf(TypeProjectionImpl(Variance.IN_VARIANCE, type))))
        if (!isContainsOnlyUnsignedTypes()) {
            result += builtIns.numberType
        }
        result
    }

    fun getApproximatedType(): CangJieType = when {
        builtIns.int32Type in possibleTypes -> builtIns.int32Type
        builtIns.int64Type in possibleTypes -> builtIns.int64Type
        builtIns.int8Type in possibleTypes -> builtIns.int8Type
        builtIns.int16Type in possibleTypes -> builtIns.int16Type

        module.uInt32Type in possibleTypes -> module.uInt32Type
        module.uInt64Type in possibleTypes -> module.uInt64Type
        module.uInt8Type in possibleTypes -> module.uInt8Type
        module.uInt16Type in possibleTypes -> module.uInt16Type

        else -> throw IllegalStateException()
    }


    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<CangJieType> = supertypes

//    override fun isFinal(): Boolean = true

    override fun isDenotable(): Boolean = false

    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns(): CangJieBuiltIns = module.builtIns

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    override fun toString(): String {
        return "IntegerLiteralType${valueToString()}"
    }

    fun checkConstructor(constructor: TypeConstructor): Boolean = possibleTypes.any { it.constructor == constructor }

    private fun valueToString(): String = "[${possibleTypes.joinToString(",") { it.toString() }}]"

}
