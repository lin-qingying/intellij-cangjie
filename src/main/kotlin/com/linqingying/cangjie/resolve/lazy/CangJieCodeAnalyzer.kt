package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentProvider
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.resolve.BindingContext

interface CangJieCodeAnalyzer: TopLevelDescriptorProvider {

   val bindingContext: BindingContext

   fun getClassDescriptor(
      typeStatement: CjTypeStatement,
      location:LookupLocation
   ): ClassDescriptor


   fun resolveToDescriptor(declaration: CjDeclaration): DeclarationDescriptor
   fun getPackageFragmentProvider(): PackageFragmentProvider
//   val fileScopeProvider: FileScopeProvider
}
