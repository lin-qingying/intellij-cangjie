package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.UserDataProperty
import com.linqingying.cangjie.resolve.recordScope
import com.linqingying.cangjie.resolve.scopes.ImportingScope
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.storage.StorageManager
import com.intellij.openapi.util.Key

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
