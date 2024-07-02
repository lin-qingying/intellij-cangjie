package com.huawei.cangjie.types.error

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name

object ErrorModuleDescriptor: ModuleDescriptor {
    override val isValid: Boolean
        get() = TODO("Not yet implemented")

    override fun getPackage(fqName: FqName): PackageViewDescriptor {
        TODO("Not yet implemented")
    }

    override val builtIns: CangJieBuiltIns
        get() = TODO("Not yet implemented")

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        TODO("Not yet implemented")
    }

    override fun assertValid() {
        TODO("Not yet implemented")
    }

    override fun <T> getCapability(capability: ModuleCapability<T>): T? {
        TODO("Not yet implemented")
    }

    override val original: DeclarationDescriptor
        get() = TODO("Not yet implemented")
    override val containingDeclaration: DeclarationDescriptor?
        get() = TODO("Not yet implemented")

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        TODO("Not yet implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        TODO("Not yet implemented")
    }

    override val annotations: Annotations
        get() = TODO("Not yet implemented")
    override val name: Name
        get() = TODO("Not yet implemented")
}