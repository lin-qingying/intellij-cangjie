/*
 * Copyright 2025 LinQingYing. and contributors.
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
package cn.cangnova.cangjie.lexer

import org.jetbrains.annotations.NonNls


open class CjKeywordToken : CjSingleValueToken {
    val isSoft: Boolean

    @Deprecated("")
    protected constructor(debugName: @NonNls String, value: @NonNls String, isSoft: Boolean) : super(debugName, value) {
        this.isSoft = isSoft
    }


    protected constructor(debugName: @NonNls String, value: @NonNls String, isSoft: Boolean, tokenId: Int) : super(
        debugName,
        value,
        tokenId
    ) {
        this.isSoft = isSoft
    }

    companion object {
        /**
         * 生成关键字(在所有可能的上下文中具有关键字含义的标识符)
         */
        @Deprecated("")
        fun keyword(value: String): CjKeywordToken {
            return keyword(value, value)
        }

        @JvmStatic
        fun keyword(value: String, tokenId: Int): CjKeywordToken {
            return keyword(value, value, tokenId)
        }

        @Deprecated("")
        fun keyword(debugName: String, value: String): CjKeywordToken {
            return CjKeywordToken(debugName, value, false)
        }

        fun keyword(debugName: String, value: String, tokenId: Int): CjKeywordToken {
            return CjKeywordToken(debugName, value, false, tokenId)
        }


        @Deprecated("")
        fun softKeyword(value: String): CjKeywordToken {
            return CjKeywordToken(value, value, true)
        }

        @JvmStatic
        fun softKeyword(value: String, tokenId: Int): CjKeywordToken {
            return CjKeywordToken(value, value, true, tokenId)
        }
    }
}
