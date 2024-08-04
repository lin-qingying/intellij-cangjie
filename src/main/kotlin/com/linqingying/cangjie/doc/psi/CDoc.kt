package com.linqingying.cangjie.doc.psi

import com.linqingying.cangjie.doc.parser.CDocKnownTag
import com.linqingying.cangjie.doc.psi.impl.CDocSection
import com.linqingying.cangjie.psi.CjDeclaration
import com.intellij.psi.PsiDocCommentBase

interface CDoc: PsiDocCommentBase, CDocElement {
    override fun getOwner(): CjDeclaration?
    fun getDefaultSection(): CDocSection
    fun getAllSections(): List<CDocSection>
    fun findSectionByName(name: String): CDocSection?
    fun findSectionByTag(tag: CDocKnownTag): CDocSection?
    fun findSectionByTag(tag: CDocKnownTag, subjectName: String): CDocSection?
}
