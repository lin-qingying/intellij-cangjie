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

package cn.cangnova.cangjie.ide.formatter

import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.psi.PsiFile

class CangJieFormattingService : AsyncDocumentFormattingService() {
    override fun getFeatures(): MutableSet<FormattingService.Feature> = mutableSetOf()

    override fun canFormat(file: PsiFile): Boolean {


//        val cjfmt = file.project.toolchain?.cjfmt()
//
//        val virtualFile = file.virtualFile
//
////     保存文件到磁盘
////        FileDocumentManager.getInstance().saveDocument(file.viewProvider.document)
//        saveAllDocuments()
//
//        if (cjfmt != null) {
//            val output = cjfmt.formatFile(file.virtualFile)
//
//
//            if (output?.exitCode == 0){
////                virtualFile.refresh(false, true);
////                VirtualFileManager.getInstance().syncRefresh();
//
//                val document = FileDocumentManager.getInstance().getCachedDocument(virtualFile)
//                if (document != null && file.modificationStamp != document.modificationStamp
//                ) {
//                    FileDocumentManager.getInstance().reloadFromDisk(document)
//                }
//
//            }
////            val virtualFileListener = object : VirtualFileListener {
////                override fun contentsChanged(event: VirtualFileEvent) {
////                    if (event.file == virtualFile) {
////                        // Automatically refresh the virtual file
////                        virtualFile.refresh(true, true)
////
////
////                    }
////                }
////            }
//
//            // Register the listener
//
//            // Register the listener with LocalFileSystem
////            LocalFileSystem.getInstance().addVirtualFileListener(virtualFileListener)
//        }

        return true


    }

    override fun createFormattingTask(p0: AsyncFormattingRequest): FormattingTask? {
        return null
    }

    override fun getNotificationGroupId(): String {
        return "CangJie fmt"
    }

    override fun getName(): String {
        return "CangJie fmt"
    }

    class CangJieFormattingTask : FormattingTask {
        override fun run() {
            TODO("Not yet implemented")
        }

        override fun cancel(): Boolean = true

    }
}

