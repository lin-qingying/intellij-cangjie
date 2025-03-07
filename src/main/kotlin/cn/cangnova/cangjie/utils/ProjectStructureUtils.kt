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

package cn.cangnova.cangjie.utils

import cn.cangnova.cangjie.psi.CjFile
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
