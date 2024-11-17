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
