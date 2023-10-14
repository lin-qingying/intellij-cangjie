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
import com.huawei.cangjie.lang.core.stubs.CjPlaceholderStub;
import com.huawei.cangjie.lang.core.psi.*;
import com.intellij.psi.stubs.IStubElementType;

public class CjBlockImpl extends CjStubbedElementImpl<CjPlaceholderStub<?>> implements CjBlock {

  public CjBlockImpl(@NotNull ASTNode node) {
    super(node);
  }

  public CjBlockImpl(@NotNull CjPlaceholderStub<?> stub, @NotNull IStubElementType<?, ?> type) {
    super(stub, type);
  }

  public void accept(@NotNull CjVisitor visitor) {
    visitor.visitBlock(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CjVisitor) accept((CjVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<CjStmt> getStmtList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CjStmt.class);
  }

  @Override
  @NotNull
  public PsiElement getLbrace() {
    return notNullChild(findChildByType(LBRACE));
  }

  @Override
  @Nullable
  public PsiElement getRbrace() {
    return findChildByType(RBRACE);
  }

}
