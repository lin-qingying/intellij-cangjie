package com.linqingying.cangjie.psi.stubs.elements;

import com.intellij.psi.tree.TokenSet;

import static com.linqingying.cangjie.CjNodeTypes.BASIC_TYPE;
import static com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes.*;

public interface CjTokenSets {
    TokenSet SUPER_TYPE_LIST_ENTRIES = TokenSet.create(SUPER_TYPE_CALL_ENTRY, SUPER_TYPE_ENTRY);

    TokenSet DECLARATION_TYPES =
            TokenSet.create(CLASS,STRUCT,ENUM,EXTEND,INTERFACE);
    TokenSet INSIDE_DIRECTIVE_EXPRESSIONS = TokenSet.create(

//            IMPORT_DIRECTIVE_ITEM,
            DOT_QUALIFIED_EXPRESSION,
            REFERENCE_EXPRESSION);
    TokenSet TYPE_ELEMENT_TYPES = TokenSet.create( THIS_TYPE, USER_TYPE, BASIC_TYPE,TUPLE_TYPE, FUNCTION_TYPE, OPTIONAL_TYPE, PARENTHESIZED_TYPE);

}
