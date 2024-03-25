package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.resolve.BindingContext

interface CangJieCodeAnalyzer: TopLevelDescriptorProvider {

   val bindingContext: BindingContext

//   val fileScopeProvider: FileScopeProvider
}