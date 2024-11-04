// This is a generated file. Not intended for manual editing.
package com.linqingying.cangjie.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;

import static com.linqingying.cangjie.parsing.CangJieParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;
import static com.linqingying.cangjie.CjNodeTypes.*;
import static com.linqingying.cangjie.lexer.CjTokens.*;

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
    boolean r;
    if (t == BLOCK) {
      r = BLOCK(b, l + 1);
    }
    else {
      r = CJ_FILE(b, l + 1);
    }
    return r;
  }

  /* ********************************************************** */
  // INTNATIVE_KEYWORD |
  //                            INT8_KEYWORD |
  //                            INT16_KEYWORD |
  //                            INT32_KEYWORD |
  //                            INT64_KEYWORD |
  //                            UINTNATIVE_KEYWORD  |
  //                            UINT8_KEYWORD |
  //                            UINT16_KEYWORD |
  //                            UINT32_KEYWORD |
  //                            UINT64_KEYWORD |
  //                            FLOAT16_KEYWORD|
  //                            FLOAT32_KEYWORD |
  //                            FLOAT64_KEYWORD |
  //                            NOTHING_KEYWORD|
  //                            VARRAY_KEYWORD |
  //                            BOOL_KEYWORD|
  //                            RUNE_KEYWORD|
  //                            UNIT_KEYWORD
  public static boolean BASIC_TYPE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BASIC_TYPE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BASIC_TYPE, "<basic type>");
    r = consumeToken(b, INTNATIVE_KEYWORD);
    if (!r) r = consumeToken(b, INT8_KEYWORD);
    if (!r) r = consumeToken(b, INT16_KEYWORD);
    if (!r) r = consumeToken(b, INT32_KEYWORD);
    if (!r) r = consumeToken(b, INT64_KEYWORD);
    if (!r) r = consumeToken(b, UINTNATIVE_KEYWORD);
    if (!r) r = consumeToken(b, UINT8_KEYWORD);
    if (!r) r = consumeToken(b, UINT16_KEYWORD);
    if (!r) r = consumeToken(b, UINT32_KEYWORD);
    if (!r) r = consumeToken(b, UINT64_KEYWORD);
    if (!r) r = consumeToken(b, FLOAT16_KEYWORD);
    if (!r) r = consumeToken(b, FLOAT32_KEYWORD);
    if (!r) r = consumeToken(b, FLOAT64_KEYWORD);
    if (!r) r = consumeToken(b, NOTHING_KEYWORD);
    if (!r) r = consumeToken(b, VARRAY_KEYWORD);
    if (!r) r = consumeToken(b, BOOL_KEYWORD);
    if (!r) r = consumeToken(b, RUNE_KEYWORD);
    if (!r) r = consumeToken(b, UNIT_KEYWORD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // REFERENCE_EXPRESSION
  public static boolean BINDING_PATTERN(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BINDING_PATTERN")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = REFERENCE_EXPRESSION(b, l + 1);
    exit_section_(b, m, BINDING_PATTERN, r);
    return r;
  }

  /* ********************************************************** */
  // LBRACE statement*  RBRACE
  public static boolean BLOCK(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BLOCK")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && BLOCK_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, BLOCK, r);
    return r;
  }

  // statement*
  private static boolean BLOCK_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BLOCK_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "BLOCK_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TRUE_KEYWORD | FALSE_KEYWORD
  public static boolean BOOLEAN_CONSTANT(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BOOLEAN_CONSTANT")) return false;
    if (!nextTokenIs(b, "<boolean constant>", FALSE_KEYWORD, TRUE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BOOLEAN_CONSTANT, "<boolean constant>");
    r = consumeToken(b, TRUE_KEYWORD);
    if (!r) r = consumeToken(b, FALSE_KEYWORD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // PACKAGE_DIRECTIVE?  IMPORT_LIST  top_level_declaration*
  static boolean CJ_FILE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CJ_FILE")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = CJ_FILE_0(b, l + 1);
    r = r && IMPORT_LIST(b, l + 1);
    r = r && CJ_FILE_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // PACKAGE_DIRECTIVE?
  private static boolean CJ_FILE_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CJ_FILE_0")) return false;
    PACKAGE_DIRECTIVE(b, l + 1);
    return true;
  }

  // top_level_declaration*
  private static boolean CJ_FILE_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CJ_FILE_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!top_level_declaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "CJ_FILE_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // MODIFIER_LIST CLASS_KEYWORD IDENTIFIER TYPE_PARAMETER_LIST? (LTCOLON SUPER_TYPE_LIST)? where? CLASS_BODY SEMICOLON?
  public static boolean CLASS(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CLASS, "<class>");
    r = MODIFIER_LIST(b, l + 1);
    r = r && consumeTokens(b, 0, CLASS_KEYWORD, IDENTIFIER);
    r = r && CLASS_3(b, l + 1);
    r = r && CLASS_4(b, l + 1);
    r = r && CLASS_5(b, l + 1);
    r = r && CLASS_BODY(b, l + 1);
    r = r && CLASS_7(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TYPE_PARAMETER_LIST?
  private static boolean CLASS_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS_3")) return false;
    TYPE_PARAMETER_LIST(b, l + 1);
    return true;
  }

  // (LTCOLON SUPER_TYPE_LIST)?
  private static boolean CLASS_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS_4")) return false;
    CLASS_4_0(b, l + 1);
    return true;
  }

  // LTCOLON SUPER_TYPE_LIST
  private static boolean CLASS_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LTCOLON);
    r = r && SUPER_TYPE_LIST(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // where?
  private static boolean CLASS_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS_5")) return false;
    where(b, l + 1);
    return true;
  }

  // SEMICOLON?
  private static boolean CLASS_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS_7")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // LBRACE  member_declaration*  RBRACE
  public static boolean CLASS_BODY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS_BODY")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && CLASS_BODY_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, CLASS_BODY, r);
    return r;
  }

  // member_declaration*
  private static boolean CLASS_BODY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CLASS_BODY_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!member_declaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "CLASS_BODY_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // INTEGER_CONSTANT | RUNE_CONSTANT | STRING_TEMPLATE | FLOAT_CONSTANT | BOOLEAN_CONSTANT | UNIT_CONSTANT
  public static boolean CONSTANT_PATTERN(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CONSTANT_PATTERN")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONSTANT_PATTERN, "<constant pattern>");
    r = INTEGER_CONSTANT(b, l + 1);
    if (!r) r = RUNE_CONSTANT(b, l + 1);
    if (!r) r = STRING_TEMPLATE(b, l + 1);
    if (!r) r = FLOAT_CONSTANT(b, l + 1);
    if (!r) r = BOOLEAN_CONSTANT(b, l + 1);
    if (!r) r = UNIT_CONSTANT(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (REFERENCE_EXPRESSION DOT REFERENCE_EXPRESSION)  | (DOT_QUALIFIED_EXPRESSION DOT REFERENCE_EXPRESSION)
  public static boolean DOT_QUALIFIED_EXPRESSION(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DOT_QUALIFIED_EXPRESSION")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = DOT_QUALIFIED_EXPRESSION_0(b, l + 1);
    if (!r) r = DOT_QUALIFIED_EXPRESSION_1(b, l + 1);
    exit_section_(b, m, DOT_QUALIFIED_EXPRESSION, r);
    return r;
  }

  // REFERENCE_EXPRESSION DOT REFERENCE_EXPRESSION
  private static boolean DOT_QUALIFIED_EXPRESSION_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DOT_QUALIFIED_EXPRESSION_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = REFERENCE_EXPRESSION(b, l + 1);
    r = r && consumeToken(b, DOT);
    r = r && REFERENCE_EXPRESSION(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // DOT_QUALIFIED_EXPRESSION DOT REFERENCE_EXPRESSION
  private static boolean DOT_QUALIFIED_EXPRESSION_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DOT_QUALIFIED_EXPRESSION_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = DOT_QUALIFIED_EXPRESSION(b, l + 1);
    r = r && consumeToken(b, DOT);
    r = r && REFERENCE_EXPRESSION(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // MODIFIER_LIST ENUM_KEYWORD IDENTIFIER TYPE_PARAMETER_LIST? (LTCOLON SUPER_TYPE_LIST)? where? ENUM_BODY SEMICOLON?
  public static boolean ENUM(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENUM, "<enum>");
    r = MODIFIER_LIST(b, l + 1);
    r = r && consumeTokens(b, 0, ENUM_KEYWORD, IDENTIFIER);
    r = r && ENUM_3(b, l + 1);
    r = r && ENUM_4(b, l + 1);
    r = r && ENUM_5(b, l + 1);
    r = r && ENUM_BODY(b, l + 1);
    r = r && ENUM_7(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TYPE_PARAMETER_LIST?
  private static boolean ENUM_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_3")) return false;
    TYPE_PARAMETER_LIST(b, l + 1);
    return true;
  }

  // (LTCOLON SUPER_TYPE_LIST)?
  private static boolean ENUM_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_4")) return false;
    ENUM_4_0(b, l + 1);
    return true;
  }

  // LTCOLON SUPER_TYPE_LIST
  private static boolean ENUM_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LTCOLON);
    r = r && SUPER_TYPE_LIST(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // where?
  private static boolean ENUM_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_5")) return false;
    where(b, l + 1);
    return true;
  }

  // SEMICOLON?
  private static boolean ENUM_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_7")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // LBRACE OR? ENUM_ENTRY ( OR ENUM_ENTRY)*  member_declaration* RBRACE
  public static boolean ENUM_BODY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_BODY")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && ENUM_BODY_1(b, l + 1);
    r = r && ENUM_ENTRY(b, l + 1);
    r = r && ENUM_BODY_3(b, l + 1);
    r = r && ENUM_BODY_4(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, ENUM_BODY, r);
    return r;
  }

  // OR?
  private static boolean ENUM_BODY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_BODY_1")) return false;
    consumeToken(b, OR);
    return true;
  }

  // ( OR ENUM_ENTRY)*
  private static boolean ENUM_BODY_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_BODY_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ENUM_BODY_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ENUM_BODY_3", c)) break;
    }
    return true;
  }

  // OR ENUM_ENTRY
  private static boolean ENUM_BODY_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_BODY_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OR);
    r = r && ENUM_ENTRY(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // member_declaration*
  private static boolean ENUM_BODY_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_BODY_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!member_declaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ENUM_BODY_4", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER TYPE_LIST?
  public static boolean ENUM_ENTRY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_ENTRY")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    r = r && ENUM_ENTRY_1(b, l + 1);
    exit_section_(b, m, ENUM_ENTRY, r);
    return r;
  }

  // TYPE_LIST?
  private static boolean ENUM_ENTRY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_ENTRY_1")) return false;
    TYPE_LIST(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ((TYPE_REFERENCE DOT)? IDENTIFIER enumPatternParameters?) ( OR ((TYPE_REFERENCE DOT)? IDENTIFIER enumPatternParameters?))*
  public static boolean ENUM_PATTERN(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENUM_PATTERN, "<enum pattern>");
    r = ENUM_PATTERN_0(b, l + 1);
    r = r && ENUM_PATTERN_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (TYPE_REFERENCE DOT)? IDENTIFIER enumPatternParameters?
  private static boolean ENUM_PATTERN_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ENUM_PATTERN_0_0(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    r = r && ENUM_PATTERN_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (TYPE_REFERENCE DOT)?
  private static boolean ENUM_PATTERN_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_0_0")) return false;
    ENUM_PATTERN_0_0_0(b, l + 1);
    return true;
  }

  // TYPE_REFERENCE DOT
  private static boolean ENUM_PATTERN_0_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_0_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TYPE_REFERENCE(b, l + 1);
    r = r && consumeToken(b, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  // enumPatternParameters?
  private static boolean ENUM_PATTERN_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_0_2")) return false;
    enumPatternParameters(b, l + 1);
    return true;
  }

  // ( OR ((TYPE_REFERENCE DOT)? IDENTIFIER enumPatternParameters?))*
  private static boolean ENUM_PATTERN_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ENUM_PATTERN_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ENUM_PATTERN_1", c)) break;
    }
    return true;
  }

  // OR ((TYPE_REFERENCE DOT)? IDENTIFIER enumPatternParameters?)
  private static boolean ENUM_PATTERN_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OR);
    r = r && ENUM_PATTERN_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (TYPE_REFERENCE DOT)? IDENTIFIER enumPatternParameters?
  private static boolean ENUM_PATTERN_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ENUM_PATTERN_1_0_1_0(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    r = r && ENUM_PATTERN_1_0_1_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (TYPE_REFERENCE DOT)?
  private static boolean ENUM_PATTERN_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_1_0_1_0")) return false;
    ENUM_PATTERN_1_0_1_0_0(b, l + 1);
    return true;
  }

  // TYPE_REFERENCE DOT
  private static boolean ENUM_PATTERN_1_0_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_1_0_1_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TYPE_REFERENCE(b, l + 1);
    r = r && consumeToken(b, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  // enumPatternParameters?
  private static boolean ENUM_PATTERN_1_0_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ENUM_PATTERN_1_0_1_2")) return false;
    enumPatternParameters(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // EXTEND_KEYWORD TYPE_PARAMETER_LIST? TYPE_REFERENCE (LTCOLON SUPER_TYPE_LIST)? where?  CLASS_BODY SEMICOLON?
  public static boolean EXTEND(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EXTEND")) return false;
    if (!nextTokenIs(b, EXTEND_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EXTEND_KEYWORD);
    r = r && EXTEND_1(b, l + 1);
    r = r && TYPE_REFERENCE(b, l + 1);
    r = r && EXTEND_3(b, l + 1);
    r = r && EXTEND_4(b, l + 1);
    r = r && CLASS_BODY(b, l + 1);
    r = r && EXTEND_6(b, l + 1);
    exit_section_(b, m, EXTEND, r);
    return r;
  }

  // TYPE_PARAMETER_LIST?
  private static boolean EXTEND_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EXTEND_1")) return false;
    TYPE_PARAMETER_LIST(b, l + 1);
    return true;
  }

  // (LTCOLON SUPER_TYPE_LIST)?
  private static boolean EXTEND_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EXTEND_3")) return false;
    EXTEND_3_0(b, l + 1);
    return true;
  }

  // LTCOLON SUPER_TYPE_LIST
  private static boolean EXTEND_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EXTEND_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LTCOLON);
    r = r && SUPER_TYPE_LIST(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // where?
  private static boolean EXTEND_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EXTEND_4")) return false;
    where(b, l + 1);
    return true;
  }

  // SEMICOLON?
  private static boolean EXTEND_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EXTEND_6")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // FLOAT_LITERAL
  public static boolean FLOAT_CONSTANT(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FLOAT_CONSTANT")) return false;
    if (!nextTokenIs(b, FLOAT_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FLOAT_LITERAL);
    exit_section_(b, m, FLOAT_CONSTANT, r);
    return r;
  }

  /* ********************************************************** */
  // MODIFIER_LIST FUNC_KEYWORD  IDENTIFIER TYPE_PARAMETER_LIST?   VALUE_PARAMETER_LIST  ( COLON TYPE_REFERENCE)? where? BLOCK SEMICOLON?
  public static boolean FUNC(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FUNC")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNC, "<func>");
    r = MODIFIER_LIST(b, l + 1);
    r = r && consumeTokens(b, 0, FUNC_KEYWORD, IDENTIFIER);
    r = r && FUNC_3(b, l + 1);
    r = r && VALUE_PARAMETER_LIST(b, l + 1);
    r = r && FUNC_5(b, l + 1);
    r = r && FUNC_6(b, l + 1);
    r = r && BLOCK(b, l + 1);
    r = r && FUNC_8(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TYPE_PARAMETER_LIST?
  private static boolean FUNC_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FUNC_3")) return false;
    TYPE_PARAMETER_LIST(b, l + 1);
    return true;
  }

  // ( COLON TYPE_REFERENCE)?
  private static boolean FUNC_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FUNC_5")) return false;
    FUNC_5_0(b, l + 1);
    return true;
  }

  // COLON TYPE_REFERENCE
  private static boolean FUNC_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FUNC_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // where?
  private static boolean FUNC_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FUNC_6")) return false;
    where(b, l + 1);
    return true;
  }

  // SEMICOLON?
  private static boolean FUNC_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FUNC_8")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // AS_KEYWORD IDENTIFIER
  public static boolean IMPORT_ALIAS(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IMPORT_ALIAS")) return false;
    if (!nextTokenIs(b, AS_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, AS_KEYWORD, IDENTIFIER);
    exit_section_(b, m, IMPORT_ALIAS, r);
    return r;
  }

  /* ********************************************************** */
  // import_modifier? IMPORT_KEYWORD <<packageName>> IMPORT_ALIAS?
  public static boolean IMPORT_DIRECTIVE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IMPORT_DIRECTIVE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_DIRECTIVE, "<import directive>");
    r = IMPORT_DIRECTIVE_0(b, l + 1);
    r = r && consumeToken(b, IMPORT_KEYWORD);
    r = r && packageName(b, l + 1);
    r = r && IMPORT_DIRECTIVE_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // import_modifier?
  private static boolean IMPORT_DIRECTIVE_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IMPORT_DIRECTIVE_0")) return false;
    import_modifier(b, l + 1);
    return true;
  }

  // IMPORT_ALIAS?
  private static boolean IMPORT_DIRECTIVE_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IMPORT_DIRECTIVE_3")) return false;
    IMPORT_ALIAS(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (MULIT_IMPORT_DIRECTIVE | IMPORT_DIRECTIVE)*
  public static boolean IMPORT_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IMPORT_LIST")) return false;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_LIST, "<import list>");
    while (true) {
      int c = current_position_(b);
      if (!IMPORT_LIST_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "IMPORT_LIST", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  // MULIT_IMPORT_DIRECTIVE | IMPORT_DIRECTIVE
  private static boolean IMPORT_LIST_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IMPORT_LIST_0")) return false;
    boolean r;
    r = MULIT_IMPORT_DIRECTIVE(b, l + 1);
    if (!r) r = IMPORT_DIRECTIVE(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // INTEGER_LITERAL
  public static boolean INTEGER_CONSTANT(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTEGER_CONSTANT")) return false;
    if (!nextTokenIs(b, INTEGER_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, INTEGER_LITERAL);
    exit_section_(b, m, INTEGER_CONSTANT, r);
    return r;
  }

  /* ********************************************************** */
  // MODIFIER_LIST INTERFACE_KEYWORD IDENTIFIER TYPE_PARAMETER_LIST? (LTCOLON SUPER_TYPE_LIST)? where? INTERFACE_BODY SEMICOLON?
  public static boolean INTERFACE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, INTERFACE, "<interface>");
    r = MODIFIER_LIST(b, l + 1);
    r = r && consumeTokens(b, 0, INTERFACE_KEYWORD, IDENTIFIER);
    r = r && INTERFACE_3(b, l + 1);
    r = r && INTERFACE_4(b, l + 1);
    r = r && INTERFACE_5(b, l + 1);
    r = r && INTERFACE_BODY(b, l + 1);
    r = r && INTERFACE_7(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TYPE_PARAMETER_LIST?
  private static boolean INTERFACE_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE_3")) return false;
    TYPE_PARAMETER_LIST(b, l + 1);
    return true;
  }

  // (LTCOLON SUPER_TYPE_LIST)?
  private static boolean INTERFACE_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE_4")) return false;
    INTERFACE_4_0(b, l + 1);
    return true;
  }

  // LTCOLON SUPER_TYPE_LIST
  private static boolean INTERFACE_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LTCOLON);
    r = r && SUPER_TYPE_LIST(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // where?
  private static boolean INTERFACE_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE_5")) return false;
    where(b, l + 1);
    return true;
  }

  // SEMICOLON?
  private static boolean INTERFACE_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE_7")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // LBRACE  member_declaration*  RBRACE
  public static boolean INTERFACE_BODY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE_BODY")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && INTERFACE_BODY_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, INTERFACE_BODY, r);
    return r;
  }

  // member_declaration*
  private static boolean INTERFACE_BODY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INTERFACE_BODY_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!member_declaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "INTERFACE_BODY_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // MAIN_KEYWORD VALUE_PARAMETER_LIST ( COLON TYPE_REFERENCE)? BLOCK SEMICOLON?
  public static boolean MAIN_FUNC(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MAIN_FUNC")) return false;
    if (!nextTokenIs(b, MAIN_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MAIN_KEYWORD);
    r = r && VALUE_PARAMETER_LIST(b, l + 1);
    r = r && MAIN_FUNC_2(b, l + 1);
    r = r && BLOCK(b, l + 1);
    r = r && MAIN_FUNC_4(b, l + 1);
    exit_section_(b, m, MAIN_FUNC, r);
    return r;
  }

  // ( COLON TYPE_REFERENCE)?
  private static boolean MAIN_FUNC_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MAIN_FUNC_2")) return false;
    MAIN_FUNC_2_0(b, l + 1);
    return true;
  }

  // COLON TYPE_REFERENCE
  private static boolean MAIN_FUNC_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MAIN_FUNC_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // SEMICOLON?
  private static boolean MAIN_FUNC_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MAIN_FUNC_4")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // CASE_KEYWORD pattern  DOUBLE_ARROW  CASE_BLOCK
  public static boolean MATCH_ENTRY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MATCH_ENTRY")) return false;
    if (!nextTokenIs(b, CASE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CASE_KEYWORD);
    r = r && pattern(b, l + 1);
    r = r && consumeTokens(b, 0, DOUBLE_ARROW, CASE_BLOCK);
    exit_section_(b, m, MATCH_ENTRY, r);
    return r;
  }

  /* ********************************************************** */
  // modifier*
  public static boolean MODIFIER_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MODIFIER_LIST")) return false;
    Marker m = enter_section_(b, l, _NONE_, MODIFIER_LIST, "<modifier list>");
    while (true) {
      int c = current_position_(b);
      if (!modifier(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "MODIFIER_LIST", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // import_modifier? IMPORT_KEYWORD <<packageName>> DOT LBRACE <<packageName>> (COMMA <<packageName>>)* RBRACE
  public static boolean MULIT_IMPORT_DIRECTIVE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MULIT_IMPORT_DIRECTIVE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MULIT_IMPORT_DIRECTIVE, "<mulit import directive>");
    r = MULIT_IMPORT_DIRECTIVE_0(b, l + 1);
    r = r && consumeToken(b, IMPORT_KEYWORD);
    r = r && packageName(b, l + 1);
    r = r && consumeTokens(b, 0, DOT, LBRACE);
    r = r && packageName(b, l + 1);
    r = r && MULIT_IMPORT_DIRECTIVE_6(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // import_modifier?
  private static boolean MULIT_IMPORT_DIRECTIVE_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MULIT_IMPORT_DIRECTIVE_0")) return false;
    import_modifier(b, l + 1);
    return true;
  }

  // (COMMA <<packageName>>)*
  private static boolean MULIT_IMPORT_DIRECTIVE_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MULIT_IMPORT_DIRECTIVE_6")) return false;
    while (true) {
      int c = current_position_(b);
      if (!MULIT_IMPORT_DIRECTIVE_6_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "MULIT_IMPORT_DIRECTIVE_6", c)) break;
    }
    return true;
  }

  // COMMA <<packageName>>
  private static boolean MULIT_IMPORT_DIRECTIVE_6_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MULIT_IMPORT_DIRECTIVE_6_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && packageName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // QUEST type
  public static boolean OPTIONAL_TYPE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "OPTIONAL_TYPE")) return false;
    if (!nextTokenIs(b, QUEST)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, QUEST);
    r = r && type(b, l + 1);
    exit_section_(b, m, OPTIONAL_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // package_modifier? PACKAGE_KEYWORD package_name SEMICOLON?
  public static boolean PACKAGE_DIRECTIVE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PACKAGE_DIRECTIVE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PACKAGE_DIRECTIVE, "<package directive>");
    r = PACKAGE_DIRECTIVE_0(b, l + 1);
    r = r && consumeToken(b, PACKAGE_KEYWORD);
    r = r && package_name(b, l + 1);
    r = r && PACKAGE_DIRECTIVE_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // package_modifier?
  private static boolean PACKAGE_DIRECTIVE_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PACKAGE_DIRECTIVE_0")) return false;
    package_modifier(b, l + 1);
    return true;
  }

  // SEMICOLON?
  private static boolean PACKAGE_DIRECTIVE_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PACKAGE_DIRECTIVE_3")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // MODIFIER_LIST PROP_KEYWORD  IDENTIFIER COLON TYPE_REFERENCE PROPERTY_BODY?
  public static boolean PROPERTY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PROPERTY, "<property>");
    r = MODIFIER_LIST(b, l + 1);
    r = r && consumeTokens(b, 0, PROP_KEYWORD, IDENTIFIER, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    r = r && PROPERTY_5(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // PROPERTY_BODY?
  private static boolean PROPERTY_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_5")) return false;
    PROPERTY_BODY(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // prop_getter | prop_setter
  public static boolean PROPERTY_ACCESSOR(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_ACCESSOR")) return false;
    if (!nextTokenIs(b, "<property accessor>", GET_KEYWORD, SET_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PROPERTY_ACCESSOR, "<property accessor>");
    r = prop_getter(b, l + 1);
    if (!r) r = prop_setter(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACE  PROPERTY_ACCESSOR+  RBRACE
  public static boolean PROPERTY_BODY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_BODY")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && PROPERTY_BODY_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, PROPERTY_BODY, r);
    return r;
  }

  // PROPERTY_ACCESSOR+
  private static boolean PROPERTY_BODY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_BODY_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = PROPERTY_ACCESSOR(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!PROPERTY_ACCESSOR(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PROPERTY_BODY_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean REFERENCE_EXPRESSION(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "REFERENCE_EXPRESSION")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, REFERENCE_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // RUNE_LITERAL
  public static boolean RUNE_CONSTANT(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RUNE_CONSTANT")) return false;
    if (!nextTokenIs(b, RUNE_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, RUNE_LITERAL);
    exit_section_(b, m, RUNE_CONSTANT, r);
    return r;
  }

  /* ********************************************************** */
  // <<parseStringTemplate>>
  public static boolean STRING_TEMPLATE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "STRING_TEMPLATE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRING_TEMPLATE, "<string template>");
    r = parseStringTemplate(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // MODIFIER_LIST STRUCT_KEYWORD IDENTIFIER TYPE_PARAMETER_LIST? (LTCOLON SUPER_TYPE_LIST)? where? CLASS_BODY SEMICOLON?
  public static boolean STRUCT(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "STRUCT")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRUCT, "<struct>");
    r = MODIFIER_LIST(b, l + 1);
    r = r && consumeTokens(b, 0, STRUCT_KEYWORD, IDENTIFIER);
    r = r && STRUCT_3(b, l + 1);
    r = r && STRUCT_4(b, l + 1);
    r = r && STRUCT_5(b, l + 1);
    r = r && CLASS_BODY(b, l + 1);
    r = r && STRUCT_7(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TYPE_PARAMETER_LIST?
  private static boolean STRUCT_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "STRUCT_3")) return false;
    TYPE_PARAMETER_LIST(b, l + 1);
    return true;
  }

  // (LTCOLON SUPER_TYPE_LIST)?
  private static boolean STRUCT_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "STRUCT_4")) return false;
    STRUCT_4_0(b, l + 1);
    return true;
  }

  // LTCOLON SUPER_TYPE_LIST
  private static boolean STRUCT_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "STRUCT_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LTCOLON);
    r = r && SUPER_TYPE_LIST(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // where?
  private static boolean STRUCT_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "STRUCT_5")) return false;
    where(b, l + 1);
    return true;
  }

  // SEMICOLON?
  private static boolean STRUCT_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "STRUCT_7")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // TYPE_REFERENCE
  public static boolean SUPER_TYPE_ENTRY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SUPER_TYPE_ENTRY")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SUPER_TYPE_ENTRY, "<super type entry>");
    r = TYPE_REFERENCE(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // SUPER_TYPE_ENTRY ( AND SUPER_TYPE_ENTRY)*
  public static boolean SUPER_TYPE_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SUPER_TYPE_LIST")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SUPER_TYPE_LIST, "<super type list>");
    r = SUPER_TYPE_ENTRY(b, l + 1);
    r = r && SUPER_TYPE_LIST_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( AND SUPER_TYPE_ENTRY)*
  private static boolean SUPER_TYPE_LIST_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SUPER_TYPE_LIST_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!SUPER_TYPE_LIST_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "SUPER_TYPE_LIST_1", c)) break;
    }
    return true;
  }

  // AND SUPER_TYPE_ENTRY
  private static boolean SUPER_TYPE_LIST_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SUPER_TYPE_LIST_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AND);
    r = r && SUPER_TYPE_ENTRY(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // THIS_KEYWORD_UPPER
  public static boolean THIS_TYPE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "THIS_TYPE")) return false;
    if (!nextTokenIs(b, THIS_KEYWORD_UPPER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, THIS_KEYWORD_UPPER);
    exit_section_(b, m, THIS_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // LPAR pattern (COMMA pattern)+   RPAR
  public static boolean TUPLE_PATTERN(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TUPLE_PATTERN")) return false;
    if (!nextTokenIs(b, LPAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAR);
    r = r && pattern(b, l + 1);
    r = r && TUPLE_PATTERN_2(b, l + 1);
    r = r && consumeToken(b, RPAR);
    exit_section_(b, m, TUPLE_PATTERN, r);
    return r;
  }

  // (COMMA pattern)+
  private static boolean TUPLE_PATTERN_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TUPLE_PATTERN_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TUPLE_PATTERN_2_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!TUPLE_PATTERN_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TUPLE_PATTERN_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA pattern
  private static boolean TUPLE_PATTERN_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TUPLE_PATTERN_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && pattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LT  TYPE_PROJECTION (COMMA TYPE_PROJECTION)* GT
  public static boolean TYPE_ARGUMENT_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_ARGUMENT_LIST")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && TYPE_PROJECTION(b, l + 1);
    r = r && TYPE_ARGUMENT_LIST_2(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, TYPE_ARGUMENT_LIST, r);
    return r;
  }

  // (COMMA TYPE_PROJECTION)*
  private static boolean TYPE_ARGUMENT_LIST_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_ARGUMENT_LIST_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TYPE_ARGUMENT_LIST_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TYPE_ARGUMENT_LIST_2", c)) break;
    }
    return true;
  }

  // COMMA TYPE_PROJECTION
  private static boolean TYPE_ARGUMENT_LIST_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_ARGUMENT_LIST_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && TYPE_PROJECTION(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // REFERENCE_EXPRESSION  LTCOLON TYPE_REFERENCE (AND TYPE_REFERENCE)*
  public static boolean TYPE_CONSTRAINT(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_CONSTRAINT")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = REFERENCE_EXPRESSION(b, l + 1);
    r = r && consumeToken(b, LTCOLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    r = r && TYPE_CONSTRAINT_3(b, l + 1);
    exit_section_(b, m, TYPE_CONSTRAINT, r);
    return r;
  }

  // (AND TYPE_REFERENCE)*
  private static boolean TYPE_CONSTRAINT_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_CONSTRAINT_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TYPE_CONSTRAINT_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TYPE_CONSTRAINT_3", c)) break;
    }
    return true;
  }

  // AND TYPE_REFERENCE
  private static boolean TYPE_CONSTRAINT_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_CONSTRAINT_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AND);
    r = r && TYPE_REFERENCE(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TYPE_CONSTRAINT (COMMA TYPE_CONSTRAINT)*
  public static boolean TYPE_CONSTRAINT_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_CONSTRAINT_LIST")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TYPE_CONSTRAINT(b, l + 1);
    r = r && TYPE_CONSTRAINT_LIST_1(b, l + 1);
    exit_section_(b, m, TYPE_CONSTRAINT_LIST, r);
    return r;
  }

  // (COMMA TYPE_CONSTRAINT)*
  private static boolean TYPE_CONSTRAINT_LIST_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_CONSTRAINT_LIST_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TYPE_CONSTRAINT_LIST_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TYPE_CONSTRAINT_LIST_1", c)) break;
    }
    return true;
  }

  // COMMA TYPE_CONSTRAINT
  private static boolean TYPE_CONSTRAINT_LIST_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_CONSTRAINT_LIST_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && TYPE_CONSTRAINT(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '('  TYPE_REFERENCE (COMMA TYPE_REFERENCE)* ')'
  public static boolean TYPE_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_LIST")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_LIST, "<type list>");
    r = consumeToken(b, "(");
    r = r && TYPE_REFERENCE(b, l + 1);
    r = r && TYPE_LIST_2(b, l + 1);
    r = r && consumeToken(b, ")");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (COMMA TYPE_REFERENCE)*
  private static boolean TYPE_LIST_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_LIST_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TYPE_LIST_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TYPE_LIST_2", c)) break;
    }
    return true;
  }

  // COMMA TYPE_REFERENCE
  private static boolean TYPE_LIST_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_LIST_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && TYPE_REFERENCE(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean TYPE_PARAMETER(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_PARAMETER")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, TYPE_PARAMETER, r);
    return r;
  }

  /* ********************************************************** */
  // LT TYPE_PARAMETER (COMMA TYPE_PARAMETER)* GT
  public static boolean TYPE_PARAMETER_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_PARAMETER_LIST")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && TYPE_PARAMETER(b, l + 1);
    r = r && TYPE_PARAMETER_LIST_2(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, TYPE_PARAMETER_LIST, r);
    return r;
  }

  // (COMMA TYPE_PARAMETER)*
  private static boolean TYPE_PARAMETER_LIST_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_PARAMETER_LIST_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TYPE_PARAMETER_LIST_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TYPE_PARAMETER_LIST_2", c)) break;
    }
    return true;
  }

  // COMMA TYPE_PARAMETER
  private static boolean TYPE_PARAMETER_LIST_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_PARAMETER_LIST_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && TYPE_PARAMETER(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // REFERENCE_EXPRESSION COLON TYPE_REFERENCE
  public static boolean TYPE_PATTERN(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_PATTERN")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = REFERENCE_EXPRESSION(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    exit_section_(b, m, TYPE_PATTERN, r);
    return r;
  }

  /* ********************************************************** */
  // TYPE_REFERENCE
  public static boolean TYPE_PROJECTION(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_PROJECTION")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_PROJECTION, "<type projection>");
    r = TYPE_REFERENCE(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // type
  public static boolean TYPE_REFERENCE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TYPE_REFERENCE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_REFERENCE, "<type reference>");
    r = type(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '('')'
  public static boolean UNIT_CONSTANT(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UNIT_CONSTANT")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, UNIT_CONSTANT, "<unit constant>");
    r = consumeToken(b, "(");
    r = r && consumeToken(b, ")");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (REFERENCE_EXPRESSION   DOT )* REFERENCE_EXPRESSION (  TYPE_ARGUMENT_LIST)?
  public static boolean USER_TYPE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "USER_TYPE")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = USER_TYPE_0(b, l + 1);
    r = r && REFERENCE_EXPRESSION(b, l + 1);
    r = r && USER_TYPE_2(b, l + 1);
    exit_section_(b, m, USER_TYPE, r);
    return r;
  }

  // (REFERENCE_EXPRESSION   DOT )*
  private static boolean USER_TYPE_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "USER_TYPE_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!USER_TYPE_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "USER_TYPE_0", c)) break;
    }
    return true;
  }

  // REFERENCE_EXPRESSION   DOT
  private static boolean USER_TYPE_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "USER_TYPE_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = REFERENCE_EXPRESSION(b, l + 1);
    r = r && consumeToken(b, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  // (  TYPE_ARGUMENT_LIST)?
  private static boolean USER_TYPE_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "USER_TYPE_2")) return false;
    USER_TYPE_2_0(b, l + 1);
    return true;
  }

  // (  TYPE_ARGUMENT_LIST)
  private static boolean USER_TYPE_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "USER_TYPE_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TYPE_ARGUMENT_LIST(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (IDENTIFIER  COLON  TYPE_REFERENCE) |
  //                     (IDENTIFIER EXCL COLON  TYPE_REFERENCE (EQ  expression)?)
  public static boolean VALUE_PARAMETER(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = VALUE_PARAMETER_0(b, l + 1);
    if (!r) r = VALUE_PARAMETER_1(b, l + 1);
    exit_section_(b, m, VALUE_PARAMETER, r);
    return r;
  }

  // IDENTIFIER  COLON  TYPE_REFERENCE
  private static boolean VALUE_PARAMETER_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // IDENTIFIER EXCL COLON  TYPE_REFERENCE (EQ  expression)?
  private static boolean VALUE_PARAMETER_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, EXCL, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    r = r && VALUE_PARAMETER_1_4(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (EQ  expression)?
  private static boolean VALUE_PARAMETER_1_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_1_4")) return false;
    VALUE_PARAMETER_1_4_0(b, l + 1);
    return true;
  }

  // EQ  expression
  private static boolean VALUE_PARAMETER_1_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_1_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '('  VALUE_PARAMETER (COMMA VALUE_PARAMETER)* RPAR
  public static boolean VALUE_PARAMETER_LIST(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VALUE_PARAMETER_LIST")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VALUE_PARAMETER_LIST, "<value parameter list>");
    r = consumeToken(b, "(");
    r = r && VALUE_PARAMETER(b, l + 1);
    r = r && VALUE_PARAMETER_LIST_2(b, l + 1);
    r = r && consumeToken(b, RPAR);
    exit_section_(b, l, m, r, false, null);
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

  /* ********************************************************** */
  // MODIFIER_LIST?  ((( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   COLON TYPE_REFERENCE ) |
  //             (( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   EQ  expression) |
  //             (( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   COLON TYPE_REFERENCE EQ  expression)) SEMICOLON?
  public static boolean VARIABLE(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VARIABLE, "<variable>");
    r = VARIABLE_0(b, l + 1);
    r = r && VARIABLE_1(b, l + 1);
    r = r && VARIABLE_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // MODIFIER_LIST?
  private static boolean VARIABLE_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_0")) return false;
    MODIFIER_LIST(b, l + 1);
    return true;
  }

  // (( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   COLON TYPE_REFERENCE ) |
  //             (( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   EQ  expression) |
  //             (( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   COLON TYPE_REFERENCE EQ  expression)
  private static boolean VARIABLE_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = VARIABLE_1_0(b, l + 1);
    if (!r) r = VARIABLE_1_1(b, l + 1);
    if (!r) r = VARIABLE_1_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   COLON TYPE_REFERENCE
  private static boolean VARIABLE_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = VARIABLE_1_0_0(b, l + 1);
    r = r && consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD
  private static boolean VARIABLE_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, CONST_KEYWORD);
    if (!r) r = consumeToken(b, VAR_KEYWORD);
    if (!r) r = consumeToken(b, LET_KEYWORD);
    return r;
  }

  // ( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   EQ  expression
  private static boolean VARIABLE_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = VARIABLE_1_1_0(b, l + 1);
    r = r && consumeTokens(b, 0, IDENTIFIER, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD
  private static boolean VARIABLE_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_1_1_0")) return false;
    boolean r;
    r = consumeToken(b, CONST_KEYWORD);
    if (!r) r = consumeToken(b, VAR_KEYWORD);
    if (!r) r = consumeToken(b, LET_KEYWORD);
    return r;
  }

  // ( CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD) IDENTIFIER   COLON TYPE_REFERENCE EQ  expression
  private static boolean VARIABLE_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = VARIABLE_1_2_0(b, l + 1);
    r = r && consumeTokens(b, 0, IDENTIFIER, COLON);
    r = r && TYPE_REFERENCE(b, l + 1);
    r = r && consumeToken(b, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // CONST_KEYWORD | VAR_KEYWORD | LET_KEYWORD
  private static boolean VARIABLE_1_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_1_2_0")) return false;
    boolean r;
    r = consumeToken(b, CONST_KEYWORD);
    if (!r) r = consumeToken(b, VAR_KEYWORD);
    if (!r) r = consumeToken(b, LET_KEYWORD);
    return r;
  }

  // SEMICOLON?
  private static boolean VARIABLE_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VARIABLE_2")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  /* ********************************************************** */
  // UNDERLINE
  public static boolean WILDCARD_PATTERN(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WILDCARD_PATTERN")) return false;
    if (!nextTokenIs(b, UNDERLINE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, UNDERLINE);
    exit_section_(b, m, WILDCARD_PATTERN, r);
    return r;
  }

  /* ********************************************************** */
  // LPAR    pattern (  COMMA   pattern)*   RPAR
  static boolean enumPatternParameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumPatternParameters")) return false;
    if (!nextTokenIs(b, LPAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAR);
    r = r && pattern(b, l + 1);
    r = r && enumPatternParameters_2(b, l + 1);
    r = r && consumeToken(b, RPAR);
    exit_section_(b, m, null, r);
    return r;
  }

  // (  COMMA   pattern)*
  private static boolean enumPatternParameters_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumPatternParameters_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!enumPatternParameters_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "enumPatternParameters_2", c)) break;
    }
    return true;
  }

  // COMMA   pattern
  private static boolean enumPatternParameters_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumPatternParameters_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && pattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 1
  static boolean expression(PsiBuilder b, int l) {
    return consumeToken(b, "1");
  }

  /* ********************************************************** */
  // PUBLIC_KEYWORD | PROTECTED_KEYWORD | INTERNAL_KEYWORD | PRIVATE_KEYWORD
  static boolean import_modifier(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_modifier")) return false;
    boolean r;
    r = consumeToken(b, PUBLIC_KEYWORD);
    if (!r) r = consumeToken(b, PROTECTED_KEYWORD);
    if (!r) r = consumeToken(b, INTERNAL_KEYWORD);
    if (!r) r = consumeToken(b, PRIVATE_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // FUNC | VARIABLE | PROPERTY
  static boolean member_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_declaration")) return false;
    boolean r;
    r = FUNC(b, l + 1);
    if (!r) r = VARIABLE(b, l + 1);
    if (!r) r = PROPERTY(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // <<abstractKeyword>> |
  //                                        <<openKeyword>>|
  //                                 OVERRIDE_KEYWORD|
  //                                    PRIVATE_KEYWORD|
  //                                          PUBLIC_KEYWORD|
  //                                          PROTECTED_KEYWORD|
  //                                          INTERNAL_KEYWORD|
  //                                          STATIC_KEYWORD|
  //                                          MUT_KEYWORD|
  //                                          OPERATOR_KEYWORD|
  //                                          <<sealedKeyword>>|
  //                                         <<constKeyword>>|
  //                                        FOREIGN_KEYWORD|
  //                                         UNSAFE_KEYWORD|
  //                                          REDEF_KEYWORD
  static boolean modifier(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "modifier")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = abstractKeyword(b, l + 1);
    if (!r) r = openKeyword(b, l + 1);
    if (!r) r = consumeToken(b, OVERRIDE_KEYWORD);
    if (!r) r = consumeToken(b, PRIVATE_KEYWORD);
    if (!r) r = consumeToken(b, PUBLIC_KEYWORD);
    if (!r) r = consumeToken(b, PROTECTED_KEYWORD);
    if (!r) r = consumeToken(b, INTERNAL_KEYWORD);
    if (!r) r = consumeToken(b, STATIC_KEYWORD);
    if (!r) r = consumeToken(b, MUT_KEYWORD);
    if (!r) r = consumeToken(b, OPERATOR_KEYWORD);
    if (!r) r = sealedKeyword(b, l + 1);
    if (!r) r = constKeyword(b, l + 1);
    if (!r) r = consumeToken(b, FOREIGN_KEYWORD);
    if (!r) r = consumeToken(b, UNSAFE_KEYWORD);
    if (!r) r = consumeToken(b, REDEF_KEYWORD);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PUBLIC_KEYWORD | PROTECTED_KEYWORD | INTERNAL_KEYWORD
  static boolean package_modifier(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "package_modifier")) return false;
    boolean r;
    r = consumeToken(b, PUBLIC_KEYWORD);
    if (!r) r = consumeToken(b, PROTECTED_KEYWORD);
    if (!r) r = consumeToken(b, INTERNAL_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // DOT_QUALIFIED_EXPRESSION
  static boolean package_name(PsiBuilder b, int l) {
    return DOT_QUALIFIED_EXPRESSION(b, l + 1);
  }

  /* ********************************************************** */
  // CONSTANT_PATTERN | WILDCARD_PATTERN | TUPLE_PATTERN | TYPE_PATTERN | ENUM_PATTERN | BINDING_PATTERN
  static boolean pattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pattern")) return false;
    boolean r;
    r = CONSTANT_PATTERN(b, l + 1);
    if (!r) r = WILDCARD_PATTERN(b, l + 1);
    if (!r) r = TUPLE_PATTERN(b, l + 1);
    if (!r) r = TYPE_PATTERN(b, l + 1);
    if (!r) r = ENUM_PATTERN(b, l + 1);
    if (!r) r = BINDING_PATTERN(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // GET_KEYWORD LBRACE RBRACE BLOCK
  static boolean prop_getter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "prop_getter")) return false;
    if (!nextTokenIs(b, GET_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, GET_KEYWORD, LBRACE, RBRACE);
    r = r && BLOCK(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // SET_KEYWORD LBRACE IDENTIFIER RBRACE BLOCK
  static boolean prop_setter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "prop_setter")) return false;
    if (!nextTokenIs(b, SET_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, SET_KEYWORD, LBRACE, IDENTIFIER, RBRACE);
    r = r && BLOCK(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 1
  static boolean statement(PsiBuilder b, int l) {
    return consumeToken(b, "1");
  }

  /* ********************************************************** */
  // MAIN_FUNC | FUNC | CLASS | VARIABLE | STRUCT | ENUM | INTERFACE | EXTEND
  static boolean top_level_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_declaration")) return false;
    boolean r;
    r = MAIN_FUNC(b, l + 1);
    if (!r) r = FUNC(b, l + 1);
    if (!r) r = CLASS(b, l + 1);
    if (!r) r = VARIABLE(b, l + 1);
    if (!r) r = STRUCT(b, l + 1);
    if (!r) r = ENUM(b, l + 1);
    if (!r) r = INTERFACE(b, l + 1);
    if (!r) r = EXTEND(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // BASIC_TYPE |  THIS_TYPE | USER_TYPE | OPTIONAL_TYPE
  static boolean type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type")) return false;
    boolean r;
    r = BASIC_TYPE(b, l + 1);
    if (!r) r = THIS_TYPE(b, l + 1);
    if (!r) r = USER_TYPE(b, l + 1);
    if (!r) r = OPTIONAL_TYPE(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // WHERE_KEYWORD TYPE_CONSTRAINT_LIST
  static boolean where(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "where")) return false;
    if (!nextTokenIs(b, WHERE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WHERE_KEYWORD);
    r = r && TYPE_CONSTRAINT_LIST(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

}
