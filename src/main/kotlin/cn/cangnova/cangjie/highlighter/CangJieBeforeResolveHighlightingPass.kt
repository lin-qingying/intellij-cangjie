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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.codeHighlighting.*
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor



class CangJieBeforeResolveHighlightingPass(file: CjFile, document: Document) : AbstractHighlightingPassBase(file, document) {
    override fun runAnnotatorWithContext(element: PsiElement, holder: HighlightInfoHolder) {
        val visitor = BeforeResolveHighlightingVisitor(holder)
        val extensions = EP_NAME.extensionList.map { it.createVisitor(holder) }

        element.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.accept(visitor)
                extensions.forEach(element::accept)
                super.visitElement(element)
            }
        })
    }

    class Factory : TextEditorHighlightingPassFactory, DumbAware {
        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            if (file !is CjFile) return null
            return CangJieBeforeResolveHighlightingPass(file, editor.document)
        }
    }

    class Registrar : TextEditorHighlightingPassFactoryRegistrar {
        override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
            registrar.registerTextEditorHighlightingPass(
                Factory(),
               TextEditorHighlightingPassRegistrar.Anchor.BEFORE,
              Pass.UPDATE_FOLDING,
                false,
                 false
            )
        }
    }

    companion object {
        val EP_NAME = ExtensionPointName.create<BeforeResolveHighlightingExtension>("cn.cangnova.cangjie.beforeResolveHighlightingVisitor")
    }
}


interface BeforeResolveHighlightingExtension {
    fun createVisitor(holder: HighlightInfoHolder): AbstractHighlightingVisitor
}
