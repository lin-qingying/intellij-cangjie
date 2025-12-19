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

package org.cangnova.cangjie.builtins

import org.cangnova.cangjie.builtins.StandardNames.BASIC_PACKAGE_FQ_NAME
import org.cangnova.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_NAME
import org.cangnova.cangjie.builtins.StandardNames.COMPARABLE
import org.cangnova.cangjie.builtins.StandardNames.COUNTABLE
import org.cangnova.cangjie.builtins.StandardNames.EQUATABLE
import org.cangnova.cangjie.builtins.StandardNames.FUTURE
import org.cangnova.cangjie.builtins.StandardNames.FqNames.anyUFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.arrayClassFqNameToPrimitiveType
import org.cangnova.cangjie.builtins.StandardNames.FqNames.arrayUFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.ast
import org.cangnova.cangjie.builtins.StandardNames.FqNames.boolUFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.core
import org.cangnova.cangjie.builtins.StandardNames.FqNames.float16UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.float32UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.float64UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int16UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int32UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int64UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.int8UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.nothingUFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.optionUFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.primitiveArrayTypeShortNames
import org.cangnova.cangjie.builtins.StandardNames.FqNames.runeUFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.stringUFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.sync
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint16UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint32UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.uint8UFqName
import org.cangnova.cangjie.builtins.StandardNames.FqNames.unitUFqName
import org.cangnova.cangjie.builtins.StandardNames.RANGE
import org.cangnova.cangjie.builtins.StandardNames.RESOURCE
import org.cangnova.cangjie.builtins.StandardNames.STD_PACKAGE_NAME
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.resolveClassByFqName
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.functions.FunctionTypeKind

open class CangJieBuiltIns(
    val projectDescriptor: ProjectDescriptor,
    val storageManager: StorageManager,
) {


    companion object {
        val DefaultBuiltIns: CangJieBuiltIns =
            CangJieBuiltIns(ProjectDescriptor.ERROR, LockBasedStorageManager("DefaultBuiltIns"))


        /**
         * @return true if the containing package of the descriptor is "cangjie" or any subpackage of "cangjie"
         */
        fun isUnderCangJiePackage(descriptor: DeclarationDescriptor): Boolean {
            var current: DeclarationDescriptor? = descriptor
            while (current != null) {
                if (current is PackageFragmentDescriptor) {
                    return current.fqName.startsWith(
                        BUILT_INS_PACKAGE_NAME
                    )
                }
                current = current.containingDeclaration
            }
            return false
        }

        @JvmStatic
        fun isString(type: CangJieType?): Boolean {
            return type != null && isNotNullConstructedFromGivenClass(
                type,
                stringUFqName
            )
        }

        fun isOptionNothing(type: CangJieType): Boolean {
            return isNothing(type)
                    && TypeUtils.isOptionType(type)
        }

        fun getPrimitiveArrayType(descriptor: DeclarationDescriptor): PrimitiveType? {
            return if (primitiveArrayTypeShortNames.contains(descriptor.name))
                arrayClassFqNameToPrimitiveType.get(DescriptorUtils.getFqName(descriptor))
            else
                null
        }

        fun isPrimitiveArray(type: CangJieType): Boolean {
            val descriptor =
                type.constructor.declarationDescriptor
            return descriptor != null && getPrimitiveArrayType(descriptor) != null
        }

        //        fun isUByteArray(type: CangJieType): Boolean {
//            return  isConstructedFromGivenClass (
//                type,
//                uByteArrayFqName.toUnsafe()
//            )
//        }
//        fun isUnsignedArrayType(type: CangJieType): Boolean {
//            return  isUInt8Array(type) ||  isUInt16Array(
//                type
//            ) ||  isUInt32Array(type) ||  isUInt64Array(
//                type
//            )
//        }
        //        fun isNotNullOrNullableFunctionSupertype(type:CangJieType): Boolean {
//            return  isConstructedFromGivenClass(type, functionSupertype)
//        }
        val BUILTINS_MODULE_NAME: Name =
            Name.special("<built-ins module>")

        @JvmStatic
        fun isBoolean(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(
                type,
                boolUFqName
            )
        }

        fun isOptionType(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, optionUFqName)
        }

        fun isRune(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, runeUFqName)
        }

        fun isTuple(type: CangJieType): Boolean {
            return type.constructor.declarationDescriptor is TupleClassDescriptor
        }

        fun isNothingOrNullableNothing(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, nothingUFqName)
        }

        fun isArray(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, arrayUFqName)
        }

