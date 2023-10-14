// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.huawei.cangjie.lang.core.psi.ext.CjElement;
import com.huawei.cangjie.lang.core.psi.ext.CjInferenceContextOwner;
import com.huawei.cangjie.lang.core.psi.ext.CjNameIdentifierOwner;
import com.huawei.cangjie.lang.core.psi.ext.CjItemsOwner;

public class CangJieVisitor extends PsiElementVisitor {

  public void visitAExpr(@NotNull CangJieAExpr o) {
    visitExpr(o);
  }

  public void visitBlock(@NotNull CangJieBlock o) {
    visitCjItemsOwner(o);
  }

  public void visitDefaultParameterValue(@NotNull CangJieDefaultParameterValue o) {
    visitCjInferenceContextOwner(o);
  }

  public void visitExpr(@NotNull CangJieExpr o) {
    visitCjElement(o);
  }

  public void visitFunction(@NotNull CangJieFunction o) {
    visitCjNameIdentifierOwner(o);
  }

  public void visitItem(@NotNull CangJieItem o) {
    visitCjElement(o);
  }

  public void visitMainFunc(@NotNull CangJieMainFunc o) {
    visitCjElement(o);
  }

  public void visitMainFuncParam(@NotNull CangJieMainFuncParam o) {
    visitCjElement(o);
  }

  public void visitNamed(@NotNull CangJieNamed o) {
    visitCjNameIdentifierOwner(o);
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

  public void visitTypeReferenceCodeFragmentElement(@NotNull CangJieTypeReferenceCodeFragmentElement o) {
    visitCjElement(o);
  }

  public void visitValueParameter(@NotNull CangJieValueParameter o) {
    visitCjElement(o);
  }

  public void visitValueParameterList(@NotNull CangJieValueParameterList o) {
    visitCjElement(o);
  }

  public void visitNamedtest(@NotNull CangJieNamedtest o) {
    visitCjElement(o);
  }

  public void visitCjInferenceContextOwner(@NotNull CjInferenceContextOwner o) {
    visitElement(o);
  }

  public void visitCjItemsOwner(@NotNull CjItemsOwner o) {
    visitElement(o);
  }

  public void visitCjNameIdentifierOwner(@NotNull CjNameIdentifierOwner o) {
    visitElement(o);
  }

  public void visitCjElement(@NotNull CjElement o) {
    visitElement(o);
  }

}
