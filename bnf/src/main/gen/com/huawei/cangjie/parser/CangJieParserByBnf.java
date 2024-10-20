// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;
import static com.huawei.cangjie.CjNodeTypes.*;
import static com.huawei.cangjie.lexer.CjTokens.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class CangJieParserByBnf implements PsiParser, LightPsiParser {

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
    return CJ_FILE(b, l + 1);
  }

  /* ********************************************************** */
  // MAIN_FUNC*
  static boolean CJ_FILE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CJ_FILE")) return false;
    while (true) {
      int c = current_position_(b);
      if (!MAIN_FUNC(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "CJ_FILE", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // MAIN_KEYWORD VALUE_PARAMETER_LIST
  public static boolean MAIN_FUNC(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MAIN_FUNC")) return false;
    if (!nextTokenIs(b, MAIN_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MAIN_KEYWORD);
    r = r && VALUE_PARAMETER_LIST(b, l + 1);
    exit_section_(b, m, MAIN_FUNC, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean VALUE_PARAMETER(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, VALUE_PARAMETER, r);
    return r;
  }

  /* ********************************************************** */
  // LPAR  VALUE_PARAMETER (COMMA VALUE_PARAMETER)* RPAR
  public static boolean VALUE_PARAMETER_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_LIST")) return false;
    if (!nextTokenIs(b, LPAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAR);
    r = r && VALUE_PARAMETER(b, l + 1);
    r = r && VALUE_PARAMETER_LIST_2(b, l + 1);
    r = r && consumeToken(b, RPAR);
    exit_section_(b, m, VALUE_PARAMETER_LIST, r);
    return r;
  }

  // (COMMA VALUE_PARAMETER)*
  private static boolean VALUE_PARAMETER_LIST_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_LIST_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!VALUE_PARAMETER_LIST_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "VALUE_PARAMETER_LIST_2", c)) break;
    }
    return true;
  }

  // COMMA VALUE_PARAMETER
  private static boolean VALUE_PARAMETER_LIST_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_LIST_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && VALUE_PARAMETER(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

}
