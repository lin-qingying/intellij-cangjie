package com.huawei.cangjie.doc.psi

import com.huawei.cangjie.doc.parser.CDocKnownTag
import com.huawei.cangjie.doc.psi.impl.CDocSection
import com.huawei.cangjie.psi.CjDeclaration
import com.intellij.psi.PsiDocCommentBase

interface CDoc: PsiDocCommentBase, CDocElement {
    override fun getOwner(): CjDeclaration?
    fun getDefaultSection(): CDocSection
    fun getAllSections(): List<CDocSection>
    fun findSectionByName(name: String): CDocSection?
    fun findSectionByTag(tag: CDocKnownTag): CDocSection?
    fun findSectionByTag(tag: CDocKnownTag, subjectName: String): CDocSection?
}
