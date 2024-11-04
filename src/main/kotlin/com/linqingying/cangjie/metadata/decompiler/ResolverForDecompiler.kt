package com.linqingying.cangjie.metadata.decompiler

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName

interface ResolverForDecompiler {
    fun resolveTopLevelClass(classId: ClassId): ClassDescriptor?

    fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor>
}
