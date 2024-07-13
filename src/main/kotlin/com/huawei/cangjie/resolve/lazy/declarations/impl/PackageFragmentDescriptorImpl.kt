package com.huawei.cangjie.resolve.lazy.declarations.impl

import com.huawei.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.huawei.cangjie.builtins.StandardNames.FqNames.unit
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.DeclarationDescriptorNonRootImpl
//import com.huawei.cangjie.descriptors.impl.basic.BasicDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.utils.Printer


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
        return visitor.visitPackageFragmentDescriptor(this, data)

    }

    override fun getSource(): SourceElement {
        return SourceElement.NO_SOURCE
    }

    override fun toString(): String = debugString
}


//fun craetePackageFragmentDescriptor(
//    storageManager: StorageManager,
//    module: ModuleDescriptor,
//    fqName: FqName
//): AbstractPackageFragmentDescriptorBuiltlnImpl {
//
//    return when (fqName) {
//        BUILT_INS_PACKAGE_FQ_NAME -> PackageFragmentDescriptorBasicImpl(storageManager, module, fqName)
//        else ->
//            TODO("Not yet implemented")
//
//    }
//}

abstract class AbstractPackageFragmentDescriptorBuiltlnImpl(
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName)
//
//class PackageFragmentDescriptorBasicImpl(
//    storageManager: StorageManager,
//    module: ModuleDescriptor,
//    fqName: FqName
//) : AbstractPackageFragmentDescriptorBuiltlnImpl(module, fqName) {
//    val basicMemberScope = BasicMemberScope()
//
//    val UNIT_DESCRIPTOR = BasicDescriptor(basicMemberScope,storageManager, unit.shortName())
//
//    val DESCRIPTOR_MAP = mapOf(
//        unit.shortName() to UNIT_DESCRIPTOR
//    )
//
//
//    inner class BasicMemberScope : MemberScope {
//        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor> {
//            TODO("Not yet implemented")
//        }
//
//        override fun getFunctionNames(): Set<Name> {
//            TODO("Not yet implemented")
//        }
//
//        override fun getVariableNames(): Set<Name> {
//            TODO("Not yet implemented")
//        }
//
//        override fun getClassifierNames(): Set<Name>? {
//            TODO("Not yet implemented")
//        }
//
//        override fun getContributedFunctions(
//            name: Name,
//            location: LookupLocation
//        ): Collection<SimpleFunctionDescriptor> {
//            TODO("Not yet implemented")
//        }
//
//        override fun printScopeStructure(p: Printer) {
//            p.println("Basic member scope")
//        }
//
//        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor {
////            TODO("Not yet implemented")
//            return DESCRIPTOR_MAP.get(name)!!
//        }
//
//        override fun getContributedDescriptors(
//            kindFilter: DescriptorKindFilter,
//            nameFilter: (Name) -> Boolean
//        ): Collection<DeclarationDescriptor> {
//            TODO("Not yet implemented")
//        }
//
//    }
//
//    override fun getMemberScope(): MemberScope {
//        return basicMemberScope
//    }
//}
