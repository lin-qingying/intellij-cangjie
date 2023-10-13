// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.huawei.cangjie.lang.core.psi.ext.CjItemsOwner;
import com.intellij.psi.StubBasedPsiElement;
import com.huawei.cangjie.lang.core.stubs.CjPlaceholderStub;

public interface CangJieMainFunc extends CjItemsOwner, StubBasedPsiElement<CjPlaceholderStub<?>> {

  @Nullable
  CangJieMainFuncParam getMainFuncParam();

  @Nullable
  CangJieType getType();

  @Nullable
  PsiElement getLparen();

  @NotNull
  PsiElement getMain();

  @Nullable
  PsiElement getRparen();

}
