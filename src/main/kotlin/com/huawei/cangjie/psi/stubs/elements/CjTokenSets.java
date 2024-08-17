package com.huawei.cangjie.psi.stubs.elements;

import com.intellij.psi.tree.TokenSet;

import static com.huawei.cangjie.CjNodeTypes.BASIC_TYPE;
import static com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes.*;

public interface CjTokenSets {
    TokenSet SUPER_TYPE_LIST_ENTRIES = TokenSet.create(SUPER_TYPE_CALL_ENTRY, SUPER_TYPE_ENTRY);

    TokenSet DECLARATION_TYPES =
            TokenSet.create(CLASS);
    TokenSet INSIDE_DIRECTIVE_EXPRESSIONS = TokenSet.create(

//            IMPORT_DIRECTIVE_ITEM,
            REFERENCE_EXPRESSION, DOT_QUALIFIED_EXPRESSION);
    TokenSet TYPE_ELEMENT_TYPES = TokenSet.create(USER_TYPE, BASIC_TYPE);

}
