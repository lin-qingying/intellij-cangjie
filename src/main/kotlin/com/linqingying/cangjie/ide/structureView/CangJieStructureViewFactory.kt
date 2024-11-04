package com.linqingying.cangjie.ide.structureView

import com.linqingying.cangjie.ide.AbstractCangJieIconProvider
import com.linqingying.cangjie.ide.CangJieIconProvider
import com.linqingying.cangjie.psi.CjFile
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.NodeProvider
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile


class CangJieStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is CjFile) {
            return null
        }

        val isSingleClassFile: Boolean = AbstractCangJieIconProvider. isSingleClassFile(psiFile)

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return object : CangJieStructureViewModel(psiFile, editor, CangJieStructureViewElement(psiFile, false)) {

                    override fun getNodeProviders(): List<NodeProvider<*>> {
                        return NODE_PROVIDERS
                    }
                }
            }

            override fun isRootNodeShown(): Boolean {
                return !isSingleClassFile
            }
        }
    }

    companion object {
        private val NODE_PROVIDERS = listOf<NodeProvider<*>>(CangJieInheritedMembersNodeProvider())
    }
}
