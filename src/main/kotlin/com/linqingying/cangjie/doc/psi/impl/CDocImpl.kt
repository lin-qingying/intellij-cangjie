package com.linqingying.cangjie.doc.psi.impl

import com.linqingying.cangjie.doc.lexer.CDocTokens
import com.linqingying.cangjie.doc.parser.CDocKnownTag
import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.psiUtil.getChildOfType
import com.linqingying.cangjie.psi.psiUtil.getChildrenOfType
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.linqingying.cangjie.utils.toLowerCaseAsciiOnly
import com.intellij.lang.Language

import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType

class CDocImpl(buffer: CharSequence?):LazyParseablePsiElement(CDocTokens.CDOC, buffer),CDoc{

    override fun getLanguage(): Language = CangJieLanguage

    override fun toString(): String = node.elementType.toString()

    override fun getTokenType(): IElementType = CjTokens.DOC_COMMENT

    override fun getOwner(): CjDeclaration? = getParentOfType(true)

    override fun getDefaultSection(): CDocSection = getChildOfType()!!

    override fun getAllSections(): List<CDocSection> =
        getChildrenOfType<CDocSection>().toList()

    override fun findSectionByName(name: String): CDocSection? =
        getChildrenOfType<CDocSection>().firstOrNull { it.name == name }

    override fun findSectionByTag(tag: CDocKnownTag): CDocSection? =
        findSectionByName(tag.name.toLowerCaseAsciiOnly())

    override fun findSectionByTag(tag: CDocKnownTag, subjectName: String): CDocSection? =
        getChildrenOfType<CDocSection>().firstOrNull {
            it.name == tag.name.toLowerCaseAsciiOnly() && it.getSubjectName() == subjectName
        }
}
