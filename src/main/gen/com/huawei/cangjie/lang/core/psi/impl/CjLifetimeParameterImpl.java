// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.huawei.cangjie.lang.core.psi.CjElementTypes.*;
import com.huawei.cangjie.lang.core.psi.ext.CjLifetimeParameterImplMixin;
import com.huawei.cangjie.lang.core.psi.*;
import com.huawei.cangjie.lang.core.stubs.CjLifetimeParameterStub;
import com.intellij.psi.stubs.IStubElementType;

public class CjLifetimeParameterImpl extends CjLifetimeParameterImplMixin implements CjLifetimeParameter {

  public CjLifetimeParameterImpl(@NotNull ASTNode node) {
    super(node);
  }

  public CjLifetimeParameterImpl(@NotNull CjLifetimeParameterStub stub, @NotNull IStubElementType<?, ?> type) {
    super(stub, type);
  }

  public void accept(@NotNull CjVisitor visitor) {
    visitor.visitLifetimeParameter(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CjVisitor) accept((CjVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CjLifetimeParamBounds getLifetimeParamBounds() {
    return PsiTreeUtil.getChildOfType(this, CjLifetimeParamBounds.class);
  }

  @Override
  @NotNull
  public PsiElement getQuoteIdentifier() {
    return notNullChild(findChildByType(QUOTE_IDENTIFIER));
  }

}
