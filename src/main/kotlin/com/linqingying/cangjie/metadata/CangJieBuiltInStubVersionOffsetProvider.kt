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

package com.linqingying.cangjie.metadata

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.metadata.decompiler.BuiltInsVirtualFileProvider


interface CangJieBuiltInStubVersionOffsetProvider {
    fun getVersionOffset(): Int

    companion object {
        fun getVersionOffset(): Int =
            ApplicationManager.getApplication().getService(CangJieBuiltInStubVersionOffsetProvider::class.java)
                ?.getVersionOffset() ?: 0
    }
}

/**
 * Applies no changes to the K1 IDE stub version and adds a big constant offset to the K2 IDE stub version for .kotlin_builtins files.
 * It should be practically impossible to get a big enough stub version with K1 for it to clash with the K2 version range.
 * See the comment in [CangJieBuiltInStubVersionOffsetProvider] for the reasons why the offset is needed.
 */
internal class IdeCangJieBuiltInStubVersionOffsetProvider : CangJieBuiltInStubVersionOffsetProvider {
    override fun getVersionOffset(): Int {
        return 0
    }
}


interface CangJieBuiltInDecompilationInterceptor {
    fun readFile(bytes: ByteArray, file: VirtualFile): CangJieMetadataStubBuilder.FileWithMetadata?

    companion object {
        fun readFile(
            project: Project,
            bytes: ByteArray,
            file: VirtualFile
        ): CangJieMetadataStubBuilder.FileWithMetadata? =
            project.service<CangJieBuiltInDecompilationInterceptor>().readFile(bytes, file)
//            ApplicationManager.getApplication().getService(CangJieBuiltInDecompilationInterceptor::class.java)
//                ?.readFile(bytes, file)
    }
}


internal class IdeCangJieBuiltInDecompilationInterceptor(val project: Project) :
    CangJieBuiltInDecompilationInterceptor {
    override fun readFile(bytes: ByteArray, file: VirtualFile): CangJieMetadataStubBuilder.FileWithMetadata? {
        if (file in BuiltInsVirtualFileProvider.getInstance(project).getBuiltinVirtualFiles())

            return BuiltInDefinitionFile.read(  bytes, file, filterOutClassesExistingAsClassFiles = false)
        else return null
    }
}
