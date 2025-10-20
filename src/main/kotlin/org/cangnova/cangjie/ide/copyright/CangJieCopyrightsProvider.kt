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

package org.cangnova.cangjie.ide.copyright

import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens.SHEBANG_COMMENT
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.util.containers.TreeTraversal
import com.maddyhome.idea.copyright.CopyrightProfile
import com.maddyhome.idea.copyright.psi.UpdateCopyright
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider
import com.maddyhome.idea.copyright.psi.UpdatePsiFileCopyright


class CangJieCopyrightsProvider : UpdateCopyrightsProvider() {
    public override fun createInstance(
        project: Project?,
        module: Module?,
        file: VirtualFile?,
        base: FileType?,
        options: CopyrightProfile?
    ): UpdateCopyright? {
        return CangJieCopyright(project, module, file, options)
    }
}

internal class CangJieCopyright(
    project: Project?,
    module: Module?,
    root: VirtualFile?,
    copyrightProfile: CopyrightProfile?
) :
    UpdatePsiFileCopyright(project, module, root, copyrightProfile) {

    override fun accept(): Boolean =
        file.fileType === CangJieFileType.INSTANCE

    override fun scanFile() {
        val comments = file.getExistingComments()
        val anchor =
            if (file.firstChild?.isShebangComment() == true) file.firstChild?.nextSibling else comments.lastOrNull()
        checkComments(/* last = */ anchor, /* commentHere = */ true, comments)
    }
}


private fun PsiFile.getExistingComments(): List<PsiComment> =
    SyntaxTraverser.psiTraverser(/* root = */ this)
        .withTraversal(TreeTraversal.LEAVES_DFS)
        .traverse()
        .skipWhile { element: PsiElement -> element.isShebangComment() }
        .takeWhile { element: PsiElement ->
            (element is PsiComment && element.getParent() !is CjDeclaration) ||
                    element is PsiWhiteSpace ||
                    element.text.isEmpty() ||
                    element.parent is CDoc
        }
        .map { element: PsiElement -> if (element.parent is CDoc) element.parent else element }
        .filterIsInstance<PsiComment>()
        .toList()

private fun PsiElement.isShebangComment(): Boolean =
    this is PsiComment && tokenType === SHEBANG_COMMENT
