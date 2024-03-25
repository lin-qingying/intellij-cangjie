package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.psi.CjFile

//@DefaultImplementation(FileScopeProviderImpl::class)
//interface FileScopeProvider {
//    fun getFileResolutionScope(file: CjFile): LexicalScope = getFileScopes(file).lexicalScope
//    fun getImportResolver(file: CjFile): ImportForceResolver = getFileScopes(file).importForceResolver
//
//    fun getFileScopes(file: CjFile): FileScopes
//
//    object ThrowException : FileScopeProvider {
//        override fun getFileScopes(file: CjFile) = throw UnsupportedOperationException("Should not be called")
//    }
//}