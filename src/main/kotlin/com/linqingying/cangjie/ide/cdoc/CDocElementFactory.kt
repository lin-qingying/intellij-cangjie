package com.linqingying.cangjie.ide.cdoc

import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.doc.psi.impl.CDocName
import com.linqingying.cangjie.psi.CjFunction
import com.linqingying.cangjie.psi.CjPsiFactory
import com.linqingying.cangjie.psi.psiUtil.getChildOfType
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
