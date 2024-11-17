/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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
