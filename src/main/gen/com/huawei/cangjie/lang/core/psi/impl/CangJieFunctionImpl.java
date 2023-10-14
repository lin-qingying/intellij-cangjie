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

public class CangJieFunctionImpl extends CjFunctionImplMixin implements CangJieFunction {

  public CangJieFunctionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public CangJieFunctionImpl(@NotNull CjFunctionStub stub, @NotNull IStubElementType<?, ?> type) {
    super(stub, type);
  }

  public void accept(@NotNull CangJieVisitor visitor) {
    visitor.visitFunction(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CangJieVisitor) accept((CangJieVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public CangJieNamed getNamed() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, CangJieNamed.class));
  }

  @Override
  @Nullable
  public CangJieType getType() {
    return PsiTreeUtil.getChildOfType(this, CangJieType.class);
  }

  @Override
  @NotNull
  public CangJieValueParameterList getValueParameterList() {
    return notNullChild(PsiTreeUtil.getStubChildOfType(this, CangJieValueParameterList.class));
  }

  @Override
  @NotNull
  public PsiElement getFunc() {
    return notNullChild(findChildByType(FUNC));
  }

  @Override
  @Nullable
  public PsiElement getUnsafe() {
    return findChildByType(UNSAFE);
  }

}
