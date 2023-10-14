// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.huawei.cangjie.lang.core.stubs.StubImplementationsKt;
import com.huawei.cangjie.lang.core.psi.impl.*;
import com.intellij.psi.impl.source.tree.CompositePsiElement;

public interface CjElementTypes {

  IElementType A_EXPR = new CjElementType("A_EXPR");
  IElementType BLOCK = StubImplementationsKt.factory("BLOCK");
  IElementType DEFAULT_PARAMETER_VALUE = StubImplementationsKt.factory("DEFAULT_PARAMETER_VALUE");
  IElementType EXPR = new CjElementType("EXPR");
  IElementType FUNCTION = StubImplementationsKt.factory("FUNCTION");
  IElementType ITEM = new CjElementType("ITEM");
  IElementType LABEL_DECL = new CjElementType("LABEL_DECL");
  IElementType LIFETIME = StubImplementationsKt.factory("LIFETIME");
  IElementType LIFETIME_PARAMETER = StubImplementationsKt.factory("LIFETIME_PARAMETER");
  IElementType LIFETIME_PARAM_BOUNDS = new CjElementType("LIFETIME_PARAM_BOUNDS");
  IElementType MAIN_FUNC = StubImplementationsKt.factory("MAIN_FUNC");
  IElementType MAIN_FUNC_PARAM = new CjElementType("MAIN_FUNC_PARAM");
  IElementType PARAM_STMT = new CjElementType("PARAM_STMT");
  IElementType STATEMENT_CODE_FRAGMENT_ELEMENT = new CjElementType("STATEMENT_CODE_FRAGMENT_ELEMENT");
  IElementType STMT = new CjElementType("STMT");
  IElementType TYPE_REFERENCE = new CjElementType("TYPE_REFERENCE");
  IElementType TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT = new CjElementType("TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT");
  IElementType VALUE_PARAMETER = StubImplementationsKt.factory("VALUE_PARAMETER");
  IElementType VALUE_PARAMETER_LIST = StubImplementationsKt.factory("VALUE_PARAMETER_LIST");

