// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.huawei.cangjie.lang.core.psi.CjElementTypes.*;
import com.huawei.cangjie.lang.core.psi.ext.CjFunctionImplMixin;
import com.huawei.cangjie.lang.core.psi.*;
import com.huawei.cangjie.lang.core.stubs.CjFunctionStub;
import com.intellij.psi.stubs.IStubElementType;

public class CjFunctionImpl extends CjFunctionImplMixin implements CjFunction {

  public CjFunctionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public CjFunctionImpl(@NotNull CjFunctionStub stub, @NotNull IStubElementType<?, ?> type) {
    super(stub, type);
  }

  public void accept(@NotNull CjVisitor visitor) {
    visitor.visitFunction(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CjVisitor) accept((CjVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CjTypeReference getTypeReference() {
    return PsiTreeUtil.getChildOfType(this, CjTypeReference.class);
  }

  @Override
  @Nullable
  public CjValueParameterList getValueParameterList() {
    return PsiTreeUtil.getStubChildOfType(this, CjValueParameterList.class);
  }

  @Override
  @NotNull
  public PsiElement getFunc() {
    return notNullChild(findChildByType(FUNC));
  }

  @Override
  @NotNull
  public PsiElement getIdentifier() {
    return notNullChild(findChildByType(IDENTIFIER));
  }

  @Override
  @Nullable
  public PsiElement getUnsafe() {
    return findChildByType(UNSAFE);
  }

}
