// This is a generated file. Not intended for manual editing.
package com.huawei.cangjie.grammar.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.huawei.cangjie.psi.CjElement;
import com.intellij.psi.StubBasedPsiElement;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;

public interface CjImportList extends CjElement, StubBasedPsiElement<CangJiePlaceHolderStub<?>> {

  @Nullable
  CjEnd getEnd();

  @NotNull
  List<CjImportAllOrSpecified> getImportAllOrSpecifiedList();

}
