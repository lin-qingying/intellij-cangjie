// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.grammar.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.huawei.cangjie.grammar.psi.CjElementTypes.*;
import static com.huawei.cangjie.grammar.psi.CangJieParserUtil.*;
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
    return File(b, l + 1);
  }

  /* ********************************************************** */
  // preamble?
  static boolean File(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "File")) return false;
    preamble(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ';'
  public static boolean end(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "end")) return false;
    if (!nextTokenIs(b, SEMI)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMI);
    exit_section_(b, m, END, r);
    return r;
  }

  /* ********************************************************** */
  // AS   identifier
  public static boolean importAlias(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importAlias")) return false;
    if (!nextTokenIs(b, AS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, AS, IDENTIFIER);
    exit_section_(b, m, IMPORT_ALIAS, r);
    return r;
  }

  /* ********************************************************** */
  // (identifier   DOT  )+ MUL
  public static boolean importAll(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importAll")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = importAll_0(b, l + 1);
    r = r && consumeToken(b, MUL);
    exit_section_(b, m, IMPORT_ALL, r);
    return r;
  }

  // (identifier   DOT  )+
  private static boolean importAll_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importAll_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = importAll_0_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!importAll_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "importAll_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // identifier   DOT
  private static boolean importAll_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importAll_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (importAll
  //         | importSpecified)  importAlias?
  public static boolean importAllOrSpecified(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importAllOrSpecified")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = importAllOrSpecified_0(b, l + 1);
    r = r && importAllOrSpecified_1(b, l + 1);
    exit_section_(b, m, IMPORT_ALL_OR_SPECIFIED, r);
    return r;
  }

  // importAll
  //         | importSpecified
  private static boolean importAllOrSpecified_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importAllOrSpecified_0")) return false;
    boolean r;
    r = importAll(b, l + 1);
    if (!r) r = importSpecified(b, l + 1);
    return r;
  }

  // importAlias?
  private static boolean importAllOrSpecified_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importAllOrSpecified_1")) return false;
    importAlias(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (FROM   identifier)?  IMPORT  importAllOrSpecified
  //     (  COMMA   importAllOrSpecified)* end?
  public static boolean importList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importList")) return false;
    if (!nextTokenIs(b, "<import list>", FROM, IMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_LIST, "<import list>");
    r = importList_0(b, l + 1);
    r = r && consumeToken(b, IMPORT);
    r = r && importAllOrSpecified(b, l + 1);
    r = r && importList_3(b, l + 1);
    r = r && importList_4(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (FROM   identifier)?
  private static boolean importList_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importList_0")) return false;
    importList_0_0(b, l + 1);
    return true;
  }

  // FROM   identifier
  private static boolean importList_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importList_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, FROM, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  // (  COMMA   importAllOrSpecified)*
  private static boolean importList_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importList_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!importList_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "importList_3", c)) break;
    }
    return true;
  }

  // COMMA   importAllOrSpecified
  private static boolean importList_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importList_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && importAllOrSpecified(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // end?
  private static boolean importList_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importList_4")) return false;
    end(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (identifier   DOT  )+ identifier
  public static boolean importSpecified(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importSpecified")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = importSpecified_0(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    exit_section_(b, m, IMPORT_SPECIFIED, r);
    return r;
  }

  // (identifier   DOT  )+
  private static boolean importSpecified_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importSpecified_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = importSpecified_0_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!importSpecified_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "importSpecified_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // identifier   DOT
  private static boolean importSpecified_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importSpecified_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // package   packageName end?
  public static boolean packageDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageDeclaration")) return false;
    if (!nextTokenIs(b, PACKAGE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PACKAGE);
    r = r && packageName(b, l + 1);
    r = r && packageDeclaration_2(b, l + 1);
    exit_section_(b, m, PACKAGE_DECLARATION, r);
    return r;
  }

  // end?
  private static boolean packageDeclaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageDeclaration_2")) return false;
    end(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // identifier  (  DOT   identifier)*
  public static boolean packageName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageName")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    r = r && packageName_1(b, l + 1);
    exit_section_(b, m, PACKAGE_NAME, r);
    return r;
  }

  // (  DOT   identifier)*
  private static boolean packageName_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageName_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!packageName_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "packageName_1", c)) break;
    }
    return true;
  }

  // DOT   identifier
  private static boolean packageName_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageName_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DOT, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // packageDeclaration? importList*
  public static boolean preamble(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "preamble")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PREAMBLE, "<preamble>");
    r = preamble_0(b, l + 1);
    r = r && preamble_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // packageDeclaration?
  private static boolean preamble_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "preamble_0")) return false;
    packageDeclaration(b, l + 1);
    return true;
  }

  // importList*
  private static boolean preamble_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "preamble_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!importList(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "preamble_1", c)) break;
    }
    return true;
  }

}
