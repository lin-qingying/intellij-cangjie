// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.huawei.cangjie.lang.core.psi.ext.CjElement;
import com.huawei.cangjie.lang.core.psi.ext.CjInferenceContextOwner;
import com.huawei.cangjie.lang.core.psi.ext.CjItemsOwner;
import com.huawei.cangjie.lang.core.psi.ext.CjNameIdentifierOwner;
import com.huawei.cangjie.lang.core.psi.ext.CjQualifiedNamedElement;
import com.huawei.cangjie.lang.core.psi.ext.CjMandatoryReferenceElement;
import com.huawei.cangjie.lang.core.psi.ext.CjGenericParameter;

public class CjVisitor extends PsiElementVisitor {

  public void visitAExpr(@NotNull CjAExpr o) {
    visitExpr(o);
  }

  public void visitBlock(@NotNull CjBlock o) {
    visitItemsOwner(o);
  }

  public void visitDefaultParameterValue(@NotNull CjDefaultParameterValue o) {
    visitInferenceContextOwner(o);
  }

  public void visitExpr(@NotNull CjExpr o) {
    visitElement(o);
  }

  public void visitFunction(@NotNull CjFunction o) {
    visitNameIdentifierOwner(o);
    // visitQualifiedNamedElement(o);
  }

  public void visitItem(@NotNull CjItem o) {
    visitElement(o);
  }

  public void visitLabelDecl(@NotNull CjLabelDecl o) {
    visitNameIdentifierOwner(o);
  }

  public void visitLifetime(@NotNull CjLifetime o) {
    visitNameIdentifierOwner(o);
    // visitMandatoryReferenceElement(o);
  }

  public void visitLifetimeParamBounds(@NotNull CjLifetimeParamBounds o) {
    visitElement(o);
  }

  public void visitLifetimeParameter(@NotNull CjLifetimeParameter o) {
    visitGenericParameter(o);
  }

  public void visitMainFunc(@NotNull CjMainFunc o) {
    visitItemsOwner(o);
  }

  public void visitMainFuncParam(@NotNull CjMainFuncParam o) {
    visitElement(o);
  }

  public void visitParamStmt(@NotNull CjParamStmt o) {
    visitElement(o);
  }

  public void visitStatementCodeFragmentElement(@NotNull CjStatementCodeFragmentElement o) {
    visitElement(o);
  }

  public void visitStmt(@NotNull CjStmt o) {
    visitElement(o);
  }

  public void visitTypeReference(@NotNull CjTypeReference o) {
    visitElement(o);
  }

  public void visitTypeReferenceCodeFragmentElement(@NotNull CjTypeReferenceCodeFragmentElement o) {
    visitElement(o);
  }

  public void visitValueParameter(@NotNull CjValueParameter o) {
    visitElement(o);
  }

  public void visitValueParameterList(@NotNull CjValueParameterList o) {
    visitElement(o);
  }

  public void visitGenericParameter(@NotNull CjGenericParameter o) {
    visitElement(o);
  }

  public void visitInferenceContextOwner(@NotNull CjInferenceContextOwner o) {
    visitElement(o);
  }

  public void visitItemsOwner(@NotNull CjItemsOwner o) {
    visitElement(o);
  }

  public void visitNameIdentifierOwner(@NotNull CjNameIdentifierOwner o) {
    visitElement(o);
  }

  public void visitElement(@NotNull CjElement o) {
    super.visitElement(o);
  }

}
