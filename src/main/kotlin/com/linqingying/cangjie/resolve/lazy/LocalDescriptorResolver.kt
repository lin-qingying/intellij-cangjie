package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.psi.CjDeclaration


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