//        fun isBooleanOrNullableBoolean(type: CangJieType): Boolean {
//            return isConstructedFromGivenClass(type, bool)
//        }
//
//        fun isRuneOrNullableChar(type: CangJieType): Boolean {
//            return isConstructedFromGivenClass(type, rune)
//        }

        fun isDefaultBound(type: CangJieType): Boolean {
            return isAny(type)
        }

        fun isPrimitiveType(type: CangJieType): Boolean {
            val descriptor: ClassifierDescriptor? =
                type.constructor.declarationDescriptor
            return descriptor is ClassDescriptor && isPrimitiveClass(
                descriptor
            )
        }

        @JvmStatic
        fun isSpecialClassWithNoSupertypes(descriptor: ClassDescriptor): Boolean {
            return classFqNameEquals(
                descriptor,
                anyUFqName
            ) || classFqNameEquals(descriptor, nothingUFqName)
        }

        //        fun isNullableAny(type: CangJieType): Boolean {
//            return isAnyOrNullableAny(type) && type.isMarkedOption
//        }
        @JvmStatic
        fun isAny(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, anyUFqName)
        }

        fun isAny(descriptor: ClassDescriptor): Boolean {
            return classFqNameEquals(descriptor, anyUFqName)
        }
//        @JvmStatic
//        fun getEnumEntryType(argument: SimpleType): SimpleType {
//            val projectionType: Variance = Variance.INVARIANT
//            val types =
//                listOf(
//                    TypeProjectionImpl(
//                        projectionType,
//                        argument
//                    )
//                )
//            return simpleNotNullType(TypeAttributes.Empty, getEnum(), types)
//        }

        @JvmStatic
        fun isNothing(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, nothingUFqName)
        }

//        fun isPrimitiveTypeOrNullablePrimitiveType(type: CangJieType): Boolean {
//            val descriptor =
//                type.constructor.getDeclarationDescriptor()
//            return descriptor is ClassDescriptor && isPrimitiveClass(
//                descriptor
//            )
//        }

        fun isPrimitiveClass(descriptor: ClassDescriptor): Boolean {
            return getPrimitiveType(descriptor) != null
        }

        fun getPrimitiveType(descriptor: DeclarationDescriptor): PrimitiveType? {
            return if (StandardNames.FqNames.primitiveTypeShortNames.contains(descriptor.name)
            ) StandardNames.FqNames.fqNameToPrimitiveType.get(DescriptorUtils.getFqName(descriptor))
            else null
        }

        @JvmStatic
        fun isUnit(type: CangJieType): Boolean {
            return isNotNullConstructedFromGivenClass(type, unitUFqName)
        }

