package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.UserDataProperty
import com.huawei.cangjie.resolve.recordScope
import com.huawei.cangjie.resolve.scopes.ImportingScope
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.storage.StorageManager
import com.intellij.openapi.util.Key
data class FileScopes(val lexicalScope: LexicalScope, val importingScope: ImportingScope, val importForceResolver: ImportForceResolver)

@DefaultImplementation(FileScopeProviderImpl::class)
interface FileScopeProvider {
    fun getFileResolutionScope(file: CjFile): LexicalScope = getFileScopes(file).lexicalScope
    fun getImportResolver(file: CjFile): ImportForceResolver = getFileScopes(file).importForceResolver

    fun getFileScopes(file: CjFile): FileScopes

    object ThrowException : FileScopeProvider {
        override fun getFileScopes(file: CjFile) = throw UnsupportedOperationException("Should not be called")
    }
}

class FileScopeProviderImpl(
    private val fileScopeFactory: FileScopeFactory,
    private val bindingTrace: BindingTrace,
    private val storageManager: StorageManager
) : FileScopeProvider {

    private val cache = storageManager.createMemoizedFunction<CjFile, FileScopes> { file ->
        val scopes = (file.originalFile as CjFile?)?.fileScopesCustomizer?.createFileScopes(fileScopeFactory)
            ?: fileScopeFactory.createScopesForFile(file)

        bindingTrace.recordScope(scopes.lexicalScope, file)
        scopes
    }

    override fun getFileScopes(file: CjFile) = cache(file)
}

interface FileScopesCustomizer {
    fun createFileScopes(fileScopeFactory: FileScopeFactory): FileScopes
}

var CjFile.fileScopesCustomizer: FileScopesCustomizer? by UserDataProperty(Key.create("FILE_SCOPES_CUSTOMIZER"))
