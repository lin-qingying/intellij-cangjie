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
    return Item(b, l + 1);
  }

  /* ********************************************************** */
  // '{'   (Item )* '}'
  public static boolean Block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, BLOCK, null);
    r = consumeToken(b, LBRACE);
    p = r; // pin = 1
    r = r && report_error_(b, Block_1(b, l + 1));
    r = p && consumeToken(b, RBRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (Item )*
  private static boolean Block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!Block_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Block_1", c)) break;
    }
    return true;
  }

  // (Item )
  private static boolean Block_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Block_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Item(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // unsafe? FUNC identifier   ReturnType? (ShallowBlock)
  public static boolean Function(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function")) return false;
    if (!nextTokenIs(b, "", FUNC, UNSAFE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION, null);
    r = Function_0(b, l + 1);
    r = r && consumeTokens(b, 2, FUNC, IDENTIFIER);
    p = r; // pin = identifier
    r = r && report_error_(b, Function_3(b, l + 1));
    r = p && Function_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // unsafe?
  private static boolean Function_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function_0")) return false;
    consumeToken(b, UNSAFE);
    return true;
  }

  // ReturnType?
  private static boolean Function_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function_3")) return false;
    ReturnType(b, l + 1);
    return true;
  }

  // (ShallowBlock)
  private static boolean Function_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Function_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ShallowBlock(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ( Function )
  static boolean Item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Item")) return false;
    if (!nextTokenIs(b, "<item>", FUNC, UNSAFE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<item>");
    r = Function(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ':' 'a'
  public static boolean ReturnType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnType")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, RETURN_TYPE, "<return type>");
    r = consumeToken(b, ":");
    p = r; // pin = 1
    r = r && consumeToken(b, "a");
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<parseCodeBlockLazy>>
  static boolean ShallowBlock(PsiBuilder b, int l) {
    return parseCodeBlockLazy(b, l + 1);
  }

  /* ********************************************************** */
  // '{'  '}'
  public static boolean SimpleBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleBlock")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, BLOCK, null);
    r = consumeTokens(b, 1, LBRACE, RBRACE);
    p = r; // pin = 1
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

}
