package com.huawei.cangjie.utils

import com.huawei.cangjie.psi.CjFile
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.psi.PsiElement
import org.jetbrains.jps.model.ex.JpsElementTypeBase
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRootType


fun PsiElement.isUnderCangJieSourceRootTypes(): Boolean {
    val cjFile = this.containingFile.safeAs<CjFile>() ?: return false
    val file = cjFile.virtualFile?.takeIf { it !is VirtualFileWindow && it.fileSystem !is NonPhysicalFileSystem }
        ?: return false
    val projectFileIndex = ProjectRootManager.getInstance(cjFile.project).fileIndex
    return projectFileIndex.isInTestSourceContent(file)
}

val ALL_CANGJIE_SOURCE_ROOT_TYPES = setOf(SourceCangJieRootType, TestSourceCangJieRootType)

val CANGJIE_AWARE_SOURCE_ROOT_TYPES: Set<JpsModuleSourceRootType<JavaSourceRootProperties>> =
    ALL_CANGJIE_SOURCE_ROOT_TYPES

val CANGJIE_AWARE_SOURCE_AND_RESOURCES_ROOT_TYPES: Set<JpsModuleSourceRootType<*>> =
    CANGJIE_AWARE_SOURCE_ROOT_TYPES + JavaModuleSourceRootTypes.RESOURCES

sealed class CangJieSourceRootType : JpsElementTypeBase<JavaSourceRootProperties>(),
    JpsModuleSourceRootType<JavaSourceRootProperties> {

    override fun createDefaultProperties() = JpsJavaExtensionService.getInstance().createSourceRootProperties("")

}

object SourceCangJieRootType : CangJieSourceRootType()

object TestSourceCangJieRootType : CangJieSourceRootType() {
    override fun isForTests() = true
}
