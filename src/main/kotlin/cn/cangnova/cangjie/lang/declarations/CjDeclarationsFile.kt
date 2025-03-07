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

package cn.cangnova.cangjie.lang.declarations

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import cn.cangnova.cangjie.metadata.decompiler.CangJieDecompiledFileViewProvider
import cn.cangnova.cangjie.metadata.decompiler.DecompiledText
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.utils.LockedClearableLazyValue

class CjDeclarationsFile(
    private val provider: FileViewProvider,

    ) : CjFile(
    provider

) {
    override fun toString(): String {
        return "CangJieDeclaration File: $name"
    }

    override fun getFileType(): FileType {
        return CangJieDeclarationsFileType
    }
}

open class CjDecompiledFile(
    private val provider: CangJieDecompiledFileViewProvider,
    buildDecompiledText: (VirtualFile) -> DecompiledText
) : CjFile(provider, true) {

    private val decompiledText = LockedClearableLazyValue(Any()) {
        buildDecompiledText(provider.virtualFile)
    }

    override fun getText(): String? {
        return decompiledText.get().text
    }

    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }

}
