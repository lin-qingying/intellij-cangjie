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

package org.cangnova.cangjie.quickfix

import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.psi.CjFile

abstract class CangJieQuickFixAction<out T : PsiElement>(element: T) : QuickFixActionBase<T>(element) {
    protected open fun isAvailable(project: Project, editor: Editor?, file: CjFile) = true

    override fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val cjFile = file as? CjFile ?: return false
        return isAvailable(project, editor, cjFile)
    }

    final override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        val element = element ?: return
        if (file is CjFile &&
            (startInWriteAction() && getElementToMakeWritable(file) == file || IntentionPreviewUtils.prepareElementForWrite(element))) {
            invoke(project, editor, file)
        }
    }

    protected abstract operator fun invoke(project: Project, editor: Editor?, file: CjFile)

    override fun startInWriteAction() = true
}
