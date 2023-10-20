package com.huawei.cangjie.psi;

import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CjTypeElement extends CjElement {


    CjTypeElement[] EMPTY_ARRAY = new CjTypeElement[0];

    ArrayFactory<CjTypeElement> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new CjTypeElement[count];

    // may contain null
    @NotNull
    List<CjTypeReference> getTypeArgumentsAsTypes();
}