//        @JvmStatic
//        fun isNothingOrNullableNothing(type: CangJieType): Boolean {
//            return isConstructedFromGivenClass(type, nothing)
//        }

        fun isInt8(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, int8UFqName)
        }

        fun isFloat16(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, float16UFqName)
        }

        fun isFloat32(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, float32UFqName)
        }

        fun isFloat64(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, float64UFqName)
        }

        fun isIntegral(type: CangJieType): Boolean {
            return isInt8(type) || isInt16(type) || isInt32(type) || isInt64(type)
                    || isUInt8(type) || isUInt16(type) || isUInt32(type) || isUInt64(type)
        }

        fun isInt16(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, int16UFqName)
        }


        fun isInt32(type: CangJieType): Boolean {

            return isConstructedFromGivenClass(type, int32UFqName)
        }

        fun isInt64(type: CangJieType): Boolean {

            return isConstructedFromGivenClass(type, int64UFqName)
        }

        fun isUInt8(type: CangJieType): Boolean {

            return isConstructedFromGivenClass(type, uint8UFqName)
        }

        fun isUInt16(type: CangJieType): Boolean {


            return isConstructedFromGivenClass(type, uint16UFqName)
        }

        fun isUInt32(type: CangJieType): Boolean {

            return isConstructedFromGivenClass(type, uint32UFqName)

        }

        fun isUnsignedNumber(type: CangJieType): Boolean {
            return isUInt8(type)
                    || isUInt16(type)
                    || isUInt32(type)
                    || isUInt64(type)
        }

        @JvmStatic
        fun isUInt64(type: CangJieType): Boolean {

            return isConstructedFromGivenClass(type, StandardNames.FqNames.uint64FqName.toUnsafe())
        }

        @JvmStatic
        fun isFloat(type: CangJieType): Boolean {

            return isFloat16(type)
                    || isFloat32(type)
                    || isFloat64(type)


        }

        @JvmStatic
        fun isIntOrFloat(type: CangJieType): Boolean {
            return isNumber(type) || isFloat(type)
        }

        @JvmStatic
        fun isNumber(type: CangJieType): Boolean {

            return isInt8(type)
                    || isInt16(type)
                    || isInt32(type)
                    || isInt64(type)
                    || isUInt8(type)
                    || isUInt16(type)
                    || isUInt32(type)
                    || isUInt64(type)

        }


    }

    val stdlibTypes get() = projectDescriptor.stdlibTypes

    val defaultBound: SimpleType get() = stdlibTypes.anyType

    //    fun getNumberType(): SimpleType {
