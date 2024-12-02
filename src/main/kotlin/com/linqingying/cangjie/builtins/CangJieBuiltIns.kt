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

package com.linqingying.cangjie.builtins

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.linqingying.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.linqingying.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_NAME
import com.linqingying.cangjie.builtins.StandardNames.FqNames.anyFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.anyUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.arrayClassFqNameToPrimitiveType
import com.linqingying.cangjie.builtins.StandardNames.FqNames.arrayFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.arrayUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.boolUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.comparableFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.countableFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.equatableFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.exceptionFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.float16UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.float32UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.float64UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.futureFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.int16UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.int32UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.int64UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.int8UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.iterableFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.nothingUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.objectFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.optionFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.optionUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.primitiveArrayTypeShortNames
import com.linqingying.cangjie.builtins.StandardNames.FqNames.rangeFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.reentrantMutexFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.resourceFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.runeUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.stringFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.stringUFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.tokensFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.uint16UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.uint32UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.uint8UFqName
import com.linqingying.cangjie.builtins.StandardNames.FqNames.unitUFqName
import com.linqingying.cangjie.builtins.StandardNames.STD_AST_PACKAGE_FQ_NAME
import com.linqingying.cangjie.builtins.StandardNames.STD_CORE_PACKAGE_FQ_NAME
import com.linqingying.cangjie.builtins.StandardNames.STD_SYNC_PACKAGE_FQ_NAME
import com.linqingying.cangjie.builtins.functions.FunctionTypeKind
import com.linqingying.cangjie.context.ProjectContext
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.FunctionClassDescriptor
import com.linqingying.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.TupleClassDescriptor
import com.linqingying.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.linqingying.cangjie.descriptors.impl.basic.BuiltInTypeDescriptor
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.caches.IdeaResolverForProject
import com.linqingying.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.linqingying.cangjie.resolve.descriptorUtil.resolveClassByFqName
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.NotNullLazyValue
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.storage.getValue
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.classFqNameEquals
import com.linqingying.cangjie.types.util.isConstructedFromGivenClass
import com.linqingying.cangjie.types.util.isNotNullConstructedFromGivenClass
import java.util.*

open class CangJieBuiltIns(
    val project: Project? = null,
    val storageManager: StorageManager,
//    val cangJieModuleInfo: CangJieModuleInfo
//    val moduleInfo: ModuleInfo? = null
) {
    companion object {
        // This function only checks presence of Deprecated annotation at declaration-site, it doesn't take into account @DeprecatedSinceCangJie
        // To check that a referenced descriptor is actually deprecated at call-site, use DeprecationResolver
        fun isDeprecated(declarationDescriptor: DeclarationDescriptor): Boolean {
            if (declarationDescriptor.original.annotations.hasAnnotation(StandardNames.FqNames.deprecated)) return true

            if (declarationDescriptor is PropertyDescriptor) {
                val isVar: Boolean =
                    declarationDescriptor.isVar
                val getter =
                    declarationDescriptor.getter
                val setter =
                    declarationDescriptor.setter
                return getter != null && isDeprecated(getter) && (!isVar || setter != null && isDeprecated(setter))
            }

            return false
        }

        //    内置类型名称
        enum class BuiltCangJieTypeName(private val _typeName: String) {
            CPOINTER("CPointer"),
            CSTRING("CString");

            val typeName: Name
                get() {
                    return Name.identifier(_typeName)

                }
        }

        fun isBooleanOrNullableBoolean(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, boolUFqName)
        }

        fun isUnitOrNullableUnit(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, unitUFqName)
        }

        fun isAnyOrNullableAny(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, anyUFqName)
        }

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

        fun isNullableNothing(type: CangJieType): Boolean {
            return isNothing(type)
                    && TypeUtils.isNullableType(type)
        }

        fun getPrimitiveArrayType(descriptor: DeclarationDescriptor): PrimitiveType? {
            return if (primitiveArrayTypeShortNames.contains(descriptor.name))
                arrayClassFqNameToPrimitiveType.get(DescriptorUtils.getFqName(descriptor))
            else
                null
        }

        fun isPrimitiveArray(type: CangJieType): Boolean {
            val descriptor =
                type.constructor.getDeclarationDescriptor()
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


//        fun isNullableNothing(type: CangJieType): Boolean {
//            return (isNothingOrNullableNothing(type)
//                    && TypeUtils.isNullableType(type))
//        }

//        private fun isConstructedFromGivenClassAndNotNullable(
//            type: CangJieType,
//            fqName: FqNameUnsafe
//        ): Boolean {
//            return isConstructedFromGivenClass(
//                type,
//                fqName
//            ) && !type.isMarkedOption
//        }


    }

    val builtInPackagesImportedByDefault: NotNullLazyValue<List<PackageViewDescriptor>> =
        storageManager.createLazyValue {
            listOf(
                builtInsModule.getPackage(BUILT_INS_PACKAGE_FQ_NAME)

            )

        }


    init {

        createBuiltInsModule(true)
    }

    fun getBuiltInPackagesImportedByDefault(): Collection<PackageViewDescriptor> {
        return builtInPackagesImportedByDefault.invoke()
    }

    val defaultBound: SimpleType get() = anyType

    //    fun getNumberType(): SimpleType {
