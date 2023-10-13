// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.huawei.cangjie.lang.core.stubs.StubImplementationsKt;
import com.huawei.cangjie.lang.core.psi.impl.*;

public interface CjElementTypes {

  IElementType A = new CjTokenType("A");
  IElementType BLOCK = StubImplementationsKt.factory("BLOCK");
  IElementType EOL = new CjTokenType("EOL");
  IElementType EXPR = new CjTokenType("EXPR");
  IElementType FUNCTION = new CjTokenType("FUNCTION");
  IElementType FUNC_PARAMETERS = new CjTokenType("FUNC_PARAMETERS");
  IElementType ITEM = new CjTokenType("ITEM");
  IElementType MAIN_FUNC = StubImplementationsKt.factory("MAIN_FUNC");
  IElementType MAIN_FUNC_CODE_FRAGMENT_ELEMENT = new CjTokenType("MAIN_FUNC_CODE_FRAGMENT_ELEMENT");
  IElementType MAIN_FUNC_PARAM = new CjTokenType("MAIN_FUNC_PARAM");
  IElementType PARAM_STMT = new CjTokenType("PARAM_STMT");
  IElementType STATEMENT_CODE_FRAGMENT_ELEMENT = new CjTokenType("STATEMENT_CODE_FRAGMENT_ELEMENT");
  IElementType STMT = new CjTokenType("STMT");
  IElementType TYPE = new CjTokenType("TYPE");

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
      if (type == A) {
        return new CangJieAImpl(node);
      }
      else if (type == BLOCK) {
        return new CangJieBlockImpl(node);
      }
      else if (type == EOL) {
        return new CangJieEolImpl(node);
      }
      else if (type == EXPR) {
        return new CangJieExprImpl(node);
      }
      else if (type == FUNCTION) {
        return new CangJieFunctionImpl(node);
      }
      else if (type == FUNC_PARAMETERS) {
        return new CangJieFuncParametersImpl(node);
      }
      else if (type == ITEM) {
        return new CangJieItemImpl(node);
      }
      else if (type == MAIN_FUNC) {
        return new CangJieMainFuncImpl(node);
      }
      else if (type == MAIN_FUNC_CODE_FRAGMENT_ELEMENT) {
        return new CangJieMainFuncCodeFragmentElementImpl(node);
      }
      else if (type == MAIN_FUNC_PARAM) {
        return new CangJieMainFuncParamImpl(node);
      }
      else if (type == PARAM_STMT) {
        return new CangJieParamStmtImpl(node);
      }
      else if (type == STATEMENT_CODE_FRAGMENT_ELEMENT) {
        return new CangJieStatementCodeFragmentElementImpl(node);
      }
      else if (type == STMT) {
        return new CangJieStmtImpl(node);
      }
      else if (type == TYPE) {
        return new CangJieTypeImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
