// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.huawei.cangjie.lang.core.psi.CjElementTypes.*;
import static com.huawei.cangjie.lang.core.parser.CangJieParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;
import static com.huawei.cangjie.lang.core.parser.CangJieParserUtil.PathParsingMode.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class CangJieParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    boolean r;
    if (t == STATEMENT_CODE_FRAGMENT_ELEMENT) {
      r = StatementCodeFragmentElement(b, l + 1);
    }
    else if (t == TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT) {
      r = TypeReferenceCodeFragmentElement(b, l + 1);
    }
    else {
      r = CangJie(b, l + 1);
    }
    return r;
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(A_EXPR, EXPR),
  };

  /* ********************************************************** */
  // 'abcdefg'
  public static boolean AExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _UPPER_, A_EXPR, "<a expr>");
    r = consumeToken(b, "abcdefg");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // STRING_LITERAL | BYTE_STRING_LITERAL | CSTRING_LITERAL
  //                       | RAW_STRING_LITERAL | RAW_BYTE_STRING_LITERAL | RAW_CSTRING_LITERAL
  //                       | CHAR_LITERAL | BYTE_LITERAL
  //                       | FLOAT_LITERAL | <<parseFloatLiteral>> | INTEGER_LITERAL
  //                       | BOOL_LITERAL
  static boolean AnyLitToken(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AnyLitToken")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, STRING_LITERAL);
    if (!r) r = consumeTokenFast(b, BYTE_STRING_LITERAL);
    if (!r) r = consumeTokenFast(b, CSTRING_LITERAL);
    if (!r) r = consumeTokenFast(b, RAW_STRING_LITERAL);
    if (!r) r = consumeTokenFast(b, RAW_BYTE_STRING_LITERAL);
    if (!r) r = consumeTokenFast(b, RAW_CSTRING_LITERAL);
    if (!r) r = consumeTokenFast(b, CHAR_LITERAL);
    if (!r) r = consumeTokenFast(b, BYTE_LITERAL);
    if (!r) r = consumeTokenFast(b, FLOAT_LITERAL);
    if (!r) r = parseFloatLiteral(b, l + 1);
    if (!r) r = consumeTokenFast(b, INTEGER_LITERAL);
    if (!r) r = consumeTokenFast(b, BOOL_LITERAL);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // STRING_LITERAL | BYTE_STRING_LITERAL | CSTRING_LITERAL
  //                             | RAW_STRING_LITERAL | RAW_BYTE_STRING_LITERAL | RAW_CSTRING_LITERAL
  //                             | CHAR_LITERAL | BYTE_LITERAL
  //                             | FLOAT_LITERAL | INTEGER_LITERAL
  //                             | BOOL_LITERAL
  static boolean AnyLitToken_first(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AnyLitToken_first")) return false;
    boolean r;
    r = consumeToken(b, STRING_LITERAL);
    if (!r) r = consumeToken(b, BYTE_STRING_LITERAL);
    if (!r) r = consumeToken(b, CSTRING_LITERAL);
    if (!r) r = consumeToken(b, RAW_STRING_LITERAL);
    if (!r) r = consumeToken(b, RAW_BYTE_STRING_LITERAL);
    if (!r) r = consumeToken(b, RAW_CSTRING_LITERAL);
    if (!r) r = consumeToken(b, CHAR_LITERAL);
    if (!r) r = consumeToken(b, BYTE_LITERAL);
    if (!r) r = consumeToken(b, FLOAT_LITERAL);
    if (!r) r = consumeToken(b, INTEGER_LITERAL);
    if (!r) r = consumeToken(b, BOOL_LITERAL);
    return r;
  }

  /* ********************************************************** */
  // ':' TypeReference
  static boolean ByType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ByType")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, ":");
    p = r; // pin = 1
    r = r && TypeReference(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // RootItems
  static boolean CangJie(PsiBuilder b, int l) {
    return RootItems(b, l + 1);
  }

  /* ********************************************************** */
  // '=' ExprStmt
  public static boolean DefaultParameterValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefaultParameterValue")) return false;
    if (!nextTokenIs(b, EQ)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DEFAULT_PARAMETER_VALUE, null);
    r = consumeToken(b, EQ);
    p = r; // pin = 1
    r = r && ExprStmt(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // AExpr
  public static boolean Expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, EXPR, "<expr>");
    r = AExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Expr ';'?
  static boolean ExprStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExprStmt")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Expr(b, l + 1);
    r = r && ExprStmt_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean ExprStmt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExprStmt_1")) return false;
    consumeToken(b, ";");
    return true;
  }

  /* ********************************************************** */
  // '{'( Function | ExprStmt)*  '}'
  public static boolean FuncBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncBlock")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, BLOCK, null);
    r = consumeToken(b, LBRACE);
    p = r; // pin = 1
    r = r && report_error_(b, FuncBlock_1(b, l + 1));
    r = p && consumeToken(b, RBRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ( Function | ExprStmt)*
  private static boolean FuncBlock_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncBlock_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!FuncBlock_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "FuncBlock_1", c)) break;
    }
    return true;
  }

  // Function | ExprStmt
  private static boolean FuncBlock_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncBlock_1_0")) return false;
    boolean r;
    r = Function(b, l + 1);
    if (!r) r = ExprStmt(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // {'b' ':' } TypeReference DefaultParameterValue?
  public static boolean FuncParameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VALUE_PARAMETER, "<func parameter>");
    r = FuncParameter_0(b, l + 1);
    r = r && TypeReference(b, l + 1);
    r = r && FuncParameter_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // 'b' ':'
  private static boolean FuncParameter_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "b");
    r = r && consumeToken(b, ":");
    exit_section_(b, m, null, r);
    return r;
  }

  // DefaultParameterValue?
  private static boolean FuncParameter_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_2")) return false;
    DefaultParameterValue(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // !(Pat_first  |   ')' | '{' | ';')
  static boolean FuncParameter_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !FuncParameter_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // Pat_first  |   ')' | '{' | ';'
  private static boolean FuncParameter_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_recover_0")) return false;
    boolean r;
    r = Pat_first(b, l + 1);
    if (!r) r = consumeToken(b, RPAREN);
    if (!r) r = consumeToken(b, LBRACE);
    if (!r) r = consumeToken(b, ";");
    return r;
  }

  /* ********************************************************** */
  // !(')' | '{' | ';') FuncParameter (',' | &')')
  static boolean FuncParameter_with_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_with_recover")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = FuncParameter_with_recover_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, FuncParameter(b, l + 1));
    r = p && FuncParameter_with_recover_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, CangJieParser::FuncParameter_recover);
    return r || p;
  }

  // !(')' | '{' | ';')
  private static boolean FuncParameter_with_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_with_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !FuncParameter_with_recover_0_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ')' | '{' | ';'
  private static boolean FuncParameter_with_recover_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_with_recover_0_0")) return false;
    boolean r;
    r = consumeToken(b, RPAREN);
    if (!r) r = consumeToken(b, LBRACE);
    if (!r) r = consumeToken(b, ";");
    return r;
  }

  // ',' | &')'
  private static boolean FuncParameter_with_recover_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_with_recover_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ",");
    if (!r) r = FuncParameter_with_recover_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &')'
  private static boolean FuncParameter_with_recover_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameter_with_recover_2_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RPAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '(' !','  FuncParameter_with_recover*    ')'
  public static boolean FuncParameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameters")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, VALUE_PARAMETER_LIST, null);
    r = consumeToken(b, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, FuncParameters_1(b, l + 1));
    r = p && report_error_(b, FuncParameters_2(b, l + 1)) && r;
    r = p && consumeToken(b, RPAREN) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !','
  private static boolean FuncParameters_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameters_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, ",");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // FuncParameter_with_recover*
  private static boolean FuncParameters_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameters_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!FuncParameter_with_recover(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "FuncParameters_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // unsafe? FUNC identifier FuncParameters  ByType?    ShallowBlock ';'?
  public static boolean Function(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function")) return false;
    if (!nextTokenIs(b, "<function>", FUNC, UNSAFE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _UPPER_, FUNCTION, "<function>");
    r = Function_0(b, l + 1);
    r = r && consumeTokens(b, 2, FUNC, IDENTIFIER);
    p = r; // pin = identifier
    r = r && report_error_(b, FuncParameters(b, l + 1));
    r = p && report_error_(b, Function_4(b, l + 1)) && r;
    r = p && report_error_(b, ShallowBlock(b, l + 1)) && r;
    r = p && Function_6(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // unsafe?
  private static boolean Function_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function_0")) return false;
    consumeToken(b, UNSAFE);
    return true;
  }

  // ByType?
  private static boolean Function_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function_4")) return false;
    ByType(b, l + 1);
    return true;
  }

  // ';'?
  private static boolean Function_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function_6")) return false;
    consumeToken(b, ";");
    return true;
  }

  /* ********************************************************** */
  // MainFunc | Function
  public static boolean Item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ITEM, "<item>");
    r = MainFunc(b, l + 1);
    if (!r) r = Function(b, l + 1);
    register_hook_(b, LEFT_BINDER, ADJACENT_LINE_COMMENTS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Item ';'?
  static boolean ItemStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ItemStmt")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Item(b, l + 1);
    r = r && ItemStmt_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean ItemStmt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ItemStmt_1")) return false;
    consumeToken(b, ";");
    return true;
  }

  /* ********************************************************** */
  // FUNC |   identifier
  static boolean Item_first(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Item_first")) return false;
    if (!nextTokenIs(b, "", FUNC, IDENTIFIER)) return false;
    boolean r;
    r = consumeToken(b, FUNC);
    if (!r) r = consumeToken(b, IDENTIFIER);
    return r;
  }

  /* ********************************************************** */
  // QUOTE_IDENTIFIER ':'
  public static boolean LabelDecl(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LabelDecl")) return false;
    if (!nextTokenIs(b, QUOTE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, QUOTE_IDENTIFIER);
    r = r && consumeToken(b, ":");
    exit_section_(b, m, LABEL_DECL, r);
    return r;
  }

  /* ********************************************************** */
  // QUOTE_IDENTIFIER
  public static boolean Lifetime(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Lifetime")) return false;
    if (!nextTokenIs(b, QUOTE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, QUOTE_IDENTIFIER);
    exit_section_(b, m, LIFETIME, r);
    return r;
  }

  /* ********************************************************** */
  // ':' Lifetime ('+' Lifetime)*
  public static boolean LifetimeParamBounds(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LifetimeParamBounds")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, LIFETIME_PARAM_BOUNDS, "<lifetime param bounds>");
    r = consumeToken(b, ":");
    p = r; // pin = 1
    r = r && report_error_(b, Lifetime(b, l + 1));
    r = p && LifetimeParamBounds_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ('+' Lifetime)*
  private static boolean LifetimeParamBounds_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LifetimeParamBounds_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!LifetimeParamBounds_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "LifetimeParamBounds_2", c)) break;
    }
    return true;
  }

  // '+' Lifetime
  private static boolean LifetimeParamBounds_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LifetimeParamBounds_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, "+");
    r = r && Lifetime(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // QUOTE_IDENTIFIER LifetimeParamBounds?
  public static boolean LifetimeParameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LifetimeParameter")) return false;
    if (!nextTokenIs(b, QUOTE_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, QUOTE_IDENTIFIER);
    r = r && LifetimeParameter_1(b, l + 1);
    exit_section_(b, m, LIFETIME_PARAMETER, r);
    return r;
  }

  // LifetimeParamBounds?
  private static boolean LifetimeParameter_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LifetimeParameter_1")) return false;
    LifetimeParamBounds(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // MAIN '(' MainFuncParam? ')' ByType? ShallowBlock ';'?
  public static boolean MainFunc(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MainFunc")) return false;
    if (!nextTokenIs(b, MAIN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, MAIN_FUNC, null);
    r = consumeTokens(b, 1, MAIN, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, MainFunc_2(b, l + 1));
    r = p && report_error_(b, consumeToken(b, RPAREN)) && r;
    r = p && report_error_(b, MainFunc_4(b, l + 1)) && r;
    r = p && report_error_(b, ShallowBlock(b, l + 1)) && r;
    r = p && MainFunc_6(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // MainFuncParam?
  private static boolean MainFunc_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MainFunc_2")) return false;
    MainFuncParam(b, l + 1);
    return true;
  }

  // ByType?
  private static boolean MainFunc_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MainFunc_4")) return false;
    ByType(b, l + 1);
    return true;
  }

  // ';'?
  private static boolean MainFunc_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MainFunc_6")) return false;
    consumeToken(b, ";");
    return true;
  }

  /* ********************************************************** */
  // 'Array<String>'
  public static boolean MainFuncParam(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MainFuncParam")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MAIN_FUNC_PARAM, "<main func param>");
    r = consumeToken(b, "Array<String>");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // identifier  ByType
  public static boolean ParamStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParamStmt")) return false;
    if (!nextTokenIs(b, "<param>", IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PARAM_STMT, "<param>");
    r = consumeToken(b, IDENTIFIER);
    p = r; // pin = 1
    r = r && ByType(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '&' | '(' | '[' | Path_first | AnyLitToken_first
  static boolean Pat_first(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pat_first")) return false;
    boolean r;
    r = consumeToken(b, AND);
    if (!r) r = consumeToken(b, LPAREN);
    if (!r) r = consumeToken(b, LBRACK);
    if (!r) r = Path_first(b, l + 1);
    if (!r) r = AnyLitToken_first(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // identifier  | super
  static boolean Path_first(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Path_first")) return false;
    if (!nextTokenIs(b, "", IDENTIFIER, SUPER)) return false;
    boolean r;
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, SUPER);
    return r;
  }

  /* ********************************************************** */
  // !Item_first
  static boolean RootItem_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RootItem_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !Item_first(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !<<eof>> Item
  static boolean RootItem_with_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RootItem_with_recover")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = RootItem_with_recover_0(b, l + 1);
    p = r; // pin = 1
    r = r && Item(b, l + 1);
    exit_section_(b, l, m, r, p, CangJieParser::RootItem_recover);
    return r || p;
  }

  // !<<eof>>
  private static boolean RootItem_with_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RootItem_with_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !eof(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // RootItem_with_recover*
  static boolean RootItems(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RootItems")) return false;
    while (true) {
      int c = current_position_(b);
      if (!RootItem_with_recover(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "RootItems", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // <<parseCodeBlockLazy>>
  static boolean ShallowBlock(PsiBuilder b, int l) {
    return parseCodeBlockLazy(b, l + 1);
  }

  /* ********************************************************** */
  // Stmt?
  public static boolean StatementCodeFragmentElement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StatementCodeFragmentElement")) return false;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT_CODE_FRAGMENT_ELEMENT, "<statement code fragment element>");
    Stmt(b, l + 1);
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // ExprStmt | Item
  public static boolean Stmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Stmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STMT, "<stmt>");
    r = ExprStmt(b, l + 1);
    if (!r) r = Item(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // 'a'
  public static boolean TypeReference(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeReference")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_REFERENCE, "<type>");
    r = consumeToken(b, "a");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // TypeReference?
  public static boolean TypeReferenceCodeFragmentElement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeReferenceCodeFragmentElement")) return false;
    Marker m = enter_section_(b, l, _NONE_, TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT, "<type reference code fragment element>");
    TypeReference(b, l + 1);
    exit_section_(b, l, m, true, false, null);
    return true;
  }

}
