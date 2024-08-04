package com.linqingying.cangjie.doc.lexer;

import com.linqingying.cangjie.doc.parser.CDocLinkParser;
import com.linqingying.cangjie.doc.parser.CDocParser;
import com.linqingying.cangjie.doc.psi.impl.CDocImpl;
import com.linqingying.cangjie.lang.CangJieLanguage;
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
