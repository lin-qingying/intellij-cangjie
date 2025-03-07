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

package cn.cangnova.cangjie.ide.refactoring.move

import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
fun CangJieMoveTarget.getTargetModule(project: Project) = targetFileOrDir?.let { ModuleUtilCore.findModuleForFile(it, project) }


sealed interface CangJieMoveTarget{
    val targetFileOrDir: VirtualFile?

    val targetContainerFqName: FqName?
    fun getTargetPsiIfExists(originalPsi: PsiElement): CjElement?
    fun getOrCreateTargetPsi(originalPsi: PsiElement): CjElement
    object Empty : CangJieMoveTarget {
        override val targetContainerFqName: FqName? = null

        override val targetFileOrDir: VirtualFile? = null

        override fun getOrCreateTargetPsi(originalPsi: PsiElement): CjElement = throw UnsupportedOperationException()

        override fun getTargetPsiIfExists(originalPsi: PsiElement): CjElement? = null
    }

    class Directory(targetPackageFqName: FqName, override val targetFileOrDir: VirtualFile) : CangJieMoveTarget {
        override val targetContainerFqName = targetPackageFqName

        override fun getOrCreateTargetPsi(originalPsi: PsiElement): CjFile {
            val file = originalPsi.containingFile ?: error("PSI element in not contained in any file: $originalPsi")
            return file as CjFile
        }

        override fun getTargetPsiIfExists(originalPsi: PsiElement): CjElement? = null
    }
}
