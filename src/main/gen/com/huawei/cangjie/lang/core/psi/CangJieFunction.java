// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.huawei.cangjie.lang.core.psi.ext.CjNameIdentifierOwner;
import com.intellij.psi.StubBasedPsiElement;
import com.huawei.cangjie.lang.core.stubs.CjFunctionStub;

public interface CangJieFunction extends CjNameIdentifierOwner, StubBasedPsiElement<CjFunctionStub> {

  @NotNull
  CangJieNamed getNamed();

  @Nullable
  CangJieType getType();

  @NotNull
  CangJieValueParameterList getValueParameterList();

  @NotNull
  PsiElement getFunc();

  @Nullable
  PsiElement getUnsafe();

}