//        return number.getDefaultType()
//    }
    fun getTuple(parameterCount: Int): ClassDescriptor {
        return TupleClassDescriptor.create(storageManager, builtInsModule, parameterCount)

    }

    fun getFunction(parameterCount: Int): ClassDescriptor {
        return FunctionClassDescriptor.create(storageManager, builtInsModule, FunctionTypeKind.Function, parameterCount)

    }


    private fun getPrimitiveClassBasicDescriptor(type: PrimitiveType): ClassDescriptor {
        return getBuiltInBasicTypeByName(type.typeName.asString())
    }

    fun getArrayType(
        projectionType: Variance,
        argument: CangJieType,
        annotations: Annotations
    ): SimpleType {
        val types =
            listOf(
                TypeProjectionImpl(
                    projectionType,
                    argument
                )
            )
        return CangJieTypeFactory.simpleNotNullType(
            annotations.toDefaultAttributes(),
            array,
            types
        )
    }

    private class Primitives(
        val primitiveTypeToArrayCangJieType: Map<PrimitiveType, SimpleType>,
        val primitiveCangJieTypeToCangJieArrayType: Map<CangJieType, SimpleType>,
        val cangjieArrayTypeToPrimitiveCangJieType: Map<SimpleType, SimpleType>
    )

    private val primitives: NotNullLazyValue<Primitives> = storageManager.createLazyValue {
        val primitiveTypeToArrayCangJieType = EnumMap<PrimitiveType, SimpleType>(PrimitiveType::class.java)
        val primitiveCangJieTypeToCangJieArrayType = HashMap<CangJieType, SimpleType>()
        val cangjieArrayTypeToPrimitiveCangJieType = HashMap<SimpleType, SimpleType>()

        for (primitive in PrimitiveType.entries) {
            val type = getPrimitiveBuiltInCangJieType(primitive.typeName)
            val arrayType = getPrimitiveBuiltInCangJieType(primitive.arrayTypeName)

            primitiveTypeToArrayCangJieType[primitive] = arrayType
            primitiveCangJieTypeToCangJieArrayType[type] = arrayType
            cangjieArrayTypeToPrimitiveCangJieType[arrayType] = type
        }

        Primitives(
            primitiveTypeToArrayCangJieType,
            primitiveCangJieTypeToCangJieArrayType,
            cangjieArrayTypeToPrimitiveCangJieType
        )
    }

    fun getNullableNothingType(): SimpleType {
        return nothingType.makeOptionalAsSpecified(true)
    }

    fun isMemberOfAny(descriptor: DeclarationDescriptor): Boolean {
        return descriptor.containingDeclaration === any
    }

    fun getArrayType(

        argument: CangJieType
    ): SimpleType {
        return getArrayType(
            Variance.INVARIANT,
            argument,
            Annotations.EMPTY
        )
    }

    private fun getPrimitiveClassDescriptor(type: PrimitiveType): ClassDescriptor {
        return getBuiltInClassByName(type.typeName.asString())
    }

    val customizer: CangJieBuiltInsCustomizer by storageManager.createLazyValue {
        CangJieBuiltInsCustomizer(
            builtInsModule, storageManager
        )
    }
    private val myStdSyncBuiltInClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = getStdSyncBuiltInsPackageScope().getContributedClassifier(
            name,
            NoLookupLocation.FROM_BUILTINS
        )
        if (classifier == null) {
            throw AssertionError("Built-in class " + BUILT_INS_PACKAGE_FQ_NAME.child(name) + " is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }
    private val myStdAstBuiltInClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = getStdAstBuiltInsPackageScope().getContributedClassifier(
            name,
            NoLookupLocation.FROM_BUILTINS
        )
        if (classifier == null) {
            throw AssertionError("Built-in class " + BUILT_INS_PACKAGE_FQ_NAME.child(name) + " is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }
    private val myStdCoreBuiltInClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = getStdCoreBuiltInsPackageScope().getContributedClassifier(
            name,
            NoLookupLocation.FROM_BUILTINS
        )
        if (classifier == null) {
            throw AssertionError("Built-in class " + BUILT_INS_PACKAGE_FQ_NAME.child(name) + " is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }
    private val myBuiltInClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = getBuiltInsPackageScope().getContributedClassifier(
            name,
            NoLookupLocation.FROM_BUILTINS
        )
        if (classifier == null) {
            throw AssertionError("Built-in class " + BUILT_INS_PACKAGE_FQ_NAME.child(name) + " is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }


    var sourcesModuleDescriptor: ModuleDescriptor? = null

    var myBuiltInsModule: ModuleDescriptorImpl? = null

    val builtInsModule: ModuleDescriptorImpl
        get() {

            assert(myBuiltInsModule != null || postponedBuiltInsModule != null) { "Uninitialized built-ins module" }
            if (myBuiltInsModule == null) {
                myBuiltInsModule = postponedBuiltInsModule!!.invoke()
            }
            return myBuiltInsModule!!
        }
    private var postponedBuiltInsModule: NotNullLazyValue<ModuleDescriptorImpl>? =
        null

    fun getPrimitiveBasicCangJieType(type: PrimitiveType): SimpleType {
        return getPrimitiveClassBasicDescriptor(type).getDefaultType()
    }

    //    获取内置类型
    fun getPrimitiveBuiltInCangJieType(name: Name): SimpleType {
        return getPrimitiveClassBuiltInDescriptor(name).defaultType

    }

    fun getPrimitiveClassBuiltInDescriptor(type: Name): BuiltInTypeDescriptor {
        return getBuiltInTypeByName(type.asString())
    }

    private fun getBuiltInTypeByName(name: String): BuiltInTypeDescriptor {
        return getBuiltInClassByName(name) as BuiltInTypeDescriptor

    }

    fun getPrimitiveCangJieType(type: PrimitiveType): SimpleType {
        return getPrimitiveClassDescriptor(type).getDefaultType()
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


    val nothingType: SimpleType
        get() = nothing.getDefaultType()


    protected fun createBuiltInsModule(isFallback: Boolean) {
        myBuiltInsModule = ModuleDescriptorImpl(
            project as? ProjectEx,
            BUILTINS_MODULE_NAME, storageManager,
            this,
//            null
            isBuiltInsModule = true
        )
        builtInsModule.initialize(
            BuiltInsLoader.Instance.createPackageFragmentProvider(
                storageManager,
                builtInsModule,
//                                getClassDescriptorFactories(),
//                                getPlatformDependentDeclarationFilter(),
//                                getAdditionalClassPartsProvider(),
                isFallback,
                project
            )
        )
        builtInsModule.setDependencies(builtInsModule)
    }

//    fun getBuiltInsModule(): ModuleDescriptorImpl {
//        assert(builtInsModule != null || postponedBuiltInsModule != null) { "Uninitialized built-ins module" }
//        if (builtInsModule == null) {
//            builtInsModule = postponedBuiltInsModule?.invoke()
//        }
//        return builtInsModule
//    }

    fun setPostponedBuiltinsModuleComputation(computation: () -> ModuleDescriptorImpl) {
        postponedBuiltInsModule = storageManager.createLazyValue(computation)
    }
//    protected open fun getClassDescriptorFactories(): Iterable<ClassDescriptorFactory> {
//        return listOf< ClassDescriptorFactory>(
//           BuiltInFictitiousFunctionClassFactory(
//                storageManager,
//                getBuiltInsModule()
//            )
//        )
//    }
fun getStdSyncBuiltInsPackageScope(): MemberScope {
    return builtInsModule.getPackage(STD_SYNC_PACKAGE_FQ_NAME).memberScope

}
    fun getStdAstBuiltInsPackageScope(): MemberScope {
        return builtInsModule.getPackage(STD_AST_PACKAGE_FQ_NAME).memberScope

    }

    fun getStdCoreBuiltInsPackageScope(): MemberScope {
        return builtInsModule.getPackage(STD_CORE_PACKAGE_FQ_NAME).memberScope

//        return getBuiltInsStdCoreScope()
    }

    fun getBuiltInsPackageScope(): MemberScope {
        return builtInsModule.getPackage(BUILT_INS_PACKAGE_FQ_NAME).memberScope

//        return getBuiltInsStdCoreScope()
    }

    fun isBooleanOrSubtype(type: CangJieType): Boolean {
        return CangJieTypeChecker.DEFAULT.isSubtypeOf(type, boolType)
    }

    private fun getBuiltInClassByName(simpleName: String): ClassDescriptor {
        return myBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }

    private fun getStdCoreClassByName(simpleName: String): ClassDescriptor {
        return myStdCoreBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }
    private fun getStdSyncClassByName(simpleName: String): ClassDescriptor {
        return myStdSyncBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }

    private fun getStdAstClassByName(simpleName: String): ClassDescriptor {
        return myStdAstBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }


    fun getBuiltInBasicTypeByName(simpleName: String): BasicTypeDescriptor {
        return getBuiltInClassByName(simpleName) as BasicTypeDescriptor
    }


    val unit: BasicTypeDescriptor get() = getBuiltInBasicTypeByName("Unit")
    val unitType: BasicType get() = unit.defaultType
    val nothing: ClassDescriptor get() = getBuiltInClassByName("Nothing")

    val resource: ClassDescriptor
        get() {

            return try {
                getStdCoreClassByName("Resource")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, resourceFqName)!!
            }
        }
    val throwable: ClassDescriptor
        get() {
            return try {
                getStdCoreClassByName("Exception")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, exceptionFqName)!!
            }
        }
    val throwableType: CangJieType
        get() = throwable.defaultType
    val `object`: ClassDescriptor
        get() {
            return try {
                getStdCoreClassByName("Object")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, objectFqName)!!
            }

        }
    val objectType: CangJieType
        get() {
            return `object`.defaultType
        }

    //    int
    val int64Type get() = getPrimitiveBasicCangJieType(PrimitiveType.INT64)
    val int32Type get() = getPrimitiveBasicCangJieType(PrimitiveType.INT32)
    val int16Type get() = getPrimitiveBasicCangJieType(PrimitiveType.INT16)
    val int8Type get() = getPrimitiveBasicCangJieType(PrimitiveType.INT8)
    val intNativeType get() = getPrimitiveBasicCangJieType(PrimitiveType.INTNATIVE)

    val uint64Type = getPrimitiveBasicCangJieType(PrimitiveType.UINT64)
    val uint32Type = getPrimitiveBasicCangJieType(PrimitiveType.UINT32)
    val uint16Type = getPrimitiveBasicCangJieType(PrimitiveType.UINT16)
    val uint8Type = getPrimitiveBasicCangJieType(PrimitiveType.UINT8)
    val uintNativeType get() = getPrimitiveBasicCangJieType(PrimitiveType.UINTNATIVE)


    //    float
    val float64Type get() = getPrimitiveBasicCangJieType(PrimitiveType.FLOAT64)
    val float32Type get() = getPrimitiveBasicCangJieType(PrimitiveType.FLOAT32)
    val float16Type get() = getPrimitiveBasicCangJieType(PrimitiveType.FLOAT16)
    val runeType get() = getPrimitiveBasicCangJieType(PrimitiveType.Rune)

    val boolType get() = getPrimitiveBasicCangJieType(PrimitiveType.BOOL)


    //    内置类型
//    这两个内置类型是std.core包中的，如果有必要，需要加上包名，现在没有加
    val CPointerType get() = getPrimitiveBuiltInCangJieType(BuiltCangJieTypeName.CPOINTER.typeName)
    val CStringType get() = getPrimitiveBuiltInCangJieType(BuiltCangJieTypeName.CSTRING.typeName)


    //    标准库
    val any: ClassDescriptor
        get() {
            return try {
                getStdCoreClassByName("Any")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, anyFqName)!!
            }

        }
    val anyType: SimpleType
        get() {
            return any.getDefaultType()
        }
    val countable: ClassDescriptor
        get() = findClassDescriptorByFqName(project ?: storageManager.project, countableFqName)!!
    val countableType: SimpleType
        get() {
            return countable.getDefaultType()
        }
    val equatable: ClassDescriptor
        get() = findClassDescriptorByFqName(project ?: storageManager.project, equatableFqName)!!
    val equatableType: SimpleType
        get() {
            return equatable.getDefaultType()
        }
    val iterable: ClassDescriptor
        get() = findClassDescriptorByFqName(project ?: storageManager.project, iterableFqName)!!
    val iterableType: SimpleType
        get() {
            return iterable.getDefaultType()
        }

    val comparable: ClassDescriptor
        get() = findClassDescriptorByFqName(project ?: storageManager.project, comparableFqName)!!
    val ccomparableType: SimpleType
        get() {
            return comparable.getDefaultType()
        }
    val future: ClassDescriptor
        get() = findClassDescriptorByFqName(project ?: storageManager.project, futureFqName)!!
    val futureType: SimpleType
        get() {
            return future.getDefaultType()
        }
    val range: ClassDescriptor
        get() = findClassDescriptorByFqName(project ?: storageManager.project, rangeFqName)!!
    val rangeType: SimpleType
        get() {
            return range.getDefaultType()
        }
    val string: ClassDescriptor
        get() {


            return try {
                getStdCoreClassByName("String")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, stringFqName)!!

            }
        }
    val array: ClassDescriptor
        get() = findClassDescriptorByFqName(project ?: storageManager.project, arrayFqName)!!
    val arrayType: SimpleType
        get() {
            return array.getDefaultType()
        }
    val stringType: SimpleType
        get() {
            return string.getDefaultType()
        }
    val option: ClassDescriptor
        get() {
            return try {
                getStdCoreClassByName("Option")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, optionFqName)!!
            }

        }
    val optionType: SimpleType
        get() {
            return option.defaultType

        }

    //sync
    val reentrantMutex: ClassDescriptor
        get() {
            return try {
                getStdSyncClassByName("ReentrantMutex")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, reentrantMutexFqName)!!
            }

        }
    val reentrantMutexType: SimpleType
        get() {
            return reentrantMutex.defaultType

        }

    //ast
//ast
    val tokens: ClassDescriptor
        get() {
            return try {
                getStdAstClassByName("Tokens")
            } catch (e: AssertionError) {
                findClassDescriptorByFqName(project ?: storageManager.project, tokensFqName)!!
            }

        }
    val tokensType: SimpleType
        get() {
            return tokens.defaultType

        }

    //    二进制运算规则
    val binaryOperatorRules: MutableMap<CjToken, List<BinaryOperatorRule>> = mutableMapOf()

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


}

enum class BinaryOperatorRuleResultType {
    LEFT, RIGHT, ERROR
}

data class BinaryOperatorRule(
    val leftType: CangJieType?,
    val rightType: CangJieType?,
    val resultType: BinaryOperatorRuleResultType
)

fun createBuiltIns(projectContext: ProjectContext): CangJieBuiltIns {


    return CangJieBuiltIns(projectContext.project, projectContext.storageManager)
}

fun createBuiltIns(projectContext: ProjectContext, resolver: IdeaResolverForProject): CangJieBuiltIns {


    return CangJieBuiltIns(projectContext.project, projectContext.storageManager)
}
