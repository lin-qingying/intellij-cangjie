// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.huawei.cangjie.lang.core.psi.ext.CjElement;

public class CangJieVisitor extends PsiElementVisitor {

  public void visitBlock(@NotNull CangJieBlock o) {
    visitCjElement(o);
  }

  public void visitFunction(@NotNull CangJieFunction o) {
    visitCjElement(o);
  }

  public void visitReturnType(@NotNull CangJieReturnType o) {
    visitCjElement(o);
  }

  public void visitToBeUpped(@NotNull CangJieToBeUpped o) {
    visitCjElement(o);
  }

  public void visitCjElement(@NotNull CjElement o) {
    visitElement(o);
  }

}
