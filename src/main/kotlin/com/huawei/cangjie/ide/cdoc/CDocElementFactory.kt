package com.huawei.cangjie.ide.cdoc

import com.huawei.cangjie.doc.psi.CDoc
import com.huawei.cangjie.doc.psi.impl.CDocName
import com.huawei.cangjie.psi.CjFunction
import com.huawei.cangjie.psi.CjPsiFactory
import com.huawei.cangjie.psi.psiUtil.getChildOfType
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil

class CDocElementFactory(val project: Project) {
    fun createCDocFromText(text: String): CDoc {
        val fileText = "$text fun foo { }"
        val function = CjPsiFactory(project).createDeclaration<CjFunction>(fileText)
        return PsiTreeUtil.findChildOfType(function, CDoc::class.java)!!
    }

    fun createNameFromText(text: String): CDocName {
        val kdocText = "/** @param $text foo*/"
        val kdoc = createCDocFromText(kdocText)
        val section = kdoc.getDefaultSection()
        val tag = section.findTagByName("param")
        val link = tag?.getSubjectLink()
            ?: throw IllegalArgumentException("Cannot find subject link in doc comment '$kdocText'")
        return link.getChildOfType()!!
    }
}
