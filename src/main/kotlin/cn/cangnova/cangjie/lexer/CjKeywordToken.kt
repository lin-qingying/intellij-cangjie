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

package cn.cangnova.cangjie.lexer;


import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CjKeywordToken extends CjSingleValueToken  {

    /**
     * 生成关键字(在所有可能的上下文中具有关键字含义的标识符)
     */
    @Deprecated
    public static CjKeywordToken keyword(String value) {
        return keyword(value, value);
    }

    public static CjKeywordToken keyword(String value, int tokenId) {
        return keyword(value, value, tokenId);
    }

    @Deprecated
    public static CjKeywordToken keyword(String debugName, String value) {
        return new CjKeywordToken(debugName, value, false);
    }

    public static CjKeywordToken keyword(String debugName, String value, int tokenId) {
        return new CjKeywordToken(debugName, value, false, tokenId);
    }


    @Deprecated
    public static CjKeywordToken softKeyword(String value) {
        return new CjKeywordToken(value, value, true);
    }

    public static CjKeywordToken softKeyword(String value, int tokenId) {
        return new CjKeywordToken(value, value, true, tokenId);
    }

    private final boolean myIsSoft;

    @Deprecated
    protected CjKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft) {
        super(debugName, value);
        myIsSoft = isSoft;
    }



    protected CjKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft, int tokenId) {
        super(debugName, value, tokenId);
        myIsSoft = isSoft;
    }

    public boolean isSoft() {
        return myIsSoft;
    }
}
