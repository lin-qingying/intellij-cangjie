package com.linqingying.cangjie.types.error

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.DefaultBuiltIns

object ErrorModuleDescriptor: ModuleDescriptor {
    override val isValid: Boolean = false
    override fun getPackage(fqName: FqName): PackageViewDescriptor  = throw IllegalStateException("Should not be called!")
    override val builtIns: CangJieBuiltIns  by lazy { DefaultBuiltIns }
    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptyList()
    override val expectedByModules: List<ModuleDescriptor> = emptyList()

    override fun assertValid() = throw InvalidModuleException("ERROR_MODULE is not a valid module")
    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean =  false
    override fun shouldProtectedsOf(targetModule: ModuleDescriptor): Boolean  = false
    override fun <T> getCapability(capability: ModuleCapability<T>): T? = null

    override val original: DeclarationDescriptor = this
    override val containingDeclaration: DeclarationDescriptor? = null
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R?  = null

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

    }
    override val stableName: Name = Name.special(ErrorEntity.ERROR_MODULE.debugText)

    override val annotations: Annotations
        get() = Annotations.EMPTY
    override val name: Name = stableName
}
