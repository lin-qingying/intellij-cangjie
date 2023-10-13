// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.huawei.cangjie.lang.core.psi.CjElementTypes.*;
import com.huawei.cangjie.lang.core.psi.*;
import com.intellij.psi.tree.IElementType;

public class CangJieAExprImpl extends CangJieExprImpl implements CangJieAExpr {

  public CangJieAExprImpl(@NotNull IElementType type) {
    super(type);
  }

  @Override
  public void accept(@NotNull CangJieVisitor visitor) {
    visitor.visitAExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CangJieVisitor) accept((CangJieVisitor)visitor);
    else super.accept(visitor);
  }

}
