/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.macro.file

import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.lang.CangJieMacroCallLanguage
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import javax.swing.Icon

class CjMacroCallFile(
    private val provider: FileViewProvider,

    ) : CjFile(
    provider,
    isCompiled = true
) {
    override fun toString(): String {
        return "CjMacroCallFile File: $name"
    }

    override fun getFileType(): FileType {
        return CangJieMacroCallFileType
    }

    companion object {
        /**
         * 检查 `.macrocall` 文件是否对用户可见
         *
         * 由 Registry Key `cangjie.macro.expansion.file.visible` 控制，
         * 默认为 `false`（不可见）。不可见时：
         * - 从项目视图中隐藏
         * - 从声明分析作用域中排除（避免 REDECLARATION 冲突）
         */
        @JvmStatic
        fun isUserVisible(): Boolean = Registry.`is`("cangjie.macro.expansion.file.visible", false)

        /**
         * 判断给定的 VirtualFile 是否为 `.macrocall` 文件
         */
        @JvmStatic
        fun isMacroCallFile(file: VirtualFile): Boolean = file.name.endsWith(".cj.macrocall")
    }
}

object CangJieMacroCallFileType : LanguageFileType(CangJieMacroCallLanguage) {

    val EXTENSION: String = "cj.macrocall"

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

internal class MacroCallFileTypeFactory : FileTypeFactory() {


    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(
            CangJieMacroCallFileType,
            ExtensionFileNameMatcher("cj.macrocall")
        );
    }
}