package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjDeclaration


interface LocalDescriptorResolver {
    fun resolveLocalDeclaration(declaration: CjDeclaration): DeclarationDescriptor
}

class CompilerLocalDescriptorResolver(
    private val lazyDeclarationResolver: LazyDeclarationResolver
) : LocalDescriptorResolver {
    override fun resolveLocalDeclaration(declaration: CjDeclaration): DeclarationDescriptor {
        return lazyDeclarationResolver.resolveToDescriptor(declaration)
    }
}