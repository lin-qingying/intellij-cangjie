/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.lexer.cdoc.lexer;

import org.cangnova.cangjie.lexer.cdoc.parser.CDocLinkParser;
import org.cangnova.cangjie.lexer.cdoc.parser.CDocParser;
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocImpl;
import org.cangnova.cangjie.lang.CangJieLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CDocTokens {
    ILazyParseableElementType CDOC = new ILazyParseableElementType("CDOC", CangJieLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            PsiElement parentElement = chameleon.getTreeParent().getPsi();
            Project project = parentElement.getProject();
            PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, new CDocLexer(), getLanguage(),
                    chameleon.getText());
            PsiParser parser = new CDocParser();

            return parser.parse(this, builder).getFirstChildNode();
        }

        @Nullable
        @Override
        public ASTNode createNode(CharSequence text) {
            return new CDocImpl(text);
        }
    };

    int START_Id = 0;
    int END_Id = 1;
    int LEADING_ASTERISK_Id = 2;
    int TEXT_Id = 3;
    int CODE_BLOCK_TEXT_Id = 4;
    int TAG_NAME_Id = 5;
    int MARKDOWN_ESCAPED_CHAR_Id = 6;
    int MARKDOWN_INLINE_LINK_Id = 7;
    CDocToken TAG_NAME              = new CDocToken("CDOC_TAG_NAME", TAG_NAME_Id);
    CDocToken START                 = new CDocToken("CDOC_START", START_Id);
    CDocToken END                   = new CDocToken("CDOC_END", END_Id);
    CDocToken LEADING_ASTERISK      = new CDocToken("CDOC_LEADING_ASTERISK", LEADING_ASTERISK_Id);

    CDocToken TEXT                  = new CDocToken("CDOC_TEXT", TEXT_Id);
    CDocToken CODE_BLOCK_TEXT       = new CDocToken("CDOC_CODE_BLOCK_TEXT", CODE_BLOCK_TEXT_Id);


    ILazyParseableElementType MARKDOWN_LINK = new ILazyParseableElementType("CDOC_MARKDOWN_LINK", CangJieLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(@NotNull ASTNode chameleon) {
            return CDocLinkParser.parseMarkdownLink(this, chameleon);
        }
    };



    CDocToken MARKDOWN_ESCAPED_CHAR = new CDocToken("CDOC_MARKDOWN_ESCAPED_CHAR", MARKDOWN_ESCAPED_CHAR_Id);
    CDocToken MARKDOWN_INLINE_LINK = new CDocToken("CDOC_MARKDOWN_INLINE_LINK", MARKDOWN_INLINE_LINK_Id);
    @SuppressWarnings("unused")
    TokenSet CDOC_HIGHLIGHT_TOKENS = TokenSet.create(START, END, LEADING_ASTERISK, TEXT, CODE_BLOCK_TEXT, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK);
    TokenSet CONTENT_TOKENS = TokenSet.create(TEXT, CODE_BLOCK_TEXT, TAG_NAME, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK);

}
