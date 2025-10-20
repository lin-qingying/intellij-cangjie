package org.cangnova.cangjie.lexer.cdoc.lexer;
import com.intellij.lexer.FlexLexer;
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import java.lang.Character;
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag;

%%

%unicode
%class _CDocLexer
%implements FlexLexer

%{
  public _CDocLexer() {
    this((java.io.Reader)null);
  }

  private boolean isLastToken() {
    return zzMarkedPos == zzBuffer.length();
  }

  private boolean yytextContainLineBreaks() {
    return CharArrayUtil.containLineBreaks(zzBuffer, zzStartRead, zzMarkedPos);
  }

  private boolean nextIsNotWhitespace() {
    return zzMarkedPos <= zzBuffer.length() && !Character.isWhitespace(zzBuffer.charAt(zzMarkedPos + 1));
  }

  private boolean prevIsNotWhitespace() {
    return zzMarkedPos != 0 && !Character.isWhitespace(zzBuffer.charAt(zzMarkedPos - 1));
  }
%}

%function advance
%type IElementType
%eof{
  return;
%eof}

%state LINE_BEGINNING
%state CONTENTS_BEGINNING
%state TAG_BEGINNING
%state TAG_TEXT_BEGINNING
%state CONTENTS
%state CODE_BLOCK
%state CODE_BLOCK_LINE_BEGINNING
%state CODE_BLOCK_CONTENTS_BEGINNING
%state INDENTED_CODE_BLOCK

WHITE_SPACE_CHAR    =[\ \t\f\n]
NOT_WHITE_SPACE_CHAR=[^\ \t\f\n]

DIGIT=[0-9]
ALPHA=[:jletter:]
TAG_NAME={ALPHA}({ALPHA}|{DIGIT})*
IDENTIFIER={ALPHA}({ALPHA}|{DIGIT}|".")*
QUALIFIED_NAME_START={ALPHA}
QUALIFIED_NAME_CHAR={ALPHA}|{DIGIT}|[\.]
QUALIFIED_NAME={QUALIFIED_NAME_START}{QUALIFIED_NAME_CHAR}*
CODE_LINK=\[{QUALIFIED_NAME}\]
CODE_FENCE_START=("```" | "~~~").*
CODE_FENCE_END=("```" | "~~~")

%%


<YYINITIAL> "/**"                         { yybegin(CONTENTS_BEGINNING);
                                            return CDocTokens.START;            }
"*"+ "/"                                  { if (isLastToken()) return CDocTokens.END;
                                            else return CDocTokens.TEXT; }

<LINE_BEGINNING> "*"+                     { yybegin(CONTENTS_BEGINNING);
                                            return CDocTokens.LEADING_ASTERISK; }

<CONTENTS_BEGINNING> "@"{TAG_NAME} {
    CDocKnownTag tag = CDocKnownTag.Companion.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos));
    yybegin(tag != null && tag.isReferenceRequired() ? TAG_BEGINNING : TAG_TEXT_BEGINNING);
    return CDocTokens.TAG_NAME;
}

<TAG_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
        }
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { yybegin(TAG_TEXT_BEGINNING);
                  return CDocTokens.MARKDOWN_LINK; }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {QUALIFIED_NAME} {
        yybegin(TAG_TEXT_BEGINNING);
        return CDocTokens.MARKDOWN_LINK;
    }

    [^\n] {
        yybegin(CONTENTS);
        return CDocTokens.TEXT;
    }
}

<TAG_TEXT_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
        }
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { yybegin(CONTENTS);
                  return CDocTokens.MARKDOWN_LINK; }

    [^\n] {
        yybegin(CONTENTS);
        return CDocTokens.TEXT;
    }
}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {

    ([\ ]{4}[\ ]*)|([\t]+) {
        if(yystate() == CONTENTS_BEGINNING) {
            yybegin(INDENTED_CODE_BLOCK);
            return CDocTokens.CODE_BLOCK_TEXT;
        }
    }

    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
            return TokenType.WHITE_SPACE;
        }  else {
            yybegin(yystate() == CONTENTS_BEGINNING ? CONTENTS_BEGINNING : CONTENTS);
            return CDocTokens.TEXT;  // internal white space
        }
    }

    "\\"[\[\]] {
        yybegin(CONTENTS);
        return CDocTokens.MARKDOWN_ESCAPED_CHAR;
    }

    "[" [^\[]* "](" [^)]* ")" {
        yybegin(CONTENTS);
        return CDocTokens.MARKDOWN_INLINE_LINK;
    }

    {CODE_FENCE_START} {
        yybegin(CODE_BLOCK_LINE_BEGINNING);
        return CDocTokens.TEXT;
    }


    {CODE_LINK} / [^\(\[] {
        yybegin(CONTENTS);
        return CDocTokens.MARKDOWN_LINK;
    }

    [^\n] {
        yybegin(CONTENTS);
        return CDocTokens.TEXT;
    }
}

<CODE_BLOCK_LINE_BEGINNING> {
    "*"+ {
        yybegin(CODE_BLOCK_CONTENTS_BEGINNING);
        return CDocTokens.LEADING_ASTERISK;
    }
}

<CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING> {
    {CODE_FENCE_END} / [ \t\f]* [\n] [ \t\f]* {

        yybegin(CONTENTS);
        return CDocTokens.TEXT;
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(yystate() == INDENTED_CODE_BLOCK ? LINE_BEGINNING : CODE_BLOCK_LINE_BEGINNING);
            return TokenType.WHITE_SPACE;
        }
        return CDocTokens.CODE_BLOCK_TEXT;
    }

    [^\n] {
        yybegin(yystate() == INDENTED_CODE_BLOCK ? INDENTED_CODE_BLOCK : CODE_BLOCK);
        return CDocTokens.CODE_BLOCK_TEXT;
    }
}

[\s\S] { return TokenType.BAD_CHARACTER; }
