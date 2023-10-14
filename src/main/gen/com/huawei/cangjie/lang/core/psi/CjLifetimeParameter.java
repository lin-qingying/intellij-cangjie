// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.huawei.cangjie.lang.core.psi.ext.CjGenericParameter;
import com.intellij.psi.StubBasedPsiElement;
import com.huawei.cangjie.lang.core.stubs.CjLifetimeParameterStub;

public interface CjLifetimeParameter extends CjGenericParameter, StubBasedPsiElement<CjLifetimeParameterStub> {

  @Nullable
  CjLifetimeParamBounds getLifetimeParamBounds();

  @NotNull
  PsiElement getQuoteIdentifier();

}
