package com.huawei.cangjie.resolve.lazy.declarations.impl

import com.huawei.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.huawei.cangjie.builtins.StandardNames.FqNames.bool
import com.huawei.cangjie.builtins.StandardNames.FqNames.char
import com.huawei.cangjie.builtins.StandardNames.FqNames.float16
import com.huawei.cangjie.builtins.StandardNames.FqNames.float32
import com.huawei.cangjie.builtins.StandardNames.FqNames.float64
import com.huawei.cangjie.builtins.StandardNames.FqNames.int16
import com.huawei.cangjie.builtins.StandardNames.FqNames.int32
import com.huawei.cangjie.builtins.StandardNames.FqNames.int64
import com.huawei.cangjie.builtins.StandardNames.FqNames.int8
import com.huawei.cangjie.builtins.StandardNames.FqNames.int_native
import com.huawei.cangjie.builtins.StandardNames.FqNames.nothing
import com.huawei.cangjie.builtins.StandardNames.FqNames.uint16
import com.huawei.cangjie.builtins.StandardNames.FqNames.uint32
import com.huawei.cangjie.builtins.StandardNames.FqNames.uint64
import com.huawei.cangjie.builtins.StandardNames.FqNames.uint8
import com.huawei.cangjie.builtins.StandardNames.FqNames.uint_native
import com.huawei.cangjie.builtins.StandardNames.FqNames.unit
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.DeclarationDescriptorNonRootImpl
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.utils.Printer
import kotlin.reflect.full.memberProperties


class ReexportPackageFragment(
    module: ModuleDescriptor,
    fqName: FqName,
    private val memberScope:MemberScope
) : PackageFragmentDescriptorImpl(module, fqName) {
    override fun getMemberScope(): MemberScope {
       return memberScope
    }

}

abstract class PackageFragmentDescriptorImpl(
    module: ModuleDescriptor,
    final override val fqName: FqName
) : DeclarationDescriptorNonRootImpl(module, Annotations.EMPTY, fqName.shortNameOrSpecial(), SourceElement.NO_SOURCE),
    PackageFragmentDescriptor {
    // Not inlined in order to not capture ref on 'module'
    private val debugString: String = "package $fqName of $module"

//    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
//
//    override fun getContainingDeclaration(): ModuleDescriptor {
//        return super.getContainingDeclaration() as ModuleDescriptor
//    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        return visitor.visitPackageFragmentDescriptor(this, data!!)

    }

    override fun getSource(): SourceElement {
        return SourceElement.NO_SOURCE
    }

    override fun toString(): String = debugString
}

fun craeteBasicTypePackageFragmentDescriptor(
    storageManager: StorageManager,
    module: ModuleDescriptor,

    ): PackageFragmentDescriptorBasicImpl {
    return PackageFragmentDescriptorBasicImpl(storageManager, module, BUILT_INS_PACKAGE_FQ_NAME)
}

fun craetePackageFragmentDescriptor(
    storageManager: StorageManager,
    module: ModuleDescriptor,
    fqName: FqName
): AbstractPackageFragmentDescriptorBuiltlnImpl {

    return when (fqName) {


        BUILT_INS_PACKAGE_FQ_NAME -> PackageFragmentDescriptorBasicImpl(storageManager, module, fqName)
        else ->
            TODO("Not yet implemented")

    }
}

abstract class AbstractPackageFragmentDescriptorBuiltlnImpl(
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName)

//
class PackageFragmentDescriptorBasicImpl(
    val storageManager: StorageManager,
    val module: ModuleDescriptor,
    fqName: FqName
) : AbstractPackageFragmentDescriptorBuiltlnImpl(module, fqName) {

    val basicMemberScope = BasicMemberScope()
    private fun createBasicTypeDescriptor(name: FqNameUnsafe): BasicTypeDescriptor =
        BasicTypeDescriptor.create(basicMemberScope, storageManager, name.shortName())


    //Unit
    val UNIT_DESCRIPTOR = createBasicTypeDescriptor(unit)

    //    Int
    val INT8_DESCRIPTOR = createBasicTypeDescriptor(int8)
    val INT16_DESCRIPTOR = createBasicTypeDescriptor(int16)
    val INT32_DESCRIPTOR = createBasicTypeDescriptor(int32)
    val INT64_DESCRIPTOR = createBasicTypeDescriptor(int64)
    val INTNATIVE_DESCRIPTOR = createBasicTypeDescriptor(int_native)

    //    UInt
    val UINT8_DESCRIPTOR = createBasicTypeDescriptor(uint8)
    val UINT16_DESCRIPTOR = createBasicTypeDescriptor(uint16)
    val UINT32_DESCRIPTOR = createBasicTypeDescriptor(uint32)
    val UINT64_DESCRIPTOR = createBasicTypeDescriptor(uint64)
    val UINTNATIVE_DESCRIPTOR = createBasicTypeDescriptor(uint_native)

    //    Char
    val CHAR_DESCRIPTOR = createBasicTypeDescriptor(char)

    //    Bool
    val BOOL_DESCRIPTOR = createBasicTypeDescriptor(bool)

    //Nothing
    val NOTHING_DESCRIPTOR = createBasicTypeDescriptor(nothing)

    //    Float
    val FLOAT16_DESCRIPTOR = createBasicTypeDescriptor(float16)
    val FLOAT32_DESCRIPTOR = createBasicTypeDescriptor(float32)
    val FLOAT64_DESCRIPTOR = createBasicTypeDescriptor(float64)


    val DESCRIPTOR_MAP = mutableMapOf<Name, BasicTypeDescriptor>(

    )

    init {
//        反射赋值
        val fields = this::class.memberProperties
        fields.forEach { field ->

            val descriptor = field.call(this)
            if (descriptor is BasicTypeDescriptor) {
                DESCRIPTOR_MAP[descriptor.name] = descriptor

            }


        }

    }


    inner class BasicMemberScope : MemberScope {
        override fun getContributedVariables(
            name: Name,
            location: LookupLocation
        ): Collection<@JvmWildcard VariableDescriptor> = emptyList()

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
            emptyList()

        override fun getFunctionNames(): Set<Name> = emptySet()

        override fun getVariableNames(): Set<Name> = emptySet()
        override fun getPropertyNames(): Set<Name> = emptySet()

        override fun getClassifierNames(): Set<Name> = DESCRIPTOR_MAP.map {
            it.key
        }.toSet()


        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> = emptyList()

        override fun printScopeStructure(p: Printer) {
            p.println("Basic member scope")
        }

        override fun  getContributedClassifier(name: Name, location: LookupLocation): BasicTypeDescriptor? {

            return DESCRIPTOR_MAP[name]
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<BasicTypeDescriptor> = emptyList()

    }

    override fun getMemberScope(): MemberScope {
        return basicMemberScope
    }
}
