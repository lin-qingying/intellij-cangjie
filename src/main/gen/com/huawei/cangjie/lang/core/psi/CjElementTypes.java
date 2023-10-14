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
  IElementType MAIN_FUNC = new CjElementType("MAIN_FUNC");
  IElementType MAIN_FUNC_PARAM = new CjElementType("MAIN_FUNC_PARAM");
  IElementType NAMED = new CjElementType("NAMED");
  IElementType NAMEDTEST = new CjElementType("NAMEDTEST");
  IElementType PARAM_STMT = new CjElementType("PARAM_STMT");
  IElementType STATEMENT_CODE_FRAGMENT_ELEMENT = new CjElementType("STATEMENT_CODE_FRAGMENT_ELEMENT");
  IElementType STMT = new CjElementType("STMT");
  IElementType TYPE = new CjElementType("TYPE");
  IElementType TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT = new CjElementType("TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT");
  IElementType VALUE_PARAMETER = StubImplementationsKt.factory("VALUE_PARAMETER");
  IElementType VALUE_PARAMETER_LIST = StubImplementationsKt.factory("VALUE_PARAMETER_LIST");

  IElementType AND = new CjTokenType("&");
  IElementType ANDAND = new CjTokenType("&&");
  IElementType AS = new CjTokenType("as");
  IElementType BOOL = new CjTokenType("Bool");
  IElementType BREAK = new CjTokenType("break");
  IElementType CASE = new CjTokenType("case");
  IElementType CATCH = new CjTokenType("catch");
  IElementType CHAE = new CjTokenType("Char");
  IElementType CLASS = new CjTokenType("class");
  IElementType CONTINUE = new CjTokenType("continue");
  IElementType DO = new CjTokenType("do");
  IElementType ELSE = new CjTokenType("else");
  IElementType ENUM = new CjTokenType("enum");
  IElementType EQ = new CjTokenType("=");
  IElementType EQEQ = new CjTokenType("==");
  IElementType EXTEND = new CjTokenType("extend");
  IElementType FALSE = new CjTokenType("false");
  IElementType FINALLY = new CjTokenType("finally");
  IElementType FLOAT32 = new CjTokenType("Float32");
  IElementType FLOAT64 = new CjTokenType("Float64");
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
  IElementType RBRACE = new CjTokenType("}");
  IElementType RBRACK = new CjTokenType("]");
  IElementType RETURN = new CjTokenType("return");
  IElementType RPAREN = new CjTokenType(")");
  IElementType STATIC = new CjTokenType("static");
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
        return new CangJieBlockImpl(node);
      }
      else if (type == DEFAULT_PARAMETER_VALUE) {
        return new CangJieDefaultParameterValueImpl(node);
      }
      else if (type == FUNCTION) {
        return new CangJieFunctionImpl(node);
      }
      else if (type == VALUE_PARAMETER) {
        return new CangJieValueParameterImpl(node);
      }
      else if (type == VALUE_PARAMETER_LIST) {
        return new CangJieValueParameterListImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }

    public static CompositePsiElement createElement(IElementType type) {
       if (type == A_EXPR) {
        return new CangJieAExprImpl(type);
      }
      else if (type == ITEM) {
        return new CangJieItemImpl(type);
      }
      else if (type == MAIN_FUNC) {
        return new CangJieMainFuncImpl(type);
      }
      else if (type == MAIN_FUNC_PARAM) {
        return new CangJieMainFuncParamImpl(type);
      }
      else if (type == NAMED) {
        return new CangJieNamedImpl(type);
      }
      else if (type == NAMEDTEST) {
        return new CangJieNamedtestImpl(type);
      }
      else if (type == PARAM_STMT) {
        return new CangJieParamStmtImpl(type);
      }
      else if (type == STATEMENT_CODE_FRAGMENT_ELEMENT) {
        return new CangJieStatementCodeFragmentElementImpl(type);
      }
      else if (type == STMT) {
        return new CangJieStmtImpl(type);
      }
      else if (type == TYPE) {
        return new CangJieTypeImpl(type);
      }
      else if (type == TYPE_REFERENCE_CODE_FRAGMENT_ELEMENT) {
        return new CangJieTypeReferenceCodeFragmentElementImpl(type);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
