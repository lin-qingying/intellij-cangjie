package com.huawei.cangjie.resolve.lazy.declarations.impl

import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.huawei.cangjie.builtins.StandardNames.FqNames.cpointerUFqName
import com.huawei.cangjie.builtins.StandardNames.FqNames.unitUFqName
import com.huawei.cangjie.builtins.StandardNames.FqNames.int8UFqName

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.DeclarationDescriptorNonRootImpl
import com.huawei.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BuiltInTypeDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.utils.Printer
import kotlin.reflect.full.memberProperties


class ReexportPackageFragment(
    module: ModuleDescriptor,
    fqName: FqName,
    private val memberScope: MemberScope
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
    override val containingDeclaration: ModuleDescriptor
        get() = super.containingDeclaration  as ModuleDescriptor
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

    private fun createBuiltInTypeDescriptor(
        name: FqNameUnsafe,
        parameters: List<TypeParameterDescriptor> = emptyList()
    ): BuiltInTypeDescriptor =
        BuiltInTypeDescriptor.create(basicMemberScope, storageManager, name.shortName(), parameters)


    //Unit
    val UNIT_DESCRIPTOR = createBasicTypeDescriptor(unitUFqName)

    //    Int
    val INT8_DESCRIPTOR = createBasicTypeDescriptor(int8UFqName)
    val INT16_DESCRIPTOR = createBasicTypeDescriptor( StandardNames.FqNames.int16UFqName)
    val INT32_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.int32UFqName)
    val INT64_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.int64UFqName)
    val INTNATIVE_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.int_nativeUFqName)

    //    UInt
    val UINT8_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint8UFqName)
    val UINT16_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint16UFqName)
    val UINT32_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint32UFqName)
    val UINT64_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint64UFqName)
    val UINTNATIVE_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.uint_nativeUFqName)

    //    Char
    val RUNE_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.runeUFqName)

    //    Bool
    val BOOL_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.boolUFqName)

    //Nothing
    val NOTHING_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.nothingUFqName)

    //    Float
    val FLOAT16_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.float16UFqName)
    val FLOAT32_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.float32UFqName)
    val FLOAT64_DESCRIPTOR = createBasicTypeDescriptor(StandardNames.FqNames.float64UFqName)

    val CSTRING = createBuiltInTypeDescriptor(StandardNames.FqNames.cstringUFqName, emptyList())
    val CPOINTER = createBuiltInTypeDescriptor(
        cpointerUFqName
    ).apply {
        addParameter(
            TypeParameterDescriptorImpl.createForFurtherModification(
                this, Annotations.EMPTY, Variance.INVARIANT, Name.identifier("T"), 0, SourceElement.NO_SOURCE,
                null, SupertypeLoopChecker.EMPTY, storageManager


            ).apply {


            }
        )
    }


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

        override fun getContributedClassifier(name: Name, location: LookupLocation): BasicTypeDescriptor? {

            return DESCRIPTOR_MAP[name]
        }

        override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
            return emptyList()
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
