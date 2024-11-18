/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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
    TokenSet TYPE_ELEMENT_TYPES = TokenSet.create( THIS_TYPE,VARRAY_TYPE, USER_TYPE, BASIC_TYPE,TUPLE_TYPE, FUNCTION_TYPE, OPTIONAL_TYPE, PARENTHESIZED_TYPE);

}
