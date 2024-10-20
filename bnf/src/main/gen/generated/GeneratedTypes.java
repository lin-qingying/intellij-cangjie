// This is a generated file. Not intended for manual editing.
package generated;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import generated.psi.impl.*;

public interface GeneratedTypes {

  IElementType MAIN_FUNC = new IElementType("MAIN_FUNC", null);
  IElementType VALUE_PARAMETER = new IElementType("VALUE_PARAMETER", null);
  IElementType VALUE_PARAMETER_LIST = new IElementType("VALUE_PARAMETER_LIST", null);

  IElementType COMMA = new IElementType("COMMA", null);
  IElementType IDENTIFIER = new IElementType("IDENTIFIER", null);
  IElementType LPAR = new IElementType("LPAR", null);
  IElementType MAIN_KEYWORD = new IElementType("MAIN_KEYWORD", null);
  IElementType RPAR = new IElementType("RPAR", null);

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == MAIN_FUNC) {
        return new MainFuncImpl(node);
      }
      else if (type == VALUE_PARAMETER) {
        return new ValueParameterImpl(node);
      }
      else if (type == VALUE_PARAMETER_LIST) {
        return new ValueParameterListImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
