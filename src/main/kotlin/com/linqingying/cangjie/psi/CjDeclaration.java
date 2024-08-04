package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.doc.psi.CDoc;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.Nullable;


public interface CjDeclaration extends CjExpression, CjModifierListOwner {
    CjDeclaration[] EMPTY_ARRAY = new CjDeclaration[0];

    ArrayFactory<CjDeclaration> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new CjDeclaration[count];

    @Nullable
    CDoc getDocComment();


    @Nullable
    CjExpression getExpression();


}
