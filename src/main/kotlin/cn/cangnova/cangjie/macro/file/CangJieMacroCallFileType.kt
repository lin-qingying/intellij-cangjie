/*
 * Copyright 2025 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.macro.file

import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import javax.swing.Icon

class CjMacroCallFile(
    private val provider: FileViewProvider,

    ) : CjFile(
    provider

) {
    override fun toString(): String {
        return "CjMacroCallFile File: $name"
    }

    override fun getFileType(): FileType {
        return CangJieMacroCallFileType
    }
}

object CangJieMacroCallFileType : CangJieFileType() {
    val EXTENSION: String = "macrocall"

    override fun getDisplayName(): String {
        return EXTENSION
    }

    override fun getName() = EXTENSION

    override fun getDescription(): String = DEFAULT_DESCRIPTION

    override fun getDefaultExtension() = EXTENSION

    override fun getIcon(): Icon = CangJieIcons.CANGJIE_FILE

//    override fun isBinary() = false

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private val DEFAULT_DESCRIPTION = "CangJie Macro Call"
}