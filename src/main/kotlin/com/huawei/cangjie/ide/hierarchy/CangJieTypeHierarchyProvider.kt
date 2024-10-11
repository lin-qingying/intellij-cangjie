package com.huawei.cangjie.ide.hierarchy

import com.huawei.cangjie.ide.base.projectStructure.RootKindFilter
import com.huawei.cangjie.ide.base.projectStructure.matches
import com.huawei.cangjie.ide.stubindex.CangJieClassShortNameIndex
import com.huawei.cangjie.psi.CjConstructor
import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.utils.module
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.ide.hierarchy.HierarchyBrowser
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope

class CangJieTypeHierarchyProvider : AbstractCangJieTypeHierarchyProvider() {
    private fun getTargetByReference(
        project: Project,
        editor: Editor,
        module: Module?
    ): CjTypeStatement? {

        return when (val target =
            TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().allAccepted)) {

            is CjConstructor<*> -> target.getContainingTypeStatement()
            is CjTypeStatement -> target
            is CjNamedFunction -> { // Factory methods
                val functionName = target.name ?: return null
                val returnTypeText = target.typeReference?.text
                if (returnTypeText?.substringAfter(".") != functionName) return null
                CangJieClassShortNameIndex.get(functionName, project, GlobalSearchScope.allScope(project))
                    .singleOrNull()
                    ?: return null

            }

            else -> null
        }
    }

    private fun getTargetByContainingElement(editor: Editor, file: PsiFile): CjTypeStatement? {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return null
        return element.getNonStrictParentOfType<CjTypeStatement>()

    }

    override fun getTarget(dataContext: DataContext): PsiElement? {
        val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return null

        val editor = PlatformDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
            if (!RootKindFilter.projectAndLibrarySources.matches(file)) return null
            val psiElement =
                getTargetByReference(project, editor, file.module) ?: getTargetByContainingElement(editor, file)
            if (psiElement is PsiNamedElement && psiElement.name == null) {
                return null
            }
            return psiElement
        }

        val element = LangDataKeys.PSI_ELEMENT.getData(dataContext)
        if (element is CjTypeStatement) return element

        return null
    }



}
