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
  @Nullable
  public CangJieType getType() {
    return PsiTreeUtil.getChildOfType(this, CangJieType.class);
  }

  @Override
  @Nullable
  public CangJieValueParameterList getValueParameterList() {
    return PsiTreeUtil.getStubChildOfType(this, CangJieValueParameterList.class);
  }

}