  IElementType AND = new CjTokenType("&");
  IElementType ANDAND = new CjTokenType("&&");
  IElementType AS = new CjTokenType("as");
  IElementType BOOL = new CjTokenType("Bool");
  IElementType BOOL_LITERAL = new CjTokenType("BOOL_LITERAL");
  IElementType BREAK = new CjTokenType("break");
  IElementType BYTE_LITERAL = new CjTokenType("BYTE_LITERAL");
  IElementType BYTE_STRING_LITERAL = new CjTokenType("BYTE_STRING_LITERAL");
  IElementType CASE = new CjTokenType("case");
  IElementType CATCH = new CjTokenType("catch");
  IElementType CHAE = new CjTokenType("Char");
  IElementType CHAR_LITERAL = new CjTokenType("CHAR_LITERAL");
  IElementType CLASS = new CjTokenType("class");
  IElementType CONTINUE = new CjTokenType("continue");
  IElementType CSTRING_LITERAL = new CjTokenType("CSTRING_LITERAL");
  IElementType DO = new CjTokenType("do");
  IElementType DOT = new CjTokenType(".");
  IElementType DOTDOT = new CjTokenType("..");
  IElementType ELSE = new CjTokenType("else");
  IElementType ENUM = new CjTokenType("enum");
  IElementType EQ = new CjTokenType("=");
  IElementType EQEQ = new CjTokenType("==");
  IElementType EXTEND = new CjTokenType("extend");
  IElementType FALSE = new CjTokenType("false");
  IElementType FINALLY = new CjTokenType("finally");
  IElementType FLOAT32 = new CjTokenType("Float32");
  IElementType FLOAT64 = new CjTokenType("Float64");
  IElementType FLOAT_LITERAL = new CjTokenType("FLOAT_LITERAL");
  IElementType FOR = new CjTokenType("for");
  IElementType FROM = new CjTokenType("from");
  IElementType FUNC = new CjTokenType("func");
  IElementType GT = new CjTokenType(">");
  IElementType GTEQ = new CjTokenType(">=");
  IElementType GTGT = new CjTokenType(">>");
  IElementType IDENTIFIER = new CjTokenType("identifier");
  IElementType IF = new CjTokenType("if");
  IElementType IMPORT = new CjTokenType("import");
  IElementType IN = new CjTokenType("in");
  IElementType INIT = new CjTokenType("init");
  IElementType INT16 = new CjTokenType("Int16");
  IElementType INT32 = new CjTokenType("Int32");
  IElementType INT64 = new CjTokenType("Int64");
  IElementType INT8 = new CjTokenType("Int8");
  IElementType INTEGER_LITERAL = new CjTokenType("INTEGER_LITERAL");
  IElementType INTERFACE = new CjTokenType("interface");
  IElementType LBRACE = new CjTokenType("{");
  IElementType LBRACK = new CjTokenType("[");
  IElementType LET = new CjTokenType("let");
  IElementType LPAREN = new CjTokenType("(");
  IElementType LT = new CjTokenType("<");
  IElementType LTEQ = new CjTokenType("<=");
  IElementType LTLT = new CjTokenType("<<");
  IElementType MAIN = new CjTokenType("main");
  IElementType MATCH = new CjTokenType("match");
  IElementType MUT = new CjTokenType("mut");
  IElementType NOTEQ = new CjTokenType("!=");
  IElementType OPEN = new CjTokenType("open");
  IElementType OR = new CjTokenType("|");
  IElementType OROR = new CjTokenType("||");
  IElementType PROP = new CjTokenType("prop");
  IElementType QUOTE_IDENTIFIER = new CjTokenType("QUOTE_IDENTIFIER");
  IElementType RAW_BYTE_STRING_LITERAL = new CjTokenType("RAW_BYTE_STRING_LITERAL");
  IElementType RAW_CSTRING_LITERAL = new CjTokenType("RAW_CSTRING_LITERAL");
  IElementType RAW_STRING_LITERAL = new CjTokenType("RAW_STRING_LITERAL");
  IElementType RBRACE = new CjTokenType("}");
  IElementType RBRACK = new CjTokenType("]");
  IElementType RETURN = new CjTokenType("return");
  IElementType RPAREN = new CjTokenType(")");
  IElementType SHEBANG_LINE = new CjTokenType("shebang_line");
  IElementType STATIC = new CjTokenType("static");
  IElementType STRING_LITERAL = new CjTokenType("STRING_LITERAL");
  IElementType STRUCT = new CjTokenType("struct");
  IElementType SUPER = new CjTokenType("super");
  IElementType THIS = new CjTokenType("this");
  IElementType THROW = new CjTokenType("throw");
  IElementType TRUE = new CjTokenType("true");
  IElementType TRY = new CjTokenType("try");
  IElementType UINT16 = new CjTokenType("UInt16");
  IElementType UINT32 = new CjTokenType("UInt32");
  IElementType UINT64 = new CjTokenType("UInt64");
  IElementType UINT8 = new CjTokenType("UInt8");
  IElementType UNIT = new CjTokenType("Unit");
  IElementType UNSAFE = new CjTokenType("unsafe");
  IElementType VAR = new CjTokenType("var");
  IElementType WHILE = new CjTokenType("while");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == BLOCK) {
        return new CjBlockImpl(node);
      }
      else if (type == DEFAULT_PARAMETER_VALUE) {
        return new CjDefaultParameterValueImpl(node);
      }
      else if (type == FUNCTION) {
        return new CjFunctionImpl(node);
      }
      else if (type == LIFETIME) {
        return new CjLifetimeImpl(node);
      }
      else if (type == LIFETIME_PARAMETER) {
        return new CjLifetimeParameterImpl(node);
      }
      else if (type == MAIN_FUNC) {
        return new CjMainFuncImpl(node);
      }
      else if (type == VALUE_PARAMETER) {
        return new CjValueParameterImpl(node);
      }
      else if (type == VALUE_PARAMETER_LIST) {
        return new CjValueParameterListImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }

    public static CompositePsiElement createElement(IElementType type) {
       if (type == A_EXPR) {
        return new CjAExprImpl(type);
      }
      else if (type == ITEM) {
        return new CjItemImpl(type);
      }
      else if (type == LABEL_DECL) {
        return new CjLabelDeclImpl(type);
      }
      else if (type == LIFETIME_PARAM_BOUNDS) {
        return new CjLifetimeParamBoundsImpl(type);
      }
      else if (type == MAIN_FUNC_PARAM) {
        return new CjMainFuncParamImpl(type);
      }
      else if (type == PARAM_STMT) {
        return new CjParamStmtImpl(type);
      }
      else if (type == STATEMENT_CODE_FRAGMENT_ELEMENT) {
        return new CjStatementCodeFragmentElementImpl(type);
      }
      else if (type == STMT) {
        return new CjStmtImpl(type);
      }
      else if (type == TYPE_REFERENCE) {
        return new CjTypeReferenceImpl(type);
      }
      else if (type == TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT) {
        return new CjTypeReferenceCodeFragmentElementImpl(type);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
