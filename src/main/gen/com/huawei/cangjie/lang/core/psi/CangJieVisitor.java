// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.huawei.cangjie.lang.core.psi.ext.CjElement;
import com.huawei.cangjie.lang.core.psi.ext.CjItemsOwner;

public class CangJieVisitor extends PsiElementVisitor {

  public void visitA(@NotNull CangJieA o) {
    visitCjElement(o);
  }

  public void visitBlock(@NotNull CangJieBlock o) {
    visitCjItemsOwner(o);
  }

  public void visitEol(@NotNull CangJieEol o) {
    visitCjElement(o);
  }

  public void visitExpr(@NotNull CangJieExpr o) {
    visitCjElement(o);
  }

  public void visitFuncParameters(@NotNull CangJieFuncParameters o) {
    visitCjElement(o);
  }

  public void visitFunction(@NotNull CangJieFunction o) {
    visitCjElement(o);
  }

  public void visitItem(@NotNull CangJieItem o) {
    visitCjElement(o);
  }

  public void visitMainFunc(@NotNull CangJieMainFunc o) {
    visitCjItemsOwner(o);
  }

  public void visitMainFuncCodeFragmentElement(@NotNull CangJieMainFuncCodeFragmentElement o) {
    visitCjElement(o);
  }

  public void visitMainFuncParam(@NotNull CangJieMainFuncParam o) {
    visitCjElement(o);
  }

  public void visitParamStmt(@NotNull CangJieParamStmt o) {
    visitCjElement(o);
  }

  public void visitStatementCodeFragmentElement(@NotNull CangJieStatementCodeFragmentElement o) {
    visitCjElement(o);
  }

  public void visitStmt(@NotNull CangJieStmt o) {
    visitCjElement(o);
  }

  public void visitType(@NotNull CangJieType o) {
    visitCjElement(o);
  }

  public void visitCjItemsOwner(@NotNull CjItemsOwner o) {
    visitElement(o);
  }

  public void visitCjElement(@NotNull CjElement o) {
    visitElement(o);
  }

}
