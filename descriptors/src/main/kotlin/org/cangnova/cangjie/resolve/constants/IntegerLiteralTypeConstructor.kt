/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.constants

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner


class FloatLiteralTypeConstructor : TypeConstructor {


    //
//    private val supertypes: List<CangJieType> by lazy {
////        根据标准库std.core中声明的扩展，所以基本类型一定有Any类型
////        listOf(builtIns.anyType)
//        emptyList()
//    }
    private val value: Double
    private val module: ModuleDescriptor
    val possibleTypes: Set<CangJieType>

    constructor(value: Double, module: ModuleDescriptor, parameters: CompileTimeConstant.Parameters) {
        this.value = value
        this.module = module

        val possibleTypes = mutableSetOf<CangJieType>()

        fun checkBoundsAndAddPossibleType(value: Double, cangjieType: CangJieType) {
            if (value.toLong() in cangjieType.minValue()..cangjieType.maxValue()) {
                possibleTypes.add(cangjieType)
            }
        }

        fun addSignedPossibleTypes() {
            possibleTypes.add(builtIns.float64Type)
            checkBoundsAndAddPossibleType(value, builtIns.float32Type)
            checkBoundsAndAddPossibleType(value, builtIns.float16Type)

        }


        val isConvertable = parameters.isConvertableConstVal



        when {
            isConvertable -> {
                addSignedPossibleTypes()

            }


            else -> addSignedPossibleTypes()
        }

        this.possibleTypes = possibleTypes
    }

    private constructor(value: Double, module: ModuleDescriptor, possibleTypes: Set<CangJieType>) {
        this.value = value
        this.module = module
        this.possibleTypes = possibleTypes
    }

    private val type = CangJieTypeFactory.floatLiteralType(TypeAttributes.Empty, this, false)

    private fun isContainsOnlyUnsignedTypes(): Boolean = module.allSignedLiteralTypes.all { it !in possibleTypes }
    override val supertypes: Collection<CangJieType> = emptyList()
    override val builtIns: CangJieBuiltIns
        get() = module.builtIns

    fun getApproximatedType(): CangJieType = when {
        builtIns.float16Type in possibleTypes -> builtIns.float16Type
        builtIns.float32Type in possibleTypes -> builtIns.float32Type
        builtIns.float64Type in possibleTypes -> builtIns.float64Type

        else -> throw IllegalStateException()
    }



    override val isDenotable: Boolean = false
    override val declarationDescriptor: ClassifierDescriptor? = null


    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
    override val isFinal: Boolean = true
    override val parameters: List<TypeParameterDescriptor> = emptyList()
}


