// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.lang.core.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.huawei.cangjie.lang.core.psi.ext.CjNameIdentifierOwner;
import com.huawei.cangjie.lang.core.psi.ext.CjQualifiedNamedElement;
import com.intellij.psi.StubBasedPsiElement;
import com.huawei.cangjie.lang.core.stubs.CjFunctionStub;

public interface CjFunction extends CjNameIdentifierOwner, CjQualifiedNamedElement, StubBasedPsiElement<CjFunctionStub> {

  @Nullable
  CjTypeReference getTypeReference();

  @Nullable
  CjValueParameterList getValueParameterList();

  @NotNull
  PsiElement getFunc();

  @NotNull
  PsiElement getIdentifier();

  @Nullable
  PsiElement getUnsafe();

}
