package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.resolve.BindingContext

interface CangJieCodeAnalyzer: TopLevelDescriptorProvider {

   val bindingContext: BindingContext

   fun resolveToDescriptor(declaration: CjDeclaration): DeclarationDescriptor
//   val fileScopeProvider: FileScopeProvider
}