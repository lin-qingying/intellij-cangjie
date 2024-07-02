package com.huawei.cangjie.builtins

import com.huawei.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.huawei.cangjie.builtins.StandardNames.FqNames.nothing
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.NotNullLazyValue
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.SimpleType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeUtils

open class CangJieBuiltIns(
    val storageManager: StorageManager
) {


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

    companion object {
        val BUILTINS_MODULE_NAME: Name =
            Name.special("<built-ins module>")
        @JvmStatic
        fun isUnit(type: CangJieType): Boolean {
            return isNotNullConstructedFromGivenClass(type, StandardNames.FqNames.unit)
        }
        fun isNothingOrNullableNothing(type: CangJieType): Boolean {
            return  isConstructedFromGivenClass(type, nothing)
        }
        fun isNullableNothing(type: CangJieType): Boolean {
            return ( isNothingOrNullableNothing(type)
                    &&  TypeUtils.isNullableType(type))
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


    val nullableNothingType: SimpleType get() = nothingType.makeNullableAsSpecified(true)
    val nothingType: SimpleType
        get() = nothing.getDefaultType()


    val nullableAnyType: SimpleType get() = anyType.makeNullableAsSpecified(true)

    //    val nothing: ClassDescriptor get() = getBuiltInClassByName("Nothing")
    val nothing: ClassDescriptor get() = getBuiltInClassByName("Unit")

    init {
        createBuiltInsModule(true)
    }

    val anyType: SimpleType
        get() {
            return any.getDefaultType()
        }
    val any = getBuiltInClassByName("Unit")
//    val any = getBuiltInClassByName("Any")

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

    fun getUnitType(): SimpleType {
        return getUnit().getDefaultType()
    }

    fun getBuiltInsPackageScope(): MemberScope {
        return getBuiltInsModule().getPackage(BUILT_INS_PACKAGE_FQ_NAME).memberScope
    }

    private fun getBuiltInClassByName(simpleName: String): ClassDescriptor {
        return myBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }

    fun getUnit(): ClassDescriptor {
        return getBuiltInClassByName("Unit")
    }
}