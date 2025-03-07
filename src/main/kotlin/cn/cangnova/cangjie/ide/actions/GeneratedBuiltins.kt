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

package cn.cangnova.cangjie.ide.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.ide.projectStructure.languageVersionSettings
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.resolve.caches.descriptor
import cn.cangnova.cangjie.serialization.DescriptorSerializer
import cn.cangnova.cangjie.serialization.MetadataSerializerExtension
import cn.cangnova.cangjie.serialization.builtins.BuiltInsSerializer
import java.io.File

class GeneratedBuiltins : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // 获取当前上下文中的虚拟文件
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(virtualFile.name)

        val cjFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        if (cjFile !is CjFile) return



        BuiltInsSerializer.analyzeAndSerialize(
           File( cjFile.virtualFile.path).resolve("..").resolve("out"),
            listOf( File( cjFile.virtualFile.path)),
            extraClassPath = listOf(),

            dependOnOldBuiltIns = true,
            project = cjFile.project,
        ) { totalSize, totalFiles ->
            if (System.getProperty("cangjie.builtins.serializer.log") == "true") {
                println("Total bytes written: $totalSize to $totalFiles files")
            }
        }

    }
}
