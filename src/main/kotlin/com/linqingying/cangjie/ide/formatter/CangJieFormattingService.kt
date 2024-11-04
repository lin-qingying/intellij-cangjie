package com.linqingying.cangjie.ide.formatter

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

