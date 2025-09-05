package org.cangnova.cangjie.analysis.decompiler.psi

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

interface ResolverForDecompiler {
//    fun resolveTopLevelClass(classId: ClassId): ClassDescriptor?

    fun resolveAllDeclarationsInPackage(packageFqName: FqName): List<DeclarationDescriptor>
}
