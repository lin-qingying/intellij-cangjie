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

public class CangJieValueParameterImpl extends CjStubbedElementImpl<CjValueParameterStub> implements CangJieValueParameter {

  public CangJieValueParameterImpl(@NotNull ASTNode node) {
    super(node);
  }

  public CangJieValueParameterImpl(@NotNull CjValueParameterStub stub, @NotNull IStubElementType<?, ?> type) {
    super(stub, type);
  }

  public void accept(@NotNull CangJieVisitor visitor) {
    visitor.visitValueParameter(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CangJieVisitor) accept((CangJieVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CangJieDefaultParameterValue getDefaultParameterValue() {
    return PsiTreeUtil.getStubChildOfType(this, CangJieDefaultParameterValue.class);
  }

  @Override
  @Nullable
  public CangJieType getType() {
    return PsiTreeUtil.getChildOfType(this, CangJieType.class);
  }

}
