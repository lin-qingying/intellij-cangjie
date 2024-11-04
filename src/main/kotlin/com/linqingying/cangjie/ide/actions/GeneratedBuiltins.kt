package com.linqingying.cangjie.ide.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.builtins.BuiltInsBinaryVersion
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.caches.descriptor
import com.linqingying.cangjie.serialization.DescriptorSerializer
import com.linqingying.cangjie.serialization.MetadataSerializerExtension
import com.linqingying.cangjie.serialization.builtins.BuiltInsSerializer
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
