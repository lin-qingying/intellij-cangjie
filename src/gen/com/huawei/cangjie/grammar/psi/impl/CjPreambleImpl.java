// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.grammar.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.huawei.cangjie.grammar.psi.CjElementTypes.*;
import com.huawei.cangjie.psi.CjElementImpl;
import com.huawei.cangjie.grammar.psi.*;

public class CjPreambleImpl extends CjElementImpl implements CjPreamble {

  public CjPreambleImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CjVisitor visitor) {
    visitor.visitPreamble(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CjVisitor) accept((CjVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<CjImportList> getImportListList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CjImportList.class);
  }

  @Override
  @Nullable
  public CjPackageDeclaration getPackageDeclaration() {
    return PsiTreeUtil.getChildOfType(this, CjPackageDeclaration.class);
  }

}
