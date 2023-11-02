package com.huawei.cangjie.highlighter

import com.huawei.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import com.huawei.cangjie.psi.CjFile
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
import org.jetbrains.annotations.ApiStatus


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
        val EP_NAME = ExtensionPointName.create<BeforeResolveHighlightingExtension>("com.huawei.cangjie.beforeResolveHighlightingVisitor")
    }
}

@ApiStatus.Internal
interface BeforeResolveHighlightingExtension {
    fun createVisitor(holder: HighlightInfoHolder): AbstractHighlightingVisitor
}
