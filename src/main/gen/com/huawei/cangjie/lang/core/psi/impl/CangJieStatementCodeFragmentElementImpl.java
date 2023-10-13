// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.huawei.cangjie.lang.core.psi.CjElementTypes.*;
import com.huawei.cangjie.lang.core.psi.ext.CjElementImpl;
import com.huawei.cangjie.lang.core.psi.*;

public class CangJieStatementCodeFragmentElementImpl extends CjElementImpl implements CangJieStatementCodeFragmentElement {

  public CangJieStatementCodeFragmentElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CangJieVisitor visitor) {
    visitor.visitStatementCodeFragmentElement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CangJieVisitor) accept((CangJieVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CangJieStmt getStmt() {
    return PsiTreeUtil.getChildOfType(this, CangJieStmt.class);
  }

}