//        return number.getDefaultType()
//    }
    fun getTuple(parameterCount: Int): ClassDescriptor {
        return TupleClassDescriptor.create(storageManager, builtInsModule, parameterCount)

    }

    fun getFunction(parameterCount: Int): FunctionClassDescriptor {
        return FunctionClassDescriptor.create(storageManager, builtInsModule, FunctionTypeKind.Function, parameterCount)

    }


    fun geOptionNothingType(): SimpleType {
        return nothingType.makeOptionAsSpecified(true)
    }

    fun isMemberOfAny(descriptor: DeclarationDescriptor): Boolean {
        return descriptor.containingDeclaration === stdlibTypes.any
    }


    private val myBasicClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = BASIC_SCOPE.getContributedClassifier(
            name,
            NoLookupLocation.FROM_BASIC
        )
        if (classifier == null) {
            throw AssertionError("Basic class " + BASIC_PACKAGE_FQ_NAME.child(name) + " is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }

    private val builtInsModuleProvider: NotNullLazyValue<ModuleDescriptorImpl> =
        storageManager.createLazyValue {
            val sdk = CjProjectSdkConfig.getInstance(projectDescriptor.project).getProjectSdk()
            createBuiltInsModuleInternal(isFallback = sdk == null)
        }

    val builtInsModule: ModuleDescriptorImpl
        get() = builtInsModuleProvider()


    val binaryOperatorRules: MutableMap<CjToken, List<BinaryOperatorRule>> = mutableMapOf()

    //    填充规则
    private fun fillBinaryOperatorRules() {

        binaryOperatorRules[CjTokens.PLUS] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.MINUS] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.MUL] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.DIV] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.MULMUL] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),

            BinaryOperatorRule(float64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.PERC] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.GT] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )

        binaryOperatorRules[CjTokens.GTEQ] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.LT] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.LTEQ] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )
    }

    //    匹配规则
    fun matchBinaryOperatorRule(token: CjToken, leftType: CangJieType?, rightType: CangJieType?): BinaryOperatorRule {
        if (leftType == null || rightType == null) {
            return BinaryOperatorRule(leftType, rightType, BinaryOperatorRuleResultType.ERROR)
        }

        if (binaryOperatorRules.isEmpty()) {
            fillBinaryOperatorRules()
        }

        val leftType = if (leftType.constructor is IntersectionTypeConstructor) {
            (leftType.constructor as IntersectionTypeConstructor).getAlternativeType()
        } else {
            leftType
        }
        val rightType = if (rightType.constructor is IntegerLiteralTypeConstructor) {
            (rightType.constructor as IntegerLiteralTypeConstructor).getApproximatedType()
        } else {
            rightType
        }

//        查询规则
        val rule = binaryOperatorRules[token]
            ?: return BinaryOperatorRule(leftType, rightType, BinaryOperatorRuleResultType.ERROR)
//根据类型匹配
        for (r in rule) {
            if (r.leftType == leftType && r.rightType == rightType) {
                return r
            }
        }

        return BinaryOperatorRule(leftType, rightType, BinaryOperatorRuleResultType.ERROR)
    }

    fun getBuiltInClassByFqName(fqName: FqName): ClassDescriptor {
        val descriptor: ClassDescriptor =
            builtInsModule.resolveClassByFqName(
                fqName,
                NoLookupLocation.FROM_BUILTINS
            )
                ?: error("Can't find built-in class $fqName")
        return descriptor
    }


    /**
     * 创建内置类型模块（仅包含基本类型）
     *
     * 该模块只包含 cangjie 包下的基本类型（Int8, Bool, Unit 等）。
     * 标准库类型（std.core, std.sync 等）由单独的 stdlibModule 提供。
     */
    private fun createBuiltInsModuleInternal(isFallback: Boolean): ModuleDescriptorImpl {
        val module = ModuleDescriptorImpl(
            projectDescriptor,
            BUILTINS_MODULE_NAME,
            BUILTINS_MODULE_NAME.asString(), // displayName
            storageManager,

            )
        // 只加载基本类型，不包含标准库
        module.initialize(
            BuiltInsLoader.Instance.createBasicTypesPackageFragmentProvider(
                storageManager,
                module
            )
        )
        module.setDependencies(module)
        return module
    }


    val BASIC_SCOPE get() = builtInsModule.getPackage(BASIC_PACKAGE_FQ_NAME).memberScope
    fun isBooleanOrSubtype(type: CangJieType): Boolean {
        return CangJieTypeChecker.DEFAULT.isSubtypeOf(type, boolType)
    }


    private fun getBasicClassByName(simpleName: Name): ClassDescriptor {
        return myBasicClassesByName.invoke(simpleName)
    }


    fun createBuiltInsClassDescriptor(type: BuiltinsType): BuiltinsClassDescriptor {
        return BuiltinsClassDescriptor(type, storageManager, builtInsModule, this)
    }

    fun createPrimitiveClassDescriptor(type: PrimitiveType): PrimitiveClassDescriptor {
        return PrimitiveClassDescriptor(type, storageManager, builtInsModule, this)
    }

    //    int
    val int64Type get() = getBasicClassByName(StandardNames.INT64).defaultType
    val int32Type get() = getBasicClassByName(StandardNames.INT32).defaultType
    val int16Type get() = getBasicClassByName(StandardNames.INT16).defaultType
    val int8Type get() = getBasicClassByName(StandardNames.INT8).defaultType
    val intNativeType get() = getBasicClassByName(StandardNames.INT_NATIVE).defaultType

    val uint64Type get() = getBasicClassByName(StandardNames.UINT64).defaultType
    val uint32Type get() = getBasicClassByName(StandardNames.UINT32).defaultType
    val uint16Type get() = getBasicClassByName(StandardNames.UINT16).defaultType
    val uint8Type get() = getBasicClassByName(StandardNames.UINT8).defaultType
    val uintNativeType get() = getBasicClassByName(StandardNames.UINT_NATIVE).defaultType


    //    float
    val float64Type get() = getBasicClassByName(StandardNames.FLOAT64).defaultType
    val float32Type get() = getBasicClassByName(StandardNames.FLOAT32).defaultType
    val float16Type get() = getBasicClassByName(StandardNames.FLOAT16).defaultType
    val runeType get() = getBasicClassByName(StandardNames.RUNE).defaultType

    val boolType get() = getBasicClassByName(StandardNames.BOOL).defaultType
    val nothingType get() = getBasicClassByName(StandardNames.NOTHING).defaultType

    val unitType get() = getBasicClassByName(StandardNames.UNIT).defaultType


}

enum class BinaryOperatorRuleResultType {
    LEFT, RIGHT, ERROR
}

data class BinaryOperatorRule(
    val leftType: CangJieType?,
    val rightType: CangJieType?,
    val resultType: BinaryOperatorRuleResultType
)