class IntegerLiteralTypeConstructor : TypeConstructor {
    companion object {
        /**
         * 查找给定类型集合的公共超类型
         *
         * @param types 一个包含多个简单类型的集合
         * @return 这些类型的公共超类型，如果不存在则返回null
         */
        fun findCommonSuperType(types: Collection<SimpleType>): SimpleType? =
            findCommonSuperTypeOrIntersectionType(types, Companion.Mode.COMMON_SUPER_TYPE)

        /**
         * 查找给定类型集合的交集类型
         *
         * @param types 一个包含多个简单类型的集合
         * @return 这些类型的交集类型，如果不存在则返回null
         */
        fun findIntersectionType(types: Collection<SimpleType>): SimpleType? =
            findCommonSuperTypeOrIntersectionType(types, Companion.Mode.INTERSECTION_TYPE)

        /**
         * 模式枚举，用于确定类型查找的模式
         */
        private enum class Mode {
            COMMON_SUPER_TYPE, // 查找公共超类型的模式
            INTERSECTION_TYPE // 查找交集类型的模式
        }

        /**
         * 根据给定的类型集合和模式，寻找这些类型的公共超类型或交集类型
         * 此函数主要用于类型系统中的类型统一处理，根据不同的输入情况，返回相应的类型结果
         *
         * @param types 类型的集合，用于寻找公共超类型或交集类型
         * @param mode 模式，决定了是寻找公共超类型还是交集类型
         * @return 根据输入的类型集合和模式，返回对应的公共超类型或交集类型，如果没有则返回null
         */
        private fun findCommonSuperTypeOrIntersectionType(types: Collection<SimpleType>, mode: Mode): SimpleType? {
            // 如果类型集合为空，则直接返回null，因为没有类型可以处理
            if (types.isEmpty()) return null
            // 使用reduce函数对类型集合进行累积处理，每次处理两个类型，根据模式不同，执行不同的逻辑
            return types.reduce { left: SimpleType?, right: SimpleType? -> fold(left, right, mode) }
        }

        /**
         * 将两个SimpleType对象根据指定模式进行合并
         *
         * 此函数的目的是在特定模式下合并两个类型对象，以实现类型推断或优化
         * 它特别处理了整数字面量类型的构造器，以实现更精确的类型结果
         *
         * @param left 第一个SimpleType对象，可能为null
         * @param right 第二个SimpleType对象，可能为null
         * @param mode 指定的合并模式，决定如何处理输入的类型
         * @return 合并后的SimpleType对象，如果无法合并或输入无效则返回null
         */
        private fun fold(left: SimpleType?, right: SimpleType?, mode: Mode): SimpleType? {
            // 如果任一输入为null，则无法进行合并，直接返回null
            if (left == null || right == null) return null

            // 获取两个输入类型的构造器
            val leftConstructor = left.constructor
            val rightConstructor = right.constructor

            // 根据不同的构造器类型进行合并处理
            return when {
                // 当两个构造器都是整数字面量类型时，按照特殊规则进行合并
                leftConstructor is IntegerLiteralTypeConstructor && rightConstructor is IntegerLiteralTypeConstructor ->
                    fold(leftConstructor, rightConstructor, mode)

                // 当左构造器是整数字面量类型时，以特殊规则与右类型进行合并
                leftConstructor is IntegerLiteralTypeConstructor -> fold(leftConstructor, right)
                // 当右构造器是整数字面量类型时，以特殊规则与左类型进行合并
                rightConstructor is IntegerLiteralTypeConstructor -> fold(rightConstructor, left)
                // 当以上条件都不满足时，返回null，表示无法进行合并
                else -> null
            }
        }

        /**
         * 根据指定模式合并两个整数字面量类型构造器
         *
         * 此函数用于根据提供的模式，将两个整数字面量类型构造器合并为一个简单类型
         * 它主要在类型推断或类型检查时使用，以确定两个类型之间的公共超类型或交集类型
         *
         * @param left 第一个整数字面量类型构造器
         * @param right 第二个整数字面量类型构造器
         * @param mode 指定合并模式，可以是COMMON_SUPER_TYPE（寻找公共超类型）或INTERSECTION_TYPE（寻找类型交集）
         * @return 返回合并后的简单类型
         */
        private fun fold(
            left: IntegerLiteralTypeConstructor,
            right: IntegerLiteralTypeConstructor,
            mode: Mode
        ): SimpleType {
            // 根据提供的模式确定可能的类型集合
            val possibleTypes = when (mode) {
                Mode.COMMON_SUPER_TYPE -> left.possibleTypes intersect right.possibleTypes
                Mode.INTERSECTION_TYPE -> left.possibleTypes union right.possibleTypes
            }
            // 创建一个新的整数字面量类型构造器，结合了left和right的可能类型
            val constructor = IntegerLiteralTypeConstructor(left.value, left.module, possibleTypes)
            // 使用CangJie类型工厂创建并返回一个新的整数字面量类型
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
            possibleTypes.add(builtIns.int64Type)
            checkBoundsAndAddPossibleType(value, builtIns.int32Type)
            checkBoundsAndAddPossibleType(value, builtIns.int16Type)
            checkBoundsAndAddPossibleType(value, builtIns.int8Type)
        }

        fun addUnsignedPossibleTypes() {
            possibleTypes.add(module.uInt64Type)
            checkBoundsAndAddPossibleType(value, module.uInt32Type)

            checkBoundsAndAddPossibleType(value, module.uInt16Type)
            checkBoundsAndAddPossibleType(value, module.uInt8Type)

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

    override val supertypes: List<CangJieType> by lazy {
//        根据标准库std.core中声明的扩展，所以基本类型一定有Any类型
//        listOf(builtIns.anyType)
        emptyList()
    }

    fun getApproximatedType(): CangJieType = when {
        builtIns.int64Type in possibleTypes -> builtIns.int64Type
        builtIns.int32Type in possibleTypes -> builtIns.int32Type
        builtIns.int16Type in possibleTypes -> builtIns.int16Type
        builtIns.int8Type in possibleTypes -> builtIns.int8Type

        module.uInt32Type in possibleTypes -> module.uInt32Type
        module.uInt64Type in possibleTypes -> module.uInt64Type
        module.uInt8Type in possibleTypes -> module.uInt8Type
        module.uInt16Type in possibleTypes -> module.uInt16Type

        else -> throw IllegalStateException()
    }

    override val parameters: List<TypeParameterDescriptor> = emptyList()



    override val builtIns: CangJieBuiltIns
        get() = module.builtIns
    override val isDenotable: Boolean = false
    override val declarationDescriptor: ClassifierDescriptor? = null



    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
    override val isFinal: Boolean = true

    override fun toString(): String {
        return "IntegerLiteralType${valueToString()}"
    }

    fun checkConstructor(constructor: TypeConstructor): Boolean = possibleTypes.any { it.constructor == constructor }

    private fun valueToString(): String = "[${possibleTypes.joinToString(",") { it.toString() }}]"

}
