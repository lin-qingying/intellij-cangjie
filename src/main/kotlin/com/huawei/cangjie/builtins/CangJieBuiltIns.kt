package com.huawei.cangjie.builtins

import com.huawei.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.huawei.cangjie.builtins.StandardNames.FqNames.any
import com.huawei.cangjie.builtins.StandardNames.FqNames.boolean
import com.huawei.cangjie.builtins.StandardNames.FqNames.int16
import com.huawei.cangjie.builtins.StandardNames.FqNames.int32
import com.huawei.cangjie.builtins.StandardNames.FqNames.int64
import com.huawei.cangjie.builtins.StandardNames.FqNames.int8
import com.huawei.cangjie.builtins.StandardNames.FqNames.nothing
import com.huawei.cangjie.builtins.StandardNames.getFunctionName
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.resolveClassByFqName
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.NotNullLazyValue
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.SimpleType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.util.TypeUtils


open class CangJieBuiltIns(
    val storageManager: StorageManager
) {
    companion object {
        val BUILTINS_MODULE_NAME: Name =
            Name.special("<built-ins module>")

        fun isBoolean(type: CangJieType): Boolean {
            return isConstructedFromGivenClassAndNotNullable(
                type,
                boolean
            )
        }

        fun isDefaultBound(type: CangJieType): Boolean {
            return isNullableAny(type)
        }

        fun isPrimitiveType(type: CangJieType): Boolean {
            return !type.isMarkedNullable && isPrimitiveTypeOrNullablePrimitiveType(
                type
            )
        }

        fun isNullableAny(type: CangJieType): Boolean {
            return isAnyOrNullableAny(type) && type.isMarkedNullable
        }

        fun isAnyOrNullableAny(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, any)
        }

        fun isNothing(type: CangJieType): Boolean {
            return (isNothingOrNullableNothing(type)
                    && !TypeUtils.isNullableType(type))
        }

        fun isPrimitiveTypeOrNullablePrimitiveType(type: CangJieType): Boolean {
            val descriptor =
                type.constructor.getDeclarationDescriptor()
            return descriptor is ClassDescriptor && isPrimitiveClass(
                descriptor
            )
        }

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
            return isNotNullConstructedFromGivenClass(type, StandardNames.FqNames.unit)
        }

        fun isNothingOrNullableNothing(type: CangJieType): Boolean {
            return isConstructedFromGivenClass(type, nothing)
        }

        fun isInt8(type: CangJieType): Boolean {
            return isConstructedFromGivenClassAndNotNullable(type, int8)
        }

        fun isInt16(type: CangJieType): Boolean {
            return isConstructedFromGivenClassAndNotNullable(type, int16)
        }


        fun isInt32(type: CangJieType): Boolean {

            return isConstructedFromGivenClassAndNotNullable(type, int32)
        }

        fun isInt64(type: CangJieType): Boolean {

            return isConstructedFromGivenClassAndNotNullable(type, int64)
        }

        fun isUInt8(type: CangJieType): Boolean {

            return isConstructedFromGivenClassAndNotNullable(type, StandardNames.FqNames.uInt8FqName.toUnsafe())
        }

        fun isUInt16(type: CangJieType): Boolean {


            return isConstructedFromGivenClassAndNotNullable(type, StandardNames.FqNames.uInt16FqName.toUnsafe())
        }

        fun isUInt32(type: CangJieType): Boolean {

            return isConstructedFromGivenClassAndNotNullable(type, StandardNames.FqNames.uInt32FqName.toUnsafe())

        }


        fun isUInt64(type: CangJieType): Boolean {

            return isConstructedFromGivenClassAndNotNullable(type, StandardNames.FqNames.uInt64FqName.toUnsafe())
        }

        fun isNullableNothing(type: CangJieType): Boolean {
            return (isNothingOrNullableNothing(type)
                    && TypeUtils.isNullableType(type))
        }

        private fun isConstructedFromGivenClassAndNotNullable(
            type: CangJieType,
            fqName: FqNameUnsafe
        ): Boolean {
            return isConstructedFromGivenClass(
                type,
                fqName
            ) && !type.isMarkedNullable
        }

        private fun isConstructedFromGivenClass(
            type: CangJieType,
            fqName: FqNameUnsafe
        ): Boolean {
            return isTypeConstructorForGivenClass(type.constructor, fqName)
        }

        fun isTypeConstructorForGivenClass(
            typeConstructor: TypeConstructor,
            fqName: FqNameUnsafe
        ): Boolean {
            val descriptor =
                typeConstructor.getDeclarationDescriptor()
            return descriptor is ClassDescriptor && classFqNameEquals(
                descriptor,
                fqName
            )
        }

        private fun classFqNameEquals(
            descriptor: ClassifierDescriptor,
            fqName: FqNameUnsafe
        ): Boolean {
            // Quick check to avoid creation of full FqName instance
            return descriptor.name == fqName.shortName() && fqName == DescriptorUtils.getFqName(
                descriptor
            )
        }

        private fun isNotNullConstructedFromGivenClass(
            type: CangJieType,
            fqName: FqNameUnsafe
        ): Boolean {
            return !type.isMarkedNullable && isConstructedFromGivenClass(
                type,
                fqName
            )
        }


    }

    init {
        createBuiltInsModule(true)
    }


    val defaultBound: SimpleType get() = nullableAnyType

    fun getNumberType(): SimpleType {
        return number.getDefaultType()
    }

    fun getFunction(parameterCount: Int): ClassDescriptor {
        return getBuiltInClassByName(getFunctionName(parameterCount))
    }

    val number: ClassDescriptor get() = getBuiltInClassByName("Number")

    val comparable: ClassDescriptor get() = getBuiltInClassByName("Comparable")


    private fun getPrimitiveClassDescriptor(type: PrimitiveType): ClassDescriptor {
        return getBuiltInClassByName(type.typeName.asString())
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

    private var builtInsModule: ModuleDescriptorImpl? = null
    private var postponedBuiltInsModule: NotNullLazyValue<ModuleDescriptorImpl>? =
        null

    fun getPrimitiveCangJieType(type: PrimitiveType): SimpleType {
        return getPrimitiveClassDescriptor(type).getDefaultType()
    }

    fun getBuiltInClassByFqName(fqName: FqName): ClassDescriptor {
        val descriptor: ClassDescriptor =
            getBuiltInsModule().resolveClassByFqName(
                fqName,
                NoLookupLocation.FROM_BUILTINS
            )
                ?: error("Can't find built-in class $fqName")
        return descriptor
    }


    val nullableNothingType: SimpleType get() = nothingType.makeNullableAsSpecified(true)
    val nothingType: SimpleType
        get() = nothing.getDefaultType()


    val nullableAnyType: SimpleType get() = anyType.makeNullableAsSpecified(true)


    protected fun createBuiltInsModule(isFallback: Boolean) {
        builtInsModule = ModuleDescriptorImpl(
            BUILTINS_MODULE_NAME, storageManager,
            this,
//            null
        )
        builtInsModule?.initialize(
            BuiltInsLoader.Instance.createPackageFragmentProvider(
                storageManager,
                builtInsModule!!,
//                getClassDescriptorFactories(),
//                getPlatformDependentDeclarationFilter(),
//                getAdditionalClassPartsProvider(),
                isFallback
            )
        )
        builtInsModule?.setDependencies(builtInsModule!!)
    }

    fun getBuiltInsModule(): ModuleDescriptorImpl {
        assert(builtInsModule != null || postponedBuiltInsModule != null) { "Uninitialized built-ins module" }
        if (builtInsModule == null) {
            builtInsModule = postponedBuiltInsModule?.invoke()
        }
        return builtInsModule!!
    }

    fun setPostponedBuiltinsModuleComputation(computation: () -> ModuleDescriptorImpl) {
        postponedBuiltInsModule = storageManager.createLazyValue(computation)
    }


    fun getBuiltInsPackageScope(): MemberScope {
        return getBuiltInsModule().getPackage(BUILT_INS_PACKAGE_FQ_NAME).memberScope
    }


    fun getBuiltInsBasicTypeScope(): MemberScope {
        TODO()
    }

    private fun getBuiltInClassByName(simpleName: String): ClassDescriptor {
        return myBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }

    fun getBuiltInBasicTypeByName(simpleName: String): BasicTypeDescriptor {
        return getBuiltInClassByName(simpleName) as BasicTypeDescriptor
    }

    val unit get() = getBuiltInClassByName("Unit")
    val unitType: SimpleType get() = unit.getDefaultType()
    val nothing: ClassDescriptor get() = getBuiltInClassByName("Unit")

    val any get() = getBuiltInClassByName("Unit")
    val anyType: SimpleType
        get() {
            return any.getDefaultType()
        }


    //    int
    val int64Type get() = getPrimitiveCangJieType(PrimitiveType.INT64)
    val int32Type get() = getPrimitiveCangJieType(PrimitiveType.INT32)
    val int16Type get() = getPrimitiveCangJieType(PrimitiveType.INT16)
    val int8Type get() = getPrimitiveCangJieType(PrimitiveType.INT8)

    val uint64Type = getPrimitiveCangJieType(PrimitiveType.UINT64)
    val uint32Type = getPrimitiveCangJieType(PrimitiveType.UINT32)
    val uint16Type = getPrimitiveCangJieType(PrimitiveType.UINT16)
    val uint8Type = getPrimitiveCangJieType(PrimitiveType.UINT8)

}



