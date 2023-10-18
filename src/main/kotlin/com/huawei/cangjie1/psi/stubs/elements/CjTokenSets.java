package com.huawei.cangjie1.psi.stubs.elements;

import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes.*;

public interface  CjTokenSets {

     TokenSet DECLARATION_TYPES =
             TokenSet.create(CLASS );
     TokenSet INSIDE_DIRECTIVE_EXPRESSIONS = TokenSet.create(REFERENCE_EXPRESSION, DOT_QUALIFIED_EXPRESSION);
     TokenSet TYPE_ELEMENT_TYPES = TokenSet.create(USER_TYPE);

}
