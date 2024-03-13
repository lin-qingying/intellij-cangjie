// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.grammar.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.huawei.cangjie.lexer.CjToken;
import com.huawei.cangjie.grammar.psi.impl.*;

public interface CjElementTypes {

  IElementType END = new IElementType("END", null);
  IElementType IMPORT_ALIAS = new IElementType("IMPORT_ALIAS", null);
  IElementType IMPORT_ALL = new IElementType("IMPORT_ALL", null);
  IElementType IMPORT_ALL_OR_SPECIFIED = new IElementType("IMPORT_ALL_OR_SPECIFIED", null);
  IElementType IMPORT_LIST = new IElementType("IMPORT_LIST", null);
  IElementType IMPORT_SPECIFIED = new IElementType("IMPORT_SPECIFIED", null);
  IElementType PACKAGE_DECLARATION = new IElementType("PACKAGE_DECLARATION", null);
  IElementType PACKAGE_NAME = new IElementType("PACKAGE_NAME", null);
  IElementType PREAMBLE = new IElementType("PREAMBLE", null);

  IElementType ABSTRACT = new CjToken("abstract");
  IElementType ADD = new CjToken("+");
  IElementType ADD_ASSIGN = new CjToken("+=");
  IElementType AND = new CjToken("&&");
  IElementType AND_ASSIGN = new CjToken("&&=");
  IElementType ARROW = new CjToken("->");
  IElementType AS = new CjToken("as");
  IElementType ASSIGN = new CjToken("=");
  IElementType AT = new CjToken("@");
  IElementType BACKARROW = new CjToken("<-");
  IElementType BACKSLASH = new CjToken("\\\\");
  IElementType BITAND = new CjToken("&");
  IElementType BITAND_ASSIGN = new CjToken("&=");
  IElementType BITOR = new CjToken("|");
  IElementType BITOR_ASSIGN = new CjToken("|=");
  IElementType BITXOR = new CjToken("^");
  IElementType BITXOR_ASSIGN = new CjToken("^=");
  IElementType BOOLEAN = new CjToken("Bool");
  IElementType BREAK = new CjToken("break");
  IElementType CASE = new CjToken("case");
  IElementType CATCH = new CjToken("catch");
  IElementType CHAR = new CjToken("Char");
  IElementType CLASS = new CjToken("class");
  IElementType CLOSEDRANGEOP = new CjToken("..=");
  IElementType COLON = new CjToken(":");
  IElementType COMMA = new CjToken(",");
  IElementType COMPOSITION = new CjToken("~>");
  IElementType CONST = new CjToken("const");
  IElementType CONTINUE = new CjToken("continue");
  IElementType DEC = new CjToken("--");
  IElementType DIV = new CjToken("/");
  IElementType DIV_ASSIGN = new CjToken("/=");
  IElementType DO = new CjToken("do");
  IElementType DOLLAR = new CjToken("$");
  IElementType DOT = new CjToken(".");
  IElementType DOUBLE_ARROW = new CjToken("=>");
  IElementType ELLIPSIS = new CjToken("...");
  IElementType ELSE = new CjToken("else");
  IElementType ENUM = new CjToken("enum");
  IElementType EQUAL = new CjToken("==");
  IElementType EXP = new CjToken("**");
  IElementType EXP_ASSIGN = new CjToken("**=");
  IElementType EXTEND = new CjToken("extend");
  IElementType FALSE = new CjToken("false");
  IElementType FINALLY = new CjToken("finally");
  IElementType FLOAT16 = new CjToken("Float16");
  IElementType FLOAT32 = new CjToken("Float32");
  IElementType FLOAT64 = new CjToken("Float64");
  IElementType FOR = new CjToken("for");
  IElementType FOREIGN = new CjToken("foreign");
  IElementType FROM = new CjToken("from");
  IElementType FUNC = new CjToken("func");
  IElementType GE = new CjToken(">=");
  IElementType GET = new CjToken("get");
  IElementType GT = new CjToken(">");
  IElementType HASH = new CjToken("#");
  IElementType IDENTIFIER = new CjToken("identifier");
  IElementType IF = new CjToken("if");
  IElementType IMPORT = new CjToken("import");
  IElementType IN = new CjToken("in");
  IElementType INC = new CjToken("++");
  IElementType INIT = new CjToken("init");
  IElementType INOUT = new CjToken("inout");
  IElementType INT16 = new CjToken("Int16");
  IElementType INT32 = new CjToken("Int32");
  IElementType INT64 = new CjToken("Int64");
  IElementType INT8 = new CjToken("Int8");
  IElementType INTERFACE = new CjToken("interface");
  IElementType INTNATIVE = new CjToken("IntNative");
  IElementType IS = new CjToken("is");
  IElementType LCURL = new CjToken("{");
  IElementType LE = new CjToken("<=");
  IElementType LET = new CjToken("let");
  IElementType LINESTREXPRSTART = new CjToken("${");
  IElementType LPAREN = new CjToken("(");
  IElementType LSHIFT = new CjToken("<<");
  IElementType LSHIFT_ASSIGN = new CjToken("<<=");
  IElementType LSQUARE = new CjToken("[");
  IElementType LT = new CjToken("<");
  IElementType MACRO = new CjToken("macro");
  IElementType MAIN = new CjToken("main");
  IElementType MATCH = new CjToken("match");
  IElementType MOD = new CjToken("%");
  IElementType MOD_ASSIGN = new CjToken("%=");
  IElementType MUL = new CjToken("*");
  IElementType MUL_ASSIGN = new CjToken("*=");
  IElementType MUT = new CjToken("mut");
  IElementType NOT = new CjToken("!");
  IElementType NOTEQUAL = new CjToken("!=");
  IElementType NOTHING = new CjToken("Nothing");
  IElementType OPEN = new CjToken("open");
  IElementType OPERATOR = new CjToken("operator");
  IElementType OR = new CjToken("||");
  IElementType OR_ASSIGN = new CjToken("||=");
  IElementType OVERRIDE = new CjToken("override");
  IElementType PACKAGE = new CjToken("package");
  IElementType PIPELINE = new CjToken("|>");
  IElementType PRIVATE = new CjToken("private");
  IElementType PROP = new CjToken("prop");
  IElementType PROTECTED = new CjToken("protected");
  IElementType PUBLIC = new CjToken("public");
  IElementType QUEST = new CjToken("?");
  IElementType QUOTE = new CjToken("quote");
  IElementType QUOTESYMBOL = new CjToken("`");
  IElementType QUOTE_OPEN = new CjToken("\"");
  IElementType RANGEOP = new CjToken("..");
  IElementType RCURL = new CjToken("}");
  IElementType REDEF = new CjToken("redef");
  IElementType RETURN = new CjToken("return");
  IElementType RPAREN = new CjToken(")");
  IElementType RSHIFT = new CjToken(">>");
  IElementType RSHIFT_ASSIGN = new CjToken(">>=");
  IElementType RSQUARE = new CjToken("]");
  IElementType SEMI = new CjToken(";");
  IElementType SET = new CjToken("set");
  IElementType SPAWN = new CjToken("spawn");
  IElementType STATIC = new CjToken("static");
  IElementType STRUCT = new CjToken("struct");
  IElementType SUB = new CjToken("-");
  IElementType SUB_ASSIGN = new CjToken("-=");
  IElementType SUPER = new CjToken("super");
  IElementType SYNCHRONIZED = new CjToken("synchronized");
  IElementType THIS = new CjToken("this");
  IElementType THISTYPE = new CjToken("This");
  IElementType THROW = new CjToken("throw");
  IElementType TRIPLE_QUOTE_OPEN = new CjToken("\"\"\"");
  IElementType TRUE = new CjToken("true");
  IElementType TRY = new CjToken("try");
  IElementType TYPE_ALIAS = new CjToken("type");
  IElementType UINT16 = new CjToken("UInt16");
  IElementType UINT32 = new CjToken("UInt32");
  IElementType UINT64 = new CjToken("UInt64");
  IElementType UINT8 = new CjToken("UInt8");
  IElementType UINTNATIVE = new CjToken("UIntNative");
  IElementType UNIT = new CjToken("Unit");
  IElementType UNSAFE = new CjToken("unsafe");
  IElementType UPPERBOUND = new CjToken("<:");
  IElementType VAR = new CjToken("var");
  IElementType WHERE = new CjToken("where");
  IElementType WHILE = new CjToken("while");
  IElementType WILDCARD = new CjToken("_");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == END) {
        return new CjEndImpl(node);
      }
      else if (type == IMPORT_ALIAS) {
        return new CjImportAliasImpl(node);
      }
      else if (type == IMPORT_ALL) {
        return new CjImportAllImpl(node);
      }
      else if (type == IMPORT_ALL_OR_SPECIFIED) {
        return new CjImportAllOrSpecifiedImpl(node);
      }
      else if (type == IMPORT_LIST) {
        return new CjImportListImpl(node);
      }
      else if (type == IMPORT_SPECIFIED) {
        return new CjImportSpecifiedImpl(node);
      }
      else if (type == PACKAGE_DECLARATION) {
        return new CjPackageDeclarationImpl(node);
      }
      else if (type == PACKAGE_NAME) {
        return new CjPackageNameImpl(node);
      }
      else if (type == PREAMBLE) {
        return new CjPreambleImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
