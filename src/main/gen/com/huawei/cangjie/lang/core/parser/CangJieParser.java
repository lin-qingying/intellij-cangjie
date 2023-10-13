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

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class CangJieParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    boolean r;
    if (t == MAIN_FUNC_CODE_FRAGMENT_ELEMENT) {
      r = MainFuncCodeFragmentElement(b, l + 1);
    }
    else if (t == STATEMENT_CODE_FRAGMENT_ELEMENT) {
      r = StatementCodeFragmentElement(b, l + 1);
    }
    else {
      r = CangJie(b, l + 1);
    }
    return r;
  }

  /* ********************************************************** */
  // 'aa'
  public static boolean A(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "A")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _UPPER_, A, "<a>");
    r = consumeToken(b, "aa");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ':' Type
  static boolean ByType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ByType")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, ":");
    p = r; // pin = 1
    r = r && Type(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // RootItems
  static boolean CangJie(PsiBuilder b, int l) {
    return RootItems(b, l + 1);
  }

  /* ********************************************************** */
  // '\n'   | '\r\n'
  public static boolean EOL(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EOL")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EOL, "<eol>");
    r = consumeToken(b, "\\n");
    if (!r) r = consumeToken(b, "\\r\\n");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // A
  public static boolean Expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR, "<expr>");
    r = A(b, l + 1);
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
  // '(' !',' ParamStmt*  ')'
  public static boolean FuncParameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameters")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FUNC_PARAMETERS, null);
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

  // ParamStmt*
  private static boolean FuncParameters_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FuncParameters_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ParamStmt(b, l + 1)) break;
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
  // ( Function)
  public static boolean Item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Item")) return false;
    if (!nextTokenIs(b, "<item>", FUNC, UNSAFE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ITEM, "<item>");
    r = Function(b, l + 1);
    register_hook_(b, LEFT_BINDER, ADJACENT_LINE_COMMENTS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Item ';'?
  static boolean ItemStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ItemStmt")) return false;
    if (!nextTokenIs(b, "", FUNC, UNSAFE)) return false;
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
  // MainFunc?
  public static boolean MainFuncCodeFragmentElement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MainFuncCodeFragmentElement")) return false;
    Marker m = enter_section_(b, l, _NONE_, MAIN_FUNC_CODE_FRAGMENT_ELEMENT, "<main func code fragment element>");
    MainFunc(b, l + 1);
    exit_section_(b, l, m, true, false, null);
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
  // !<<eof>> Item
  static boolean RootItem_with_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RootItem_with_recover")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = RootItem_with_recover_0(b, l + 1);
    p = r; // pin = 1
    r = r && Item(b, l + 1);
    exit_section_(b, l, m, r, p, null);
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
  public static boolean Type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Type")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE, "<type>");
    r = consumeToken(b, "a");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
