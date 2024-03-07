// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.grammar.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.huawei.cangjie.grammar.psi.CjElementTypes.*;
import com.huawei.cangjie.psi.CjElementImplStub;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.grammar.psi.*;
import com.intellij.psi.stubs.IStubElementType;

public class CjImportListImpl extends CjElementImplStub<CangJiePlaceHolderStub<?>> implements CjImportList {

  public CjImportListImpl(@NotNull CangJiePlaceHolderStub<?> stub, @NotNull IStubElementType type) {
    super(stub, type);
  }

  public CjImportListImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CjVisitor visitor) {
    visitor.visitImportList(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CjVisitor) accept((CjVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CjEnd getEnd() {
    return PsiTreeUtil.getChildOfType(this, CjEnd.class);
  }

  @Override
  @NotNull
  public List<CjImportAllOrSpecified> getImportAllOrSpecifiedList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CjImportAllOrSpecified.class);
  }

}
