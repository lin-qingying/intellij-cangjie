// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.huawei.cangjie.lang.core.psi.CjElementTypes.*;
import com.huawei.cangjie.lang.core.psi.ext.CjStubbedElementImpl;
import com.huawei.cangjie.lang.core.stubs.CjValueParameterStub;
import com.huawei.cangjie.lang.core.psi.*;
import com.intellij.psi.stubs.IStubElementType;

public class CjValueParameterImpl extends CjStubbedElementImpl<CjValueParameterStub> implements CjValueParameter {

  public CjValueParameterImpl(@NotNull ASTNode node) {
    super(node);
  }

  public CjValueParameterImpl(@NotNull CjValueParameterStub stub, @NotNull IStubElementType<?, ?> type) {
    super(stub, type);
  }

  public void accept(@NotNull CjVisitor visitor) {
    visitor.visitValueParameter(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CjVisitor) accept((CjVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CjDefaultParameterValue getDefaultParameterValue() {
    return PsiTreeUtil.getStubChildOfType(this, CjDefaultParameterValue.class);
  }

  @Override
  @Nullable
  public CjTypeReference getTypeReference() {
    return PsiTreeUtil.getChildOfType(this, CjTypeReference.class);
  }

}
