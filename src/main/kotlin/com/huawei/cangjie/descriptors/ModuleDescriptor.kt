package com.huawei.cangjie.descriptors

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name

class ModuleCapability<T>(val name: String) {
    override fun toString() = name
}

interface ModuleDescriptor : DeclarationDescriptor{
    val isValid: Boolean
    fun getPackage(fqName: FqName): PackageViewDescriptor
    val builtIns: CangJieBuiltIns
    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>
    override val containingDeclaration: DeclarationDescriptor?
        get() = null
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        return visitor.visitModuleDeclaration(this, data)

    }
    fun assertValid()

    fun <T> getCapability(capability: ModuleCapability<T>): T?
}